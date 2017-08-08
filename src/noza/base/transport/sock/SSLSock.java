package noza.base.transport.sock;

import noza.base.exception.TlsException;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import java.io.IOException;
import java.io.EOFException;
import java.nio.channels.SelectionKey;


import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;

/*
 * Transport layer for SSL communication
 */
public class SSLSock extends Sock
{
    private static final String PROTOCOL = "tls";

    private final SSLEngine sslEngine;
    private final boolean enableRenegotiation;

    private HandshakeStatus handshakeStatus;
    private SSLEngineResult handshakeResult;
    private boolean handshakeComplete = false;
    private boolean closing = false;

    private ByteBuffer netReadBuffer;
    private ByteBuffer netWriteBuffer;
    private ByteBuffer appReadBuffer;
    private ByteBuffer emptyBuf = ByteBuffer.allocate(0);


    public SSLSock(SocketChannel channel, SSLEngine sslEngine, boolean enableRenegotiation) throws IOException
    {
        super(null, channel, PROTOCOL);

        this.sslEngine = sslEngine;
        this.enableRenegotiation = enableRenegotiation;

        startHandshake();
    }

    /**
     * starts sslEngine handshake process
     */
    protected void startHandshake() throws IOException
    {

        this.netReadBuffer = ByteBuffer.allocate(netReadBufferSize());
        this.netWriteBuffer = ByteBuffer.allocate(netWriteBufferSize());
        this.appReadBuffer = ByteBuffer.allocate(applicationBufferSize());

        //clear & set netRead & netWrite buffers
        netWriteBuffer.position(0);
        netWriteBuffer.limit(0);
        netReadBuffer.position(0);
        netReadBuffer.limit(0);
        handshakeComplete = false;
        closing = false;
        //initiate handshake
        sslEngine.beginHandshake();
        handshakeStatus = sslEngine.getHandshakeStatus();
    }



    public boolean ready()
    {
        return handshakeComplete;
    }

