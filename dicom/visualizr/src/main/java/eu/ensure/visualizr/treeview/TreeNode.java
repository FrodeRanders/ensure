package eu.ensure.visualizr.treeview;

import eu.ensure.visualizr.model.DicomFile;

/**
 *
 */
public class TreeNode{
    private final String name;
    private final DicomFile dicomFile;

    /**
     *
     * @param name
     */
    public TreeNode(String name) {
        this.name = name;
        this.dicomFile = null;
    }

    public TreeNode(DicomFile dicomFile) {
        this.dicomFile = dicomFile;
        this.name = dicomFile.getName();
    }

    public boolean isDICOMDIR() {
        return "DICOMDIR".equals(getName());
    }

    public String getName() {
        return name;
    }

    public DicomFile getDicomFile() {
        return dicomFile;
    }
}
