package eu.ensure.packproc.warc;

import eu.ensure.packproc.ProcessorException;
import eu.ensure.packproc.ProcessorManager;
import eu.ensure.packproc.model.StructureEntry;
import org.gautelis.vopn.xml.Namespaces;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Created by froran on 2016-04-18.
 */
public class ResourceIndexer implements ResourceHandler<HttpResponse> {
    private static final Logger log = LogManager.getLogger(ResourceIndexer.class);
    private static final Charset ascii = Charset.forName("ASCII");

    private final ProcessorManager manager;

    public ResourceIndexer(ProcessorManager manager) {
        this.manager = manager;
    }

    public void process(
            StructureEntry entry, HttpResponse httpResponse, String contentType, InputStream inputStream,
            OMElement configuration, Namespaces namespaces
    ) throws ProcessorException, IOException {

        // We need a 'type'-attribute (the XPath expression)
        OMAttribute expr = configuration.getAttribute(new QName("type"));
        if (null == expr) {
            throw new ProcessorException("Could not locate the 'type'-attribute to the <index /> operation");
        }

        // TODO? Use content type info to select decoder: text/xml;charset=UTF-8
        String type = manager.resolve(expr.getAttributeValue());

        if (contentType.startsWith(type)) {
            byte[] buf = new byte[0x2000];
            int bytesRead;
            while ((bytesRead = inputStream.read(buf)) > 0) {
                ByteBuffer bytes = ByteBuffer.wrap(buf, 0, bytesRead);

            /*
             * TODO: Here is where we handle different types of input.
             *       Right now, this is only a stub.
             */
                if (contentType.startsWith("text")) {
                    // Since we are reading blocks, we may split chars
                    // which will make the decoder crash. We will ignore
                    // this since we are only doing this for test purposes
                    try {
                        String data = ascii.decode(bytes).toString();
                        log.debug("index: \n" + data);

                    } catch (Throwable ignore) {
                        log.debug("index <text> chunk");
                    }
                } else {
                    log.debug("index <binary data> chunk");
                }

                bytes.clear();
            }
        } else {
            log.debug("Not indexing content of type: " + contentType);
        }
    }

}
