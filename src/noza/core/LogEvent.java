package noza.core;

import noza.base.log.Level;
import noza.base.poller.Event;
import noza.core.worker.Worker;

public class LogEvent implements Event
{
    public final Level level;
    public final String timestamp;
    public final String owner;
    public final Throwable t;
    public final Object[] args;

    public LogEvent(Level level,
                    String timestamp, String owner, Throwable t, Object[] args)
    {
        this.level     = level;
        this.timestamp = timestamp;
        this.owner     = owner;
        this.t         = t;
        this.args      = args;

    }

    @Override
    public void onEvent(Worker worker)
    {
        worker.handleLogEvent(this);
    }

}
