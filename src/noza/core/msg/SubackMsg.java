package noza.core.msg;

import noza.base.common.Buffer;
import noza.base.common.Util;
import noza.core.Mqtt;
import noza.core.client.Client;


public class SubackMsg extends Msg
{
    public static final String STR     = "SUBACK";
    public static final byte TYPE      = 0x09;
    public static final byte HDR_FLAGS = 0x00;

    public SubscribeMsg subscribe;


    public SubackMsg(int hdrLen, int remaining, byte flags)
    {
        super(TYPE, hdrLen, remaining, flags);
    }

    public SubackMsg(SubscribeMsg subscribe)
    {
        this.subscribe = subscribe;
        this.remaining = subscribe.topics.size();
        this.hdrType   = TYPE;
        this.hdrFlags  = HDR_FLAGS;
        this.hdrLen    = Msg.lengthOf(remaining) + 1;
    }

    public static SubackMsg create(int hdrLen, int remaining, byte flags)
    {
        return new SubackMsg(hdrLen, remaining, flags);
    }

    @Override
    public void encode()
    {
        if (!rawReady) {
            remaining = subscribe.topics.size() + Mqtt.PACKET_ID_LEN;
            hdrLen = 1 + Msg.lengthOf(remaining);

            if (rawMsg == null) {
                rawMsg = new Buffer(hdrLen + remaining);
            }

            rawMsg.clear();

            rawMsg.put((byte) (TYPE << 4 | HDR_FLAGS));
            rawMsg.putRemaining(Mqtt.PACKET_ID_LEN + subscribe.topics.size());
            rawMsg.putShort(subscribe.packetId);

            for (Topic topic : subscribe.topics) {
                rawMsg.put((byte) topic.getResult());
            }

            rawMsg.flip();
            rawReady = true;
        }
    }

    @Override
    public void decode()
    {
        rawMsg.advance(hdrLen);

        if (hdrFlags != HDR_FLAGS) {
            throw new IllegalArgumentException("Malformed SUBACK msg ");
        }

        subscribe.packetId = rawMsg.getShort();
        remaining -= Mqtt.PACKET_ID_LEN;

        /*do {
            Topic topic = new Topic();

            topic.qos  = rawMsg.get();
            subscribe.topics.add(topic);

            remaining -= Mqtt.QOS_BYTE_LEN;

        } while (remaining > 0);*/
    }

    @Override
    public boolean handle(Client client)
    {
        return client.handleSubackMsg(this);
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
        str.append("\t Packet ID           : ").append(subscribe.packetId)           .append(nl);

        for (Topic topic : subscribe.topics) {
            str.append("\t Topic               : ").append(topic.getStr())           .append(nl);
            str.append("\t QOS                 : ").append(topic.getQos())           .append(nl);
        }

        str.append("\t ----------------------------------------------------")        .append(nl);

        return str.toString();

    }
}
