package noza.base.log;

import noza.base.poller.Event;
import noza.core.LogEvent;

public interface LogWriter
{
    void addEvent(Event event);
    void handleLogEvent(LogEvent event);
}
