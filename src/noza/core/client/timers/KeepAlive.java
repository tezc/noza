package noza.core.client.timers;


import noza.base.poller.Timer;
import noza.core.client.Client;

public class KeepAlive extends Timer
{
    Client client;

    public KeepAlive(Client client, boolean periodic, long interval, long timeout)
    {
        super(periodic, interval, timeout);
        this.client = client;
    }

    @Override
    public void onTimeout()
    {
        client.handleKeepAliveTimeout();
    }
}
