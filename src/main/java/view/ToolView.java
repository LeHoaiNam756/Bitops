package view;

import core.cfg.Coverage;
import core.generation.ConcolicTesting;
import core.generation.Project;
import core.generation.RandomTestInput;
import core.instrument.InstrumentationPlan;
import core.instrument.InstrumentationPlanner;
import core.instrument.TracePoint;
import core.parser.ParseEntry;
import core.testdriver.TestData;
import core.testdriver.TestResult;
import core.testpath.AllPathsFinder;
import core.testpath.LoopCondensationFlowPathFinder;
import core.testpath.PathFinder;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;


public class ToolView {
    public Label projectPath;
    public RadioButton originalConcolic;
    public RadioButton greedyPathFinder;
    public RadioButton flowNetworkPathFinder;
    public RadioButton statementCoverage;
    public RadioButton branchCoverage;
    public RadioButton mcdcCoverage;
    public Label fullCoverageLabel;
    public Label memoryUsageLabel;
    public Label runtimeLabel;
    public TreeView<String> projectTree;
    public TableView<FormattedTestData> reportTable;
    public TableColumn<FormattedTestData, String> testInputsColumn;
    public TableColumn<FormattedTestData, String> testCoverageColumn;
    public TableColumn<FormattedTestData, String> testOutputsColumn;
    public ListView<String> sourceList;

    // Maps to keep track of which tree items correspond to which files/methods
    private final Map<TreeItem<String>, File> fileItemMap = new HashMap<>();
    private final Map<TreeItem<String>, MethodLocation> methodItemMap = new HashMap<>();
    private final Map<File, List<String>> fileContentCache = new HashMap<>();
    private final EnumMap<LineCoverageState, Set<Integer>> highlightedCoverageLines =
            new EnumMap<>(LineCoverageState.class);

    private File currentFile;
    private Project currentProject;
    private MethodLocation currentMethodLocation;
    private TestResult currentTestResult;
    private InstrumentationPlan currentInstrumentationPlan;

    private static class MethodLocation {
        final File file;
        final int startLine;
        final int endLine;
        final String methodName;
        final MethodDeclaration methodDeclaration;

        MethodLocation(File file, int startLine, int endLine, String methodName,
                       MethodDeclaration methodDeclaration) {
            this.file = file;
            this.startLine = startLine;
            this.endLine = endLine;
            this.methodName = methodName;
            this.methodDeclaration = methodDeclaration;
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
        flowNetworkPathFinder.setToggleGroup(modeGroup);
        originalConcolic.setSelected(true);

        ToggleGroup coverageGroup = new ToggleGroup();
        statementCoverage.setToggleGroup(coverageGroup);
        branchCoverage.setToggleGroup(coverageGroup);
        mcdcCoverage.setToggleGroup(coverageGroup);
        statementCoverage.setSelected(true);

        // Configure report table columns
        if (testInputsColumn != null) {
            testInputsColumn.setCellValueFactory(cd -> {
                FormattedTestData data = cd.getValue();
                if (data == null || data.input() == null) {
                    return new SimpleStringProperty("");
                }
                String inputs = data.input().entrySet().stream()
                        .map(entry -> entry.getKey() + ":" + formatValue(entry.getValue()))
                        .collect(Collectors.joining(", "));
                return new SimpleStringProperty(inputs);
            });
        }
        if (testCoverageColumn != null) {
            testCoverageColumn.setCellValueFactory(cd -> {
                FormattedTestData data = cd.getValue();
                String cov = data != null ? String.valueOf(data.coverage()) : "0";
                return new SimpleStringProperty(cov);
            });
        }
        if (testOutputsColumn != null) {
            testOutputsColumn.setCellValueFactory(cd -> {
                FormattedTestData data = cd.getValue();
                String output = data != null ? data.output() : "";
                return new SimpleStringProperty(output == null ? "null" : output);
            });
        }
        if (reportTable != null) {
            reportTable.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, newRow) -> {
                if (newRow != null) {
                    highlightTestCoverage(newRow);
                }
            });
        }
        if (fullCoverageLabel != null) {
            fullCoverageLabel.setOnMouseClicked(event -> highlightFullCoverage());
            fullCoverageLabel.setStyle(fullCoverageLabel.getStyle() + "; -fx-cursor: hand;");
        }

