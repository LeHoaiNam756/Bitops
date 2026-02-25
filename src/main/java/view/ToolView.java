package view;

import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ToolView {
    public Label projectPath;
    public RadioButton statementCoverage;
    public RadioButton branchCoverage;
    public RadioButton mcdcCoverage;
    public TreeView<String> projectTree;
    public TableView<String> reportTable;

    public void handleUploadProject(ActionEvent actionEvent) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Upload Project (.zip)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP Projects", "*.zip"));
        File selected = chooser.showOpenDialog(projectTree.getScene().getWindow());
        if (selected != null) {
            projectPath.setText("Project: " + selected.getName());
            try {
                loadProjectFromZip(selected);
            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR, "Failed to load project: " + e.getMessage(),
                        ButtonType.OK).showAndWait();
            }
        }
    }

    private void loadProjectFromZip(File zipFile) throws IOException {
        Path tempRoot = Files.createTempDirectory("temp_storage_");

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path target = tempRoot.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }

        TreeItem<String> rootItem = createTree(tempRoot.toFile());
        rootItem.setValue(zipFile.getName());
        rootItem.setExpanded(true);

        projectTree.setRoot(rootItem);
        projectTree.setShowRoot(true);
    }

    public TreeItem<String> createTree(File file) {
        TreeItem<String> item = new TreeItem<>(file.getName());
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    item.getChildren().add(createTree(child));
                }
            }
        }
        return item;
    }
}
