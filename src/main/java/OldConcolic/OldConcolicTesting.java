package OldConcolic;

import core.TestGeneration.ConcolicTesting;
import org.eclipse.jdt.core.dom.*;
import core.CFG.CfgNode;
import core.CFG.Utils.ASTHelper;
import core.TestGeneration.path.FindPath;
import core.TestGeneration.path.MarkedPath;
import core.TestGeneration.result.TestResult;
import core.TestGeneration.testDriver.TestDriverGenerator;
import core.TestGeneration.testDriver.TestDriverRunner;

import java.util.*;

public class OldConcolicTesting extends ConcolicTesting {
    @Override
    protected TestResult generateTests(int id, ASTHelper.Coverage coverage) {
        TestResult testResult = new TestResult();
        testResult.setId(id);

        TestDriverGenerator.generateTestDriver((MethodDeclaration) testUnit, this.parameterClasses,
                fullyClonedClassName, simpleClassName);
        TestDriverRunner.reset();

        Object[] testInputs = OldSymbolicExecution.createRandomTestData(this.parameterClasses);
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


            OldSymbolicExecution symbolicExecution = new OldSymbolicExecution(parameterList, testPath);
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
}

