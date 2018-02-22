package noza.core.msg;

import noza.base.common.Buffer;
import noza.base.common.Util;
import noza.core.worker.Worker;
import noza.api.msgs.Publish;
import noza.core.Mqtt;
import noza.core.client.Client;

import java.nio.ByteBuffer;
import java.util.UUID;


public class PublishMsg extends Msg implements Publish
{
    public static final String STR     = "PUBLISH";
    public static final int TYPE       = 0x03;

    public static final int RETAIN     = 0x01;
    public static final int QOS_FLAGS  = 0x06;
    public static final int DUP        = 0x08;

    public static final int QUEUED           = 0;
    public static final int WAIT_FOR_PUBACK  = 1;
    public static final int WAIT_FOR_PUBREL  = 2;
    public static final int WAIT_FOR_PUBREC  = 3;
    public static final int WAIT_FOR_PUBCOMP = 4;

    public int packetId;
    public int state;
    public String topic;
    public ByteBuffer payload;
    public byte qos;
    public boolean retained;
    public boolean dup;

    private String id;
    private boolean stored;
    private Worker worker;


    public PublishMsg(String id, boolean stored, int packetId, int state, int qos,
                      boolean retained, boolean dup,
                      String topic, ByteBuffer payload)
    {
        super(TYPE, 0, 0, (byte) 0);

        if (id == null) {
            id = UUID.randomUUID().toString();
        }

        this.id              = id;
        this.stored          = stored;
        this.packetId        = packetId;
        this.state           = state;
        this.qos             = (byte) qos;
        this.retained        = retained;
        this.dup             = dup;
        this.topic           = topic;
        this.payload         = payload;
        this.rawMsg          = null;
        this.packetId        = 1;
    }

    public PublishMsg(PublishMsg publish)
    {
        super(TYPE, 0, 0, (byte) 0);

        id             = publish.id;
        stored         = publish.stored;
        packetId       = publish.packetId;
        topic          = publish.topic;
        payload        = publish.payload;
        qos            = publish.qos;
        retained       = publish.retained;
        dup            = publish.dup;
        hdrType        = publish.hdrType;
        hdrLen         = publish.hdrLen;
        remaining      = publish.remaining;
        hdrFlags       = publish.hdrFlags;
        rawReady       = publish.rawReady;
        packetId       = publish.packetId;

        if (rawReady) {
            rawMsg = new Buffer(publish.rawMsg);
        }
    }

    public PublishMsg(int hdrLen, int remaining, byte flags)
    {
        super(TYPE, hdrLen, remaining, flags);

        id = UUID.randomUUID().toString();
    }

    public void setStored()
    {
        stored = true;
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public int getPacketId()
    {
        return packetId;
    }

    @Override
    public String getTopic()
    {
        return topic;
    }

    @Override
    public ByteBuffer getPayload()
    {
        return payload;
    }

    @Override
    public byte getQos()
    {
        return qos;
    }

    @Override
    public boolean isRetained()
    {
        return retained;
    }

    @Override
    public boolean isDup()
    {
        return dup;
    }

    public boolean isStored()
    {
        return stored;
    }

    public static PublishMsg create(int hdrLen, int remaining, byte flags)
    {
        return new PublishMsg(hdrLen, remaining, flags);
    }

    public PublishMsg duplicate()
    {
        return new PublishMsg(this);
    }

    @Override
    public boolean handle(Client client)
    {
        return client.handlePublishMsg(this);
    }

    @Override
    public void encode()
    {
        if (!rawReady) {
            remaining = 0;

            remaining += Msg.STR_SIZELEN;
            remaining += topic.length();
            remaining += payload.remaining();

            if (qos > Msg.QOS0) {
                remaining += Msg.STR_SIZELEN;
            }

            hdrLen = 1 + lengthOf(remaining);

            if (rawMsg == null) {
                rawMsg = new Buffer(hdrLen + remaining);
            }

            rawMsg.clear();

            byte flags;

            flags = (byte) (TYPE << 4);
            flags |= (qos << 1);
            flags |= dup ? DUP : 0;
            flags |= retained ? RETAIN : 0;

            rawMsg.put(flags);
            rawMsg.putRemaining(remaining);
            rawMsg.putString(topic);

            if (qos > Msg.QOS0) {
                rawMsg.putShort((short) packetId);
            }

            rawMsg.put(payload);

            rawMsg.flip();
            rawReady = true;
        }
    }

    @Override
    public void decode()
    {
        rawMsg.advance(hdrLen);

        dup    = (hdrFlags & DUP) != 0;
        retained = (hdrFlags & RETAIN) != 0;
        qos    = (byte)((hdrFlags & QOS_FLAGS) >> 1);

        topic = rawMsg.getString();
        int len = remaining;
        len -= topic.length() + Msg.STR_SIZELEN;

        if (qos > Msg.QOS0) {
            packetId   = rawMsg.getShort();
            len -= Msg.PACKET_ID_LEN;
        }

        payload = rawMsg.getBuffer(len);
    }

    @Override
    public String toString()
    {
        String nl = Util.newLine();
        int payloadLen = (payload != null) ? payload.remaining() : 0;

        StringBuilder str = new StringBuilder(512);

        str.append(nl);
        str.append("\t ----------------------------------------------------")        .append(nl);
        str.append("\t Message Type        : ").append(STR)                          .append(nl);
        str.append("\t Total Length        : ").append(hdrLen + remaining)           .append(nl);
        str.append("\t Remaining Length    : ").append(Util.toUnsignedStr(remaining)).append(nl);
        str.append("\t Header Flags        : ").append(Util.byteToBinary(hdrFlags))  .append(nl);
        str.append("\t QOS                 : ").append(qos                   )       .append(nl);
        str.append("\t Flag Duplicate      : ").append(dup)                          .append(nl);
        str.append("\t Flag Retain         : ").append(retained)                     .append(nl);
        str.append("\t Topic               : ").append(topic)                        .append(nl);
        str.append("\t Packet ID           : ").append(Util.toUnsignedStr(packetId)) .append(nl);
        str.append("\t Payload             : ").append(payloadLen).append(" bytes")  .append(nl);
        str.append("\t ----------------------------------------------------")        .append(nl);

        return str.toString();

    }
}
