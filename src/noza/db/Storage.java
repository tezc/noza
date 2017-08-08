package noza.db;


import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public interface Storage
{
    void storeClient(String id, String username, int state, boolean cleanSession,
                     String willMsg, String localAddress, String remoteAddress,
                     boolean commit);

    void storeClientState(String id, int state);
    void storeSubscription(String clientId, String topic, int qos);
    void storeMsg(String id, int packetId, String topic, int qos,
                  InputStream data, boolean retain, boolean commit);

    void storeClientInMsg(String clientId, String msgId, int packetId, int state);
    void storeClientInMsgState(String clientId, String msgId, int state);
    void storeClientOutMsg(String clientId, String msgId, int packetId, int state);
    void storeClientOutMsgState(String clientId, String msgId, int state);

    void removeClient(String id, boolean commit);
    void removeSubscription(String clientId, String topic);
    void removeSubscriptions(String clientId, List<String> topics);
    void removeMsg(String msgId);
    void removeClientInMsg(String clientId, String msgId);
    void removeClientOutMsg(String clientId, String msgId);
    void removeClientWillMsg(String clientId);

    ResultSet getClients();
    ResultSet getClient(String clientId);
    ResultSet getClientSubs(String clientId);
    ResultSet getMsg(String msgId);
    ResultSet getClientInMsgs(String clientId);
    ResultSet getClientOutMsgIds(String clientId);
    ResultSet getClientOutMsgs(String clientId);

    PreparedStatement getClientOutMsgStatement();
    PreparedStatement getStoreMsgStatement();
    void storeClientOutMsgBatch(PreparedStatement statement, String clientId,
                                String msgId, int packetId, int state);
    void executeBatch(PreparedStatement statement);
    void commit();
    void rollback();
}
