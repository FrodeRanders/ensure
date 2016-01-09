package eu.ensure.packproc.warc;

import org.apache.http.MessageConstraintException;
import org.apache.http.config.MessageConstraints;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.io.HttpTransportMetrics;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.util.ByteArrayBuffer;
import org.apache.http.util.CharArrayBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

/**
 * Created by froran on 2015-12-15.
 */
public class InputBuffer implements SessionInputBuffer {
    private final HttpTransportMetricsImpl metrics;
    private final byte[] buffer;
    private final ByteArrayBuffer linebuffer;
    private final int minChunkLimit;
    private final MessageConstraints constraints;
    private final CharsetDecoder decoder;
    private ReadableByteChannel inputChannel;
    private int bufferpos;
    private int bufferlen;
    private CharBuffer cbuf;


    public InputBuffer(HttpTransportMetricsImpl metrics, int buffersize, int minChunkLimit, MessageConstraints constraints, CharsetDecoder chardecoder) {
        this.metrics = metrics;
        this.buffer = new byte[buffersize];
        this.bufferpos = 0;
        this.bufferlen = 0;
        this.minChunkLimit = minChunkLimit >= 0 ? minChunkLimit : 512;
        this.constraints = constraints != null ? constraints : MessageConstraints.DEFAULT;
        this.linebuffer = new ByteArrayBuffer(buffersize);
        this.decoder = chardecoder;
    }

    public InputBuffer(HttpTransportMetricsImpl metrics, int buffersize) {
        this(metrics, buffersize, buffersize, (MessageConstraints) null, Charset.defaultCharset().newDecoder());
    }

    public void bind(final ReadableByteChannel inputChannel) {
        this.inputChannel = inputChannel;
    }

    public boolean isBound() {
        return null != this.inputChannel;
    }

    public int capacity() {
        return this.buffer.length;
    }

    public int length() {
        return this.bufferlen - this.bufferpos;
    }

    public int available() {
        return this.capacity() - this.length();
    }

    /*
     * Reads bytes from input channel
     *
     * @param      b     the buffer into which the data is read.
     * @param      off   the start offset in array <code>b</code>
     *                   at which the data is written.
     * @param      len   the maximum number of bytes to read.
     * @return     the total number of bytes read into the buffer, or
     *             <code>-1</code> if there is no more data because the end of
     *             the stream has been reached.
     */
    private int streamRead(byte[] b, int off, int len) throws IOException {

        ByteBuffer bytes = ByteBuffer.allocateDirect(len);
        int bytesRead = inputChannel.read(bytes);
        if (bytesRead > 0) {
            bytes.flip();
            //CharBuffer buf = decoder.decode(bytes);
            bytes.get(b, off, bytes.remaining());
            return bytesRead;
        }
        return -1; // EOF kinda'
    }

    public int fillBuffer() throws IOException {
        int l;
        if (this.bufferpos > 0) {
            l = this.bufferlen - this.bufferpos;
            if (l > 0) {
                System.arraycopy(this.buffer, this.bufferpos, this.buffer, 0, l);
            }

            this.bufferpos = 0;
            this.bufferlen = l;
        }

        int off = this.bufferlen;
        int len = this.buffer.length - off;
        l = this.streamRead(this.buffer, off, len);
        if (l == -1) {
            return -1;
        } else {
            this.bufferlen = off + l;
            this.metrics.incrementBytesTransferred((long)l);
            return l;
        }
    }

    public boolean hasBufferedData() {
        return this.bufferpos < this.bufferlen;
    }

    public void clear() {
        this.bufferpos = 0;
        this.bufferlen = 0;
    }

    // SessionInputBuffer.read()
    public int read() throws IOException {
        while (true) {
            if (!this.hasBufferedData()) {
                int noRead = this.fillBuffer();
                if (noRead != -1) {
                    continue;
                }

                return -1;
            }

            return this.buffer[this.bufferpos++] & 255;
        }
    }

