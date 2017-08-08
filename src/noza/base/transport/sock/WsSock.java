package noza.base.transport.sock;

import noza.base.common.Util;
import noza.base.exception.WebSocketException;
import noza.base.common.Buffer;
import noza.base.exception.HttpException;
import noza.base.poller.Fd;
import noza.base.transport.listener.WsListener;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class WsSock extends Sock implements Fd
{
    private static final String PROTOCOL       = "ws";
    private static final String WEBSOCKET_KEY  = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final int MIN_MSG_LEN    = 2;
    private static final int MASK           = 0x80;
    private static final int OPCODE_BYTE    = 1;
    private static final int MASK_LEN       = 4;

    private static final int FIN_FLAG       = 0x80;

    private static final int OPCODE_CONTINUATION = 0x00;
    private static final int OPCODE_TEXT         = 0x01;
    private static final int OPCODE_BINARY       = 0x02;
    private static final int OPCODE_CLOSE        = 0x08;
    private static final int OPCODE_PING         = 0x09;
    private static final int OPCODE_PONG         = 0x0A;


    private static final int PAYLOAD_LEN_1  = 1;
    private static final int PAYLOAD_LEN_2  = 3;
    private static final int PAYLOAD_LEN_3  = 9;
    private static final int PAYLOAD_MAX_1  = 125;
    private static final int PAYLOAD_MAX_2  = 65536;
    private static final int HDR_LEN_1      = OPCODE_BYTE + PAYLOAD_LEN_1;
    private static final int HDR_LEN_2      = OPCODE_BYTE + PAYLOAD_LEN_2;
    private static final int HDR_LEN_3      = OPCODE_BYTE + PAYLOAD_LEN_3;
    private static final int CLIENT_HDR_LEN_1      = OPCODE_BYTE + MASK_LEN + PAYLOAD_LEN_1;
    private static final int CLIENT_HDR_LEN_2      = OPCODE_BYTE + MASK_LEN + PAYLOAD_LEN_2;
    private static final int CLIENT_HDR_LEN_3      = OPCODE_BYTE + MASK_LEN + PAYLOAD_LEN_3;


    private final Buffer wsHdr;

    private boolean lenDecoded;
;
    private byte mask[];
    private boolean connected;
    private final WsListener listener;
    private ByteBuffer httpBuf;

    private boolean headerDecoded;
    private long payloadLen;

    private ByteBuffer recvBuf;
    private ByteBuffer sendBuf;


    public WsSock(SockOwner owner, SocketChannel channel, WsListener listener)
    {
        super(owner, channel, PROTOCOL);

        this.listener      = listener;
        this.wsHdr         = new Buffer(6);
        this.headerDecoded = false;
        this.connected     = false;
        this.httpBuf       = ByteBuffer.allocate(1024);
        this.mask          = new byte[4];
    }

    @Override
    public ByteBuffer recv()
    {
        return null;
    }

    @Override
    public boolean send()
    {
        int pos = sendBuf.position();
        int len = sendBuf.position() - HDR_LEN_3;

        int hdrLen;

        if (len < PAYLOAD_MAX_1) {
            hdrLen = HDR_LEN_1;
        }
        else if (len < PAYLOAD_MAX_2) {
            hdrLen = HDR_LEN_2;
        }
        else {
            hdrLen = HDR_LEN_3;
        }

        sendBuf.position(sendBuf.position() - hdrLen - len);

        int flag = (OPCODE_BINARY | FIN_FLAG);
        sendBuf.put((byte) flag);

        switch (hdrLen)
        {
            case HDR_LEN_1:
                sendBuf.put((byte) len);
                break;
            case HDR_LEN_2:
                sendBuf.put((byte) 126);
                sendBuf.putShort((short) len);
                break;
            case HDR_LEN_3:
                sendBuf.put((byte) 127);
                sendBuf.putLong((long) len);
                break;
        }

        sendBuf.position(pos);
        sendBuf.flip();
        sendBuf.position(pos - hdrLen - len);

        int n = super.write(sendBuf);
        if (n == -1) {
            connected = false;

            return false;
        }

        if (!sendBuf.hasRemaining()) {
            owner.freeDirectBuf(sendBuf);
            sendBuf = null;
        }
        else {
            sendBuf.compact();
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);

            return false;
        }

        return true;
    }

    @Override
    public ByteBuffer getSendBuf()
    {
        if (sendBuf == null) {
            sendBuf = owner.allocDirectBuf();
        }

        sendBuf.position(sendBuf.position() + HDR_LEN_3);

        return sendBuf;
    }

    @Override
    public void releaseResources()
    {

    }

    @Override
    public int read(ByteBuffer recvBuf)
    {
        if (!connected) {
            return httpHandshake(recvBuf);
        }

        recvBuf.clear();

        int read = super.read(recvBuf);
        if (read == -1 || read == 0) {
            return read;
        }

        recvBuf.flip();

        if (!headerDecoded) {

            wsHdr.put(recvBuf);
            if (wsHdr.cap() - wsHdr.remaining() < MIN_MSG_LEN) {
                return 0;
            }

            byte two = wsHdr.get(1);
            if ((two & MASK) == 0) {
                throw new WebSocketException("Mask is not set");
            }

            int len = two & 0x7F;

            int hdrLen;

            if (len < 125) {
                hdrLen = CLIENT_HDR_LEN_1;
            }
            else if (len == 126) {
                hdrLen = CLIENT_HDR_LEN_2;
            }
            else if (len == 127) {
                hdrLen = CLIENT_HDR_LEN_3;
            }
            else {
                throw new WebSocketException("Unknown Payload len : " + payloadLen);
            }

            if (wsHdr.cap() - wsHdr.remaining() >= hdrLen) {
                wsHdr.flip();
                wsHdr.advance(2);

                switch (hdrLen) {
                    case CLIENT_HDR_LEN_1:
                        payloadLen = len;
                        break;
                    case CLIENT_HDR_LEN_2:
                        payloadLen = wsHdr.getShort();
                        break;
                    case CLIENT_HDR_LEN_3:
                        payloadLen = wsHdr.getLong();
                        break;
                }

                mask = Util.toArray(wsHdr.getInt());

                headerDecoded = true;
            }
            else {
                return 0;
            }
        }

        int opcode = wsHdr.get(0) & 0x0F;
        switch (opcode) {
            case OPCODE_CONTINUATION:
                break;
            case OPCODE_TEXT:
                throw new WebSocketException("Unexpected opcode : TEXT");
            case OPCODE_BINARY:
                break;
            case OPCODE_CLOSE:
                connected = false;
                return -1;
            case OPCODE_PING:
                break;
            case OPCODE_PONG:
                break;
        }

        if (wsHdr.remaining() + recvBuf.remaining() >= payloadLen) {

            int hdrRemaining = wsHdr.remaining();
            for (int i = 0; i < hdrRemaining; i++) {
                recvBuf.put(i, (byte) (wsHdr.get() ^ mask[i % 4]));
            }

            for (int i = hdrRemaining; i < payloadLen; i++) {
                recvBuf.put(i, (byte) (recvBuf.get() ^ mask[i % 4]));
            }

            recvBuf.position((int) payloadLen);

            headerDecoded = false;
            wsHdr.clear();

            return (int) payloadLen;
        }
        else {
            recvBuf.compact();
            return 0;
        }
    }

    private int httpHandshake(ByteBuffer recvBuf)
    {
        recvBuf.clear();

        int n = super.read(recvBuf);
        if (n == -1) {
            close();
            return -1;
        }
        else if (n == 0) {
            return 0;
        }

        recvBuf.flip();

        int len = Math.min(recvBuf.remaining(), httpBuf.remaining());

        recvBuf.get(httpBuf.array(), httpBuf.position(), len);
        httpBuf.position(httpBuf.position() + len);

        for (int i = 0; i < httpBuf.capacity() - httpBuf.remaining() - 3; i++) {
            if (httpBuf.get(i)     == '\r' &&
                httpBuf.get(i + 1) == '\n' &&
                httpBuf.get(i + 2) == '\r' &&
                httpBuf.get(i + 3) == '\n') {

                handleRequest(new String(httpBuf.array(), 0, i + 3));
                httpBuf = null;
                connected = true;
                break;
            }
        }

        return 0;
    }

    private void handleRequest(String request)
    {
        if (!request.startsWith("GET ")) {
            throw new HttpException("Unexpected HTTP method : " + request);
        }

        String uri = request.substring(5, request.indexOf(' ', 5));
        if (!uri.equals(listener.getRequestUri())) {
            throw new HttpException("Unexpected Request-Uri : " + request);
        }

        if (!request.contains("Upgrade: websocket")) {
            throw new HttpException(
                "Missing header : Upgrade: websocket" + request);
        }

        if (!request.contains("Sec-WebSocket-Version: 13")) {
            throw new HttpException(
                "Missing header : Sec-WebSocket-Version: 13" + request);
        }


        int index = request.indexOf("Sec-WebSocket-Protocol: ");
/*
        if (index == -1) {
            throw new HttpException(
                "Missing header : Sec-WebSocket-Protocol: mqtt " + request);
        }


        index += "Sec-WebSocket-Protocol: ".length();

        String subProtocols = request.substring(index,
                                                request.indexOf("\r\n", index));
        if (!subProtocols.contains("mqtt")) {
            throw new HttpException(
                "Missing header : Sec-WebSocket-Protocol: mqtt" + request);
        }
*/
        index = request.indexOf("Sec-WebSocket-Key: ");
        if (index == -1) {
            throw new HttpException(
                "Missing header : Sec-WebSocket-Key" + request);
        }

        index += "Sec-WebSocket-Key: ".length();

        String key = request.substring(index, request.indexOf("\r\n", index));
        key += WEBSOCKET_KEY;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            key = Base64.getEncoder().encodeToString(md.digest(key.getBytes()));

        }
        catch (NoSuchAlgorithmException e) {
            throw new HttpException(e);
        }

        byte[] response;

        try {
            response = (
                "HTTP/1.1 101 Switching Protocols\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Upgrade: websocket\r\n" +
                    "Sec-WebSocket-Protocol: mqtt\r\n" +
                    "Sec-WebSocket-Accept: " + key + "\r\n\r\n").getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new HttpException(e);
        }

        write(ByteBuffer.wrap(response));
    }

}
