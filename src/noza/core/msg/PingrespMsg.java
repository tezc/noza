package noza.core.msg;

import noza.base.common.Buffer;
import noza.base.common.Util;
import noza.base.exception.MqttException;
import noza.core.client.Client;


public class PingrespMsg extends Msg
{
    public static final String STR         = "PINGRESP";

    public static final byte TYPE          = 0x0D;
    public static final byte HDR_FLAGS     = 0x00;
    public static final int FIXED_HDR_LEN  = 0x02;
    public static final int VAR_HDR_LEN    = 0x00;
    public static final int REMAINING      = 0x00;
    public static final int LENGTH         = FIXED_HDR_LEN + REMAINING;


    public PingrespMsg()
    {
        super(PingrespMsg.TYPE,
              PingrespMsg.FIXED_HDR_LEN,
              PingrespMsg.REMAINING,
              PingrespMsg.HDR_FLAGS);
    }

    public static PingrespMsg create(int hdrLen, int remaining, byte flags)
    {
        if (hdrLen != PingrespMsg.FIXED_HDR_LEN) {
            throw new MqttException("Header len : " + hdrLen);
        }

        if (remaining != PingrespMsg.REMAINING) {
            throw new MqttException("Remaining : " + remaining);
        }

        if (flags != PingrespMsg.HDR_FLAGS) {
            throw new MqttException("Header flags : " + flags);
        }

        return new PingrespMsg();
    }

    @Override
    public boolean handle(Client client)
    {
        return client.handlePingrespMsg(this);
    }

    @Override
    public void encode()
    {
        if (!rawReady) {
            if (rawMsg == null) {
                rawMsg = new Buffer(PingrespMsg.LENGTH);
            }

            rawMsg.clear();

            rawMsg.put((byte) (PingrespMsg.TYPE << 4 | PingrespMsg.HDR_FLAGS));
            rawMsg.putRemaining(PingrespMsg.REMAINING);

            rawMsg.flip();
            rawReady = true;
        }
    }

    @Override
    public void decode()
    {
        if (remaining != PingrespMsg.REMAINING) {
            throw new MqttException("Remaining len : " + remaining);
        }

        if (hdrFlags != PingrespMsg.HDR_FLAGS) {
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
