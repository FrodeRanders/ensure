package eu.ensure.visualizr;

import eu.ensure.commons.lang.LoggingUtils;
import junit.framework.TestCase;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

/**
 * Created by froran on 2016-01-28.
 */
public class VisualizrTest extends TestCase {
    private static final Logger log = LoggingUtils.setupLoggingFor(VisualizrTest.class, "log4j2.xml");

    @Test
    public void testVisualizr()
    {
        VisualizrGui.main(new String[] {});
    }


}

