/*
 * Copyright (C) 2012-2014 Frode Randers
 * All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The research leading to the implementation of this software package
 * has received funding from the European Community´s Seventh Framework
 * Programme (FP7/2007-2013) under grant agreement n° 270000.
 *
 * Frode Randers was at the time of creation of this software module
 * employed as a doctoral student by Luleå University of Technology
 * and remains the copyright holder of this material due to the
 * Teachers Exemption expressed in Swedish law (LAU 1949:345)
 */
package  eu.ensure.commons.io;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;

/**
 * Various handy file IO related functions.
 * <p>
 * Created by Frode Randers at 2012-12-18 01:09
 */
public class FileIO {

    /**
     * A nice one: http://thomaswabner.wordpress.com/2007/10/09/fast-stream-copy-using-javanio-channels/
     */
    public static void fastChannelCopy(final ReadableByteChannel src, final WritableByteChannel dest) throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
        while (src.read(buffer) != -1) {
            // prepare the buffer to be drained
            buffer.flip();
            // write to the channel, may block
            dest.write(buffer);
            // If partial transfer, shift remainder down
            // If buffer is empty, same as doing clear()
            buffer.compact();
        }
        // EOF will leave buffer in fill state
        buffer.flip();
        // make sure the buffer is fully drained.
        while (buffer.hasRemaining()) {
            dest.write(buffer);
        }
    }

    /**
     * Copies a file or a directory (including subdirectories)
     */
    public static void copy(final File src, final File dest) throws IOException {
        if (src.isDirectory()) {
            if (!dest.exists()) {
                dest.mkdir();
            }

            // Copy everything in directory
            String[] children = src.list();
            for (int i = 0; i < children.length; i++) {
                copy(new File(src, children[i]), new File(dest, children[i]));
            }
        } else {
            ReadableByteChannel in = null;
            WritableByteChannel out = null;
            try {
                in = Channels.newChannel(new FileInputStream(src));
                out = Channels.newChannel(new FileOutputStream(dest));

                fastChannelCopy(in, out);
            } finally {
                if (null != out) out.close();
                if (null != in) in.close();
            }
        }
    }

    /**
     * Writes from an InputStream to a file
     */
    public static File writeToFile(InputStream inputStream, File file) throws IOException {

        RandomAccessFile raf = null;
        try {
            // Wrap the input stream in some nice garment
            ReadableByteChannel inputChannel = Channels.newChannel(inputStream);

            // Wrap file in a file channel
            raf = new RandomAccessFile(file, "rw");
            FileChannel fileChannel = raf.getChannel();

            // Transfer from input stream to file
            fastChannelCopy(inputChannel, fileChannel);

        } finally {
            // Close resources and such
            if (null != raf) raf.close(); // closes fileChannel as well
        }

        return file;
    }


    /**
     * Writes from an InputStream to a temporary file
     */
    public static File writeToTempFile(InputStream inputStream, String prefix, String suffix) throws IOException {

        File file = File.createTempFile(prefix, "." + suffix);
        writeToFile(inputStream, file);
        return file;
    }

    /**
     * Writes from a String to a temporary file
     */
    public static File writeToTempFile(String buf, String prefix, String suffix) throws IOException {

        InputStream is = new ByteArrayInputStream(buf.getBytes("UTF-8"));
        return writeToTempFile(is, prefix, suffix);
    }

    /**
     * Writes a ByteBuffer (internally a series of byte[]) to a temporary file
     */
    public static File writeToTempFile(ByteBuffer byteBuffer, String prefix, String suffix) throws IOException {
        File tmpOutputFile = null;
        RandomAccessFile outputRaf = null;
        try {
            // Create temporary file
            tmpOutputFile = File.createTempFile(prefix, "." + suffix);
            outputRaf = new RandomAccessFile(tmpOutputFile, "rw");
            FileChannel outputChannel = outputRaf.getChannel();

            outputChannel.write(byteBuffer);

        } catch (IOException ioe) {
            String info = "Failed to write to temporary file: " + ioe.getMessage();
            throw new IOException(info, ioe);

        } finally {
            // Close temporary resources and such
            try {
                if (null != outputRaf) outputRaf.close(); // closes the associated FileChannel as well
            } catch (Throwable ignore) {
            }
        }
        return tmpOutputFile;
    }

    /**
     * Writes a list of byte[] to a temporary file
     */
    public static File writeToTempFile(List<byte[]> bytesList, String prefix, String suffix) throws IOException {
        File tmpOutputFile = null;
        RandomAccessFile outputRaf = null;
        try {
            // Create temporary file
            tmpOutputFile = File.createTempFile(prefix, "." + suffix);
            outputRaf = new RandomAccessFile(tmpOutputFile, "rw");
            FileChannel outputChannel = outputRaf.getChannel();

            for (byte[] bytes : bytesList) {
                ByteArrayInputStream is = null;
                try {
                    is = new ByteArrayInputStream(bytes);
                    ReadableByteChannel inputChannel = Channels.newChannel(is);
                    fastChannelCopy(inputChannel, outputChannel);
                } catch (Exception e) {
                    String info = "Failed to write to temporary file: " + e.getMessage();
                    throw new IOException(info, e);
                } finally {
                    if (null != is) is.close();
                }
            }
        } catch (IOException ioe) {
            String info = "Failed to write to temporary file: " + ioe.getMessage();
            throw new IOException(info, ioe);

        } finally {
            // Close temporary resources and such
            try {
                if (null != outputRaf) outputRaf.close(); // closes the associated FileChannel as well
            } catch (Throwable ignore) {
            }
        }
        return tmpOutputFile;
    }

    /**
     * Removes a file or, if a directory, a directory substructure...
     * <p>
     * @param d a file or a directory
     */
    public static boolean delete(File d) {
        if (null == d || !d.exists())
            return true; // by definition

        if (d.isDirectory()) {
            File[] files = d.listFiles(); // and directories
            if (null != files) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        delete(f);
                    } else {
                        f.delete();
                    }
                }
            }
        }
        return d.delete();
    }

    /**
     * Retrieves file from a remote location identified by a URL.
     * <p>
     *
     * @param url
     * @return
     * @throws IOException
     */
    public static File getRemoteFile(URL url, boolean keepAlive) throws IOException {
        File downloadedFile = null;

        ReadableByteChannel inputChannel = null;
        WritableByteChannel outputChannel = null;
        try {
            downloadedFile = File.createTempFile("downloaded-", ".bytes");

            URLConnection conn = url.openConnection();
            if (keepAlive) {
                conn.setRequestProperty("connection", "Keep-Alive");
            }
            conn.setUseCaches(false);

            inputChannel = Channels.newChannel(conn.getInputStream());
            outputChannel = Channels.newChannel(new FileOutputStream(downloadedFile));
            fastChannelCopy(inputChannel, outputChannel);
        } finally {
            if (null != outputChannel) outputChannel.close();
            if (null != inputChannel) inputChannel.close();
        }
        return downloadedFile;
    }
}
