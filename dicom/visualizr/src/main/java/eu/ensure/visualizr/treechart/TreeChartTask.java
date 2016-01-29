package eu.ensure.visualizr.treechart;

import eu.ensure.visualizr.VisualizrGuiController;
import eu.ensure.visualizr.model.DicomFile;
import eu.ensure.visualizr.model.DicomObject;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.paint.Color;

import java.io.IOException;

import de.chimos.ui.treechart.layout.NodePosition;
import de.chimos.ui.treechart.layout.TreePane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TreeChartTask extends Task<TreeChartTask.DicomObjectTreeChart> {
    private static final Logger log = LogManager.getLogger(TreeChartTask.class);

    public class DicomObjectTreeChart {
        private TreePane pane;
        private String message = "";
        private Color messageColor = Color.BLACK;

        public DicomObjectTreeChart(TreePane pane) {
            this.pane = pane;
        }

        public DicomObjectTreeChart(TreePane pane, String message) {
            this(pane);
            this.message = message;
        }

        public DicomObjectTreeChart(TreePane pane, String message, Color messageColor) {
            this(pane, message);
            this.messageColor = messageColor;
        }

        public String getMessage() {
            return this.message;
        }

        public Color getMessageColor() {
            return this.messageColor;
        }

        public TreePane getTreePane() {
            return this.pane;
        }
    }

    private final VisualizrGuiController caller;
    private final DicomFile rootDicomFile;

    private static final double X_SPACING = 80d;
    private static final double Y_SPACING = 100d;

    public TreeChartTask(DicomFile dicomFile, VisualizrGuiController caller) {
        this.rootDicomFile = dicomFile;
        this.caller = caller;
    }

    /**
     */
    private Node loadDicomObject(DicomObject dicomObject) {
        Node element = null;
        try {
            log.debug("Putting " + dicomObject.getName() + " onto chart area");
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("TreeNode.fxml"));
            element = fxmlLoader.load();

            TreeNodeController elementController = fxmlLoader.<TreeNodeController>getController();
            elementController.setDicomObject(dicomObject);
            elementController.setParentController(caller);

        } catch (IOException ex) {
            String info = "Could not load DICOM object onto chart area: " + dicomObject.getName();
            log.error(info, ex);
        }
        return element;
    }


    /**
     */
    private void loadTreeNodeChildren(DicomObject dicomObject, TreePane treePane, NodePosition parentPosition) throws Exception {

        NodePosition position;
        if (null == parentPosition) {
            position = NodePosition.ROOT;
        } else {
            position = parentPosition.getChild(0);
        }

        final Node node = loadDicomObject(dicomObject);
        treePane.addChild(node, position);

        int childIndex = 0;
        for (DicomObject sequence : dicomObject.getSequences()) {
            NodePosition childPosition = position.getChild(childIndex++);
            loadTreeNodeChildren(sequence, treePane, childPosition);
        }
    }

    @Override
    protected DicomObjectTreeChart call() throws Exception {
        TreePane treePane = new TreePane();
        treePane.setXAxisSpacing(X_SPACING);
        treePane.setYAxisSpacing(Y_SPACING);

        log.debug("Building tree chart");

        loadTreeNodeChildren(rootDicomFile.getRootObject(), treePane, null);

        this.updateProgress(1, 1);
        return new DicomObjectTreeChart(treePane);
    }
}
