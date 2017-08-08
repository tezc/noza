package noza.core.msg;

import noza.base.common.Buffer;
import noza.base.common.Util;
import noza.base.exception.MqttException;
import noza.core.client.Client;

public class PubcompMsg extends Msg
{
    public static final String STR        = "PUBCOMP";
    public static final byte TYPE         = 0x07;
    public static final byte HDR_FLAGS    = 0x00;
    public static final int FIXED_HDR_LEN = 0x02;
    public static final int VAR_HDR_LEN   = 0x02;
    public static final byte REMAINING    = 0x02;

    public static final int LENGTH        = FIXED_HDR_LEN + VAR_HDR_LEN;
    
    public int packetId;

    public PubcompMsg(int packetId)
    {
        this.packetId = packetId;
    }

    private PubcompMsg(int hdrLen, int remaining, byte flags)
    {
        super(TYPE, hdrLen, remaining, flags);
    }

    public static PubcompMsg create(int hdrLen, int remaining, byte flags)
    {
        if (hdrLen != PubcompMsg.FIXED_HDR_LEN) {
            throw new MqttException("Header len : " + hdrLen);
        }

        if (remaining != PubcompMsg.REMAINING) {
            throw new MqttException("Remaining : " + remaining);
        }

        if (flags != PubcompMsg.HDR_FLAGS) {
            throw new MqttException("Header flags : " + flags);
        }

        return new PubcompMsg(hdrLen, remaining, flags);
    }

    @Override
    public boolean handle(Client client)
    {
        return client.handlePubcompMsg(this);
    }

    @Override
    public void encode()
    {
        if (!rawReady) {
            if (rawMsg == null) {
                rawMsg = new Buffer(LENGTH);
            }

            rawMsg.clear();

            rawMsg.put((byte) (TYPE << 4 | HDR_FLAGS));
            rawMsg.putRemaining(REMAINING);
            rawMsg.putShort((short) packetId);

            rawMsg.flip();
            rawReady = true;
        }
    }

    @Override
    public void decode()
    {
        rawMsg.advance(hdrLen);
        packetId = rawMsg.getShort();
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
        str.append("\t Packet ID           : ").append(Util.toUnsignedStr(packetId)) .append(nl);
        str.append("\t ----------------------------------------------------")        .append(nl);

        return str.toString();
    }

}
