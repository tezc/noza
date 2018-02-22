package noza.core.msg;

import noza.base.common.Buffer;
import noza.base.common.Util;
import noza.core.Mqtt;
import noza.core.client.Client;

import java.util.ArrayList;
import java.util.List;


public class SubscribeMsg extends Msg
{
    public static final String STR     = "SUBSCRIBE";
    public static final byte TYPE      = 0x08;
    public static final byte HDR_FLAGS = 0x02;

    public short packetId;
    public List<Topic> topics;

    public SubscribeMsg(int hdrLen, int remaining, byte flags)
    {
        super(TYPE, hdrLen, remaining, flags);
        topics = new ArrayList<>();
    }

    public static SubscribeMsg create(int hdrLen, int remaining, byte flags)
    {
        return new SubscribeMsg(hdrLen, remaining, flags);
    }

    @Override
    public boolean handle(Client client)
    {
        return client.handleSubscribeMsg(this);
    }

    @Override
    public void encode()
    {
        if (!rawReady) {
            remaining = Msg.PACKET_ID_LEN;

            for (Topic topic : topics) {
                remaining += Msg.QOS_BYTE_LEN +
                             Msg.STR_SIZELEN + topic.getStr().length();
            }

            hdrLen = 1 + lengthOf(remaining);

            if (rawMsg == null) {
                rawMsg = new Buffer(hdrLen + remaining);
            }

            rawMsg.clear();

            rawMsg.put((byte) (TYPE << 4 | HDR_FLAGS));
            rawMsg.putShort(packetId);

            for (Topic topic : topics) {
                rawMsg.putString(topic.getStr());
                rawMsg.put((byte) topic.getQos());
            }

            rawMsg.flip();
            rawReady = true;
        }
    }

    @Override
    public void decode()
    {
        rawMsg.advance(hdrLen);

        packetId   = rawMsg.getShort();
        remaining -= Msg.PACKET_ID_LEN;

        do {
            Topic topic = new Topic(rawMsg.getString(), rawMsg.get());
            topics.add(topic);

            remaining -= (topic.getStr().length() + Msg.STR_SIZELEN +
                                                    Msg.QOS_BYTE_LEN);
        } while (remaining != 0);
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
        str.append("\t Packet ID           : ").append(packetId)                     .append(nl);

        for (Topic topic : topics) {
            str.append("\t Topic               : ").append(topic.getStr())           .append(nl);
            str.append("\t QOS                 : ").append(topic.getQos())           .append(nl);
        }

        str.append("\t ----------------------------------------------------")        .append(nl);

        return str.toString();
    }
}
