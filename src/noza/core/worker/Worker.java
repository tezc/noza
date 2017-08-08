package noza.core.worker;

import noza.api.beans.ClientBean;
import noza.base.config.Configs;
import noza.base.poller.Event;
import noza.base.transport.listener.Listener;
import noza.base.transport.listener.ListenerOwner;
import noza.core.Core;
import noza.core.msg.ConnackMsg;
import noza.core.worker.events.ConnectedClient;
import noza.db.Db;
import noza.api.Noza;
import noza.core.msg.Topic;
import noza.base.config.Config;
import noza.base.log.LogWriter;
import noza.base.transport.sock.Sock;
import noza.base.log.Log;
import noza.base.log.LogOwner;
import noza.base.poller.Poller;
import noza.base.poller.Timer;
import noza.core.BufferPool;
import noza.core.LogEvent;
import noza.core.client.Client;
import noza.core.msg.ConnectMsg;
import noza.core.msg.PublishMsg;
import noza.core.worker.events.PublishMsgBatch;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.*;


public class Worker implements Runnable, LogOwner, BufferPool, ListenerOwner, LogWriter
{
    private String name;
    private Thread thread;
    private int id;
    private int largeBufferSize;
    private Poller poller;
    private Core core;
    private Db db;
    private HashMap<String, Client> clients;
    private List<Client> pendings;
    private ByteBuffer tempBuf;

    private ArrayDeque<ByteBuffer> bufs;
    private ArrayDeque<ByteBuffer> directBufs;
    private ArrayDeque<ByteBuffer> largeBufs;
    private ArrayDeque<ByteBuffer> largeDirectBufs;

    private List<Client> msgReceivers;

    private List<Listener> listeners;

    private PublishMsgBatch batch;

    public Worker(String name, int id, Core core, Configs configs)
    {
        this.id              = id;
        this.name            = name;
        this.core            = core;
        this.db              = core.createDbConnection();
        this.thread          = new Thread(this, name);
        this.poller          = new Poller(this);
        this.tempBuf         = ByteBuffer.allocateDirect((1024 * 256) - 16);
        this.bufs            = new ArrayDeque<>();
        this.largeBufs       = new ArrayDeque<>();
        this.directBufs      = new ArrayDeque<>();
        this.largeDirectBufs = new ArrayDeque<>();
        this.pendings        = new ArrayList<>();
        this.clients         = new HashMap<>();
        this.listeners       = new ArrayList<>();
        this.msgReceivers    = new ArrayList<>();
        this.largeBufferSize = configs.get(Config.BROKER_LARGE_BUFSIZE);
    }

    public int getId()
    {
        return id;
    }

    public Configs getConfig()
    {
        return core.getConfigs();
    }

    @Override
    public String toString()
    {
        return name;
    }

    @Override
    public ByteBuffer allocBuf()
    {
        ByteBuffer buf = bufs.poll();
        if (buf == null) {
            buf = ByteBuffer.allocate(4080);
        }

        return buf;
    }

    @Override
    public void freeBuf(ByteBuffer buf)
    {
        buf.clear();
        bufs.push(buf);
    }

    @Override
    public ByteBuffer allocLargeBuf()
    {
        ByteBuffer buf = largeBufs.poll();
        if (buf == null) {
            buf = ByteBuffer.allocate(largeBufferSize);
        }

        return buf;
    }

    @Override
    public void freeLargeBuf(ByteBuffer buf)
    {
        buf.clear();
        largeBufs.push(buf);
    }

    @Override
    public ByteBuffer allocDirectBuf()
    {
        ByteBuffer buf = directBufs.poll();
        if (buf == null) {
            buf = ByteBuffer.allocate(4080);
        }

        return buf;
    }

    @Override
    public void freeDirectBuf(ByteBuffer buf)
    {
        buf.clear();
        directBufs.push(buf);
    }

    @Override
    public ByteBuffer allocLargeDirectBuf()
    {
        ByteBuffer buf = largeDirectBufs.poll();
        if (buf == null) {
            buf = ByteBuffer.allocate(largeBufferSize);
        }

        return buf;
    }

    @Override
    public void freeLargeDirectBuf(ByteBuffer buf)
    {
        buf.clear();
        largeDirectBufs.push(buf);
    }