    /**
     * does channel.finishConnect()
     */
    public boolean finishConnect() throws IOException
    {
        boolean connected = channel.finishConnect();
        if (connected)
            key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT | SelectionKey.OP_READ);
        return connected;
    }

    /**
     * disconnects selectionKey.
     */
    public void disconnect()
    {
        key.cancel();
    }


    @Override
    public boolean isOpen()
    {
        return channel.isOpen();
    }

    @Override
    public ByteBuffer recv()
    {
        return null;
    }

    @Override
    public boolean send()
    {
        write(sendBuf);

        return true;
    }

    @Override
    public ByteBuffer getSendBuf()
    {
        return ByteBuffer.allocateDirect(4080);
    }

    @Override
    public void releaseResources()
    {

    }

    @Override
    public boolean isConnected()
    {
        return channel.isConnected();
    }


    /**
     * Sends a SSL close message and closes channel.
     */
    @Override
    public void close()
    {
        if (closing)
            return;
        closing = true;
        sslEngine.closeOutbound();
        try {
            if (isConnected()) {
                if (!flush(netWriteBuffer)) {
                    throw new IOException(
                        "Remaining data in the network buffer, can't send SSL close message.");
                }
                //prep the buffer for the close message
                netWriteBuffer.clear();
                //perform the close, since we called sslEngine.closeOutbound
                SSLEngineResult wrapResult = sslEngine.wrap(emptyBuf, netWriteBuffer);
                //we should be in a close state
                if (wrapResult.getStatus() != SSLEngineResult.Status.CLOSED) {
                    throw new IOException(
                        "Unexpected status returned by SSLEngine.wrap, expected CLOSED, received " + wrapResult
                            .getStatus() + ". Will not send close message to peer.");
                }
                netWriteBuffer.flip();
                flush(netWriteBuffer);
            }
        }
        catch (IOException ie) {
            //log.warn("Failed to send SSL Close message ", ie);
        }
        finally {
            try {
                try {
                    channel.socket().close();
                    channel.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }

            }
            finally {
                key.attach(null);
                key.cancel();
            }
        }
    }


    public boolean hasPendingWrites()
    {
        return netWriteBuffer.hasRemaining();
    }


    private boolean flush(ByteBuffer buf) throws IOException
    {
        int remaining = buf.remaining();
        if (remaining > 0) {
            int written = channel.write(buf);
            return written >= remaining;
        }
        return true;
    }


    @Override
    public void handshake()
    {
        try {
            boolean read = key.isReadable();
            boolean write = key.isWritable();
            handshakeComplete = false;
            handshakeStatus = sslEngine.getHandshakeStatus();
            if (!flush(netWriteBuffer)) {
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                return;
            }
            try {
                switch (handshakeStatus) {

                    case NEED_TASK:
                        handshakeStatus = runDelegatedTasks();
                        break;

                    case NEED_WRAP:
                        handshakeResult = handshakeWrap(write);
                        if (handshakeResult.getStatus() == Status.BUFFER_OVERFLOW) {
                            int currentNetWriteBufferSize = netWriteBufferSize();
                            netWriteBuffer.compact();
                            //netWriteBuffer = Utils.ensureCapacity(netWriteBuffer, currentNetWriteBufferSize);
                            netWriteBuffer.flip();
                            if (netWriteBuffer.limit() >= currentNetWriteBufferSize) {
                                throw new IllegalStateException(
                                    "Buffer overflow when available data size (" + netWriteBuffer
                                        .limit() + ") >= network buffer size (" + currentNetWriteBufferSize + ")");
                            }
                        }
                        else if (handshakeResult.getStatus() == Status.BUFFER_UNDERFLOW) {
                            throw new IllegalStateException(
                                "Should not have received BUFFER_UNDERFLOW during handshake WRAP.");
                        }
                        else if (handshakeResult.getStatus() == Status.CLOSED) {
                            throw new EOFException();
                        }

                        //if handshake status is not NEED_UNWRAP or unable to flush netWriteBuffer contents
                        //we will break here otherwise we can do need_unwrap in the same call.
                        if (handshakeStatus != HandshakeStatus.NEED_UNWRAP || !flush(netWriteBuffer)) {
                            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                            break;
                        }

                    case NEED_UNWRAP:

                        do {
                            handshakeResult = handshakeUnwrap(read);
                            if (handshakeResult.getStatus() == Status.BUFFER_OVERFLOW) {
                                int currentAppBufferSize = applicationBufferSize();
                                //appReadBuffer = Utils.ensureCapacity(appReadBuffer, currentAppBufferSize);
                                if (appReadBuffer.position() > currentAppBufferSize) {
                                    throw new IllegalStateException(
                                        "Buffer underflow when available data size (" + appReadBuffer
                                            .position() + ") > packet buffer size (" + currentAppBufferSize + ")");
                                }
                            }
                        } while (handshakeResult.getStatus() == Status.BUFFER_OVERFLOW);
                        if (handshakeResult.getStatus() == Status.BUFFER_UNDERFLOW) {
                            int currentNetReadBufferSize = netReadBufferSize();
                            //netReadBuffer = Utils.ensureCapacity(netReadBuffer, currentNetReadBufferSize);
                            if (netReadBuffer.position() >= currentNetReadBufferSize) {
                                throw new IllegalStateException(
                                    "Buffer underflow when there is available data");
                            }
                        }
                        else if (handshakeResult.getStatus() == Status.CLOSED) {
                            throw new EOFException(
                                "SSL handshake status CLOSED during handshake UNWRAP");
                        }

                        //if handshakeStatus completed than fall-through to finished status.
                        //after handshake is finished there is no data left to read/write in channel.
                        //so the selector won't invoke this channel if we don't go through the handshakeFinished here.
                        if (handshakeStatus != HandshakeStatus.FINISHED) {
                            if (handshakeStatus == HandshakeStatus.NEED_WRAP) {
                                key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                            }
                            else if (handshakeStatus == HandshakeStatus.NEED_UNWRAP) {
                                key.interestOps(SelectionKey.OP_READ);
                            }
                            break;
                        }
                    case FINISHED:
                        handshakeFinished();
                        break;
                    case NOT_HANDSHAKING:
                        handshakeFinished();
                        break;
                    default:
                        throw new IllegalStateException(String.format(
                            "Unexpected status [%s]", handshakeStatus));
                }

            }
            catch (IOException e) {
                handshakeFailure();
                throw new TlsException(e);
            }
        }
        catch (IOException e) {
            handshakeFailure();
            throw new TlsException(e);
        }
    }

    private void renegotiate() throws IOException
    {
        if (!enableRenegotiation)
            throw new SSLHandshakeException("Renegotiation is not supported");
        handshake();
    }


    /**
     * Executes the SSLEngine tasks needed.
     *
     * @return HandshakeStatus
     */
    private HandshakeStatus runDelegatedTasks()
    {
        for (; ; ) {
            Runnable task = delegatedTask();
            if (task == null) {
                break;
            }
            task.run();
        }
        return sslEngine.getHandshakeStatus();
    }

    /**
     * Checks if the handshake status is finished
     * Sets the interestOps for the selectionKey.
     */
    private void handshakeFinished() throws IOException
    {
        // SSLEngine.getHandshakeStatus is transient and it doesn't record FINISHED status properly.
        // It can move from FINISHED status to NOT_HANDSHAKING after the handshake is completed.
        // Hence we also need to check handshakeResult.getHandshakeStatus() if the handshake finished or not
        if (handshakeResult.getHandshakeStatus() == HandshakeStatus.FINISHED) {
            //we are complete if we have delivered the last package
            handshakeComplete = !netWriteBuffer.hasRemaining();
            //remove OP_WRITE if we are complete, otherwise we still have data to write
            if (!handshakeComplete)
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            else {
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                SSLSession session = sslEngine.getSession();
                //log.debug("SSL handshake completed successfully with peerHost '{}' peerPort {} peerPrincipal '{}' cipherSuite '{}'",
                //        session.getPeerHost(), session.getPeerPort(), peerPrincipal(), session.getCipherSuite());
            }
        }
        else {
            throw new IOException("NOT_HANDSHAKING during handshake");
        }
    }


    private SSLEngineResult handshakeWrap(boolean doWrite) throws IOException
    {

        if (netWriteBuffer.hasRemaining())
            throw new IllegalStateException(
                "handshakeWrap called with netWriteBuffer not empty");
        //this should never be called with a network buffer that contains data
        //so we can clear it here.
        netWriteBuffer.clear();
        SSLEngineResult result = sslEngine.wrap(emptyBuf, netWriteBuffer);
        //prepare the results to be written
        netWriteBuffer.flip();
        handshakeStatus = result.getHandshakeStatus();
        if (result.getStatus() == SSLEngineResult.Status.OK && result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
            handshakeStatus = runDelegatedTasks();
        }

        if (doWrite)
            flush(netWriteBuffer);
        return result;
    }


    private SSLEngineResult handshakeUnwrap(boolean doRead) throws IOException
    {
        SSLEngineResult result;
        if (doRead) {
            int read = channel.read(netReadBuffer);
            if (read == -1)
                throw new EOFException("EOF during handshake.");
        }
        boolean cont;
        do {
            //prepare the buffer with the incoming data
            netReadBuffer.flip();
            result = sslEngine.unwrap(netReadBuffer, appReadBuffer);
            netReadBuffer.compact();
            handshakeStatus = result.getHandshakeStatus();
            if (result.getStatus() == SSLEngineResult.Status.OK &&
                result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
                handshakeStatus = runDelegatedTasks();
            }
            cont = result.getStatus() == SSLEngineResult.Status.OK &&
                      handshakeStatus == HandshakeStatus.NEED_UNWRAP;

        } while (netReadBuffer.position() != 0 && cont);

        return result;
    }


    public int read(ByteBuffer dst)
    {
        try {
            if (closing)
                return -1;
            int read = 0;
            if (!handshakeComplete) {
                handshake();
                System.out.println("handshake : " + handshakeComplete);
                return read;
            }

            //if we have unread decrypted data in appReadBuffer read that into dst buffer.
            if (appReadBuffer.position() > 0) {
                read = readFromAppBuffer(dst);
            }

            if (dst.remaining() > 0) {
                //netReadBuffer = Utils.ensureCapacity(netReadBuffer, netReadBufferSize());
                if (netReadBuffer.remaining() > 0) {
                    int netread = channel.read(netReadBuffer);
                    if (netread == 0 && netReadBuffer.position() == 0)
                        return read;
                    else if (netread < 0)
                        throw new EOFException("EOF during read");
                }
                do {
                    netReadBuffer.flip();
                    SSLEngineResult unwrapResult = sslEngine.unwrap(netReadBuffer, appReadBuffer);
                    netReadBuffer.compact();
                    // handle ssl renegotiation.
                    if (unwrapResult.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING &&
                        unwrapResult.getStatus() == Status.OK) {
                        renegotiate();
                        break;
                    }

                    if (unwrapResult.getStatus() == Status.OK) {
                        read += readFromAppBuffer(dst);
                    }
                    else if (unwrapResult.getStatus() == Status.BUFFER_OVERFLOW) {
                        int currentApplicationBufferSize = applicationBufferSize();
                        //appReadBuffer = Utils.ensureCapacity(appReadBuffer, currentApplicationBufferSize);
                        if (appReadBuffer.position() >= currentApplicationBufferSize) {
                            throw new IllegalStateException(
                                "Buffer overflow when available data size (" + appReadBuffer
                                    .position() + ") >= application buffer size (" + currentApplicationBufferSize + ")");
                        }

                        // appReadBuffer will extended upto currentApplicationBufferSize
                        // we need to read the existing content into dst before we can do unwrap again. If there are no space in dst
                        // we can break here.
                        if (dst.hasRemaining())
                            read += readFromAppBuffer(dst);
                        else
                            break;
                    }
                    else if (unwrapResult.getStatus() == Status.BUFFER_UNDERFLOW) {
                        int currentNetReadBufferSize = netReadBufferSize();
                        //netReadBuffer = Utils.ensureCapacity(netReadBuffer, currentNetReadBufferSize);
                        if (netReadBuffer.position() >= currentNetReadBufferSize) {
                            throw new IllegalStateException(
                                "Buffer underflow when available data size (" + netReadBuffer
                                    .position() + ") > packet buffer size (" + currentNetReadBufferSize + ")");
                        }
                        break;
                    }
                    else if (unwrapResult.getStatus() == Status.CLOSED) {
                        // If data has been read and unwrapped, return the data. Close will be handled on the next poll.
                        if (appReadBuffer.position() == 0 && read == 0)
                            throw new EOFException();
                        else
                            break;
                    }
                } while (netReadBuffer.position() != 0);
            }
            return read;
        }
        catch (IOException e) {

        }

        return 0;
    }



    @Override
    public int write(ByteBuffer src)
    {
        try {
            int written = 0;
            if (closing)
                throw new IllegalStateException("Channel is in closing state");
            if (!handshakeComplete) {
                handshake();
                System.out.println("handshake : " + handshakeComplete);
                return written;
            }

            if (!flush(netWriteBuffer))
                return written;

            netWriteBuffer.clear();
            SSLEngineResult wrapResult = sslEngine.wrap(src, netWriteBuffer);
            netWriteBuffer.flip();

            //handle ssl renegotiation
            if (wrapResult.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING && wrapResult
                .getStatus() == Status.OK) {
                renegotiate();
                return written;
            }

            if (wrapResult.getStatus() == Status.OK) {
                written = wrapResult.bytesConsumed();
                flush(netWriteBuffer);
            }
            else if (wrapResult.getStatus() == Status.BUFFER_OVERFLOW) {
                int currentNetWriteBufferSize = netWriteBufferSize();
                netWriteBuffer.compact();
                //netWriteBuffer = Utils.ensureCapacity(netWriteBuffer, currentNetWriteBufferSize);
                netWriteBuffer.flip();
                if (netWriteBuffer.limit() >= currentNetWriteBufferSize)
                    throw new IllegalStateException(
                        "SSL BUFFER_OVERFLOW when available data size (" + netWriteBuffer
                            .limit() + ") >= network buffer size (" + currentNetWriteBufferSize + ")");
            }
            else if (wrapResult.getStatus() == Status.BUFFER_UNDERFLOW) {
                throw new IllegalStateException(
                    "SSL BUFFER_UNDERFLOW during write");
            }
            else if (wrapResult.getStatus() == Status.CLOSED) {
                throw new EOFException();
            }
            return written;
        }
        catch (IOException e) {

        }

        return 0;
    }


    /**
     * returns delegatedTask for the SSLEngine.
     */
    protected Runnable delegatedTask()
    {
        return sslEngine.getDelegatedTask();
    }

    /**
     * transfers appReadBuffer contents (decrypted data) into dst bytebuffer
     *
     * @param dst ByteBuffer
     */
    private int readFromAppBuffer(ByteBuffer dst)
    {
        appReadBuffer.flip();
        int remaining = Math.min(appReadBuffer.remaining(), dst.remaining());
        if (remaining > 0) {
            int limit = appReadBuffer.limit();
            appReadBuffer.limit(appReadBuffer.position() + remaining);
            dst.put(appReadBuffer);
            appReadBuffer.limit(limit);
        }
        appReadBuffer.compact();
        return remaining;
    }

    protected int netReadBufferSize()
    {
        return sslEngine.getSession().getPacketBufferSize();
    }

    protected int netWriteBufferSize()
    {
        return sslEngine.getSession().getPacketBufferSize();
    }

    protected int applicationBufferSize()
    {
        return sslEngine.getSession().getApplicationBufferSize();
    }

    protected ByteBuffer netReadBuffer()
    {
        return netReadBuffer;
    }

    private void handshakeFailure()
    {
        //Release all resources such as internal buffers that SSLEngine is managing
        sslEngine.closeOutbound();
        try {
            sslEngine.closeInbound();
        }
        catch (SSLException e) {
            // log.debug("SSLEngine.closeInBound() raised an exception.", e);
        }
    }


    public boolean isMute()
    {
        return key.isValid() && (key.interestOps() & SelectionKey.OP_READ) == 0;
    }


    public boolean hasBytesBuffered()
    {
        return netReadBuffer.position() != 0 || appReadBuffer.position() != 0;
    }
}
