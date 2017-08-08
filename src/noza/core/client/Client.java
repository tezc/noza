package noza.core.client;

import noza.base.config.Configs;
import noza.base.exception.MqttException;
import noza.base.transport.sock.SockOwner;
import noza.core.ClientRecord;
import noza.core.client.timers.KeepAlive;
import noza.core.msg.*;
import noza.core.worker.Worker;
import noza.core.msg.Topic;
import noza.base.common.MqttDecoder;
import noza.base.transport.sock.Sock;
import noza.base.poller.Timer;
import noza.core.BufferPool;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class Client implements SockOwner
{
    public static final int ACCEPTED           = 0;
    public static final int CONNECTED          = 1;
    public static final int DISCONNECTED       = 2;

    private ClientRecord record;

    private int state;
    private Worker worker;
    private long timestamp;

    private ByteBuffer sendBuf;
    private final Sock sock;

    private byte connectFlags;
    private String clientId;
    private String userName;
    private String password;
    private long keepAlive;
    private PublishMsg willMessage;

    private boolean cleanSession;
    private boolean hasUsername;
    private boolean hasPassword;
    private boolean hasWill;

    private short packetId;

    private ArrayList<Timer> timers;
    private ArrayList<String> subscriptions;
    private ArrayDeque<Msg> incomings;
    private ArrayDeque<Msg> outgoings;
    private ArrayDeque<PublishMsg> inFlights;

    private ArrayDeque<PublishMsg> pendings;

    private MqttDecoder decoder;

    public Client(Configs configs, Worker worker, Sock sock)
    {
        this.state         = ACCEPTED;
        this.worker        = worker;
        this.sock          = sock;
        this.record        = new ClientRecord(null, null);
        this.timestamp     = worker.timestamp();
        this.timers        = new ArrayList<>();
        this.subscriptions = new ArrayList<>();
        this.incomings     = new ArrayDeque<>();
        this.outgoings     = new ArrayDeque<>();
        this.inFlights     = new ArrayDeque<>();
        this.pendings      = new ArrayDeque<>();
        this.decoder       = new MqttDecoder(configs);

        sock.setOwner(this);
    }

    public void handleBatchMsgs()
    {
        worker.logInfo("Handling msg count : ", pendings.size());
        for (PublishMsg publish : pendings) {
            sendPublish(publish);
        }

        pendings.clear();

        flush();
    }

    public int addPending(PublishMsg publish)
    {
        pendings.add(publish);

        return pendings.size();
    }

    public String getLocalAddress()
    {
        return sock.getLocalAddress();
    }

    public String getRemoteAddress()
    {
        return sock.getRemoteAddress();
    }

    public String getAddress()
    {
        return sock.toString();
    }

    public void setClientId(String clientId)
    {
        this.clientId = clientId;
        this.record.setClientId(clientId);
    }

    public boolean isCleanSession()
    {
        return cleanSession;
    }

    public void setWorker(Worker worker)
    {
        this.worker = worker;
        record.setWorker(worker);
    }

    public void dispatch(boolean sessionPresent)
    {
        worker.connectedClient(this, sessionPresent);
    }

    public void disconnect(boolean gracefully)
    {
        if (state == CONNECTED) {
            state = DISCONNECTED;
            try {
                sock.close();
            }
            finally {

                if (cleanSession) {
                    worker.removeSubscriptions(clientId, subscriptions);
                    worker.removeStoredClient(clientId);
                }
                else {
                    worker.storeClientState(clientId, DISCONNECTED);
                }

                for (Timer timer : timers) {
                    worker.removeTimer(timer);
                }

                worker.removeClient(clientId);

                if (!gracefully) {
                    if (hasWill) {
                        worker.publish(userName, clientId, willMessage);
                    }
                }
            }
        }
    }

    public void unsubscribe(UnsubscribeMsg unsubscribe)
    {
        for (String topic : unsubscribe.topics) {
            worker.removeSubscription(clientId, topic);
            if (!cleanSession) {
                worker.removeStoredSubscription(clientId, topic);
            }
        }

        sendUnsuback(unsubscribe);
    }

    private boolean verifyKeepAlive()
    {
        long diffSeconds = (worker.timestamp() - timestamp) / 1000;
        long limit       = keepAlive * 3 / 2;

        return (diffSeconds < limit);
    }

    public void unregister()
    {
        sock.cancel();
    }

    public ClientRecord getRecord()
    {
        return record;
    }

    public String getClientId()
    {
        return clientId;
    }

    public String getUsername()
    {
        return userName;
    }

    public String getPassword()
    {
        return password;
    }

    private void retriveStoredData()
    {
        List<PublishMsg> publishes;

        publishes = worker.getStoredClientOutMsgs(clientId);
        inFlights.addAll(publishes);
        outgoings.addAll(publishes);

        //publishes = worker.getStoredClientInMsgs(clientId);
       // inFlights.addAll(publishes);
    }

    public void start(boolean sessionPresent)
    {
        if (sessionPresent && !cleanSession) {
            retriveStoredData();
        }

        worker.storeClient(clientId,
                           userName,
                           Client.CONNECTED,
                           cleanSession,
                           sessionPresent,
                           willMessage,
                           sock.getLocalAddress(),
                           sock.getRemoteAddress());

        sendConnack(ConnackMsg.ACCEPTED, sessionPresent & (!cleanSession));

        addKeepAliveTimer();
        sock.setOwner(this);
        worker.register(sock, SelectionKey.OP_READ);

        handleMsgs();

        flush();
    }

    public void flush()
    {
        if (state == CONNECTED) {
            while (!outgoings.isEmpty()) {
                ByteBuffer sendBuf = sock.getSendBuf();
                Msg msg = outgoings.element();

                msg.encode();
                msg.writeTo(sendBuf);

                if (msg.written()) {
                    worker.logInfo("Message is sent to ", clientId, msg);
                    outgoings.pop();
                }

                if (sendBuf.remaining() == 0 || outgoings.size() == 0) {
                    sendBuf.flip();
                    if (!sock.send()) {
                        return;
                    }
                }
            }
        }
    }

    private void recvPuback(PubackMsg puback)
    {
        boolean found = false;

        for (PublishMsg publish : inFlights) {
            if (publish.getPacketId() == puback.packetId) {
                inFlights.remove(publish);
                worker.removeStoredClientOutMsg(clientId, publish.getId());
                found = true;
                break;
            }
        }

        if (!found) {
            throw new Error(
                "Unexpected PUBACK with packet getId : " + puback.packetId);
        }
    }

    private void recvPubrec(PubrecMsg pubrec)
    {
        boolean found = false;

        for (PublishMsg publish : inFlights) {
            if (publish.getPacketId() == pubrec.packetId) {
                worker.storeClientOutMsgState(clientId,
                                              publish.getId(), PublishMsg.WAIT_FOR_PUBCOMP);
                sendPubrel(pubrec.packetId);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new Error(
                "Unexpected PUBREC with packet getId : " + pubrec.packetId);
        }
    }

    private void recvPubrel(PubrelMsg pubrel)
    {
        boolean found = false;

        for (PublishMsg publish : inFlights) {
            if (publish.getPacketId() == pubrel.packetId) {
                inFlights.remove(publish);
                worker.removeStoredClientInMsg(clientId, publish.getId());

                break;
            }
        }

        sendPubcomp(pubrel.packetId);
    }

    private void recvPubcomp(PubcompMsg pubcomp)
    {
        for (PublishMsg publish : inFlights) {
            if (publish.getPacketId() == pubcomp.packetId) {
                inFlights.remove(publish);
                worker.removeStoredClientOutMsg(clientId, publish.getId());

                break;
            }
        }
    }

    public void sendConnack(byte status, boolean sessionPresent)
    {
        outgoings.addFirst(new ConnackMsg(status, sessionPresent));
    }

    private void sendPingresp()
    {
        outgoings.add(new PingrespMsg());
    }

    private void sendUnsuback(UnsubscribeMsg unsubscribe)
    {
        outgoings.add(new UnsubackMsg(unsubscribe.packetId));
    }

    private void sendSuback(SubscribeMsg subscribe)
    {
        outgoings.add(new SubackMsg(subscribe));
    }

    public void sendPublish(PublishMsg publish)
    {
        PublishMsg pub = new PublishMsg(publish);

        outgoings.add(pub);

        if (pub.isStored()) {
            inFlights.add(pub);
        }
    }

    private void sendPuback(PublishMsg publish)
    {
        outgoings.add(new PubackMsg(publish.packetId));
    }

    private void sendPubrec(PublishMsg publish)
    {
        inFlights.add(publish);
        outgoings.add(new PubrecMsg(publish.packetId));
    }

    private void sendPubrel(int packetId)
    {
        outgoings.add(new PubrelMsg(packetId));
    }

    private void sendPubcomp(int packetId)
    {
        outgoings.add(new PubcompMsg(packetId));
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(256);

        builder.append(clientId);

        if (sock != null && sock.isOpen()) {
            builder.append("@");
            builder.append(sock.toString());
        }

        return builder.toString();
    }

    public boolean handleConnectMsg(ConnectMsg connect)
    {
        incomings.pop();

        this.record.setClientId(connect.clientId);

        this.state         = CONNECTED;
        this.clientId      = connect.clientId;
        this.connectFlags  = connect.connectFlags;
        this.userName      = connect.username;
        this.password      = connect.password;
        this.willMessage   = connect.willMessage;
        this.keepAlive     = connect.keepAlive;

        this.cleanSession  = connect.cleanSession;
        this.hasUsername   = connect.usernamePresent;
        this.hasPassword   = connect.passwordPresent;
        this.hasWill       = connect.willPresent;

        worker.handleConnected(this, connect);

        return false;
    }

    public boolean handleConnackMsg(ConnackMsg connack)
    {
        throw new MqttException("Unexpected message : " + connack.toString());
    }

    public boolean handleSubackMsg(SubackMsg suback)
    {
        throw new MqttException("Unexpected message : " + suback.toString());
    }

    public boolean handleUnsubackMsg(UnsubackMsg suback)
    {
        throw new MqttException("Unexpected message : " + suback.toString());
    }

    public boolean handlePingrespMsg(PingrespMsg pingresp)
    {
        throw new MqttException("Unexpected message : " + pingresp.toString());
    }

    public boolean handleDisconnectMsg(DisconnectMsg msg)
    {
        disconnect(true);
        return false;
    }


    public boolean handleSubscribeMsg(SubscribeMsg msg)
    {
        sendSuback(msg);

        for (Topic topic : msg.topics) {
            boolean result = worker.addSubscription(this, topic, true);
            if (!result) {
                topic.setResult(Topic.QOS_FAIL);
                worker.logInfo(topic.toString(), " rejected.");
                continue;
            }

            if (!cleanSession) {
                worker.storeSubscription(clientId, topic.getStr(), topic.getQos());
            }
        }

        flush();

        return true;
    }


    public boolean handleUnsubscribeMsg(UnsubscribeMsg msg)
    {
        unsubscribe(msg);
        flush();

        return true;
    }

    public void handlePublishEvent(PublishMsg msg)
    {
        sendPublish(msg);
        flush();
    }

    public boolean handlePublishMsg(PublishMsg msg)
    {
        if (msg.qos == Topic.QOS1) {
            sendPuback(msg);
        }
        else if (msg.qos == Topic.QOS2) {
            msg.setStored();
            worker.storeMsg(msg);
            worker.storeClientInMsg(clientId, msg.getId(), msg.getPacketId(), PublishMsg.WAIT_FOR_PUBREL);
            sendPubrec(msg);
        }

        worker.publish(userName, clientId, msg);
        flush();

        return true;
    }


    public boolean handlePubackMsg(PubackMsg msg)
    {
        recvPuback(msg);

        return true;
    }

    public boolean handlePubrecMsg(PubrecMsg msg)
    {
        recvPubrec(msg);
        flush();

        return true;
    }

    public boolean handlePubrelMsg(PubrelMsg msg)
    {
        recvPubrel(msg);
        flush();

        return true;
    }

    public boolean handlePubcompMsg(PubcompMsg msg)
    {
        recvPubcomp(msg);
        flush();

        return true;
    }

    public boolean handlePingreqMsg(PingreqMsg msg)
    {
        timestamp = worker.timestamp();
        sendPingresp();
        flush();

        return true;
    }

    public void handleReadEvent()
    {
        int loadCap = 8;
        boolean willDisconnect = false;

        ByteBuffer buf = worker.getTempBuf();

        do {
            loadCap--;

            buf.clear();

            int read = sock.read(buf);
            if (read == -1) {
                willDisconnect = true;
                break;
            }
            else if (read == 0) {
                break;
            }

            buf.flip();

            while (buf.hasRemaining()) {
                Msg msg = decoder.read(buf);
                if (msg == null) {
                    break;
                }

                incomings.add(msg);
            }

            timestamp = worker.timestamp();

        } while (loadCap > 0);

        handleMsgs();

        if (willDisconnect) {
            disconnect(false);
        }
    }

    public void handleWriteEvent()
    {
        if (!sock.send()) {
            return;
        }

        flush();
    }

    public void handleMsgs()
    {
        for (Msg msg : incomings) {
            worker.logInfo("Recv msg for Client : ", this , msg);

            boolean cont = msg.handle(this);
            if (!cont || state == DISCONNECTED) {
                break;
            }
        }

        incomings.clear();
    }

    public void handleKeepAliveTimeout()
    {
        try {
            if (!verifyKeepAlive()) {
                disconnect(false);
            }
        }
        catch (Exception e) {
            worker.logError("Exception on client : \n", toString(), e);
            disconnect(false);
        }
    }

    private void addKeepAliveTimer()
    {
        if (keepAlive == 0) {
            return;
        }

        long expire = (keepAlive * 3 / 2) * 1000;

        KeepAlive timer = new KeepAlive(this, true, expire, expire + worker.timestamp());

        worker.addTimer(timer);
        timers.add(timer);
    }

    @Override
    public void handleReadEvent(Sock sock)
    {
        worker.handleRead(this);
    }

    @Override
    public void handleWriteEvent(Sock sock)
    {
        worker.handleWrite(this);
    }

    @Override
    public BufferPool getPool()
    {
        return worker;
    }
}
