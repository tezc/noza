package noza.core.msg;

import noza.base.common.Buffer;
import noza.base.common.Util;
import noza.base.exception.MqttException;
import noza.core.client.Client;

public class PubackMsg extends Msg
{
    public static final String STR        = "PUBACK";
    public static final byte TYPE         = 0x04;
    public static final byte HDR_FLAGS    = 0x00;
    public static final int FIXED_HDR_LEN = 0x02;
    public static final int VAR_HDR_LEN   = 0x02;
    public static final byte REMAINING    = 0x02;

    public static final int LENGTH        = FIXED_HDR_LEN + REMAINING;

    public int packetId;

    public PubackMsg(int packetId)
    {
        this.packetId = packetId;
    }

    private PubackMsg(int hdrLen, int remaining, byte flags)
    {
        super(PubackMsg.TYPE, hdrLen, remaining, flags);
    }

    public static PubackMsg create(int hdrLen, int remaining, byte flags)
    {
        if (hdrLen != PubackMsg.FIXED_HDR_LEN) {
            throw new MqttException("Header len : " + hdrLen);
        }

        if (remaining != PubackMsg.REMAINING) {
            throw new MqttException("Remaining : " + remaining);
        }

        if (flags != PubackMsg.HDR_FLAGS) {
            throw new MqttException("Header flags : " + flags);
        }

        return new PubackMsg(hdrLen, remaining, flags);
    }

    @Override
    public boolean handle(Client client)
    {
        return client.handlePubackMsg(this);
    }

    @Override
    public void encode()
    {
        if (!rawReady) {
            if (rawMsg == null) {
                rawMsg = new Buffer(PubackMsg.LENGTH);
            }

            rawMsg.clear();

            rawMsg.put((byte) (PubackMsg.TYPE << 4 | PubackMsg.HDR_FLAGS));
            rawMsg.putRemaining(PubackMsg.REMAINING);
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
