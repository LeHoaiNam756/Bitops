package core.TestGeneration;

import org.eclipse.jdt.core.dom.*;
import core.CFG.CfgBlockNode;
import core.CFG.CfgBoolExprNode;
import core.CFG.CfgNode;
import core.CFG.Utils.ASTHelper;
import core.SymbolicExecution.SymbolicExecution;
import core.TestGeneration.path.FindPath;
import core.TestGeneration.path.MarkedPath;
import core.TestGeneration.result.RamStorage;
import core.TestGeneration.result.TestData;
import core.TestGeneration.result.TestResult;
import core.TestGeneration.testDriver.TestDriverGenerator;
import core.TestGeneration.testDriver.TestDriverRunner;
import core.TestGeneration.testDriver.TestDriverUtils;
import core.utils.CloneProject;
import core.utils.FilePath;
import core.utils.ProjectParser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ConcolicTesting {
    private List<ASTNode> unitsASTNodeList;
    protected ASTNode testUnit;
    protected List<ASTNode> parameterList;
    private CompilationUnit compilationUnit;
    protected Class<?>[] parameterClasses;
    private List<String> parameterNames;
    protected String simpleClassName;
    protected String fullyClonedClassName;
    protected CfgNode rootCfgNode;
    protected CfgNode finalEndCfgNode;
    protected Set<CfgNode> totalCfgNodes;

    public TestResult runConcolicTesting(int id, String filePath, String className, String methodName,
                                         ASTHelper.Coverage coverage) {
        long startTime = System.currentTimeMillis();
        setup(filePath, className, methodName, coverage);
        setupCfgTree(coverage);
        setupParameters();

        TestResult testResult = generateTests(id, coverage);
        long endTime = System.currentTimeMillis();
        double memoryUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        testResult.setTimeToGenerate(endTime - startTime);
        testResult.setMemoryUsed(memoryUsed / (1024 * 1024));
        return testResult;
    }


    protected TestResult generateTests(int id, ASTHelper.Coverage coverage) {
        TestResult testResult = new TestResult();
        testResult.setId(id);

        TestDriverGenerator.generateTestDriver((MethodDeclaration) testUnit, this.parameterClasses,
                fullyClonedClassName, simpleClassName);
        TestDriverRunner.reset();

        Object[] testInputs = SymbolicExecution.createRandomTestData(this.parameterClasses);
        executeTestAndRecord(testInputs, testResult, coverage);

        CfgNode uncoveredNode = FindPath.getUncoveredNode(totalCfgNodes, MarkedPath.getVisitedNodes());
        CfgNode prevUncoveredNode = null;
        while (uncoveredNode != null) {
            if (uncoveredNode.equals(prevUncoveredNode)) {
                uncoveredNode.setFakeVisited(true);
            }
            prevUncoveredNode = uncoveredNode;
            MarkedPath.reset();
            List<FindPath.PathNode> testPath = FindPath.findPathThrough(rootCfgNode, uncoveredNode, finalEndCfgNode);
            if (testPath == null) {
                uncoveredNode.setFakeVisited(true);
                uncoveredNode = FindPath.getUncoveredNode(totalCfgNodes, MarkedPath.getVisitedNodes());
                continue;
            }


            SymbolicExecution symbolicExecution = new SymbolicExecution(parameterList, testPath);
            try {
                symbolicExecution.execute();
            } catch (RuntimeException e) {
                System.err.println("Test fail due to UNSATISFIABLE constraint to cover node at " +
                        uncoveredNode.getStartPosition() + ": " + uncoveredNode.getContent() + " - "
                        + e.getMessage());
                uncoveredNode.setFakeVisited(true);
                uncoveredNode = FindPath.getUncoveredNode(totalCfgNodes, MarkedPath.getVisitedNodes());
                continue;
            }

            Object[] newTestInputs = symbolicExecution.getTestInputFromModel(parameterClasses);
            executeTestAndRecord(newTestInputs, testResult, coverage);

            uncoveredNode = FindPath.getUncoveredNode(totalCfgNodes, MarkedPath.getVisitedNodes());
        }

        testResult.setCoveragePercent(calculateFullUnitCoverage(coverage));

        return testResult;
    }

    protected void executeTestAndRecord(Object[] testInputs, TestResult testResult, ASTHelper.Coverage coverage) {
        TestDriverRunner.runTestDriver(FilePath.PATH_TO_TEST_DRIVER, testInputs);
        MarkedPath.markPathToCfg(rootCfgNode);
        MarkedPath.getMarkedStatements().forEach(markedStatement -> {
            RamStorage.getCoveredStatements().add(markedStatement);
        });
        testResult.addToFullTestData(
                new TestData(
                        this.parameterNames, this.parameterClasses, testInputs,
                        Set.copyOf(MarkedPath.getMarkedStatements()),
                        RamStorage.getOutputs().get(RamStorage.getOutputs().size() - 1),
                        calculateUnitCoverage(coverage)
                )
        );
    }

    private void setup(String filePath, String className, String methodName,
                       ASTHelper.Coverage coverage) {
        RamStorage.reset();
        MarkedPath.resetVisitedNodes();
        this.compilationUnit = ProjectParser.getCompilationUnit(filePath);
        this.unitsASTNodeList = ProjectParser.parseFile(filePath);
        setupFullyClonedClassName(className, filePath, coverage);
        setupTestUnit(methodName);
        MarkedPath.resetFullTestSuiteCoveredStatements();
    }

    private void setupTestUnit(String methodName) {
        for (ASTNode method : unitsASTNodeList) {
            if (((MethodDeclaration) method).getName().getIdentifier().equals(methodName)) {
                this.testUnit = method;
                break;
            }
        }
        if (testUnit == null) {
            throw new IllegalArgumentException("Method not found: " + methodName);
        }
        @SuppressWarnings("unchecked")
        List<ASTNode> params = ((MethodDeclaration) testUnit).parameters();
        this.parameterList = new ArrayList<>(params);
    }

    private void setupFullyClonedClassName(String className, String filePath,
                                           ASTHelper.Coverage coverage) {
        try {
            String newPath = getRootProjectPath(filePath);
            Path rootPackagePath = CloneProject.findRootPackage(Paths.get(newPath));
            CloneProject.cloneProject(rootPackagePath.toString(), FilePath.PATH_TO_CLONED_PROJECT, coverage, className);
            className = className.replace(".java", "");
            this.simpleClassName = getClassFromCU(compilationUnit);

            String relative = filePath.substring(rootPackagePath.toString().length() + 1);
            int lastSlash = relative.lastIndexOf(File.separator);
            if (lastSlash != -1) {
                relative = relative.substring(0, lastSlash + 1);
            } else {
                relative = "";
            }

            String packetName = relative.replace(File.separator, ".");

            this.fullyClonedClassName = FilePath.CLONED_PROJECT_ROOT_PACKAGE + "." + packetName + this.simpleClassName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to clone project for class: " + className, e);
        }
    }

    private static String getClassFromCU(CompilationUnit compilationUnit) {
        List<TypeDeclaration> classes = new ArrayList<>();
        compilationUnit.accept(new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                classes.add(node);
                return super.visit(node);
            }
        });
        return classes.get(0).getName().toString();
    }

    private String getRootProjectPath(String filePath) {
        Path path = Paths.get(filePath).toAbsolutePath().normalize();
        Path current = path.getParent();

        while (current != null) {
            if (Files.exists(current.resolve("pom.xml")) ||
                    Files.exists(current.resolve("build.gradle"))) {
                return current.toString();
            }
            current = current.getParent();
        }

        current = path.getParent();
        while (current != null) {
            if (current.getFileName() != null &&
                    current.getFileName().toString().equals("src")) {
                return current.getParent().toString();
            }
            current = current.getParent();
        }

        throw new IllegalStateException("Could not determine project root for: " + filePath);
    }

    private void setupCfgTree(ASTHelper.Coverage coverage) {
        Block unitBody = null;
        if (testUnit instanceof MethodDeclaration) {
            unitBody = ((MethodDeclaration) testUnit).getBody();
        }

        if (unitBody == null) {
            throw new RuntimeException("Test unit body is null");
        }

        rootCfgNode = new CfgNode();
        finalEndCfgNode = new CfgNode();
        rootCfgNode.setBeginCfgNode(true);
        finalEndCfgNode.setEndCfgNode(true);

        CfgNode block = new CfgBlockNode();
        block.setAst(unitBody);
        block.setBeforeNode(rootCfgNode);
        block.setAfterNode(finalEndCfgNode);
        rootCfgNode.setAfterNode(block);
        finalEndCfgNode.setBeforeNode(block);

        ASTHelper.generateCfg(block, this.compilationUnit, coverage);
        traverseAndSetTotalCfgNodes(rootCfgNode);
    }

    private void traverseAndSetTotalCfgNodes(CfgNode rootCfgNode) {
        totalCfgNodes = new HashSet<>();
        traverseAndSetTotalCfgNodes(rootCfgNode, totalCfgNodes);
    }

    public void traverseAndSetTotalCfgNodes(CfgNode rootCfgNode, Set<CfgNode> targetSet) {
        if (rootCfgNode == null) {
            return;
        }

        Objects.requireNonNull(targetSet, "targetSet must not be null");
        targetSet.clear();

        Set<CfgNode> visited = new HashSet<>();
        Queue<CfgNode> queue = new LinkedList<>();
        queue.add(rootCfgNode);

        while (!queue.isEmpty()) {
            CfgNode node = queue.poll();

            if (node == null || visited.contains(node)) {
                continue;
            }

            visited.add(node);
            targetSet.add(node);

            if (node instanceof CfgBoolExprNode) {
                @SuppressWarnings("PatternVariableCanBeUsed")
                CfgBoolExprNode boolNode = (CfgBoolExprNode) node;
                queue.add(boolNode.getTrueNode());
                queue.add(boolNode.getFalseNode());
            } else {
                queue.add(node.getAfterNode());
            }
        }
    }

    private void setupParameters() {
        this.parameterClasses = TestDriverUtils.getParameterClasses(this.parameterList);
        this.parameterNames = TestDriverUtils.getParameterNames(this.parameterList);
    }

    private double calculateUnitCoverage(ASTHelper.Coverage coverage) {
        if (coverage == ASTHelper.Coverage.STATEMENT) {
            return calculateUnitStatementCoverage();
        } else {
            return calculateUnitBranchAndMCDCCoverage();
        }
    }

    private Map<String, Integer> findMethodInformation() {
        MethodDeclaration testMethod = (MethodDeclaration) testUnit;
        String testMethodName = testMethod.getName().getIdentifier();
        List<?> testMethodParams = testMethod.parameters();

        return CloneProject.getInformationOfMethods().entrySet().stream()
                .filter(entry -> {
                    ASTNode key = entry.getKey();
                    if (!(key instanceof MethodDeclaration)) {
                        return false;
                    }
                    @SuppressWarnings("PatternVariableCanBeUsed")
                    MethodDeclaration method = (MethodDeclaration) key;
                    if (!method.getName().getIdentifier().equals(testMethodName)) {
                        return false;
                    }
                    List<?> methodParams = method.parameters();
                    if (methodParams.size() != testMethodParams.size()) {
                        return false;
                    }
                    for (int i = 0; i < testMethodParams.size(); i++) {
                        SingleVariableDeclaration testParam = (SingleVariableDeclaration) testMethodParams.get(i);
                        SingleVariableDeclaration methodParam = (SingleVariableDeclaration) methodParams.get(i);
                        if (!testParam.getType().toString().equals(methodParam.getType().toString())) {
                            return false;
                        }
                    }
                    return true;
                })
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private double calculateUnitStatementCoverage() {
        Map<String, Integer> methodInfo = findMethodInformation();
        double totalStatements = methodInfo != null ? methodInfo.get("TotalStatement") : 0.0;
        double coveredStatements = MarkedPath.getTotalCoveredStatement();
        if (totalStatements == 0) {
            return 0.0;
        }
        return (coveredStatements / totalStatements) * 100;
    }

    private double calculateUnitBranchAndMCDCCoverage() {
        Map<String, Integer> methodInfo = findMethodInformation();
        double totalBranches = methodInfo != null ? methodInfo.get("TotalBranch") : 0.0;
        double coveredBranches = MarkedPath.getTotalCoveredBranchAndMCDC();
        if (totalBranches == 0) {
            return 0.0;
        }
        return (coveredBranches / totalBranches) * 100;
    }

    protected double calculateFullUnitCoverage(ASTHelper.Coverage coverage) {
        if (coverage == ASTHelper.Coverage.STATEMENT) {
            return calculateFullUnitStatementCoverage();
        } else {
            return calculateFullUnitBranchAndMCDCCoverage();
        }
    }

    private double calculateFullUnitBranchAndMCDCCoverage() {
        Map<String, Integer> methodInfo = findMethodInformation();
        double totalBranches = methodInfo != null ? methodInfo.get("TotalBranch") : 0.0;
        double coveredBranches = MarkedPath.getFullTestSuiteTotalCoveredBranch();
        if (totalBranches == 0) {
            return 0.0;
        }
        return (coveredBranches / totalBranches) * 100;
    }

    private double calculateFullUnitStatementCoverage() {
        Map<String, Integer> methodInfo = findMethodInformation();
        double totalStatements = methodInfo != null ? methodInfo.get("TotalStatement") : 0.0;
        double coveredStatements = MarkedPath.getFullTestSuiteTotalCoveredStatements();
        if (totalStatements == 0) {
            return 0.0;
        }
        return (coveredStatements / totalStatements) * 100;
    }
}
