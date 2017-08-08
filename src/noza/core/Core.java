package noza.core;

import noza.api.Callbacks;
import noza.api.Noza;
import noza.api.beans.ClientBean;
import noza.base.config.Config;
import noza.base.config.Configs;
import noza.base.log.Log;
import noza.base.log.LogOwner;
import noza.core.client.Client;
import noza.core.msg.ConnectMsg;
import noza.core.msg.PublishMsg;
import noza.core.msg.Topic;
import noza.core.subscription.Subs;
import noza.core.worker.events.PublishMsgBatch;
import noza.core.worker.Worker;
import noza.db.Db;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


public class Core implements LogOwner
{
    private static final String VERSION = "Version 0.1.0";
    static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss.SSS");

    private static final int STOPPED = 0;
    private static final int RUNNING = 1;

    private static final SecureRandom rand = new SecureRandom();

    private int state;
    private Configs configs;
    private Callbacks callbacks;

    private final String name;
    private final AtomicInteger workerId;
    private final Log log;
    private final ArrayList<Worker> workers;
    private final Worker dispatcher;
    private final HashMap<String, ClientRecord> clients;
    private final Subs subs;
    private final Db db;


    private final boolean allowAnonymous;
    private final boolean guiOn;
    private final boolean usernameAsClientId;
    private final boolean storeRetain;
    private final boolean storeQos0;

    private final int maxMsgSize;
    private final int maxInflight;
    private final int clientExpireDuration;
    private final int auditInterval;

    public Core(Callbacks callbacks, String configsPath)
    {
        name                 = "[  Core-API  ]";
        state                = STOPPED;
        this.callbacks       = callbacks;
        configs              = new Configs(configsPath);
        dispatcher           = new Worker("[ Dispatcher ]", 0, this, configs);
        clients              = new HashMap<>();
        workerId             = new AtomicInteger(0);
        subs                 = new Subs(configs);
        workers              = new ArrayList<>();
        log                  = new Log(dispatcher);
        db                   = createDbConnection();

        allowAnonymous       = configs.get(Config.BROKER_ALLOW_ANONYMOUS);
        guiOn                = configs.get(Config.BROKER_GUI_ON);
        maxMsgSize           = configs.get(Config.MQTT_MSG_MAXSIZE);
        maxInflight          = configs.get(Config.MQTT_MAX_INFLIGHT);
        clientExpireDuration = configs.get(Config.BROKER_CLIENT_EXPIRE_DURATION);
        auditInterval        = configs.get(Config.BROKER_AUDIT_INTERVAL);
        usernameAsClientId   = configs.get(Config.BROKER_USERNAME_AS_CLIENTID);
        storeRetain          = configs.get(Config.MQTT_STORE_RETAIN);
        storeQos0            = configs.get(Config.MQTT_STORE_QOS0);
    }

    public int getMaxMsgSize()
    {
        return maxMsgSize;
    }

    public int workerCount()
    {
        return workers.size();
    }

    public List<Worker> getWorkers()
    {
        return workers;
    }

    public Db createDbConnection()
    {
        return new Db(configs);
    }

    private String generateId()
    {
        return UUID.randomUUID().toString();
    }

    public void verifyTopic(Topic topic)
    {

    }

    public boolean addSubscription(Client client, Topic topic, boolean processRetain)
    {
        return subs.subscribe(client, topic.getStr(), topic.getQos());
    }

    public void removeSubscription(String clientId, String topic)
    {
        subs.removeSubscription(clientId, topic);
    }

    public void removeSubscriptions(String clientId, List<String> subscriptions)
    {
        subs.removeSubscriptions(clientId, subscriptions);
    }

    public boolean publish(String username, String clientId, PublishMsgBatch batch)
    {
        return subs.publish(username, clientId, batch);
    }

    public void start() throws RuntimeException
    {
        if (state == RUNNING) {
            throw new RuntimeException("Core is already running");
        }

        init();

        state = RUNNING;
    }

    public void stop()
    {

    }

    public Configs getConfigs()
    {
        return configs;
    }

    public void setCallbacks(Callbacks callbacks)
    {
        this.callbacks = callbacks;
    }

