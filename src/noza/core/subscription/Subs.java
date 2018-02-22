package noza.core.subscription;

import noza.base.config.Config;
import noza.base.config.Configs;
import noza.core.ClientRecord;
import noza.core.msg.PublishMsg;
import noza.core.msg.Topic;
import noza.core.worker.events.PublishMsgBatch;
import noza.core.client.Client;

import java.util.*;

public class Subs
{
    private Map<String, Node> subs;
    private List<Node> wildcardSubs;
    private Map<String, Node> sharedSubs;

    private List<Rule> publishRules;
    private List<Rule> subscribeRules;

    public Subs(Configs configs)
    {
        subs           = new HashMap<>();
        wildcardSubs   = new ArrayList<>();
        sharedSubs     = new HashMap<>();
        publishRules   = new ArrayList<>();
        subscribeRules = new ArrayList<>();

        List<Map<Config, String>> rules = configs.get(Config.SUBSCRIPTION_PATTERN_LIST);
        for (Map<Config, String> rule : rules) {
            addRule(rule.get(Config.SUBSCRIPTION_PATTERN_USERNAME),
                    rule.get(Config.SUBSCRIPTION_PATTERN_CLIENTID),
                    rule.get(Config.SUBSCRIPTION_PATTERN_ACCESS),
                    rule.get(Config.SUBSCRIPTION_PATTERN_TOPIC));
        }
    }

    public void addRule(String username, String clientId, String access, String topic)
    {
        Rule rule = new Rule(username, clientId, access, topic);
        if (rule.isPublishRule()) {
            publishRules.add(rule);
        }

        if (rule.isSubscribeRule()) {
            subscribeRules.add(rule);
        }
    }

    public boolean addSubscription(ClientRecord record, String username,
                                   String topic, int qos)
    {
        TopicData data = new TopicData(topic, true, false);

        for (Rule rule : subscribeRules) {
            if (!rule.verifyForSubscription(username, record.getClientId(), data)) {
                return false;
            }
        }

        Node node = !data.isShared() ? subs.get(topic) : sharedSubs.get(topic);
        if (node == null) {
            node = new Node(data);
            subs.put(node.getFilter(), node);

            if (!node.isRegular()) {
                wildcardSubs.add(node);
            }

            if (node.isShared()) {
                sharedSubs.put(node.getTopic(), node);
            }
        }

        node.addSubscriber(record, qos);

        return true;
    }

    public boolean subscribe(Client client, String topic, int qos)
    {
        synchronized (this) {
            boolean subscribed = addSubscription(client.getRecord(),
                                                 client.getUsername(),
                                                 topic,
                                                 qos);
            if (!subscribed) {
                return false;
            }

            Node node = subs.get(topic);

            for (Node regularNode : subs.values()) {
                if (regularNode.isRegular() && regularNode.hasRetainedFor(qos)) {
                    if (node.match(regularNode)) {
                        client.sendPublish(regularNode.getRetained(), false);
                    }
                }
            }
        }

        return true;
    }

    public boolean publish(String username, String clientId, PublishMsgBatch batch)
    {
        PublishMsg msg = batch.getMsg();
        TopicData data = new TopicData(msg.topic, false, false);

        for (Rule rule : publishRules) {
            if (!rule.verifyForPublish(username, clientId, data)) {
                return false;
            }
        }

        synchronized (this) {
            Node node = subs.get(msg.topic);
            if (node != null) {
                if (msg.retained) {
                    node.setRetained(msg);
                }

                for (ClientData client : node.getClients()) {
                    if (client.qos >= msg.qos) {
                        batch.add(client.record);
                    }
                }
            }

            for (Node nodeWc : wildcardSubs) {
                if (nodeWc.match(data)) {
                    for (ClientData client : nodeWc.getClients()) {
                        if (client.qos >= msg.qos) {
                            batch.add(client.record);
                        }
                    }
                }
            }

            batch.flush();
        }

        return true;
    }

    public boolean removeSubscription(String clientId, String topic)
    {
        boolean removed = false;
        synchronized (this) {
            Node node = subs.get(topic);
            if (node != null) {
                removed = node.removeSubscriber(clientId);

                if (node.subscriberCount() == 0 && !node.isPermanent()) {
                    subs.remove(node.getTopic());

                    if (!node.isRegular()) {
                        wildcardSubs.remove(node);
                    }
                }
            }
            else {
                node = sharedSubs.get(topic);
                if (node != null) {
                    removed = node.removeSubscriber(clientId);

                    if (node.subscriberCount() == 0 && !node.isPermanent()) {
                        subs.remove(node.getFilter());
                        sharedSubs.remove(node.getTopic());

                        if (!node.isRegular()) {
                            wildcardSubs.remove(node);
                        }
                    }

                }
            }
        }


        return removed;
    }

    public void removeSubscriptions(String clientId, List<String> topics)
    {
        synchronized (this) {
            for (String topic : topics) {
                removeSubscription(clientId, topic);
            }
        }
    }
}
