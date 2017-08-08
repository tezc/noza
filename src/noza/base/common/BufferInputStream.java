package noza.base.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;


public class BufferInputStream extends InputStream
{
    ByteBuffer buf;

    public BufferInputStream(ByteBuffer buf)
    {
        this.buf = buf.duplicate();
    }

    @Override
    public int read() throws IOException
    {
        if (!buf.hasRemaining()) {
            return -1;
        }

        return buf.get();
    }
}
