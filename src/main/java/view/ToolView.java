package view;

import OldConcolic.OldConcolicTesting;
import core.CFG.Utils.ASTHelper;
import core.TestGeneration.ConcolicTesting;
import core.TestGeneration.result.ParameterData;
import core.TestGeneration.result.TestData;
import core.TestGeneration.result.TestResult;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class ToolView {
    public Label projectPath;
    public RadioButton originalConcolic;
    public RadioButton greedyPathFinder;
    public RadioButton statementCoverage;
    public RadioButton branchCoverage;
    public RadioButton mcdcCoverage;
    public Label fullCoverageLabel;
    public Label memoryUsageLabel;
    public Label runtimeLabel;
    public TreeView<String> projectTree;
    public TableView<TestData> reportTable;
    public TableColumn<TestData, String> testInputsColumn;
    public TableColumn<TestData, String> testCoverageColumn;
    public TableColumn<TestData, String> testOutputsColumn;
    public ListView<String> sourceList;

    // Maps to keep track of which tree items correspond to which files/methods
    private final Map<TreeItem<String>, File> fileItemMap = new HashMap<>();
    private final Map<TreeItem<String>, MethodLocation> methodItemMap = new HashMap<>();
    private final Map<File, List<String>> fileContentCache = new HashMap<>();

    private File currentFile;

    private static class MethodLocation {
        final File file;
        final int startLine;
        final int endLine;
        final String methodName;

        MethodLocation(File file, int startLine, int endLine, String methodName) {
            this.file = file;
            this.startLine = startLine;
            this.endLine = endLine;
            this.methodName = methodName;
        }
    }

    @FXML
    public void initialize() {
        // 1. Setup selection modes
        sourceList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Toggle groups for mode and coverage
        ToggleGroup modeGroup = new ToggleGroup();
        originalConcolic.setToggleGroup(modeGroup);
        greedyPathFinder.setToggleGroup(modeGroup);
        originalConcolic.setSelected(true);

        ToggleGroup coverageGroup = new ToggleGroup();
        statementCoverage.setToggleGroup(coverageGroup);
        branchCoverage.setToggleGroup(coverageGroup);
        mcdcCoverage.setToggleGroup(coverageGroup);
        statementCoverage.setSelected(true);

        // Configure report table columns
        if (testInputsColumn != null) {
            testInputsColumn.setCellValueFactory(cd -> {
                TestData data = cd.getValue();
                if (data == null || data.getParameterDataList() == null) {
                    return new SimpleStringProperty("");
                }
                String inputs = data.getParameterDataList().stream()
                        .map(p -> p.getName() + ":" + String.valueOf(p.getValue()))
                        .collect(Collectors.joining(", "));
                return new SimpleStringProperty(inputs);
            });
        }
        if (testCoverageColumn != null) {
            testCoverageColumn.setCellValueFactory(cd -> {
                TestData data = cd.getValue();
                String cov = data != null ? String.format("%.2f%%", data.getUnitCoverage()) : "";
                return new SimpleStringProperty(cov);
            });
        }
        if (testOutputsColumn != null) {
            testOutputsColumn.setCellValueFactory(cd -> {
                TestData data = cd.getValue();
                Object output = data != null ? data.getOutput() : null;
                return new SimpleStringProperty(output == null ? "null" : output.toString());
            });
        }

        // 2. TreeView Listener
        projectTree.getSelectionModel().selectedItemProperty().addListener((obs,
                                                                            oldItem,
                                                                            newItem) -> {
            if (newItem == null) return;

            if (methodItemMap.containsKey(newItem)) {
                MethodLocation loc = methodItemMap.get(newItem);
                showFile(loc.file);
                highlightMethod(loc);
            } else if (fileItemMap.containsKey(newItem)) {
                File file = fileItemMap.get(newItem);
                showFile(file);
            }
        });

        // 3. Optimized Cell Factory
        sourceList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);

                    // Define styles once
                    String baseStyle = "-fx-font-family: 'monospace'; -fx-padding: 0 5 0 5;";
                    String highlightStyle = "-fx-background-color: #b3e5fc; -fx-text-fill: black;";

                    // Check selection state directly instead of adding listeners
                    if (isSelected()) {
                        setStyle(baseStyle + highlightStyle);
                    } else {
                        setStyle(baseStyle);
                    }
                }
            }
        });
    }
    public void handleUploadProject() {
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
        Path tempRoot = Files.createTempDirectory("ide_preview_");

        tempRoot.toFile().deleteOnExit();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path target = tempRoot.resolve(entry.getName()).normalize();

                if (!target.startsWith(tempRoot)) {
                    throw new IOException("Entry is outside of the target dir: " + entry.getName());
                }

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

        projectTree.setRoot(rootItem);
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
        } else if (file.getName().endsWith(".java")) {
            // Remember that this tree item represents a Java source file
            fileItemMap.put(item, file);
            analyzeJavaFile(item, file);
        }
        return item;
    }

    private void analyzeJavaFile(TreeItem<String> fileItem, File file) {
        try {
            CompilationUnit cu = getCompilationUnit(file);
            if (cu == null) {
                fileItem.getChildren().add(new TreeItem<>("[Error parsing file]" + file.getName()));
                return;
            }

            cu.accept(new ASTVisitor() {
                @Override
                public boolean visit(MethodDeclaration node) {
                    String methodName = node.getName().getIdentifier();
                    StringBuilder params = new StringBuilder("(");
                    for (int i = 0; i < node.parameters().size(); i++) {
                        SingleVariableDeclaration param = (SingleVariableDeclaration) node.parameters().get(i);
                        params.append(param.getType().toString());
                        if (i < node.parameters().size() - 1) {
                            params.append(", ");
                        }
                    }
                    params.append(")");

                    TreeItem<String> methodItem = new TreeItem<>(methodName + params);
                    fileItem.getChildren().add(methodItem);

                    int startLine = cu.getLineNumber(node.getStartPosition());
                    int endLine = cu.getLineNumber(node.getStartPosition() + node.getLength());
                    methodItemMap.put(methodItem, new MethodLocation(file, startLine, endLine, methodName));

                    return false;
                }
            });

        } catch (Exception e) {
            fileItem.getChildren().add(new TreeItem<>("[Error parsing: " + e.getMessage() + "]"));
        }
    }

    private CompilationUnit getCompilationUnit(File file) {
        try {
            String source = new String(Files.readAllBytes(file.toPath()));
            ASTParser parser = ASTParser.newParser(AST.JLS8);
            parser.setSource(source.toCharArray());
            parser.setKind(ASTParser.K_COMPILATION_UNIT);

            Map<String, String> options = JavaCore.getOptions();
            JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
            parser.setCompilerOptions(options);

            return (CompilationUnit) parser.createAST(null);
        } catch (IOException e) {
            System.err.println("Error reading file: " + file.getPath() + " - " + e.getMessage());
            return null;
        }
    }

    private void showFile(File file) {
        currentFile = file;
        List<String> lines = fileContentCache.get(file);
        if (lines == null) {
            try {
                lines = Files.readAllLines(file.toPath());
                fileContentCache.put(file, lines);
            } catch (IOException e) {
                sourceList.getItems().setAll("Error reading file: " + e.getMessage());
                return;
            }
        }
        sourceList.getItems().setAll(lines);
    }

    private void highlightMethod(MethodLocation loc) {
        if (currentFile == null || !currentFile.equals(loc.file)) {
            return;
        }

        // Java lines are 1-based, ListView rows are 0-based
        int startIndex = Math.max(0, loc.startLine - 1);
        int endIndex = Math.max(startIndex, loc.endLine - 1);

        MultipleSelectionModel<String> selectionModel = sourceList.getSelectionModel();
        selectionModel.clearSelection();
        selectionModel.selectRange(startIndex, endIndex + 1); // end is exclusive
        sourceList.scrollTo(startIndex);

    }

    public void handleRunConcolic() {
        TreeItem<String> selectedItem = projectTree.getSelectionModel().getSelectedItem();
        if (selectedItem == null || !methodItemMap.containsKey(selectedItem)) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Please select a method in the project tree to run concolic testing.",
                    ButtonType.OK).showAndWait();
            return;
        }

        MethodLocation loc = methodItemMap.get(selectedItem);
        File file = loc.file;
        String methodName = loc.methodName;

        ASTHelper.Coverage coverage = getSelectedCoverage();
        ConcolicTesting engine = createConcolicEngine();

        try {
            TestResult result = engine.runConcolicTesting(
                    1,
                    file.getAbsolutePath(),
                    file.getName(),
                    methodName,
                    coverage
            );
            updateSummary(result);
            updateReportTable(result);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    "Concolic testing failed: " + e.getMessage(),
                    ButtonType.OK).showAndWait();
            e.printStackTrace();
        }
    }

    private ASTHelper.Coverage getSelectedCoverage() {
        if (branchCoverage.isSelected()) {
            return ASTHelper.Coverage.BRANCH;
        }
        if (mcdcCoverage.isSelected()) {
            return ASTHelper.Coverage.MCDC;
        }
        return ASTHelper.Coverage.STATEMENT;
    }

    private ConcolicTesting createConcolicEngine() {
        if (greedyPathFinder.isSelected()) {
            // TODO: implement others if needed
            return new OldConcolicTesting();
        }
        return new ConcolicTesting();
    }

    private void updateSummary(TestResult result) {
        if (result == null) {
            return;
        }
        if (fullCoverageLabel != null) {
            fullCoverageLabel.setText(String.format("%.2f%%", result.getCoveragePercent()));
        }
        if (memoryUsageLabel != null) {
            memoryUsageLabel.setText(String.format("%.2f MB", result.getMemoryUsed()));
        }
        if (runtimeLabel != null) {
            runtimeLabel.setText(String.format("%.2f ms", result.getTimeToGenerate()));
        }
    }

    private void updateReportTable(TestResult result) {
        if (reportTable == null) {
            return;
        }
        List<TestData> data = result != null ? result.getFullTestData().stream().toList() : null;
        ObservableList<TestData> items = data == null
                ? FXCollections.observableArrayList()
                : FXCollections.observableArrayList(data);
        reportTable.setItems(items);
    }
}
