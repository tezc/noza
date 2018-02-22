package noza.core.worker;

import noza.base.config.Configs;
import noza.base.poller.Event;
import noza.base.poller.PollOwner;
import noza.core.Core;
import noza.core.client.Client;
import noza.core.msg.PublishMsg;
import noza.base.config.Config;
import noza.base.transport.sock.Sock;
import noza.base.log.Log;
import noza.base.log.LogOwner;
import noza.base.poller.Poll;
import noza.base.poller.Timer;
import noza.core.BufferPool;
import noza.core.LogEvent;

import java.nio.ByteBuffer;
import java.util.*;


public class Worker implements Runnable, LogOwner, BufferPool, PollOwner
{
    private String name;
    private Thread thread;
    private int id;
    private int largeBufferSize;
    protected Poll poll;
    protected Core core;

    private ByteBuffer tempBuf;

    private ArrayDeque<ByteBuffer> bufs;
    private ArrayDeque<ByteBuffer> directBufs;
    private ArrayDeque<ByteBuffer> largeBufs;
    private ArrayDeque<ByteBuffer> largeDirectBufs;

    public Worker(String name, int id, Core core, Configs configs)
    {
        this.id              = id;
        this.name            = name;
        this.core            = core;
        this.thread          = new Thread(this, name);
        this.poll            = new Poll(this);
        this.tempBuf         = ByteBuffer.allocateDirect((1024 * 256) - 16);
        this.bufs            = new ArrayDeque<>();
        this.largeBufs       = new ArrayDeque<>();
        this.directBufs      = new ArrayDeque<>();
        this.largeDirectBufs = new ArrayDeque<>();

        this.largeBufferSize = configs.get(Config.BROKER_LARGE_BUFSIZE);
    }

    public int getId()
    {
        return id;
    }

    public Configs getConfig()
    {
        return core.getConfigs();
    }

    @Override
    public String toString()
    {
        return name;
    }

    @Override
    public ByteBuffer allocBuf()
    {
        ByteBuffer buf = bufs.poll();
        if (buf == null) {
            buf = ByteBuffer.allocate(4080);
        }

        return buf;
    }

    @Override
    public void freeBuf(ByteBuffer buf)
    {
        buf.clear();
        bufs.push(buf);
    }

    @Override
    public ByteBuffer allocLargeBuf()
    {
        ByteBuffer buf = largeBufs.poll();
        if (buf == null) {
            buf = ByteBuffer.allocate(largeBufferSize);
        }

        return buf;
    }

    @Override
    public void freeLargeBuf(ByteBuffer buf)
    {
        buf.clear();
        largeBufs.push(buf);
    }

    @Override
    public ByteBuffer allocDirectBuf()
    {
        ByteBuffer buf = directBufs.poll();
        if (buf == null) {
            buf = ByteBuffer.allocate(4080);
        }

        return buf;
    }

    @Override
    public void freeDirectBuf(ByteBuffer buf)
    {
        buf.clear();
        directBufs.push(buf);
    }

    @Override
    public ByteBuffer allocLargeDirectBuf()
    {
        ByteBuffer buf = largeDirectBufs.poll();
        if (buf == null) {
            buf = ByteBuffer.allocate(largeBufferSize);
        }

        return buf;
    }

    @Override
    public void freeLargeDirectBuf(ByteBuffer buf)
    {
        buf.clear();
        largeDirectBufs.push(buf);
    }

    @Override
    public ByteBuffer getTempBuf()
    {
        return tempBuf;
    }

    public void start()
    {
        logInfo(toString() + " starting..");
        thread.start();
    }

    public long timestamp()
    {
        return poll.getTimestamp();
    }

    public void removeTimer(Timer timer)
    {
        poll.removeTimer(timer);
    }

    public void addEvent(Event event)
    {
        poll.addEvent(event);
    }

    @Override
    public void run()
    {
        while (true) {
            try {
                poll.loop();
            }
            catch (Exception e) {
                logError(e, "Trying to survive.. ");
            }
        }
    }

    @Override
    public Log getLogger()
    {
        return core.getLogger();
    }

    @Override
    public String getTimestampStr()
    {
        return poll.getTimestampStr();
    }

    @Override
    public String getName()
    {
        return name;
    }

    public void addTimer(Timer timer)
    {
        poll.addTimer(timer);
    }

    public void register(Sock sock, int ops)
    {
        poll.add(sock, ops);
    }



    public void connectedClientEvent(Client client, boolean sessionPresent)
    {
        throw new UnsupportedOperationException();
    }

    public void publishEvent(PublishMsg msg, List<String> clientList)
    {
        throw new UnsupportedOperationException();
    }

    public void handleLogEvent(LogEvent event)
    {
        throw new UnsupportedOperationException();
    }


}
