package eu.ensure.visualizr.treechart;

import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.ResourceBundle;

import eu.ensure.visualizr.VisualizrGuiController;
import eu.ensure.visualizr.model.DicomObject;
import eu.ensure.visualizr.model.DicomTag;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
//import javafx.scene.control.Tooltip;
//import javafx.scene.paint.Color;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * FXML Controller class
 *
 * @author Karl
 */
public class TreeNodeController implements Initializable {
    private static final Logger log = LogManager.getLogger(TreeNodeController.class);

    @FXML
    public TitledPane titlePane;
    @FXML
    public ListView tagList;

    private DicomObject dicomObject;
    private ObservableList<DicomTag> observableTags;
    private SimpleStringProperty title;
    private SimpleStringProperty titlePre;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        observableTags = FXCollections.observableArrayList();
        tagList.setItems(observableTags);
        tagList.setCellFactory(new Callback<ListView<DicomTag>, ListCell<DicomTag>>(){
            @Override
            public ListCell<DicomTag> call(ListView<DicomTag> p) {
                final Tooltip tooltip = new Tooltip();
                final ListCell<DicomTag> cell = new ListCell<DicomTag>() {
                    @Override
                    public void updateItem(DicomTag item, boolean empty){
                        super.updateItem(item, empty);
                        if(!empty){
                            this.setText(item.getId() + " " + item.getDescription());
                            if (item.isPrivate()) {
                                this.setTextFill(Color.DARKRED);
                            }

                            //this.setUnderline(item.isStatic());

                            tooltip.setText(item.getValue());
                            this.setTooltip(tooltip);
                            this.setCursor(Cursor.HAND);
                        }
                    }
                };
                return cell;
            }
        });

        this.title = new SimpleStringProperty("");
        this.titlePre = new SimpleStringProperty("");
        this.titlePane.textProperty().bind(this.titlePre.concat(this.title));
    }

    /**
     *
     *
     */
    public void setDicomObject(DicomObject dicomObject){
        this.dicomObject = dicomObject;

        String name = dicomObject.getName();
        title.setValue(name);

        try {
            this.observableTags.addAll(dicomObject.getDicomTags());
            Collections.sort(this.observableTags, new Comparator<DicomTag>(){
                @Override
                public int compare(DicomTag t, DicomTag t1) {
                    return t.getId().compareTo(t1.getId());
                }
            });
        } catch (Exception ex) {
            String info = "Could not observe loading of DICOM object: " + name;
            log.info(info, ex);

            this.observableTags.clear();
            tagList.setDisable(true);
        }
    }

    public void setTitlePrefix(String prefix){
        this.titlePre.set(prefix);
    }

    public void setParentController(final VisualizrGuiController controller){
        tagList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<DicomTag>() {
            @Override
            public void changed(ObservableValue observableObject, DicomTag oldValue, DicomTag newValue) {
                // TODO
                //controller.loadTreeChart(newValue.getType());
            }
        });
    }
}