    private void init()
    {
        log.init(configs, callbacks);

        logInfo("Starting Noza : ", VERSION);
        logInfo(configs.toString());

        List<Map<Config, Object>> list = configs.get(Config.TRANSPORT_LIST);
        for (Map<Config, Object> transport : list) {
            dispatcher.addEndpoint(transport);
        }

        int workerCount = configs.get(Config.BROKER_WORKER_COUNT);
        for (int i = 0; i < workerCount; i++) {
            Worker worker = new Worker("[  Worker-" + i + "  ]", i, this, configs);
            workers.add(worker);
        }

        workers.forEach(Worker::start);

        readDb();
        dispatcher.start();
    }

    private void readDb()
    {
        List<ClientBean> clientList = db.getClients();

        for (ClientBean client : clientList) {
            if (!client.isCleanSession()) {
                ClientRecord record = new ClientRecord(client.getClientId(),
                                                       nextWorker());

                clients.put(client.getClientId(), record);

                List<Topic> subscriptions = db.getClientSubs(client.getClientId());
                for (Topic topic : subscriptions) {
                    boolean success = subs.addSubscription(record,
                                                           client.getUsername(),
                                                           topic.getStr(),
                                                           topic.getQos());

                    if (!success) {
                        db.removeSubscription(record.getClientId(), topic.getStr());
                    }
                }
            }
        }

        PublishMsgBatch batch = new PublishMsgBatch(db, getWorkers());
        for (ClientBean client : clientList) {
            String msgId = client.getWillMsg();
            if (msgId != null) {
                batch.clear();
                PublishMsg msg = db.getMsg(msgId);
                batch.setMsg(db.getMsg(msgId));
                subs.publish(client.getUsername(), client.getClientId(), batch);
                db.removeClientWillMsg(client.getClientId());
            }

            if (client.isCleanSession()) {
                db.removeClient(client.getClientId(), true);
            }
        }


    }

    public void onLog(String owner, String level, String log)
    {
        callbacks.onLog(owner, level, log);
    }

    public void onTerminate(String msg)
    {
        callbacks.onTerminate(msg);
    }

    public int onAccept(String localAddress, String remoteAddress)
    {
        return callbacks.onAccept(localAddress, remoteAddress);
    }

    public int onConnect(String localAddress, String remoteAddress, ConnectMsg connect)
    {
        if (!verifyClientId(connect.clientId)) {
            return Noza.CLIENT_ID_REJECTED;
        }

        if (usernameAsClientId) {
            if (!connect.clientId.equals(connect.username)) {
                return Noza.ERROR;
            }
        }

        return callbacks.onConnect(localAddress, remoteAddress, connect);
    }

    public void onDisconnect(String connection, String clientId)
    {
        callbacks.onDisconnect(connection, clientId);
    }

    private Worker nextWorker()
    {
        return workers.get(workerId.getAndIncrement() % workers.size());
    }

    private boolean verifyClientId(String clientId)
    {
        for (int i = 0; i < clientId.length(); i++) {
            if (Mqtt.clientIdChars.indexOf(clientId.charAt(i)) == -1) {
                return false;
            }
        }

        if (!allowAnonymous) {
            if (clientId.length() == 0) {
                return false;
            }
        }

        return true;
    }

    public boolean addClient(Client client)
    {
        String id = client.getClientId();
        if (id.length() == 0) {
            client.setClientId(generateId());
        }

        ClientRecord prev;

        synchronized (clients) {
            prev = clients.put(id, client.getRecord());
        }

        if (prev != null) {
            client.setWorker(prev.getWorker());
            return true;
        }
        else {
            client.setWorker(nextWorker());
            return false;
        }
    }

    public void removeClient(ClientRecord record)
    {
        synchronized (clients) {
            clients.remove(record.getClientId(), record);
        }
    }

    @Override
    public Log getLogger()
    {
        return log;
    }

    @Override
    public String getTimestampStr()
    {
        return '[' + FORMATTER.format(LocalDateTime.now()) + ']';
    }

    @Override
    public String getName()
    {
        return name;
    }
}
