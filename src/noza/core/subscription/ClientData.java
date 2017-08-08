package noza.core.subscription;

import noza.core.ClientRecord;

public class ClientData
{
    public ClientRecord record;
    public int qos;

    public ClientData(ClientRecord record, int qos)
    {
        this.record = record;
        this.qos    = qos;
    }
}
