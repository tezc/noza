package noza.base.poller;

import java.util.ArrayList;
import java.util.List;

public class EventQueue
{
    private final Object lock;
    private List<Event> eventList[];
    private int eventIndex;


    @SuppressWarnings("unchecked")
    public EventQueue()
    {
        lock         = new Object();
        eventList    = new ArrayList[2];
        eventList[0] = new ArrayList<>(100);
        eventList[1] = new ArrayList<>(100);
        eventIndex   = 0;
    }

    public void addEvent(Event event)
    {
        synchronized (lock) {
            eventList[eventIndex].add(event);
        }
    }

    public List<Event> getEventList()
    {
        List<Event> list;

        synchronized (lock) {
            list = eventList[eventIndex];
            eventIndex ^= 1;
        }

        return list;
    }

    public int size()
    {
        int size;
        synchronized (lock) {
            size = eventList[eventIndex].size();
        }

        return size;
    }
}
