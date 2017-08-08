package noza.core.msg;

import noza.base.common.Buffer;
import noza.base.common.Util;
import noza.base.exception.MqttException;
import noza.core.client.Client;


public class ConnackMsg extends Msg
{
    public static final String STR                         = "CONNACK";
    public static final byte TYPE                          = 0x02;
    public static final byte HDR_FLAGS                     = 0x00;
    public static final int FIXED_HDR_LEN                  = 0x02;
    public static final int VAR_HDR_LEN                    = 0x02;
    public static final int REMAINING                      = 0x02;
    public static final int LENGTH                         = FIXED_HDR_LEN + VAR_HDR_LEN;

    public static final int SESSION_PRESENT                = 0x01;

    public static final byte ACCEPTED                      = 0x00;
    public static final byte REFUSED_UNACCEPTABLE_PROTOCOL = 0x01;
    public static final byte REFUSED_ID_REJECTED           = 0x02;
    public static final byte REFUSED_SERVER_UNAVAILABLE    = 0x03;
    public static final byte REFUSED_USERNAME_PASSWORD     = 0x04;
    public static final byte REFUSED_NOT_AUTHORIZED        = 0x05;

    private static final String returnCodeStr[] = {
            "ACCEPTED",
            "REFUSED_UNACCEPTABLE_PROTOCOL",
            "REFUSED_ID_REJECTED",
            "REFUSED_SERVER_UNAVAILABLE",
            "REFUSED_USERNAME_PASSWORD",
            "REFUSED_NOT_AUTHORIZED"
    };


    private byte returnCode;
    private boolean sessionPresent;


    public ConnackMsg(byte returnCode, boolean sessionPresent)
    {
        super(ConnackMsg.TYPE,
              ConnackMsg.FIXED_HDR_LEN,
              ConnackMsg.REMAINING,
              ConnackMsg.HDR_FLAGS);

        this.returnCode = returnCode;
        this.sessionPresent = sessionPresent;
    }

    private ConnackMsg(int hdrLen, int remaining, byte flags)
    {
        super(ConnackMsg.TYPE, hdrLen, remaining, flags);
    }

    public static ConnackMsg create(int hdrLen, int remaining, byte flags)
    {
        if (hdrLen != ConnackMsg.FIXED_HDR_LEN) {
            throw new MqttException("Header len : " + hdrLen);
        }

        if (remaining != ConnackMsg.REMAINING) {
            throw new MqttException("Remaining : " + remaining);
        }

        if (flags != ConnackMsg.HDR_FLAGS) {
            throw new MqttException("Header flags : " + flags);
        }

        return new ConnackMsg(hdrLen, remaining, flags);
    }

    @Override
    public boolean handle(Client client)
    {
        return client.handleConnackMsg(this);
    }

    @Override
    public void encode()
    {
        if (!rawReady) {
            if (rawMsg == null) {
                rawMsg = new Buffer(ConnackMsg.LENGTH);
            }

            rawMsg.clear();

            rawMsg.put((byte) ((ConnackMsg.TYPE << 4) | ConnackMsg.HDR_FLAGS));
            rawMsg.putRemaining(ConnackMsg.REMAINING);
            rawMsg.put((byte) (sessionPresent ? 1 : 0));
            rawMsg.put(returnCode);

            rawMsg.flip();
            rawReady = true;
        }
    }

    @Override
    public void decode()
    {
        rawMsg.advance(hdrLen);
        
        returnCode     = rawMsg.get();
        sessionPresent = rawMsg.get() != 0;
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
        str.append("\t Return Code         : ").append(returnCodeStr[returnCode])    .append(nl);
        str.append("\t Session Present     : ").append(sessionPresent)               .append(nl);
        str.append("\t ----------------------------------------------------")        .append(nl);

        return str.toString();
    }
}
