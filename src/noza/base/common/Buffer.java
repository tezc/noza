package noza.base.common;

import noza.api.Noza;

import java.nio.ByteBuffer;


public class Buffer
{
    private final ByteBuffer buf;


    public Buffer(byte[] src)
    {
        buf = ByteBuffer.allocate(src.length);
        put(src);
    }

    public Buffer(int size)
    {
        this.buf = ByteBuffer.allocate(size);
    }

    public Buffer(Buffer buf)
    {
        this.buf = buf.buf.duplicate();
        this.buf.rewind();
    }

    public int cap()
    {
        return buf.capacity();
    }

    public Buffer duplicate()
    {
        return new Buffer(this);
    }

    public ByteBuffer backend()
    {
        return buf;
    }

    public void put(int i, byte value)
    {
        buf.put(i, value);
    }

    public void put(byte value)
    {
        buf.put(value);
    }

    public void putShort(short value)
    {
        buf.putShort(value);
    }

    public void putInt(int value)
    {
        buf.putInt(value);
    }

    public void putString(String value)
    {
        buf.putShort((short) value.length());
        buf.put(value.getBytes());
    }

    public void put(byte[] value)
    {
        buf.put(value);
    }

    public void putRemaining(int len)
    {
        do {
            byte encodedByte = (byte) (len % 128);
            len = len / 128;

            if (len > 0) {
                encodedByte |= 128;
            }
            buf.put(encodedByte);
        } while (len > 0);
    }

    public byte get()
    {
        return buf.get();
    }

    public short getShort()
    {
        return buf.getShort();
    }

    public int getInt()
    {
        return buf.getInt();
    }

    public long getLong()
    {
        return buf.getLong();
    }

    public String getString()
    {
        int len = buf.getShort() & 0xFFFF;
        byte[] strBuf = new byte[len];

        buf.get(strBuf);

        return new String(strBuf);
    }

    public byte[] getData(int len)
    {
        byte[] dst = new byte[len];

        buf.get(dst);

        return dst;
    }

    public ByteBuffer getBuffer(int len)
    {
        ByteBuffer dup = buf.duplicate();

        dup.limit(dup.position() + len);

        return dup.slice();
    }

    public int getRemaining()
    {
        byte tmp;
        int read       = 1; //Header size
        int multiplier = 1;
        int remaining  = 0;

        do {
            if (multiplier > 128 * 128 * 128) {
                throw new IllegalArgumentException("Message exceeds max length");
            }

            if (read == buf.remaining()) {
                return Noza.ERROR;
            }

            tmp = buf.get(read++);

            remaining += (tmp & 0x7F) * multiplier;
            multiplier *= 128;

        } while ((tmp & 0x80) != 0);

        if (buf.remaining() < remaining + read) {
            return Noza.ERROR;
        }

        buf.position(buf.position() + read);

        return remaining;
    }

    public int remaining()
    {
        return buf.remaining();
    }

    public void rewind()
    {
        buf.rewind();
    }

    public int position()
    {
        return buf.position();
    }

    public void position(int pos)
    {
        buf.position(pos);
    }

    public byte get(int i)
    {
        return buf.get(i);
    }

    public void advance(int i)
    {
        buf.position(buf.position() + i);
    }

    public byte[] array()
    {
        return buf.array();
    }

    public void flip()
    {
        buf.flip();
    }

    public void compact()
    {
        buf.compact();
    }

    public void clear()
    {
        buf.clear();
    }

    public void get(ByteBuffer dest)
    {
        int len = Math.min(dest.remaining(), buf.remaining());
        dest.put(buf.array(), buf.position(), len);
        advance(len);
    }

    public void put(ByteBuffer src)
    {
        int len = Math.min(src.remaining(), buf.remaining());
        src.get(buf.array(), buf.position(), len);
        advance(len);
    }

    public void put(Buffer src)
    {
        int len = Math.min(src.remaining(), buf.remaining());
        buf.put(src.array(), src.position(), len);
        src.advance(len);
    }
}
