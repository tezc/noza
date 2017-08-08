package noza.core.msg;


import noza.base.common.Buffer;
import noza.base.common.Util;
import noza.base.exception.MqttException;
import noza.core.client.Client;


public class DisconnectMsg extends Msg
{
    public static final String STR            = "DISCONNECT";
    public static final byte TYPE             = 0x0E;
    public static final byte HDR_FLAGS        = 0x00;
    public static final int FIXED_HDR_LEN     = 0x02;
    public static final int VAR_HDR_LEN       = 0x00;
    public static final int REMAINING         = 0x00;
    public static final int LENGTH            = FIXED_HDR_LEN + VAR_HDR_LEN;

    public static final DisconnectMsg disconnect = new DisconnectMsg();


    private DisconnectMsg()
    {
        super(DisconnectMsg.TYPE,
              DisconnectMsg.FIXED_HDR_LEN,
              DisconnectMsg.REMAINING,
              DisconnectMsg.HDR_FLAGS);
    }

    public DisconnectMsg(int hdrLen, int remaining, byte flags)
    {
        super(DisconnectMsg.TYPE, hdrLen, remaining, flags);
    }

    public static DisconnectMsg create(int hdrlen, int remaining, byte flags)
    {
        if (hdrlen != DisconnectMsg.FIXED_HDR_LEN) {
            throw new MqttException("Header len : " + hdrlen);
        }

        if (remaining != DisconnectMsg.REMAINING) {
            throw new MqttException("Remaining len : " + remaining);
        }

        if (flags != DisconnectMsg.HDR_FLAGS) {
            throw new MqttException("Header flags : " + flags);
        }

        return disconnect;
    }

    @Override
    public boolean handle(Client client)
    {
        return client.handleDisconnectMsg(this);
    }

    @Override
    public void encode()
    {
        if (!rawReady) {
            if (rawMsg == null) {
                rawMsg = new Buffer(DisconnectMsg.LENGTH);
            }

            rawMsg.clear();

            rawMsg.put((byte) (DisconnectMsg.TYPE << 4 | DisconnectMsg.HDR_FLAGS));
            rawMsg.putRemaining(DisconnectMsg.REMAINING);

            rawMsg.flip();
            rawReady = true;
        }
    }

    @Override
    public void decode()
    {
        if (remaining != DisconnectMsg.REMAINING) {
            throw new MqttException("Remaining len : " + remaining);
        }

        if (hdrFlags != DisconnectMsg.HDR_FLAGS) {
            throw new MqttException("Header flags : " + hdrFlags);
        }
    }

    @Override
    public String toString()
    {
        String nl = Util.newLine();
        StringBuilder str = new StringBuilder(256);

        str.append(nl);
        str.append("\t ----------------------------------------------------")        .append(nl);
        str.append("\t Message Type        : ").append(STR)                          .append(nl);
        str.append("\t Total Length        : ").append(hdrLen + remaining)           .append(nl);
        str.append("\t Remaining Length    : ").append(Util.toUnsignedStr(remaining)).append(nl);
        str.append("\t Header Flags        : ").append(Util.byteToBinary(hdrFlags))  .append(nl);
        str.append("\t ----------------------------------------------------")        .append(nl);

        return str.toString();
    }
}
