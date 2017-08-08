package noza.api.msgs;

import java.util.List;

public interface Subscribe
{

    short getPacketId();
    List<MsgTopic> getTopics();
}
