package eu.ensure.visualizr.treeview;

import eu.ensure.visualizr.model.DicomFile;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;

/**
 *
 */
public class TreeViewTask extends Task<TreeItem<TreeNode>> {
    private final List<DicomFile> files;
    private final TreeItem<TreeNode> root;

    public TreeViewTask(List<DicomFile> files, String rootName) {
        this.files = files;
        this.root = new TreeItem<>(new TreeNode(rootName));
    }

    private void addItemToTree(DicomFile dicomFile) {
        TreeItem<TreeNode> node = new TreeItem<TreeNode>(new TreeNode(dicomFile));

        if (root.getChildren().size() > 0) {
            root.getChildren().get(0).getChildren().add(node);
        } else {
            root.getChildren().add(node);
        }
    }

    @Override
    protected TreeItem<TreeNode> call() throws Exception {
        try{
            Collections.sort(this.files, new Comparator<DicomFile>() {
                public int compare(DicomFile o1, DicomFile o2) {
                    if (o1.getName().equals(o2.getName())) {
                        return o1.getName().compareTo(o2.getName());
                    } else {
                        if (o1.getName().startsWith(o2.getName())) {
                            return -1;
                        } else if (o2.getName().startsWith(o1.getName())) {
                            return 1;
                        } else {
                            return o1.getName().compareTo(o2.getName());
                        }
                    }
                }
            });

            for (DicomFile file : this.files){
                this.addItemToTree(file);
            }
            this.updateProgress(1, 1);
        }
        catch(Exception e){
            e.printStackTrace();
        }

        return this.root;
    }
}
