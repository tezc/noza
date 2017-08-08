package noza.core;

import java.nio.ByteBuffer;

public interface BufferPool
{
    ByteBuffer allocBuf();
    void freeBuf(ByteBuffer buf);

    ByteBuffer allocLargeBuf();
    void freeLargeBuf(ByteBuffer buf);

    ByteBuffer allocDirectBuf();
    void freeDirectBuf(ByteBuffer buf);

    ByteBuffer allocLargeDirectBuf();
    void freeLargeDirectBuf(ByteBuffer buf);

    ByteBuffer getTempBuf();
}