        // 2. TreeView Listener
        projectTree.getSelectionModel().selectedItemProperty().addListener((obs,
                                                                            oldItem,
                                                                            newItem) -> {
            if (newItem == null) return;

            if (methodItemMap.containsKey(newItem)) {
                MethodLocation loc = methodItemMap.get(newItem);
                currentMethodLocation = loc;
                showFile(loc.file);
                highlightMethod(loc);
            } else if (fileItemMap.containsKey(newItem)) {
                File file = fileItemMap.get(newItem);
                currentMethodLocation = null;
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

                    String baseStyle = "-fx-font-family: 'monospace'; -fx-padding: 0 5 0 5;";
                    String style = baseStyle;
                    LineCoverageState state = coverageStateForLine(getIndex());
                    if (state != null) {
                        style += state.style();
                    } else if (isSelected()) {
                        style += "-fx-background-color: #b3e5fc; -fx-text-fill: black;";
                    }

                    setStyle(style);
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
        fileItemMap.clear();
        methodItemMap.clear();
        fileContentCache.clear();
        clearRunState();

        currentProject = new Project(zipFile.toPath());
        TreeItem<String> rootItem = new TreeItem<>(zipFile.getName());
        rootItem.setExpanded(true);

        for (ParseEntry entry : currentProject.getEntries()) {
            TreeItem<String> fileItem = new TreeItem<>(entry.relativePath().toString());
            fileItemMap.put(fileItem, entry.absolutePath().toFile());
            analyzeParseEntry(fileItem, entry);
            rootItem.getChildren().add(fileItem);
        }

        projectTree.setRoot(rootItem);
    }


    private void analyzeParseEntry(TreeItem<String> fileItem, ParseEntry entry) {
        try {
            CompilationUnit cu = entry.compilationUnit();
            File file = entry.absolutePath().toFile();

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
                    methodItemMap.put(methodItem,
                            new MethodLocation(file, startLine, endLine, methodName, node));

                    return false;
                }
            });

        } catch (Exception e) {
            fileItem.getChildren().add(new TreeItem<>("[Error parsing: " + e.getMessage() + "]"));
        }
    }

    private void showFile(File file) {
        currentFile = file;
        clearCoverageHighlight();
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
        clearCoverageHighlight();

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

        Coverage coverage = getSelectedCoverage();

        try {
            CompilationUnit rootAst = currentProject.getRootAST(loc.methodDeclaration);
            if (rootAst == null) {
                throw new IllegalStateException("Cannot find root AST for selected method: " + loc.methodName);
            }

            TestResult result = ConcolicTesting.getInstance().generate(
                    loc.methodDeclaration,
                    rootAst,
                    coverage,
                    RandomTestInput.createRandomTestData(loc.methodDeclaration),
                    getSelectedPathFinder()
            );

            updateSummary(result);
            prepareCoverageHighlighting(loc, rootAst, coverage, result);
            updateReportTable(result);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    "Concolic testing failed: " + e.getMessage(),
                    ButtonType.OK).showAndWait();
            e.printStackTrace();
        }
    }

    private Coverage getSelectedCoverage() {
        if (branchCoverage.isSelected()) {
            return Coverage.BRANCH;
        }
        if (mcdcCoverage.isSelected()) {
            return Coverage.MCDC;
        }
        return Coverage.STATEMENT;
    }

    private PathFinder getSelectedPathFinder() {
        if (flowNetworkPathFinder.isSelected()) {
            return new LoopCondensationFlowPathFinder();
        }
        return new AllPathsFinder();
    }

    private void updateSummary(TestResult result) {
        if (result == null) {
            return;
        }
        if (fullCoverageLabel != null) {
            fullCoverageLabel.setText(String.format("%.2f%%", calculateCoveragePercent(result)));
        }
        if (memoryUsageLabel != null) {
            memoryUsageLabel.setText(String.format("%.2f MB", result.memoryUsageBytes() / (1024.0 * 1024.0)));
        }
        if (runtimeLabel != null) {
            runtimeLabel.setText(String.format("%d ms", result.executionTimeMillis()));
        }
    }

    private void updateReportTable(TestResult result) {
        if (reportTable == null || result == null) {
            return;
        }
        int total = result.fullCoverage().getCovered().size()
                + result.fullCoverage().getUncovered().size()
                + result.fullCoverage().getSkipped().size();
        List<TestData> data = result.testDataList();
        List<FormattedTestData> formattedData = data.stream()
                .map(d -> new FormattedTestData(total, d))
                .collect(Collectors.toList());
        ObservableList<FormattedTestData> items = FXCollections.observableArrayList(formattedData);
        reportTable.setItems(items);
        reportTable.getSelectionModel().clearSelection();
    }

    private void prepareCoverageHighlighting(MethodLocation loc,
                                             CompilationUnit rootAst,
                                             Coverage coverage,
                                             TestResult result) {
        currentMethodLocation = loc;
        currentTestResult = result;
        currentInstrumentationPlan = new InstrumentationPlanner().plan(
                rootAst,
                ConcolicTesting.getInstance().getCfg(loc.methodDeclaration, coverage),
                coverage
        );
        clearCoverageHighlight();
    }

    private void highlightTestCoverage(FormattedTestData row) {
        if (row == null || currentInstrumentationPlan == null) {
            return;
        }
        Set<Integer> covered = row.coveredNodeIds();
        Set<Integer> uncovered = new HashSet<>(currentInstrumentationPlan.nodeIds());
        uncovered.removeAll(covered);
        applyCoverageHighlight(covered, uncovered, Collections.emptySet());
    }

    private void highlightFullCoverage() {
        if (currentTestResult == null || currentInstrumentationPlan == null) {
            return;
        }
        applyCoverageHighlight(
                currentTestResult.fullCoverage().getCovered(),
                currentTestResult.fullCoverage().getUncovered(),
                currentTestResult.fullCoverage().getSkipped()
        );
    }

    private void applyCoverageHighlight(Set<Integer> coveredNodeIds,
                                        Set<Integer> uncoveredNodeIds,
                                        Set<Integer> skippedNodeIds) {
        if (currentMethodLocation != null && (currentFile == null || !currentFile.equals(currentMethodLocation.file))) {
            showFile(currentMethodLocation.file);
        }

        highlightedCoverageLines.clear();
        highlightedCoverageLines.put(LineCoverageState.COVERED, linesForNodeIds(coveredNodeIds));
        highlightedCoverageLines.put(LineCoverageState.UNCOVERED, linesForNodeIds(uncoveredNodeIds));
        highlightedCoverageLines.put(LineCoverageState.SKIPPED, linesForNodeIds(skippedNodeIds));

        sourceList.getSelectionModel().clearSelection();
        sourceList.refresh();

        highlightedCoverageLines.values().stream()
                .flatMap(Set::stream)
                .min(Integer::compareTo)
                .ifPresent(sourceList::scrollTo);
    }

    private Set<Integer> linesForNodeIds(Set<Integer> nodeIds) {
        if (currentInstrumentationPlan == null || nodeIds == null || nodeIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Integer> lineIndexes = new HashSet<>();
        for (Integer nodeId : nodeIds) {
            if (nodeId == null) {
                continue;
            }
            currentInstrumentationPlan.pointFor(nodeId)
                    .map(TracePoint::astNode)
                    .map(ast -> ((CompilationUnit) ast.getRoot()).getLineNumber(ast.getStartPosition()) - 1)
                    .filter(line -> line >= 0)
                    .ifPresent(lineIndexes::add);
        }
        return lineIndexes;
    }

    private LineCoverageState coverageStateForLine(int lineIndex) {
        if (highlightedCoverageLines.getOrDefault(LineCoverageState.SKIPPED, Collections.emptySet()).contains(lineIndex)) {
            return LineCoverageState.SKIPPED;
        }
        if (highlightedCoverageLines.getOrDefault(LineCoverageState.UNCOVERED, Collections.emptySet()).contains(lineIndex)) {
            return LineCoverageState.UNCOVERED;
        }
        if (highlightedCoverageLines.getOrDefault(LineCoverageState.COVERED, Collections.emptySet()).contains(lineIndex)) {
            return LineCoverageState.COVERED;
        }
        return null;
    }

    private void clearRunState() {
        currentMethodLocation = null;
        currentTestResult = null;
        currentInstrumentationPlan = null;
        clearCoverageHighlight();
        if (reportTable != null) {
            reportTable.getItems().clear();
        }
    }

    private void clearCoverageHighlight() {
        highlightedCoverageLines.clear();
        if (sourceList != null) {
            sourceList.refresh();
        }
    }

    private enum LineCoverageState {
        COVERED("-fx-background-color: #c8e6c9; -fx-text-fill: black;"),
        UNCOVERED("-fx-background-color: #ffcdd2; -fx-text-fill: black;"),
        SKIPPED("-fx-background-color: #fff3cd; -fx-text-fill: black;");

        private final String style;

        LineCoverageState(String style) {
            this.style = style;
        }

        String style() {
            return style;
        }
    }


    static class FormattedTestData {
        private Map<String, Object> input;
        private int coverage;
        private String output;
        private Set<Integer> coveredNodeIds;
        public FormattedTestData(int numberOfNode, TestData td) {
            this.input = td.input();
            this.output = td.output();
            Set<Integer> coveredNodeIds = td.coveredNodeIds() == null
                    ? Collections.emptySet()
                    : td.coveredNodeIds();
            this.coveredNodeIds = coveredNodeIds;
            this.coverage = numberOfNode == 0 ? 0 : coveredNodeIds.size() * 100 / numberOfNode;
        }

        public Map<String, Object> input() {
            return input;
        }

        public String output() {
            return output;
        }

        public int coverage() {
            return coverage;
        }

        public Set<Integer> coveredNodeIds() {
            return coveredNodeIds;
        }
    }

    private double calculateCoveragePercent(TestResult result) {
        int covered = result.fullCoverage().getCovered().size();
        int total = covered
                + result.fullCoverage().getUncovered().size()
                + result.fullCoverage().getSkipped().size();
        return total == 0 ? 100 : covered * 100.0 / total;
    }

    private String formatValue(Object value) {
        if (value instanceof int[] array) return Arrays.toString(array);
        if (value instanceof long[] array) return Arrays.toString(array);
        if (value instanceof short[] array) return Arrays.toString(array);
        if (value instanceof byte[] array) return Arrays.toString(array);
        if (value instanceof char[] array) return Arrays.toString(array);
        if (value instanceof boolean[] array) return Arrays.toString(array);
        if (value instanceof float[] array) return Arrays.toString(array);
        if (value instanceof double[] array) return Arrays.toString(array);
        if (value instanceof Object[] array) return Arrays.deepToString(array);
        return String.valueOf(value);
    }
}
