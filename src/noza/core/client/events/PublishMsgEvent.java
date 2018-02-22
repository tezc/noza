package noza.core.client.events;


import noza.base.common.Util;
import noza.base.poller.Event;
import noza.core.msg.PublishMsg;
import noza.core.worker.ClientWorker;
import noza.core.worker.Worker;

import java.util.ArrayList;
import java.util.List;


public class PublishMsgEvent implements Event
{
    public List<String> clients;
    private PublishMsg publish;
    private ClientWorker worker;

    public PublishMsgEvent(ClientWorker worker)
    {
        this.clients = new ArrayList<>();
        this.worker  = worker;
    }

    public void add(String clientId)
    {
        clients.add(clientId);
    }

    public void clear()
    {
        clients.clear();
    }

    public int size()
    {
        return clients.size();
    }

    public boolean sendEvent(PublishMsg publish)
    {
        this.publish = publish;

        if (clients.size() != 0) {
            worker.addEvent(this);
            return true;
        }

        return false;
    }

    @Override
    public void onEvent(Worker worker)
    {
        worker.publishEvent(publish, clients);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(256);

        builder.append("----------------------------------------------------")                .append(
            Util.newLine());
        builder.append('\t').append("Event   : Publish Message Event")                        .append(Util.newLine());

        for (String client : clients) {
            builder.append('\t').append("Client  : ").append(client)                          .append(Util.newLine());
        }

        builder.append('\t').append("Publish : ").append(publish.toString())                  .append(Util.newLine());
        builder.append("----------------------------------------------------")                .append(Util.newLine());

        return builder.toString();
    }
}
