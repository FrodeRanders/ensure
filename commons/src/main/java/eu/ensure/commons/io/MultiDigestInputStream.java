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

import eu.ensure.commons.lang.Stacktrace;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.zip.CRC32;

/*
 * Description of MultiDigestInputStream
 * <p>
 * <p>
 * Created by Frode Randers at 2012-02-29 10:12
 */
public class MultiDigestInputStream extends FilterInputStream {

    private final static MessageDigest[] DIGEST_ARRAY_TEMPLATE = new MessageDigest[] {};

    private CRC32 crc32 = null;
    private MessageDigest[] digests = null;

    private long size = 0L;

    public MultiDigestInputStream(InputStream is) {
        super(is);

        // This is just a silly default, we will calculate the digest using several
        // algorithms
        crc32 = new CRC32();

        List<MessageDigest> digestList = new Vector<MessageDigest>();
        try {
            digestList.add(MessageDigest.getInstance("MD5"));
            digestList.add(MessageDigest.getInstance("SHA-1"));
            digestList.add(MessageDigest.getInstance("SHA-512"));
        } catch (NoSuchAlgorithmException ignore) {
        }
        digests = digestList.toArray(new MessageDigest[digestList.size()]); // may be empty if exception
    }

    /**
     * This is the preferred constructor
     * <p>
     * @param algorithms
     * @param is
     */
    public MultiDigestInputStream(String[] algorithms, InputStream is) {
        super(is);
        List<MessageDigest> digestList = new Vector<MessageDigest>();
        for (String algorithm : algorithms) {
            if (algorithm.equalsIgnoreCase("CRC32") || algorithm.equalsIgnoreCase("CRC-32")) {
                crc32 = new CRC32();
            } else {
                try {
                    digestList.add(MessageDigest.getInstance(algorithm));
                } catch (NoSuchAlgorithmException ignore) {
                }
            }
        }
        digests = digestList.toArray(new MessageDigest[digestList.size()]); // may be empty if exception
    }


    public Map<String,byte[]> getDigests() {
        Map<String, byte[]> map = new HashMap<String, byte[]>();

        // Handle CRC32
        if (null != crc32) {
            byte b[] = new byte[8];
            ByteBuffer buf = ByteBuffer.wrap(b);
            buf.putLong(crc32.getValue());
            map.put("CRC32", b);
        }

        // Handle the rest
        for (MessageDigest digest : digests) {
            map.put(digest.getAlgorithm(), digest.digest());
        }

        return map;
    }

    public long getSize() {
        return size;
    }

    @Override
    public boolean markSupported() {
        return false; // Very important!!!
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        ++size;

        if (b >= 0) {
            // Update digests
            if (null != crc32) {
                try {
                    crc32.update(b);
                }
                catch (ArrayIndexOutOfBoundsException aioob) {
                    Throwable baseCause = Stacktrace.getBaseCause(aioob);
                    String info = "Failed to update CRC32 digest: " + baseCause.getMessage();
                    throw new IOException(info, aioob);
                }
            }
            for (MessageDigest digest : digests) {
                digest.update((byte) b);
            }
        }

        return b;
    }

    @Override
    public int read(byte[] bytes) throws IOException {
        int actualLength = super.read(bytes);
        size += actualLength;

        if (actualLength > 0) {
            // Update digests
            if (null != crc32) {
                try {
                    crc32.update(bytes, /* offset */ 0, actualLength);
                }
                catch (ArrayIndexOutOfBoundsException aioob) {
                    Throwable baseCause = Stacktrace.getBaseCause(aioob);
                    String info = "Failed to update CRC32 digest: " + baseCause.getMessage();
                    throw new IOException(info, aioob);
                }
            }
            for (MessageDigest digest : digests) {
                digest.update(bytes, /* offset */ 0, actualLength);
            }
        }

        return actualLength;
    }

    @Override
    public int read(byte[] bytes, int off, int len) throws IOException {
        int actualLength = super.read(bytes, off, len);
        size += actualLength;

        if (actualLength > 0) {
            // Update digests
            if (null != crc32) {
                try {
                    crc32.update(bytes, off, actualLength);
                }
                catch (ArrayIndexOutOfBoundsException aioob) {
                    Throwable baseCause = Stacktrace.getBaseCause(aioob);
                    String info = "Failed to update CRC32 digest: " + baseCause.getMessage();
                    throw new IOException(info, aioob);
                }
            }
            for (MessageDigest digest : digests) {
                digest.update(bytes, off, actualLength);
            }
        }

        return actualLength;
    }
}
