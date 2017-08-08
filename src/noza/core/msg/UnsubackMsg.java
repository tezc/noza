package noza.core.msg;

import noza.base.common.Buffer;
import noza.base.common.Util;
import noza.core.client.Client;


public class UnsubackMsg extends Msg
{
    public static final String STR       = "UNSUBACK";
    public static final byte TYPE         = 0x0B;
    public static final byte HDR_FLAGS    = 0x00;
    public static final int FIXED_HDR_LEN = 0x02;
    public static final int VAR_HDR_LEN   = 0x02;
    public static final byte REMANING     = 0x02;

    public static final int LENGTH        = FIXED_HDR_LEN + VAR_HDR_LEN;

    public short packetId;

    public UnsubackMsg(int hdrLen, int remaining, byte flags)
    {
        super(TYPE, hdrLen, remaining, flags);
    }

    public UnsubackMsg(short packetId)
    {
        this.packetId = packetId;
    }

    public static UnsubackMsg create(int hdrLen, int remaining, byte flags)
    {
        return new UnsubackMsg(hdrLen, remaining, flags);
    }

    @Override
    public void encode()
    {
        if (!rawReady) {

            remaining = REMANING;
            hdrLen    = FIXED_HDR_LEN;

            if (rawMsg == null) {
                rawMsg = new Buffer(LENGTH);
            }

            rawMsg.clear();

            rawMsg.put((byte) (TYPE << 4 | HDR_FLAGS));
            rawMsg.putRemaining(REMANING);
            rawMsg.putShort(packetId);

            rawMsg.flip();
            rawReady = true;
        }
    }

    @Override
    public void decode()
    {
        rawMsg.advance(hdrLen);

        if (remaining != REMANING) {
            throw new IllegalArgumentException("Malformed UNSUBACK msg");
        }

        if (hdrFlags != HDR_FLAGS) {
            throw new IllegalArgumentException("Wrong UNSUBACK flags : " + hdrFlags);
        }

        packetId = rawMsg.getShort();
    }

    @Override
    public boolean handle(Client client)
    {
        return client.handleUnsubackMsg(this);
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
        str.append("\t Packet Id           : ").append(packetId)                     .append(nl);
        str.append("\t ----------------------------------------------------")        .append(nl);

        return str.toString();
    }
}