    @Override
    public ByteBuffer getTempBuf()
    {
        return tempBuf;
    }

    public void addEndpoint(Map<Config, Object> config)
    {
        listeners.add(Listener.create(this, poller.getSelector(), config));
    }

    public void start()
    {
        logInfo(toString() + " starting..");

        batch = new PublishMsgBatch(db, core.getWorkers());

        thread.start();
    }

    public long timestamp()
    {
        return poller.getTimestamp();
    }

    public void removeClient(String clientId)
    {
        clients.remove(clientId);
    }

    public void removeTimer(Timer timer)
    {
        poller.removeTimer(timer);
    }

    public void connectedClient(Client client, boolean sessionPresent)
    {
        poller.addEvent(new ConnectedClient(client, sessionPresent));
    }

    public void addEvent(Event event)
    {
        poller.addEvent(event);
    }

    public void eventsDrained()
    {
        try {
            for (Client client : msgReceivers) {
                try {
                    client.handleBatchMsgs();
                }
                catch (Exception e) {
                    client.disconnect(false);
                }
            }
        }
        finally {
            msgReceivers.clear();
        }
    }

    @Override
    public void handleLogEvent(LogEvent event)
    {
        getLogger().write(event);
    }

    public void publishEvent(PublishMsg msg, List<String> clientList)
    {
        for (String clientId : clientList) {
            try {
                Client client = clients.get(clientId);
                if (client != null) {
                    int count = client.addPending(msg);
                    if (count == 1) {
                        msgReceivers.add(client);
                    }
                }
            }
            catch (Exception e) {
                logError(e, "Trying to survive ");
            }
        }
    }

    public void connectedClientEvent(Client pending, boolean sessionPresent)
    {
        try {
            Client client = clients.get(pending.getClientId());
            if (client != null) {
                client.disconnect(true);
            }
        }
        catch (Exception e) {
            logError(e, "Trying to survive...");
        }

        pending.start(sessionPresent);
        clients.put(pending.getClientId(), pending);
    }

    @Override
    public void run()
    {
        while (true) {
            try {
                poller.loop();
            }
            catch (Exception e) {
                logError(e, "Trying to survive after : ");
            }
        }
    }

    @Override
    public void handleAcceptEvent(Listener listener)
    {
        Client client;
        Sock sock;

        try {
            sock = listener.accept();
        }
        catch (Exception e) {
            logError(e);
            return;
        }

        try {
            logInfo(listener.toString()," accepted : ", sock);

            int rc = core.onAccept(sock.getLocalAddress(),
                                   sock.getRemoteAddress());
            if (rc != Noza.OK) {
                sock.close();
                logInfo("Disconnected : ", sock.toString());
                return;
            }

            sock.register(poller.getSelector(), SelectionKey.OP_READ);
            sock.handshake();

            client = new Client(core.getConfigs(), this, sock);
        }
        catch (Exception e) {
            logError(e, " Trying to continue...");
            sock.close();
            logInfo("Disconnected : ", sock.toString());

            return;
        }

        try {
            pendings.add(client);
        }
        catch (Exception e) {
            logError(e, " Trying to continue...");
            client.disconnect(false);
        }
    }

    public void handleConnected(Client client, ConnectMsg connect)
    {
        int rc = core.onConnect(client.getLocalAddress(),
                                client.getRemoteAddress(),
                                connect);
        if (rc != Noza.OK) {
            logInfo("Rejecting client : " + client.toString());

            byte status = (rc == Noza.CLIENT_ID_REJECTED) ? ConnackMsg.REFUSED_ID_REJECTED :
                                                            ConnackMsg.REFUSED_NOT_AUTHORIZED;

            client.sendConnack(status, false);
            client.flush();
            client.disconnect(false);
            return;
        }

        logInfo("Connected client : " + client.toString());

        pendings.remove(client);

        boolean sessionPresent = core.addClient(client);

        client.unregister();
        client.dispatch(sessionPresent);
    }

    public void handleDisconnect(Client client)
    {
        clients.remove(client.getClientId());
        if (!client.isCleanSession()) {
            storeClientState(client.getClientId(), Client.DISCONNECTED);
        }

        logInfo("Disconnected client : " + client.toString());
    }

