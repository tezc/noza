package noza.core;


import noza.core.worker.Worker;

public class ClientRecord
{
    private String clientId;
    private Worker worker;

    public ClientRecord(String clientId, Worker worker)
    {
        this.clientId = clientId;
        this.worker   = worker;
    }

    public String getClientId()
    {
        return clientId;
    }

    public void setClientId(String clientId)
    {
        this.clientId = clientId;
    }

    public Worker getWorker()
    {
        return worker;
    }

    public void setWorker(Worker worker)
    {
        this.worker = worker;
    }
}
