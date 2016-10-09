package eu.ensure.packproc.warc;

import eu.ensure.packproc.ProcessorException;
import eu.ensure.packproc.model.EntryHandler;
import eu.ensure.packproc.model.StructureEntry;
import eu.ensure.vopn.xml.Namespaces;
import org.apache.axiom.om.OMElement;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by froran on 2016-04-18.
 */
public interface ResourceHandler<T> extends EntryHandler {
    void process(
            StructureEntry entryWrapper, T entry, String contentType, InputStream inputStream,
            OMElement configuration, Namespaces namespaces
    ) throws ProcessorException, IOException;
}
