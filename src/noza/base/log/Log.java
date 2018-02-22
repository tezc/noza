package noza.base.log;

import noza.api.Callbacks;
import noza.base.common.Util;
import noza.base.config.Config;
import noza.base.config.Configs;
import noza.core.LogEvent;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;

public class Log
{
    private static final int LINE_LEN = 120;
    private static final DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("dd.MM.yyyy-hh.mm.ssa");

    private String filePath;
    private int maxFileSize;
    private int maxFileCount;
    private int currentFileSize;

    private ArrayDeque<String> files;

    private Callbacks callbacks;
    private BufferedWriter writer;
    private int fileVersion;
    private Level logLevel;
    private int bufferSize;

    private LogWriter logWriter;

    private int index;
    private boolean newLine;
    private boolean logCallback;

    public Log(LogWriter logWriter)
    {
        this.logWriter   = logWriter;
        this.fileVersion = 100;
        this.index       = 0;
        this.newLine     = false;

    }

    public Level getLevel()
    {
        return logLevel;
    }

    public void init(Configs configs, Callbacks callbacks)
    {
        this.callbacks   = callbacks;

        logCallback  = configs.get(Config.BROKER_LOG_CALLBACK);
        filePath     = configs.get(Config.BROKER_LOG_FILEPATH);
        maxFileSize  = configs.get(Config.BROKER_LOG_FILESIZE);
        maxFileCount = configs.get(Config.BROKER_LOG_FILECOUNT);
        bufferSize   = configs.get(Config.BROKER_LOG_BUFFERSIZE);
        logLevel     = Level.valueOf(configs.get(Config.BROKER_LOG_LEVEL));

        if (logLevel == Level.OFF || logCallback) {
            return;
        }

        files = new ArrayDeque<>(maxFileCount);

        Path path = Paths.get(filePath);
        try {
            Files.createDirectories(path);
            Files.walk(path).
                  map(Path::toFile).
                  forEach(a -> {
                    if (a.getName().endsWith(".txt")) {
                        a.delete();
                    }
                 });
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        createLogFile();

        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    public void write(LogEvent event)
    {
        try {
            write(event.timestamp,
                  event.level, event.owner, event.t, event.args);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void trace(String timestamp, String owner, Throwable t, Object ...args)
    {
        log(Level.TRACE, timestamp, owner, t, args);
    }

    void debug(String timestamp, String owner, Throwable t, Object ...args)
    {
        log(Level.DEBUG, timestamp, owner, t, args);
    }

    void info(String timestamp, String owner, Throwable t, Object ...args)
    {
        log(Level.INFO, timestamp, owner, t, args);
    }

    void brief(String timestamp, String owner, Throwable t, Object ...args)
    {
        log(Level.BRIEF, timestamp, owner, t, args);
    }

    void warn(String timestamp, String owner, Throwable t, Object ...args)
    {
        log(Level.WARN, timestamp, owner, t, args);
    }

    void error(String timestamp, String owner, Throwable t, Object ...args)
    {
        log(Level.ERROR, timestamp, owner, t, args);
    }

    void fatal(String timestamp, String owner, Throwable t, Object ...args)
    {
        log(Level.FATAL, timestamp, owner, t, args);
    }

    private void close()
    {
        if (writer != null) {
            try {
                writer.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void writeHeaders(String timestamp,
                              Level level, String owner) throws IOException
    {
        writer.write(timestamp);
        writer.write(' ');
        writer.write(level.printable);
        writer.write(' ');
        writer.write(owner);
        writer.write(' ');

        System.out.print(timestamp + ' ' + level.printable + ' ' + owner + ' ');

        currentFileSize += (timestamp.length() +
                           level.printable.length() + owner.length() + 3);
    }

    private void write(String timestamp, Level level,
                       String owner, Throwable t, Object... args) throws IOException
    {

        if (logCallback) {
            StringBuilder builder = new StringBuilder(1024);

            if (t != null) {
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                builder.append(Util.newLine());
                builder.append(sw.toString());
            }

            for (Object arg : args) {
                builder.append(arg.toString());
                builder.append(" ");
            }

            callbacks.onLog(owner, level.toString(), builder.toString());
        }
        else {
            index = 0;
            newLine = false;

            writeHeaders(timestamp, level, owner);

            for (Object arg : args) {
                processElem(timestamp, level, owner, arg.toString());
            }

            if (!newLine) {
                writer.write(Util.newLine());
                System.out.print(Util.newLine());
            }

            if (t != null) {
                index = 0;
                newLine = false;
                writeHeaders(timestamp, level, owner);
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));

                processElem(timestamp, level, owner, sw.toString());
            }

            if (currentFileSize > maxFileSize) {
                createLogFile();
            }

            writer.flush();
        }
    }

    private void processElem(String timestamp,
                             Level level, String owner, String log) throws IOException
    {
        int argIndex = 0;

        while (argIndex < log.length()) {
            if (index >= LINE_LEN) {
                writer.write(Util.newLine());
                System.out.print(Util.newLine());
                writeHeaders(timestamp, level, owner);

                index = 0;
            }
            else if (newLine) {
                writeHeaders(timestamp, level, owner);
                index = 0;
                newLine = false;
            }

            int pos = log.indexOf(Util.newLine(), argIndex);
            boolean lf = false;
            if (pos == -1) {
                pos = log.indexOf('\n', argIndex);
                if (pos != -1) {
                    lf = true;
                }
            }
            if (pos == -1 || index + pos - argIndex > LINE_LEN) {
                pos = index + argIndex + LINE_LEN;
            }
            else {
                pos += (lf ? 0 : Util.newLine().length());
                newLine = true;
            }

            int writeLen = Math.min(pos - argIndex, log.length() - argIndex);
            writer.write(log, argIndex, writeLen);
            if (lf && newLine) {
                writer.write(Util.newLine());
                pos += 1;

            }
            System.out.print(log.substring(argIndex, argIndex + writeLen));

            argIndex = pos;
            index += writeLen;
            currentFileSize += writeLen;
        }
    }

    private void createLogFile()
    {
        if (writer != null) {
            close();
        }

        String logFile = filePath + "log." +
                         formatter.format(LocalDateTime.now()) + "." +
                         fileVersion++  + ".txt";

        if (files.size() == maxFileCount) {
            try {
                Files.delete(Paths.get(files.pop()));
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        files.add(logFile);

        try {
            writer = new BufferedWriter(new FileWriter(new File(logFile)), bufferSize);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        currentFileSize = 0;
    }


    private void log(Level level,
                     String timestamp, String owner, Throwable t, Object ...args)
    {
        if (logLevel.value > level.value) {
            return;
        }

        logWriter.addEvent(new LogEvent(level, timestamp, owner, t, args));
    }
}
