package noza.core.msg;


import noza.base.common.Buffer;
import noza.base.common.Util;
import noza.base.exception.MqttException;
import noza.core.client.Client;

public class PingreqMsg extends Msg
{
    public static final String STR        = "PINGREQ";

    public static final byte TYPE         = 0x0C;
    public static final byte HDR_FLAGS    = 0x00;

    public static final int FIXED_HDR_LEN = 0x02;
    public static final int VAR_HDR_LEN   = 0x00;
    public static final int REMAINING     = 0x00;
    public static final int LENGTH        = FIXED_HDR_LEN + REMAINING;


    public PingreqMsg()
    {
        super(PingreqMsg.TYPE,
              PingreqMsg.FIXED_HDR_LEN,
              PingreqMsg.REMAINING,
              PingreqMsg.HDR_FLAGS);
    }

    private PingreqMsg(int hdrLen, int remaining, byte flags)
    {
        super(PingreqMsg.TYPE, hdrLen, remaining, flags);
    }

    public static PingreqMsg create(int hdrLen, int remaining, byte flags)
    {
        if (hdrLen != PingreqMsg.FIXED_HDR_LEN) {
            throw new MqttException("Header len : " + hdrLen);
        }

        if (remaining != PingreqMsg.REMAINING) {
            throw new MqttException("Remaining : " + remaining);
        }

        if (flags != PingreqMsg.HDR_FLAGS) {
            throw new MqttException("Header flags : " + flags);
        }

        return new PingreqMsg();
    }

    @Override
    public boolean handle(Client client)
    {
        return client.handlePingreqMsg(this);
    }

    @Override
    public void encode()
    {
        if (!rawReady) {
            if (rawMsg == null) {
                rawMsg = new Buffer(PingreqMsg.LENGTH);
            }

            rawMsg.clear();

            rawMsg.put((byte) (PingreqMsg.TYPE << 4 | PingreqMsg.HDR_FLAGS));
            rawMsg.putRemaining(PingreqMsg.REMAINING);

            rawMsg.flip();
            rawReady = true;
        }
    }

    @Override
    public void decode()
    {
        if (remaining != PingreqMsg.REMAINING) {
            throw new MqttException("Remaining len : " + remaining);
        }

        if (hdrFlags != PingreqMsg.HDR_FLAGS) {
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
