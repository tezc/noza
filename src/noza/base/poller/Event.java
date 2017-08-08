package noza.base.poller;


import noza.core.worker.Worker;

public interface Event
{
    void onEvent(Worker worker);
}
