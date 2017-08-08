package noza.core.subscription;

import noza.api.msgs.Publish;
import noza.core.ClientRecord;
import noza.core.msg.PublishMsg;

import java.util.ArrayList;
import java.util.List;

public class Node extends TopicData
{
    private PublishMsg retained;
    private List<ClientData> clients;
    private List<ClientData> subscribers;
    private int index;


    public Node(TopicData data)
    {
        super(data);

        this.subscribers = new ArrayList<>();
        this.clients     = isShared() ? new ArrayList<>(1) : subscribers;
        this.index       = 0;
    }

    public void addSubscriber(ClientRecord record, int qos)
    {
        subscribers.add(new ClientData(record, qos));
    }

    public boolean removeSubscriber(String clientId)
    {
        return  subscribers.removeIf(s -> s.record.getClientId().equals(clientId));
    }

    public int subscriberCount()
    {
        return subscribers.size();
    }

    public boolean hasRetainedFor(int qos)
    {
        if (retained != null && retained.qos >= qos) {
            return true;
        }

        return false;
    }

    public PublishMsg getRetained()
    {
        return retained;
    }

    public List<ClientData> getClients()
    {
        if (isShared()) {
            clients.clear();
            clients.add(subscribers.get(index++ % subscribers.size()));
        }

        return clients;
    }

    public void setRetained(PublishMsg retained)
    {
        this.retained = retained;
    }

    public boolean match(TopicData topic)
    {
        TopicData filter;
        TopicData name;

        if (isFilter()) {
            filter = this;
            name   = topic;
        }
        else {
            filter = topic;
            name   = this;
        }

        if (filter.isRegular()) {
            return filter.getFilter().equals(name.topic);
        }
        else if (!filter.isMultiLevelWcOwner()) {
            if (filter.filterLevels.length != name.filterLevels.length) {
                return false;
            }

            for (int i = 0; i < filter.filterLevels.length; i++)  {
                if (filter.filterLevels[i].equals("+")) {
                    continue;
                }

                if (!filter.filterLevels[i].equals(name.filterLevels[i])) {
                    return false;
                }
            }

            return true;
        }
        else {
            if (filter.filterLevels.length > name.filterLevels.length + 1) {
                return false;
            }

            for (int i = 0; i < filter.filterLevels.length; i++) {
                if (filter.filterLevels[i].equals("+")) {
                    continue;
                }

                if (filter.filterLevels[i].equals("#")) {
                    return true;
                }

                if (!filter.filterLevels[i].equals(name.filterLevels[i])) {
                    return false;
                }
            }

            return true;
        }
    }




}
