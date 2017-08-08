package noza.base.poller;

import java.util.Comparator;


public abstract class Timer
{
    public boolean periodic;
    public long interval;
    public long timeout;

    public Timer(boolean periodic, long interval, long timeout)
    {
        this.periodic = periodic;
        this.interval = interval;
        this.timeout  = timeout;
    }

    public static class Compare implements Comparator<Timer>
    {
        @Override
        public int compare(Timer o1, Timer o2)
        {
            return (int) (o1.timeout - o2.timeout);
        }
    }

    public abstract void onTimeout();
}
