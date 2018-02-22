package noza.core;


import noza.core.worker.ClientWorker;
import noza.core.worker.Worker;

public class ClientRecord
{
    private String clientId;
    private ClientWorker worker;

    public ClientRecord(String clientId, ClientWorker worker)
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

    public ClientWorker getWorker()
    {
        return worker;
    }

    public void setWorker(ClientWorker worker)
    {
        this.worker = worker;
    }
}
