package noza.base.poller;

import noza.base.common.Util;
import noza.core.worker.Worker;
import noza.base.transport.sock.Sock;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Poller
{
    private static final DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss.SSS");

    private final Worker worker;
    private final Selector selector;

    private final AtomicBoolean awake;
    private boolean batchInQueue;

    private final PriorityQueue<noza.base.poller.Timer> timers;

    private long timestamp;
    private String timestampStr;
    private final EventQueue queue;


    public Poller(Worker worker)
    {
        try {
            this.selector = Selector.open();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.worker = worker;
        this.awake = new AtomicBoolean(false);
        this.queue = new EventQueue();
        this.timers = new PriorityQueue<>(20, new noza.base.poller.Timer.Compare());

        updateTimestamp();
    }

    public Selector getSelector()
    {
        return selector;
    }

    private void updateTimestamp()
    {
        timestamp = Util.time();
        timestampStr = '[' + formatter.format(LocalDateTime.now()) + ']';
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public String getTimestampStr()
    {
        return timestampStr;
    }

    public void add(Sock sock, int ops)
    {
        sock.register(selector, ops);
    }

    public void addEvent(Event event)
    {
        queue.addEvent(event);
        if (!awake.get()) {
            selector.wakeup();
        }
    }

    private void processSelect(Set<SelectionKey> readyKeys) throws IOException
    {
        for (SelectionKey key : readyKeys) {
            if (!key.isValid()) {
                continue;
            }

            try {
                Fd fdEvent = (Fd) key.attachment();

                if (key.isValid() && key.isAcceptable()) {
                    fdEvent.onAccept();
                }

                if (key.isValid() && key.isReadable()) {
                    fdEvent.onRead();
                }

                if (key.isValid() && key.isWritable()) {
                    fdEvent.onWrite();
                }
            }
            catch (Exception e) {
                worker.logError(e, "Trying to survive ");
            }
        }

        readyKeys.clear();
    }

    private void processEvents()
    {
        try {
            List<Event> events = queue.getEventList();
            for (Event event : events) {
                event.onEvent(worker);
            }

            worker.eventsDrained();
            events.clear();
        }
        catch (Exception e) {
            worker.logError(e, "Trying to survive ");
        }
    }

    public void loop() throws IOException
    {
        long timeout = 0;

        while (true) {

            selector.select(timeout);
            awake.set(true);

            while (true) {
                updateTimestamp();

                processSelect(selector.selectedKeys());
                processEvents();
                timeout = executeTimers();

                awake.set(false);

                if (queue.size() == 0) {
                    break;
                }

                awake.set(true);
                selector.selectNow();
            }
        }
    }

    public void addTimer(noza.base.poller.Timer timer)
    {
        timers.add(timer);
        if (!awake.get()) {
            selector.wakeup();
        }
    }

    public void removeTimer(noza.base.poller.Timer timer)
    {
        timers.remove(timer);
    }

    private long executeTimers()
    {
        while (true) {
            noza.base.poller.Timer timer = timers.peek();
            if (timer == null) {
                return 0;
            }

            if (timer.timeout > timestamp) {
                break;
            }

            timer = timers.poll();

            if (timer.periodic) {
                timer.timeout = timestamp + timer.interval;
                timers.add(timer);
            }

            timer.onTimeout();
        }

        noza.base.poller.Timer timer = timers.peek();
        if (timer == null) {
            return 0;
        }

        return timer.timeout - timestamp;
    }
}

