package noza.core.msg;



import noza.base.common.Buffer;
import noza.base.exception.MqttException;
import noza.core.client.Client;

import java.nio.ByteBuffer;

public abstract class Msg
{
    public static final int STR_SIZELEN       = 2;
    public static final int PACKET_ID_LEN     = 2;
    public static final int QOS_BYTE_LEN      = 1;
    public static final int MIN_MSG_LEN       = 2;
    public static final int MAX_FIXED_HDR_LEN = 5;
    public static final int QOS0              = 0x00;
    public static final int QOS1              = 0x01;
    public static final int QOS2              = 0x02;
    public static final int QOS_FAIL          = 0x80;

    protected int hdrType;
    protected int hdrLen;
    protected int remaining;
    protected byte hdrFlags;
    protected Buffer rawMsg;
    protected boolean rawReady;

    public Msg()
    {

    }

    public Msg(int hdrType, int hdrLen, int remaining, byte hdrFlags)
    {
        this.hdrType   = hdrType;
        this.hdrLen    = hdrLen;
        this.remaining = remaining;
        this.hdrFlags  = hdrFlags;
        this.rawReady  = false;
        this.rawMsg    = new Buffer(hdrLen + remaining);
    }

    public int needed()
    {
        return rawMsg.remaining();
    }
    
    public boolean written()
    {
        return rawMsg.remaining() == 0;
    }

    public boolean ready()
    {
        return rawReady;
    }

    public void read(Buffer buf)
    {
        if (!rawReady) {
            rawMsg.put(buf);
        }
    }

    public void read(ByteBuffer buf)
    {
        rawMsg.put(buf);

        if (rawMsg.remaining() == 0) {
            rawMsg.flip();
            rawReady = true;
        }
    }

    public abstract boolean handle(Client client);
    public abstract void encode();
    public abstract void decode();

    public void writeTo(ByteBuffer buf)
    {
        rawMsg.get(buf);
    }

    static int lengthOf(int len)
    {
        if (len < 0 || len > 268435455) {
            throw new MqttException("Invalid remaining len : " + len);
        }

        if (len < 128)  {
            return 1;
        }
        else if (len < 16384) {
            return 2;
        }
        else if (len < 2097152) {
            return 3;
        }
        else {
            return 4;
        }
    }
}
