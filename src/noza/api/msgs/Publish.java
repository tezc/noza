package noza.api.msgs;

import java.nio.ByteBuffer;

public interface Publish
{
    int getPacketId();
    String getTopic();
    ByteBuffer getPayload();
    byte getQos();
    boolean isRetained();
    boolean isDup();
    String getId();
}
