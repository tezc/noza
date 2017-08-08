package noza.core.msg;

import noza.base.common.Buffer;
import noza.base.common.Util;
import noza.core.Mqtt;
import noza.core.client.Client;

import java.util.ArrayList;
import java.util.List;


public class UnsubscribeMsg extends Msg
{
    public static final String STR     = "UNSUBSCRIBE";
    public static final byte TYPE      = 0x0A;
    public static final byte HDR_FLAGS = 0x02;

    public short packetId;
    public List<String> topics;

    public UnsubscribeMsg(int hdrLen, int remaining, byte flags)
    {
        super(TYPE, hdrLen, remaining, flags);
        this.topics = new ArrayList<>();
    }

    public static UnsubscribeMsg create(int hdrLen, int remaining, byte flags)
    {
        return new UnsubscribeMsg(hdrLen, remaining, flags);
    }

    @Override
    public boolean handle(Client client)
    {
        return client.handleUnsubscribeMsg(this);
    }

    @Override
    public void encode()
    {
        if (!rawReady) {
            remaining = Mqtt.PACKET_ID_LEN;

            for (String topic : topics) {
                remaining += Mqtt.STR_SIZELEN + topic.length();
            }

            hdrLen = 1 + Msg.lengthOf(remaining);

            if (rawMsg == null) {
                rawMsg = new Buffer(hdrLen + remaining);
            }

            rawMsg.clear();

            rawMsg.put((byte) (TYPE << 4 | HDR_FLAGS));
            rawMsg.putRemaining(remaining);
            rawMsg.putShort(packetId);

            for (String topic : topics) {
                rawMsg.putString(topic);
            }
        }

        rawMsg.flip();
        rawReady = true;
    }

    @Override
    public void decode()
    {
        rawMsg.advance(hdrLen);

        packetId   = rawMsg.getShort();
        remaining -= Mqtt.PACKET_ID_LEN;

        do {
            String topic = rawMsg.getString();
            topics.add(topic);

            remaining -= (topic.length() + Mqtt.STR_SIZELEN);
        } while (remaining > 0);
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

        for (String topic : topics) {
            str.append("\t Topic               : ").append(topic)                    .append(nl);
        }

        str.append("\t ----------------------------------------------------")        .append(nl);

        return str.toString();
    }
}
