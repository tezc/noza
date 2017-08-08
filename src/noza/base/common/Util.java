package noza.base.common;


import java.sql.Timestamp;
import java.util.ArrayList;

public class Util
{
    public static long time()
    {
        return System.currentTimeMillis();
    }

    public static String byteToBinary(byte b)
    {
        return String.format("%8s",
                             Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
    }

    public static String byteToStr(byte b)
    {
        return Integer.toUnsignedString(b & 0xFF);
    }

    public static String shortToStr(short s)
    {
        return Integer.toUnsignedString(Short.toUnsignedInt(s));
    }

    public static String toUnsignedStr(int i)
    {
        return Integer.toUnsignedString(i);
    }

    public static Timestamp now()
    {
        return java.sql.Timestamp.from(java.time.Instant.now());
    }

    public static String newLine()
    {
        return System.lineSeparator();
    }

    public static String pad(String s, int n)
    {
        return String.format("%1$-"+n+ "s", s);
    }

    public static byte[] toArray(int value) {
        return new byte[] { (byte)(value >>> 24),
                            (byte)(value >>> 16),
                            (byte)(value >>> 8),
                            (byte)(value)
        };
    }

    public static byte[] toArray(int value, byte arr[])
    {
        arr[0] = (byte) (value >>> 24);
        arr[1] = (byte) (value >>> 16);
        arr[2] = (byte) (value >>> 8);
        arr[3] = (byte) (value);

        return arr;
    }

    public static String[] splitStr(String str, char delimiter)
    {
        ArrayList<String> list = new ArrayList<>(8);

        int lastPoint = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '/') {
                list.add(str.substring(lastPoint, i));
                lastPoint = i + 1;
            }
        }

        list.add(str.substring(lastPoint, str.length()));

        return list.toArray(new String[list.size()]);
    }
}
