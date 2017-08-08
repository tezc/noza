package noza.core.msg;


import noza.base.common.Buffer;
import noza.base.common.Util;
import noza.api.msgs.Connect;
import noza.api.msgs.Publish;
import noza.base.exception.MqttException;
import noza.core.Mqtt;
import noza.core.client.Client;

import java.nio.ByteBuffer;

public class ConnectMsg extends Msg implements Connect
{
    public static final String STR          = "CONNECT";
    public static final byte TYPE           = 0x01;
    public static final byte HDR_FLAGS      = 0x00;
    public static final byte RESERVED_FLAGS = 0x01;

    public static final int VAR_HDR_LEN     = 0x0A;

    public static final int CLEAN_SESSION   = 0x02;
    public static final int WILL            = 0x04;
    public static final int WILL_QOS        = 0x18;
    public static final int WILL_RETAIN     = 0x20;
    public static final int PASSWORD        = 0x40;
    public static final int USERNAME        = 0x80;

    public static final int WILL_QOS_OFFSET = 3;

    public byte connectFlags;
    public byte protoLevel;
    public short keepAlive;

    public boolean cleanSession;
    public boolean usernamePresent;
    public boolean passwordPresent;
    public boolean willPresent;

    public String clientId;
    public String username;
    public String password;

    public PublishMsg willMessage;

    private ConnectMsg(int hdrLen, int remaining, byte flags)
    {
        super(ConnectMsg.TYPE, hdrLen, remaining, flags);

        usernamePresent = false;
        passwordPresent = false;
        willPresent     = false;
        cleanSession    = false;
    }

    public static ConnectMsg create(int hdrLen, int remaining, byte flags)
    {
        return new ConnectMsg(hdrLen, remaining, flags);
    }

    @Override
    public byte getConnectFlags()
    {
        return connectFlags;
    }

    @Override
    public byte getProtoLevel()
    {
        return protoLevel;
    }

    @Override
    public short getKeepAlive()
    {
        return keepAlive;
    }

    @Override
    public boolean isCleanSession()
    {
        return cleanSession;
    }

    @Override
    public boolean isUsernamePresent()
    {
        return usernamePresent;
    }

    @Override
    public boolean isPasswordPresent()
    {
        return passwordPresent;
    }

    @Override
    public boolean isWillPresent()
    {
        return willPresent;
    }

    public String getClientId()
    {
        return clientId;
    }

    @Override
    public String getUsername()
    {
        return username;
    }

    @Override
    public String getPassword()
    {
        return password;
    }

    @Override
    public Publish getWillMessage()
    {
        return willMessage;
    }

    @Override
    public boolean handle(Client client)
    {
        return client.handleConnectMsg(this);
    }

    @Override
    public void encode()
    {
        if (!rawReady) {
            remaining = Mqtt.STR_SIZELEN + Mqtt.MQTT_STR_LEN +                                          // "MQTT" STR
                        1 +                                                                             // Protocol Level
                        1 +                                                                             // Connect Flags
                        2 +                                                                             // Keep Alive
                        Mqtt.STR_SIZELEN + clientId.length() +                                          // Client ID
                       (willPresent ? Mqtt.STR_SIZELEN + willMessage.getTopic().length()  +             // Will Topic
                                      Mqtt.STR_SIZELEN + willMessage.getPayload().remaining() : 0) +    // Will Message
                       (usernamePresent ? Mqtt.STR_SIZELEN + username.length() : 0) +                       // Username
                       (passwordPresent ? Mqtt.STR_SIZELEN + password.length() : 0);                        // Password

            hdrLen = 1 + lengthOf(remaining);

            if (rawMsg == null) {
                rawMsg = new Buffer(hdrLen + remaining);
            }

            rawMsg.clear();

            rawMsg.put((byte) (ConnectMsg.TYPE << 4 | ConnectMsg.HDR_FLAGS));
            rawMsg.putRemaining(remaining);

            rawMsg.putString(Mqtt.MQTT_STR);
            rawMsg.put(Mqtt.PROTOCOL_LEVEL);
            rawMsg.put(connectFlags);
            rawMsg.putShort(keepAlive);
            rawMsg.putString(clientId);

            if (willPresent) {
                rawMsg.putString(willMessage.getTopic());
                rawMsg.put(willMessage.getPayload());
            }

            if (usernamePresent) {
                rawMsg.putString(username);
            }

            if (passwordPresent) {
                rawMsg.putString(password);
            }

            rawMsg.flip();
            rawReady = true;
        }
    }

