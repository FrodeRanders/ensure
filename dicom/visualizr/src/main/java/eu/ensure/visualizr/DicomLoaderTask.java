package eu.ensure.visualizr;

import eu.ensure.visualizr.model.DicomLoader;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * Created by froran on 2016-01-28.
 */
public class DicomLoaderTask extends Task<DicomLoader> {
    private static final Logger log = LogManager.getLogger(DicomLoaderTask.class);

    private final File dicomFile;

    public DicomLoaderTask(File dicomFile){
        this.dicomFile = dicomFile;
    }

    @Override
    protected DicomLoader call() throws Exception {
        DicomLoader reader = new DicomLoader(dicomFile);
        this.updateProgress(1, 1);
        return reader;
    }
}