    // SessionInputBuffer.read()
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            return 0;
        } else {
            int chunk;
            if (this.hasBufferedData()) {
                chunk = Math.min(len, this.bufferlen - this.bufferpos);
                System.arraycopy(this.buffer, this.bufferpos, b, off, chunk);
                this.bufferpos += chunk;
                return chunk;
            } else if (len > this.minChunkLimit) {
                chunk = this.streamRead(b, off, len);
                if (chunk > 0) {
                    this.metrics.incrementBytesTransferred((long)chunk);
                }

                return chunk;
            } else {
                do {
                    if (this.hasBufferedData()) {
                        chunk = Math.min(len, this.bufferlen - this.bufferpos);
                        System.arraycopy(this.buffer, this.bufferpos, b, off, chunk);
                        this.bufferpos += chunk;
                        return chunk;
                    }

                    chunk = this.fillBuffer();
                } while (chunk != -1);

                return -1;
            }
        }
    }

    // SessionInputBuffer.read()
    public int read(byte[] b) throws IOException {
        return b == null ? 0 : this.read(b, 0, b.length);
    }

    // SessionInputBuffer.readLine()
    public int readLine(CharArrayBuffer charbuffer) throws IOException {
        int maxLineLen = this.constraints.getMaxLineLength();
        int noRead = 0;
        boolean retry = true;

        while (retry) {
            int pos = -1;

            int len;
            for (len = this.bufferpos; len < this.bufferlen; ++len) {
                if (this.buffer[len] == 10) {
                    pos = len;
                    break;
                }
            }

            if (maxLineLen > 0) {
                len = this.linebuffer.length() + (pos > 0 ? pos : this.bufferlen) - this.bufferpos;
                if (len >= maxLineLen) {
                    throw new MessageConstraintException("Maximum line length limit exceeded");
                }
            }

            if (pos != -1) {
                if (this.linebuffer.isEmpty()) {
                    return this.lineFromReadBuffer(charbuffer, pos);
                }

                retry = false;
                len = pos + 1 - this.bufferpos;
                this.linebuffer.append(this.buffer, this.bufferpos, len);
                this.bufferpos = pos + 1;
            } else {
                if (this.hasBufferedData()) {
                    len = this.bufferlen - this.bufferpos;
                    this.linebuffer.append(this.buffer, this.bufferpos, len);
                    this.bufferpos = this.bufferlen;
                }

                noRead = this.fillBuffer();
                if (noRead == -1) {
                    retry = false;
                }
            }
        }

        if (noRead == -1 && this.linebuffer.isEmpty()) {
            return -1;
        } else {
            return this.lineFromLineBuffer(charbuffer);
        }
    }

    private int lineFromLineBuffer(CharArrayBuffer charbuffer) throws IOException {
        int len = this.linebuffer.length();
        if (len > 0) {
            if (this.linebuffer.byteAt(len - 1) == 10) {
                --len;
            }

            if (len > 0 && this.linebuffer.byteAt(len - 1) == 13) {
                --len;
            }
        }

        if (this.decoder == null) {
            charbuffer.append(this.linebuffer, 0, len);
        } else {
            ByteBuffer bbuf = ByteBuffer.wrap(this.linebuffer.buffer(), 0, len);
            len = this.appendDecoded(charbuffer, bbuf);
        }

        this.linebuffer.clear();
        return len;
    }

    private int lineFromReadBuffer(CharArrayBuffer charbuffer, int position) throws IOException {
        int pos = position;
        int off = this.bufferpos;
        this.bufferpos = position + 1;
        if (position > off && this.buffer[position - 1] == 13) {
            pos = position - 1;
        }

        int len = pos - off;
        if (this.decoder == null) {
            charbuffer.append(this.buffer, off, len);
        } else {
            ByteBuffer bbuf = ByteBuffer.wrap(this.buffer, off, len);
            len = this.appendDecoded(charbuffer, bbuf);
        }

        return len;
    }

    private int appendDecoded(CharArrayBuffer charbuffer, ByteBuffer bbuf) throws IOException {
        if (!bbuf.hasRemaining()) {
            return 0;
        } else {
            if (this.cbuf == null) {
                this.cbuf = CharBuffer.allocate(1024);
            }

            this.decoder.reset();

            int len;
            CoderResult result;
            for (len = 0; bbuf.hasRemaining(); len += this.handleDecodingResult(result, charbuffer, bbuf)) {
                result = this.decoder.decode(bbuf, this.cbuf, true);
            }

            result = this.decoder.flush(this.cbuf);
            len += this.handleDecodingResult(result, charbuffer, bbuf);
            this.cbuf.clear();
            return len;
        }
    }

    private int handleDecodingResult(CoderResult result, CharArrayBuffer charbuffer, ByteBuffer bbuf) throws IOException {
        if (result.isError()) {
            result.throwException();
        }

        this.cbuf.flip();
        int len = this.cbuf.remaining();

        while (this.cbuf.hasRemaining()) {
            charbuffer.append(this.cbuf.get());
        }

        this.cbuf.compact();
        return len;
    }

    // SessionInputBuffer.readLine()
    public String readLine() throws IOException {
        CharArrayBuffer charbuffer = new CharArrayBuffer(64);
        int l = this.readLine(charbuffer);
        return l != -1 ? charbuffer.toString() : null;
    }

    // SessionInputBuffer.isDataAvailable()
    public boolean isDataAvailable(int timeout) throws IOException {
        return this.hasBufferedData();
    }

    // SessionInputBuffer.getMetrics()
    public HttpTransportMetrics getMetrics() {
        return this.metrics;
    }
}