    @Override
    public void decode()
    {
        rawMsg.advance(hdrLen);

        if (hdrFlags != ConnectMsg.HDR_FLAGS) {
            throw new MqttException("Invalid CONNECT flags : " + (hdrFlags >> 4));
        }

        String mqttStr = rawMsg.getString();
        if (!mqttStr.equals(Mqtt.MQTT_STR)) {
            throw new MqttException("Invalid MQTT str : " + mqttStr);
        }

        protoLevel = rawMsg.get();
        if (protoLevel != Mqtt.PROTOCOL_LEVEL) {
            throw new MqttException("Unsupported MQTT level : " + protoLevel);
        }

        connectFlags = rawMsg.get();
        if ((connectFlags & ConnectMsg.RESERVED_FLAGS) != 0) {
            throw new MqttException("CONNECT reserved flag is set : " + connectFlags);
        }

        if ((connectFlags & ConnectMsg.WILL) == 0) {
            if ((connectFlags & ConnectMsg.WILL_QOS   ) != 0 ||
                (connectFlags & ConnectMsg.WILL_RETAIN) != 0) {
                throw new MqttException("Inconsistent CONNECT flags : " + connectFlags);
            }
        }

        keepAlive = rawMsg.getShort();
        clientId  = rawMsg.getString();

        if ((connectFlags & ConnectMsg.WILL) != 0) {
            willPresent = true;

            String willTopic = rawMsg.getString();
            ByteBuffer willData = ByteBuffer.wrap(rawMsg.getString().getBytes());

            boolean willRetain = false;

            if ((connectFlags & ConnectMsg.WILL_RETAIN) !=0) {
                willRetain = true;
            }

            int willQos = (connectFlags & ConnectMsg.WILL_QOS) >> WILL_QOS_OFFSET;

            willMessage = new PublishMsg(null,
                                         false,
                                         0,
                                         PublishMsg.QUEUED,
                                         willQos,
                                         willRetain,
                                         false,
                                         willTopic,
                                         willData);
        }

        if ((connectFlags & ConnectMsg.USERNAME) != 0) {
            usernamePresent = true;
            username = rawMsg.getString();
        }

        if ((connectFlags & ConnectMsg.PASSWORD) != 0) {
            passwordPresent = true;
            password = rawMsg.getString();
        }

        if ((connectFlags & ConnectMsg.CLEAN_SESSION) !=0 ) {
            cleanSession = true;
        }
    }

    @Override
    public String toString()
    {
        String nl = Util.newLine();
        int willLen = (willPresent) ? willMessage.getPayload().remaining() : 0;

        StringBuilder str = new StringBuilder(512);

        str.append(nl);
        str.append("\t ----------------------------------------------------")          .append(nl);
        str.append("\t Message Type        : ").append(STR)                            .append(nl);
        str.append("\t Total Length        : ").append(hdrLen + remaining)             .append(nl);
        str.append("\t Remaining Length    : ").append(Util.toUnsignedStr(remaining))  .append(nl);
        str.append("\t Header Flags        : ").append(Util.byteToBinary(hdrFlags))    .append(nl);
        str.append("\t Protocol Level      : ").append(Util.byteToStr(protoLevel))     .append(nl);
        str.append("\t Client ID           : ").append(clientId)                       .append(nl);
        str.append("\t Connect Flags       : ").append(Util.byteToBinary(connectFlags)).append(nl);
        str.append("\t Flag Will           : ").append(willPresent)                        .append(nl);

        if (willMessage != null) {
            str.append("\t Flag Will QOS       : ").append(willMessage.getQos())       .append(nl);
            str.append("\t Flag Will Retain    : ").append(willMessage.isRetained())     .append(nl);
            str.append("\t WILL TOPIC          : ").append(willMessage.getTopic())     .append(nl);
            str.append("\t WILL MESSAGE        : ").append(willLen).append(" bytes.")  .append(nl);
        }

        str.append("\t Flag Username       : ").append(usernamePresent)                    .append(nl);
        str.append("\t Flag Password       : ").append(passwordPresent)                    .append(nl);
        str.append("\t Flag Clean Session  : ").append(cleanSession)                   .append(nl);
        str.append("\t Keep Alive          : ").append(Util.shortToStr(keepAlive))     .append(nl);

        str.append("\t Username            : ").append(username)                       .append(nl);
        str.append("\t Password            : ").append(password)                       .append(nl);
        str.append("\t ----------------------------------------------------")          .append(nl);

        return str.toString();
    }
}
