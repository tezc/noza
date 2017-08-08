package noza.db;


import noza.api.beans.ClientBean;
import noza.api.msgs.Publish;
import noza.base.common.BufferInputStream;
import noza.base.config.Config;
import noza.base.config.Configs;
import noza.base.exception.DbException;
import noza.core.client.Client;
import noza.core.msg.PublishMsg;
import noza.core.msg.Topic;

import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Db
{
    private Configs configs;
    private Storage storage;

    private static final String DB_STR       = "jdbc:h2:";
    private static final String DB_PATH      = "./data/db/db";
    private static final String DB_MODE      = ";AUTO_SERVER=TRUE";
    private static final String DB_IN_MEMORY = "mem:";

    public Db(Configs configs)
    {
        this.configs  = configs;

        String username   = configs.get(Config.BROKER_DB_USERNAME);
        String password   = configs.get(Config.BROKER_DB_PASSWORD);
        boolean persisted = configs.get(Config.BROKER_DB_PERSISTED);

        String connection = DB_STR;
        if (!persisted) {
            connection += DB_IN_MEMORY;
        }

        connection += DB_PATH;

        /*if (persisted) {
            connection += DB_MODE;
        }*/

        this.storage  = new H2Db(connection, username, password);
    }

    public Storage getStorage()
    {
        return storage;
    }

    public void storeMsg(PublishMsg publish, boolean commit)
    {
        BufferInputStream stream = new BufferInputStream(publish.getPayload());

        storage.storeMsg(publish.getId(),
                         publish.getPacketId(),
                         publish.getTopic(),
                         publish.getQos(), stream, false, commit);
    }


    public void storeClient(String id, String username,
                            int state, boolean cleanSession,
                            String willMsg, String localAddress,
                            String remoteAddress, boolean commit)
    {
        storage.storeClient(id, username, state, cleanSession, willMsg,
                            localAddress, remoteAddress, commit);
    }

    public void storeClientState(String id, int state)
    {
        storage.storeClientState(id, state);
    }

    public void storeSubscription(String clientId, String topic, int qos)
    {
        storage.storeSubscription(clientId, topic, qos);
    }

    public void storeClientInMsg(String clientId,
                                 String msgId, int packetId, int state)
    {
        storage.storeClientInMsg(clientId, msgId, packetId, state);
    }

    public void storeClientInMsgState(String clientId, String msgId, int state)
    {
        storage.storeClientInMsgState(clientId, msgId, state);
    }

    public void storeClientOutMsg(String clientId,
                                  String msgId, int packetId, int state)
    {
        storage.storeClientOutMsg(clientId, msgId, packetId, state);
    }

    public void storeClientOutMsgState(String clientId, String msgId, int state)
    {
        storage.storeClientOutMsgState(clientId, msgId, state);
    }

    public void removeClient(String id, boolean commit)
    {
        storage.removeClient(id, commit);
    }

    public void removeSubscription(String clientId, String topic)
    {
        storage.removeSubscription(clientId, topic);
    }

    public void removeSubscriptions(String clientId, List<String> topics)
    {
        storage.removeSubscriptions(clientId, topics);
    }

    public void removeMsg(String msgId)
    {
        storage.removeMsg(msgId);
    }

    public void removeClientInMsg(String clientId, String msgId)
    {
        storage.removeClientInMsg(clientId, msgId);
    }

    public void removeClientOutMsg(String clientId, String msgId)
    {
        storage.removeClientOutMsg(clientId, msgId);
    }

    public void removeClientWillMsg(String clientId)
    {
        storage.removeClientWillMsg(clientId);
    }

    public List<ClientBean> getClients()
    {
        List<ClientBean> clients = new ArrayList<>();

        try {
            ResultSet result = storage.getClients();
            while (result.next()) {
                clients.add(new ClientBean(result.getString(1),
                                           result.getString(2),
                                           result.getInt(3),
                                           result.getBoolean(4),
                                           result.getString(5),
                                           result.getString(6),
                                           result.getString(7),
                                           result.getTimestamp(8).toString(),
                                           result.getTimestamp(9).toString()));
            }
        }
        catch (SQLException e) {
            throw new DbException(e);
        }

        return clients;
    }

    public Client getClient(String clientId)
    {
        return null;
    }

    public List<Topic> getClientSubs(String clientId)
    {
        List<Topic> subs = new ArrayList<>();

        try {
            ResultSet subsList = storage.getClientSubs(clientId);
            while (subsList.next()) {
                Topic topic = new Topic(subsList.getString("topic"),
                                        subsList.getInt("qos"));
                subs.add(topic);
            }
        }
        catch (SQLException e) {
            throw new DbException(e);
        }

        return subs;
    }

    public PublishMsg getMsg(String msgId)
    {
        try {
            ResultSet result = storage.getMsg(msgId);

            if (result.next()) {
                String id         = result.getString(1);
                int packetId      = result.getInt(2);
                String topic      = result.getString(3);
                int qos           = result.getInt(4);
                byte[] payload    = result.getBytes(5);
                boolean retain    = result.getBoolean(6);

                ByteBuffer payloadBuf = ByteBuffer.wrap(payload);

                return new PublishMsg(id, true, 0, PublishMsg.QUEUED, qos,
                                      retain, false, topic, payloadBuf);
            }
        }
        catch (SQLException e) {
            throw new DbException(e);
        }

        return null;
    }

    public List<PublishMsg> getClientInMsgs(String clientId)
    {
        List<PublishMsg> msgs = new ArrayList<>();

        try {
            ResultSet result = storage.getClientInMsgs(clientId);

            while (result.next()) {
                String id      = result.getString(1);
                int packetId   = result.getInt(2);
                String topic   = result.getString(3);
                int qos        = result.getInt(4);
                byte[] payload = result.getBytes(5);
                boolean retain = result.getBoolean(6);

                msgs.add(new PublishMsg(id,
                                        true,
                                        0,
                                        PublishMsg.QUEUED,
                                        qos,
                                        retain,
                                        false, topic, ByteBuffer.wrap(payload)));
            }
        }
        catch (SQLException e) {
            throw new DbException(e);
        }

        return msgs;
    }

    public List<PublishMsg> getClientOutMsgs(String clientId)
    {
        List<PublishMsg> msgs = new ArrayList<>();

        try {
            ResultSet result = storage.getClientOutMsgs(clientId);

            while (result.next()) {
                String id      = result.getString(1);
                String topic   = result.getString(2);
                int qos        = result.getInt(3);
                byte[] payload = result.getBytes(4);
                boolean retain = result.getBoolean(5);
                int packetId   = result.getInt(6);
                int state      = result.getInt(7);

                msgs.add(new PublishMsg(id, true, packetId, state, qos, retain, false,
                                        topic, ByteBuffer.wrap(payload)));
            }
        }
        catch (SQLException e) {
            throw new DbException(e);
        }

        return msgs;
    }

    public ClientOutMsgBatch createPublishOutBatch()
    {
        return new ClientOutMsgBatch(storage);
    }

    public void executeBatch(Object handle)
    {
        storage.executeBatch((PreparedStatement) handle);
    }

    public void commit()
    {
        storage.commit();
    }

    public static class ClientOutMsgBatch
    {
        private PreparedStatement clientOutMsg;

        private Storage storage;

        public ClientOutMsgBatch(Storage storage)
        {
            this.storage      = storage;
            this.clientOutMsg = storage.getClientOutMsgStatement();
        }

        public void storeClientOutMsg(String clientId,
                                      String msgId, short packetId, int state)
        {
            storage.storeClientOutMsgBatch(clientOutMsg,
                                           clientId, msgId, packetId, state);
        }

        public void execute()
        {
            try {
                clientOutMsg.executeBatch();
                storage.commit();
            }
            catch (SQLException e) {
                storage.rollback();
                throw new DbException(e);
            }
        }
    }
}
