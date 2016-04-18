/*
 * Copyright (C) 2015 Frode Randers
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
 */
package eu.ensure.packproc.warc;

import eu.ensure.packproc.model.StructureEntry;
import org.archive.format.warc.WARCConstants;
import org.archive.io.ArchiveRecord;
import org.archive.io.ArchiveRecordHeader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/*
    WARC/1.0
    WARC-Type: request
    WARC-Target-URI: http://www.archive.org/robots.txt
    Content-Type: application/http;msgtype=request
    WARC-Date: 2013-10-21T21:53:06Z
    WARC-Record-ID: <urn:uuid:3BD0AB9E-EAE8-4611-AE67-C70CEC7A6FEA>
    WARC-IP-Address: 207.241.224.2
    WARC-Warcinfo-ID: <urn:uuid:69E776C2-8240-45C7-B73D-82B6263E3C21>
    WARC-Block-Digest: sha1:CPCUG5OU46Y5YHPTFCZLZV465AFPFJYY
    Content-Length: 126

    GET /robots.txt HTTP/1.1
    User-Agent: Wget/1.14 (darwin11.4.0)
    Accept: * / *
    Host: www.archive.org
    Connection: Keep-Alive



    WARC/1.0
    WARC-Type: response
    WARC-Record-ID: <urn:uuid:9143347B-BAB5-4E84-80EC-A2E4072CA1CC>
    WARC-Warcinfo-ID: <urn:uuid:69E776C2-8240-45C7-B73D-82B6263E3C21>
    WARC-Concurrent-To: <urn:uuid:3BD0AB9E-EAE8-4611-AE67-C70CEC7A6FEA>
    WARC-Target-URI: http://www.archive.org/robots.txt
    WARC-Date: 2013-10-21T21:53:06Z
    WARC-IP-Address: 207.241.224.2
    WARC-Block-Digest: sha1:3L4DY55OVKT2IEHZEKOSIXRCQKJ7MNIE
    WARC-Payload-Digest: sha1:U32DBUPBIGUHJ4QE32J6G7BWBRHTBNE4
    Content-Type: application/http;msgtype=response
    Content-Length: 435

    HTTP/1.1 302 Moved Temporarily
    Server: nginx/1.1.19
    Date: Mon, 21 Oct 2013 21:53:06 GMT
    Content-Type: text/html
    Content-Length: 161
    Connection: keep-alive
    Location: http://archive.org/robots.txt
    Expires: Tue, 22 Oct 2013 03:53:06 GMT
    Cache-Control: max-age=21600

    <html>
    <head><title>302 Found</title></head>
    <body bgcolor="white">
    <center><h1>302 Found</h1></center>
    <hr><center>nginx/1.1.19</center>
    </body>
    </html>
*/
public class WarcRecordEntry extends StructureEntry<ArchiveRecord> {
    private ArchiveRecord entry = null;
    private ArchiveRecordHeader header = null;

    public static final String RESPONSE = WARCConstants.WARCRecordType.response.name();

    /**
     * This constructor is used to wrap a File.
     * <p>
     * @param entry
     */
    public WarcRecordEntry(ArchiveRecord entry) {
        this.entry = entry;
        this.header = entry.getHeader();
    }

    @Override
    public  String getName() {
        String tmp = (String) header.getHeaderValue(WARCConstants.HEADER_KEY_FILENAME);
        if (null == tmp || tmp.length() == 0)
            tmp = getUrl();
        return tmp;
    }

    @Override
    public  boolean isDirectory() {
        return false;
    }

    @Override
    public long getSize() {
        return header.getContentLength();
    }

    @Override
    public ArchiveRecord getWrappedObject() {
        return entry;
    }


    public String getType() {
        return (String) header.getHeaderValue(WARCConstants.HEADER_KEY_TYPE);
    }

    public String getUrl() {
        return (String) header.getHeaderValue(WARCConstants.HEADER_KEY_URI);
    }

    public String getRecordID() {
        return (String) header.getHeaderValue(WARCConstants.HEADER_KEY_ID);
    }

    public boolean isResponseRecord() {
        return RESPONSE.equalsIgnoreCase((String)header.getHeaderValue(WARCConstants.HEADER_KEY_TYPE));
    }

    public String getContentType() {
        return (String)header.getHeaderValue(WARCConstants.CONTENT_TYPE);
    }

    public InputStream getInputStream() {
        return entry;
    }
}
