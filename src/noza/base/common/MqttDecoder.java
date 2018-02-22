package noza.base.common;


import noza.base.config.Configs;
import noza.core.Mqtt;
import noza.core.msg.*;

import java.nio.ByteBuffer;

public class MqttDecoder
{
    private Configs configs;
    private Msg msg;
    private Buffer header;

    public MqttDecoder(Configs configs)
    {
        this.configs = configs;
        header       = new Buffer(Msg.MAX_FIXED_HDR_LEN);
    }

    public int needed()
    {
        if (msg == null) {
            return header.remaining();
        }
        else {
            return msg.needed();
        }
    }

    public Msg read(ByteBuffer buf)
    {
        if (msg == null) {
            header.put(buf);

            if (header.remaining() > header.cap() - Msg.MIN_MSG_LEN) {
                return null;
            }

            byte type  = (byte) ((header.get(0) & 0xF0) >> 4);
            byte flags = (byte) (header.get(0) & 0x0F);

            byte tmp;
            int hdrlen = 1; //Header size
            int multiplier = 1;
            int remaining = 0;

            do {
                if (multiplier > 128 * 128 * 128) {
                    throw new IllegalArgumentException("Message exceeds max length");
                }

                if (hdrlen == header.cap() - header.remaining()) {
                    return null;
                }

                tmp = header.get(hdrlen++);

                remaining += (tmp & 0x7F) * multiplier;
                multiplier *= 128;

            } while ((tmp & 0x80) != 0);

            switch (type) {
                case ConnectMsg.TYPE:
                    msg = ConnectMsg.create(hdrlen, remaining, flags);
                    break;
                case ConnackMsg.TYPE:
                    msg = ConnackMsg.create(hdrlen, remaining, flags);
                    break;
                case PublishMsg.TYPE:
                    msg = PublishMsg.create(hdrlen, remaining, flags);
                    break;
                case PubackMsg.TYPE:
                    msg = PubackMsg.create(hdrlen, remaining, flags);
                    break;
                case PubrecMsg.TYPE:
                    msg = PubrecMsg.create(hdrlen, remaining, flags);
                    break;
                case PubrelMsg.TYPE:
                    msg = PubrelMsg.create(hdrlen, remaining, flags);
                    break;
                case PubcompMsg.TYPE:
                    msg = PubcompMsg.create(hdrlen, remaining, flags);
                    break;
                case SubscribeMsg.TYPE:
                    msg = SubscribeMsg.create(hdrlen, remaining, flags);
                    break;
                case SubackMsg.TYPE:
                    msg = SubackMsg.create(hdrlen, remaining, flags);
                    break;
                case UnsubscribeMsg.TYPE:
                    msg = UnsubscribeMsg.create(hdrlen, remaining, flags);
                    break;
                case UnsubackMsg.TYPE:
                    msg = UnsubackMsg.create(hdrlen, remaining, flags);
                    break;
                case PingreqMsg.TYPE:
                    msg = PingreqMsg.create(hdrlen, remaining, flags);
                    break;
                case PingrespMsg.TYPE:
                    msg = PingrespMsg.create(hdrlen, remaining, flags);
                    break;
                case DisconnectMsg.TYPE:
                    msg = DisconnectMsg.create(hdrlen, remaining, flags);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown message hdr : " + type);
            }

            header.flip();
            msg.read(header);
            header.compact();
        }


        msg.read(buf);

        if (msg.ready()) {
            msg.decode();

            Msg decoded = msg;
            msg = null;

            return decoded;
        }

        return null;
    }
}
