package noza.db;

import noza.base.common.Util;
import noza.base.exception.DbException;

import java.io.InputStream;
import java.sql.*;
import java.sql.DriverManager;
import java.util.List;

public class H2Db implements Storage
{
    private static final String DRIVER     = "org.h2.Driver";

    private Connection conn;

    private PreparedStatement storeClient;
    private PreparedStatement storeClientState;
    private PreparedStatement storeSubscription;
    private PreparedStatement storeMsg;
    private PreparedStatement storeClientInMsg;
    private PreparedStatement storeClientInMsgState;
    private PreparedStatement storeClientOutMsg;
    private PreparedStatement storeClientOutMsgState;

    private PreparedStatement removeClient;
    private PreparedStatement removeSubscription;
    private PreparedStatement removeMsg;
    private PreparedStatement removeClientInMsg;
    private PreparedStatement removeClientOutMsg;
    private PreparedStatement removeClientWillMsg;

    private PreparedStatement getClient;
    private PreparedStatement getClientSubs;
    private PreparedStatement getMsg;
    private PreparedStatement getClientInMsgs;
    private PreparedStatement getClientOutMsgIds;
    private PreparedStatement getClientOutMsgs;

    private PreparedStatement getClients;


    public H2Db(String connection, String username, String password)
    {
        conn = getDBConnection(connection, username, password);

        try {
            conn.setAutoCommit(false);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

        prepareStatements();
    }

    public static void load()
    {
        try {
            Class.forName(DRIVER);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    private Connection getDBConnection(String connection,
                                       String username,
                                       String password)
    {
        try {
            return DriverManager.getConnection(connection, username, password);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void prepareStatements()
    {
        try {
            String storeClientSQL            = "MERGE INTO BROKER.CLIENTS" +
                                               "(client_id, username, state, clean_session, will_msg, " +
                                               "local_address, remote_address, latest_timestamp) values" +
                                               "(?, ?, ?, ?, ?, ?, ?, ?)";

            String storeClientStateSQL       = "UPDATE BROKER.CLIENTS " +
                                               "SET state = (?), latest_timestamp = (?) " +
                                               "WHERE client_id = (?)";

            String storeSubsSQL              = "MERGE INTO BROKER.SUBSCRIPTIONS" +
                                               "(client_id, topic, qos) values" +
                                               "(?, ?, ?)";

            String storeMsgSQL               = "INSERT INTO BROKER.MSGS" +
                                               "(msg_id, packet_id, topic, qos, data, retain, timestamp) values" +
                                               "(?, ?, ?, ?, ?, ?, ?)";

            String storeClientInMsgSQL       = "INSERT INTO BROKER.CLIENT_IN_MSGS" +
                                               "(client_id, msg_id, packet_id, state, timestamp) values" +
                                               "(?, ?, ?, ?, ?)";

            String storeClientInMsgStateSQL  = "UPDATE BROKER.CLIENT_IN_MSGS " +
                                               "SET state = (?) WHERE client_id = (?) and msg_id = (?)";

            String storeClientOutMsgSQL      = "INSERT INTO BROKER.CLIENT_OUT_MSGS" +
                                               "(client_id, msg_id, packet_id, state, timestamp) values" +
                                               "(?, ?, ?, ?, ?)";

            String storeClientOutMsgStateSQL = "UPDATE BROKER.CLIENT_OUT_MSGS " +
                                               "SET state = (?) WHERE client_id = (?) and msg_id = (?)";

            String removeClientSQL           = "DELETE FROM BROKER.CLIENTS " +
                                               "WHERE client_id = (?)";

            String removeSubsSQL             = "DELETE FROM BROKER.SUBSCRIPTIONS " +
                                               "WHERE client_id = (?) AND topic = (?)";

            String removeMsgSQL              = "DELETE FROM BROKER.MSGS " +
                                               "WHERE msg_id = (?)";

            String removeClientInMsgSQL      = "DELETE FROM BROKER.CLIENT_IN_MSGS " +
                                               "WHERE client_id = (?) AND msg_id = (?)";

            String removeClientOutMsgSQL     = "DELETE FROM BROKER.CLIENT_OUT_MSGS " +
                                               "WHERE client_id = (?) AND msg_id = (?)";

            String removeClientWillMsgSQL    = "UPDATE BROKER.CLIENTS " +
                                               "SET WILL_MSG = null WHERE client_id = (?)";

            String getClientSQL              = "SELECT * FROM BROKER.CLIENTS " +
                                               "WHERE client_id = (?)";

            String getClientSubsSQL          = "SELECT * FROM BROKER.SUBSCRIPTIONS " +
                                               "WHERE client_id = (?)";

            String getMsgSQL                 = "SELECT * FROM BROKER.MSGS " +
                                               "WHERE msg_id = (?)";

            String getClientInMsgsSQL        = "SELECT * FROM BROKER.CLIENT_IN_MSGS " +
                                               "WHERE client_id = (?) ORDER BY timestamp ASC";

            String getClientOutMsgIdsSQL     = "SELECT BROKER.MSGS.* FROM BROKER.CLIENT_OUT_MSGS " +
                                               "INNER JOIN BROKER.MSGS ON BROKER.MSGS.msg_id = BROKER.CLIENT_OUT_MSGS.msg_id " +
                                               "WHERE client_id = (?) ORDER BY timestamp ASC";


            String getClientOutMsgsSQL       = "SELECT p.msg_id, p.topic, p.qos, p.data, p.retain, c.packet_id, c.state FROM BROKER.MSGS p " +
                                               "JOIN BROKER.CLIENT_OUT_MSGS c ON c.CLIENT_ID=(?) AND p.MSG_ID = c.MSG_ID";

            String getClientsSQL             = "SELECT * FROM BROKER.CLIENTS";


            storeClient            = conn.prepareStatement(storeClientSQL);
            storeClientState       = conn.prepareStatement(storeClientStateSQL);
            storeSubscription      = conn.prepareStatement(storeSubsSQL);
            storeMsg               = conn.prepareStatement(storeMsgSQL);
            storeClientInMsg       = conn.prepareStatement(storeClientInMsgSQL);
            storeClientInMsgState  = conn.prepareStatement(storeClientInMsgStateSQL);
            storeClientOutMsg      = conn.prepareStatement(storeClientOutMsgSQL);
            storeClientOutMsgState = conn.prepareStatement(storeClientOutMsgStateSQL);

            removeClient           = conn.prepareStatement(removeClientSQL);
            removeSubscription     = conn.prepareStatement(removeSubsSQL);
            removeMsg              = conn.prepareStatement(removeMsgSQL);
            removeClientInMsg      = conn.prepareStatement(removeClientInMsgSQL);
            removeClientOutMsg     = conn.prepareStatement(removeClientOutMsgSQL);
            removeClientWillMsg    = conn.prepareStatement(removeClientWillMsgSQL);

            getClient              = conn.prepareStatement(getClientSQL);
            getClientSubs          = conn.prepareStatement(getClientSubsSQL);
            getMsg                 = conn.prepareStatement(getMsgSQL);
            getClientInMsgs        = conn.prepareStatement(getClientInMsgsSQL);
            getClientOutMsgIds     = conn.prepareStatement(getClientOutMsgIdsSQL);
            getClientOutMsgs       = conn.prepareStatement(getClientOutMsgsSQL);

            getClients             = conn.prepareStatement(getClientsSQL);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void storeClient(String clientId, String username,
                            int state, boolean cleanSession,
                            String willMsg, String localAddress,
                            String remoteAddress, boolean commit)
    {
        try {
            storeClient.setString(1, clientId);
            storeClient.setString(2, username);
            storeClient.setInt(3, state);
            storeClient.setBoolean(4, cleanSession);
            storeClient.setString(5, willMsg);
            storeClient.setString(6, localAddress);
            storeClient.setString(7, remoteAddress);
            storeClient.setTimestamp(8, Util.now());
            storeClient.execute();

            if (commit) {
                conn.commit();
            }
        }
        catch (SQLException e) {
            try {
                conn.rollback();
                throw new DbException(e);
            }
            catch (SQLException e1) {
                throw new DbException(e);
            }
        }
    }

    @Override
    public void storeClientState(String clientId, int state)
    {
        try {
            storeClientState.setInt(1, state);
            storeClientState.setTimestamp(2, Util.now());
            storeClientState.setString(3, clientId);
            storeClientState.execute();

            conn.commit();
        }
        catch (SQLException e) {
            try {
                conn.rollback();
                throw new DbException(e);
            }
            catch (SQLException e1) {
                throw new DbException(e);
            }
        }
    }

    @Override
    public void storeSubscription(String clientId, String topic, int qos)
    {
        try{
            storeSubscription.setString(1, clientId);
            storeSubscription.setString(2, topic);
            storeSubscription.setInt(3, qos);
            storeSubscription.execute();

            conn.commit();
        }
        catch (SQLException e) {
            try {
                conn.rollback();
                throw new DbException(e);
            }
            catch (SQLException e1) {
                throw new DbException(e);
            }
        }
    }

    @Override
    public void storeMsg(String id,
                         int packetId,
                         String topic,
                         int qos,
                         InputStream data,
                         boolean retain,
                         boolean commit)
    {
        try{
            storeMsg.setString(1, id);
            storeMsg.setInt(2, packetId);
            storeMsg.setString(3, topic);
            storeMsg.setInt(4, qos);
            storeMsg.setBinaryStream(5, data);
            storeMsg.setBoolean(6, retain);
            storeMsg.setTimestamp(7, Util.now());
            storeMsg.execute();

            if (commit) {
                conn.commit();
            }
        }
        catch (SQLException e) {
            try {
                conn.rollback();
                throw new DbException(e);
            }
            catch (SQLException e1) {
                throw new DbException(e);
            }
        }
    }

    @Override
    public void storeClientInMsg(String clientId, String msgId, int packetId, int state)
    {
        try {
            storeClientInMsg.setString(1, clientId);
            storeClientInMsg.setString(2, msgId);
            storeClientInMsg.setInt(3, packetId);
            storeClientInMsg.setInt(4, state);
            storeClientInMsg.setTimestamp(5, Util.now());
            storeClientInMsg.execute();

            conn.commit();
        }
        catch (SQLException e) {
            try {
                conn.rollback();
                throw new DbException(e);
            }
            catch (SQLException e1) {
                throw new DbException(e);
            }
        }
    }

    @Override
    public void storeClientInMsgState(String clientId, String msgId, int state)
    {
        try {
            storeClientInMsgState.setInt(1, state);
            storeClientInMsgState.setString(2, clientId);
            storeClientInMsgState.setString(3, msgId);
            storeClientInMsgState.execute();

            conn.commit();
        }
        catch (SQLException e) {
            try {
                conn.rollback();
                throw new DbException(e);
            }
            catch (SQLException e1) {
                throw new DbException(e);
            }
        }
    }

    @Override
    public void storeClientOutMsg(String clientId, String msgId, int packetId, int state)
    {
        try {
            storeClientOutMsg.setString(1, clientId);
            storeClientOutMsg.setString(2, msgId);
            storeClientOutMsg.setInt(3, packetId);
            storeClientOutMsg.setInt(4, state);
            storeClientOutMsg.setTimestamp(5, Util.now());
            storeClientOutMsg.execute();

            conn.commit();
        }
        catch (SQLException e) {
            try {
                conn.rollback();
                throw new DbException(e);
            }
            catch (SQLException e1) {
                throw new DbException(e);
            }
        }
    }

    @Override
    public void storeClientOutMsgState(String clientId, String msgId, int state)
    {
        try {
            storeClientOutMsgState.setInt(1, state);
            storeClientOutMsgState.setString(2, clientId);
            storeClientOutMsgState.setString(3, msgId);
            storeClientOutMsgState.execute();

            conn.commit();
        }
        catch (SQLException e) {
            try {
                conn.rollback();
                throw new DbException(e);
            }
            catch (SQLException e1) {
                throw new DbException(e);
            }
        }
    }

    @Override
    public void removeClient(String id, boolean commit)
    {
        try {
            removeClient.setString(1, id);
            removeClient.execute();

            if (commit) {
                conn.commit();
            }
        }
        catch (SQLException e) {
            try {
                conn.rollback();
                throw new DbException(e);
            }
            catch (SQLException e1) {
                throw new DbException(e);
            }
        }
    }

    @Override
    public void removeSubscription(String clientId, String topic)
    {
        try {
            removeSubscription.setString(1, clientId);
            removeSubscription.setString(2, topic);
            removeSubscription.execute();

            conn.commit();
        }
        catch (SQLException e) {
            try {
                conn.rollback();
                throw new DbException(e);
            }
            catch (SQLException e1) {
                throw new DbException(e);
            }
        }
    }

    @Override
    public void removeSubscriptions(String clientId, List<String> topics)
    {
        try {
            for (String topic : topics) {
                removeSubscription.setString(1, clientId);
                removeSubscription.setString(2, topic);
                removeSubscription.addBatch();
            }

            removeSubscription.executeBatch();
            conn.commit();
        }
        catch (SQLException e) {
            try {
                conn.rollback();
                throw new DbException(e);
            }
            catch (SQLException e1) {
                throw new DbException(e);
            }
        }
    }

    @Override
    public void removeMsg(String msgId)
    {
        try {
            removeMsg.setString(1, msgId);
            removeMsg.execute();

            conn.commit();
        }
        catch (SQLException e) {
            try {
                conn.rollback();
                throw new DbException(e);
            }
            catch (SQLException e1) {
                throw new DbException(e);
            }
        }
    }

    @Override
    public void removeClientInMsg(String clientId, String msgId)
    {
        try {
            removeClientInMsg.setString(1, clientId);
            removeClientInMsg.setString(2, msgId);
            removeClientInMsg.execute();

            conn.commit();
        }
        catch (SQLException e) {
            try {
                conn.rollback();
                throw new DbException(e);
            }
            catch (SQLException e1) {
                throw new DbException(e);
            }
        }
    }

    @Override
    public void removeClientOutMsg(String clientId, String msgId)
    {
        try {
            removeClientOutMsg.setString(1, clientId);
            removeClientOutMsg.setString(2, msgId);
            removeClientOutMsg.execute();

            conn.commit();
        }
        catch (SQLException e) {
            try {
                conn.rollback();
                throw new DbException(e);
            }
            catch (SQLException e1) {
                throw new DbException(e);
            }
        }
    }

    @Override
    public void removeClientWillMsg(String clientId)
    {
        try {
            removeClientWillMsg.setString(1, clientId);
            removeClientWillMsg.execute();

            conn.commit();
        }
        catch (SQLException e) {
            try {
                conn.rollback();
                throw new DbException(e);
            }
            catch (SQLException e1) {
                throw new DbException(e);
            }
        }
    }

    @Override
    public ResultSet getClients()
    {
        try {
            getClients.execute();
            conn.commit();

            return getClients.getResultSet();

        }
        catch (SQLException e) {
            try {
                conn.rollback();
                throw new DbException(e);
            }
            catch (SQLException e1) {
                throw new DbException(e);
            }
        }
    }

    @Override
    public ResultSet getClient(String clientId)
    {
        try {
            getClient.setString(1, clientId);
            getClient.execute();

            conn.commit();

            return getClient.getResultSet();

        }
        catch (SQLException e) {
            try {
                conn.rollback();
                throw new DbException(e);
            }
            catch (SQLException e1) {
                throw new DbException(e);
            }
        }
    }

    @Override
    public ResultSet getClientSubs(String clientId)
    {
        try {
            getClientSubs.setString(1, clientId);
            getClientSubs.execute();

            conn.commit();

            return getClientSubs.getResultSet();

        }
        catch (SQLException e) {
            try {
                conn.rollback();
                throw new DbException(e);
            }
            catch (SQLException e1) {
                throw new DbException(e);
            }
        }
    }

    @Override
    public ResultSet getMsg(String msgId)
    {
        try {
            getMsg.setString(1, msgId);
            getMsg.execute();

            conn.commit();

            return getMsg.getResultSet();

        }
        catch (SQLException e) {
            try {
                conn.rollback();
                throw new DbException(e);
            }
            catch (SQLException e1) {
                throw new DbException(e);
            }
        }
    }

    @Override
    public ResultSet getClientInMsgs(String clientId)
    {
        try {
            getClientInMsgs.setString(1, clientId);
            getClientInMsgs.execute();

            conn.commit();

            return getClientInMsgs.getResultSet();

        }
        catch (SQLException e) {
            try {
                conn.rollback();
                throw new DbException(e);
            }
            catch (SQLException e1) {
                throw new DbException(e);
            }
        }
    }

    @Override
    public ResultSet getClientOutMsgIds(String clientId)
    {
        try {
            getClientOutMsgIds.setString(1, clientId);
            getClientOutMsgIds.execute();

            conn.commit();

            return getClientOutMsgIds.getResultSet();

        }
        catch (SQLException e) {
            try {
                conn.rollback();
                throw new DbException(e);
            }
            catch (SQLException e1) {
                throw new DbException(e);
            }
        }
    }

    @Override
    public ResultSet getClientOutMsgs(String clientId)
    {
        try {
            getClientOutMsgs.setString(1, clientId);
            getClientOutMsgs.execute();

            conn.commit();

            return getClientOutMsgs.getResultSet();

        }
        catch (SQLException e) {
            try {
                conn.rollback();
                throw new DbException(e);
            }
            catch (SQLException e1) {
                throw new DbException(e);
            }
        }
    }

    @Override
    public PreparedStatement getClientOutMsgStatement()
    {
        return storeClientOutMsg;
    }

    @Override
    public PreparedStatement getStoreMsgStatement()
    {
        return storeMsg;
    }

    @Override
    public void storeClientOutMsgBatch(PreparedStatement storeClientOutMsg,
                                       String clientId,
                                       String msgId,
                                       int packetId,
                                       int state)
    {
        try {
            storeClientOutMsg.setString(1, clientId);
            storeClientOutMsg.setString(2, msgId);
            storeClientOutMsg.setInt(3, packetId);
            storeClientOutMsg.setInt(4, state);
            storeClientOutMsg.setTimestamp(5, Util.now());
            storeClientOutMsg.addBatch();
        }
        catch (SQLException e) {
            try {
                conn.rollback();
                throw new DbException(e);
            }
            catch (SQLException e1) {
                throw new DbException(e);
            }
        }
    }

    @Override
    public void executeBatch(PreparedStatement statement)
    {
        try {
            statement.executeBatch();
        }
        catch (SQLException e) {
            try {
                conn.rollback();
                throw new DbException(e);
            }
            catch (SQLException e1) {
                throw new DbException(e);
            }
        }
    }

    @Override
    public void commit()
    {
        try {
            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback();
                throw new DbException(e);
            } catch (SQLException e1) {
                throw new DbException(e);
            }
        }
    }

    @Override
    public void rollback()
    {
        try {
            conn.rollback();
        }
        catch (SQLException e) {
            throw new DbException(e);
        }
    }
}
