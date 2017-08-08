package noza.core.worker.events;

import noza.base.common.Util;
import noza.base.poller.Event;
import noza.core.client.Client;
import noza.core.worker.Worker;


public class ConnectedClient implements Event
{
    private Client client;
    private boolean sessionPresent;

    public ConnectedClient(Client client, boolean sessionPresent)
    {
        this.client         = client;
        this.sessionPresent = sessionPresent;
    }

    @Override
    public void onEvent(Worker worker)
    {
        worker.connectedClientEvent(client, sessionPresent);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(256);

        builder.append("----------------------------------------------------")                .append(
            Util.newLine());

        builder.append('\t').append("Event  : Connected Client Event")                        .append(Util.newLine());
        builder.append('\t').append("Client : ").append(client.toString())                    .append(Util.newLine());

        builder.append("----------------------------------------------------")                .append(Util.newLine());

        return builder.toString();
    }
}