    public boolean addSubscription(Client client, Topic topic, boolean processRetain)
    {
        return core.addSubscription(client, topic, processRetain);
    }

    public boolean publish(String username, String clientId, PublishMsg publish)
    {
        batch.clear();
        batch.setMsg(publish);

        return core.publish(username, clientId, batch);
    }

    @Override
    public Log getLogger()
    {
        return core.getLogger();
    }

    @Override
    public String getTimestampStr()
    {
        return poller.getTimestampStr();
    }

    @Override
    public String getName()
    {
        return name;
    }

    public void addTimer(Timer timer)
    {
        poller.addTimer(timer);
    }

    public void register(Sock sock, int ops)
    {
        poller.add(sock, ops);
    }

    public void handleRead(Client client)
    {
        try {
            client.handleReadEvent();
        }
        catch (Exception e) {
            logError(e, "Client error : ", client);
            client.disconnect(false);
        }
    }

    public void handleWrite(Client client)
    {
        try {
            client.handleWriteEvent();
        }
        catch (Exception e) {
            logError(e, "Client error : ", client);
        }
    }

    public void removeSubscription(String clientId, String topic)
    {
        core.removeSubscription(clientId, topic);
    }

    public void removeSubscriptions(String clientId, List<String> subscriptions)
    {
        core.removeSubscriptions(clientId, subscriptions);
    }

    public void storeClient(String id,
                            String username,
                            int state,
                            boolean cleanSession,
                            boolean sessionPresent,
                            PublishMsg willMsg,
                            String localAddress,
                            String remoteAddress)
    {
        String msgId = null;

        if (willMsg != null) {
            db.storeMsg(willMsg, false);
            msgId = willMsg.getId();
        }

        if (sessionPresent & cleanSession) {
            db.removeClient(id, false);
        }

        db.storeClient(id,  username, state, cleanSession, msgId,
                       localAddress, remoteAddress, true);
    }

    public void storeClientState(String id, int state)
    {
        db.storeClientState(id, state);
    }

    public void storeSubscription(String clientId, String topic, int qos)
    {
        db.storeSubscription(clientId, topic, qos);
    }

    public void storeMsg(PublishMsg publish)
    {
        db.storeMsg(publish, true);
    }

    public void storeClientInMsg(String clientId, String msgId, int packetId, int state)
    {
        db.storeClientInMsg(clientId, msgId, packetId, state);
    }

    public void storeClientInMsgState(String clientId, String msgId, int state)
    {
        db.storeClientInMsgState(clientId, msgId, state);
    }

    public void storeClientOutMsg(String clientId, String msgId, short packetId, int state)
    {
        db.storeClientOutMsg(clientId, msgId, packetId, state);
    }

    public void storeClientOutMsgState(String clientId, String msgId, int state)
    {
        db.storeClientOutMsgState(clientId, msgId, state);
    }

    public void removeStoredClient(String id)
    {
        db.removeClient(id, true);
    }

    public void removeStoredSubscription(String clientId, String topic)
    {
        db.removeSubscription(clientId, topic);
    }

    public void removeStoredSubscriptions(String clientId, List<String> topics)
    {
        db.removeSubscriptions(clientId, topics);
    }

    public void removeStoredMsg(String msgId)
    {
        db.removeMsg(msgId);
    }

    public void removeStoredClientInMsg(String clientId, String msgId)
    {
        db.removeClientInMsg(clientId, msgId);
    }

    public void removeStoredClientOutMsg(String clientId, String msgId)
    {
        db.removeClientOutMsg(clientId, msgId);
    }

    public List<ClientBean> getClients()
    {
        return db.getClients();
    }

    public Client getClient(String clientId)
    {
        return db.getClient(clientId);
    }

    public List<Topic> getClientSubs(String clientId)
    {
        return db.getClientSubs(clientId);
    }

    public PublishMsg getMsg(String msgId)
    {
        return db.getMsg(msgId);
    }

    public List<PublishMsg> getStoredClientInMsgs(String clientId)
    {
        return db.getClientInMsgs(clientId);
    }

    public List<PublishMsg> getStoredClientOutMsgs(String clientId)
    {
        return db.getClientOutMsgs(clientId);
    }
}
