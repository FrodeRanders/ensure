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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/*
 * Implements a data source that does not cache (large) files in memory.
 * <p/>
 * Is assumed to be used together with a javax.activation.DataHandler (like this)
 * <pre>
 *     InputStream is = request.getInputStream();
 *     StreamingDataSource ds = new StreamingDataSource("doc.xml", "text/xml", is);
 *     DataHandler dh = new javax.activation.DataHandler(ds);
 * </pre>
 * <p/>
 * Created by Frode Randers at 2012-11-29 15:37
 */
public class StreamingDataSource implements javax.activation.DataSource {

    private String name;
    private String mimeType;
    private InputStream inputStream;

    public StreamingDataSource(String name, String mimeType, InputStream inputStream) {
        this.name = name;
        this.mimeType = mimeType;
        this.inputStream = inputStream;
    }

    public String getName() {
            return name;
    }

    public String getContentType() {
        return mimeType;
    }

    public InputStream getInputStream() throws IOException {
        return inputStream;
    }

    public OutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException();
    }
}
