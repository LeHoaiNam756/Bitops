package core.utils;

import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.*;
import core.CFG.Utils.ASTHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import java.util.*;

public final class CloneProject {
    @Getter
    private static int totalFunctionStatement;
    @Getter
    private static int totalClassStatement;
    @Getter
    private static int totalFunctionBranch;
    @Getter
    private final static HashMap<ASTNode, Map<String, Integer>> informationOfMethods = new HashMap<>();
    private static CompilationUnit classCompilationUnit;
    private static StringBuilder command;

    public static void cloneProject(String filePath, ASTHelper.Coverage coverage) throws Exception {
        deleteFilesInDirectory(FilePath.PATH_TO_CLONED_PROJECT);
        String instrumentedFilePath = makeInstrumentedTestingFile(filePath, coverage);
        try {
            Compiler.getInstance().compileJavaFile(instrumentedFilePath
                    , FilePath.PATH_TO_MAVEN_TARGET_CLASSES);
        } catch (RuntimeException e) {
            throw new Exception("Compilation failed for file: " + instrumentedFilePath, e);
        }

    }





    private static String makeInstrumentedTestingFile(String file2TestPath, ASTHelper.Coverage coverage) {
        try {
            File file = new File(file2TestPath);
            if (!file.exists() || !file.isFile() || !file.getName().endsWith(".java")) {
                throw new NoSuchFileException("Target Java file not found: " + file2TestPath);
            }
            ProjectParser parser = new ProjectParser();
            parser.loadFile(file2TestPath);
            CompilationUnit compilationUnit = parser.getCompilationUnit();
            createFile(Path.of(FilePath.JCIA_PROJECT_ROOT_PATH, FilePath.PATH_TO_CLONED_PROJECT).toString(), file.getName());
            String sourceCode = createCloneSourceCode(compilationUnit, coverage);
            String clonedFilePath = Path.of(FilePath.JCIA_PROJECT_ROOT_PATH, FilePath.PATH_TO_CLONED_PROJECT, file.getName()).toString();
            writeDataToFile(sourceCode, clonedFilePath);
            return clonedFilePath;
        } catch (IOException e) {
            throw new RuntimeException("Error processing file: " + file2TestPath, e);
        }
    }

    /**
     * Creates a cloned source code for supporting classes without instrumentation
     * marks,
     * but updates the package declaration to be inside the cloned root.
     */
    private static String createNonInstrumentedClone(CompilationUnit compilationUnit) {
        StringBuilder result = new StringBuilder();

        if (compilationUnit.getPackage() != null) {
            result.append("package ")
                    .append(FilePath.CLONED_PROJECT_ROOT_PACKAGE)
                    .append(".")
                    .append(compilationUnit.getPackage().getName().toString())
                    .append(";\n");
        } else {
            result.append("package " + FilePath.CLONED_PROJECT_ROOT_PACKAGE + ";").append("\n");
        }

        // Imports
        @SuppressWarnings("unchecked")
        List<ASTNode> imports = compilationUnit.imports();
        for (ASTNode iImport : imports) {
            result.append(iImport);
        }

        // Add the rest of the file content directly
        @SuppressWarnings("unchecked")
        List<AbstractTypeDeclaration> types = compilationUnit.types();
        for (AbstractTypeDeclaration type : types) {
            result.append(type.toString()).append("\n");
        }

        return result.toString();
    }


    /**
     * Finds the root package directory of a Java project given a specific target
     * file Path.
     */
    public static Path findRootPackage(Path targetFile) throws IOException {
        if (!Files.exists(targetFile) || !Files.isRegularFile(targetFile)) {
            throw new NoSuchFileException("Target Java file not found: " + targetFile.toString());
        }

        String pkg = readPackageDecl(targetFile);
        int depth = (pkg == null || pkg.isEmpty()) ? 0 : pkg.split("\\.").length;

        Path root = targetFile.getParent();
        for (int i = 0; i < depth && root != null; i++) {
            root = root.getParent();
        }

        if (root == null) {
            return targetFile.getParent().toAbsolutePath().normalize();
        }

        return root.toAbsolutePath().normalize();
    }

    /**
     * Gets files in a directory.
     */
    private static File[] getFilesInDirectory(String directoryPath) {
        File directory = new File(directoryPath);

        if (!directory.isDirectory()) {
            throw new RuntimeException("Invalid Dir: " + directory.getPath());
        }

        return directory.listFiles();
    }

    /**
     * Deletes all files in a directory or creates it if it doesn't exist.
     */
    public static void deleteFilesInDirectory(String directoryPath) throws IOException {
        if (Files.exists(Path.of(directoryPath))) {
            FileUtils.cleanDirectory(new File(directoryPath));
        } else {
            FileUtils.forceMkdir(new File(directoryPath));
        }
    }

    /**
     * Creates a clone directory.
     */
    public static void createDirectory(String parent, String child) {
        File newDirectory = new File(parent, child);

        boolean created = newDirectory.mkdir();

        if (!created) {
            System.out.println("Existed Dir");
        }
    }

    /**
     * Creates a clone file.
     */
    private static void createFile(String directoryPath, String fileName) {
        File directory = new File(directoryPath);
        if (!directory.isDirectory()) {
            throw new RuntimeException("Invalid dir");
        }

        File newFile = new File(directory, fileName);

        try {
            boolean created = newFile.createNewFile();

            if (!created) {
                System.out.println("Existed file");
            }
        } catch (IOException e) {
            throw new RuntimeException("Can't create file");
        }
    }

    /**
     * Creates the cloned source code with instrumentation marks.
     */
    private static String createCloneSourceCode(CompilationUnit compilationUnit,
            ASTHelper.Coverage coverage) throws IOException {
        StringBuilder result = new StringBuilder();

        if (compilationUnit.getPackage() != null) {
            result.append("package ")
                    .append(FilePath.CLONED_PROJECT_ROOT_PACKAGE)
                    .append(".")
                    .append(compilationUnit.getPackage().getName().toString())
                    .append(";\n");
        } else {
            result.append("package " + FilePath.CLONED_PROJECT_ROOT_PACKAGE + ";").append("\n");
        }

        // Imports
        for (ASTNode iImport : (List<ASTNode>) compilationUnit.imports()) {
            result.append(iImport);
        }

        result.append("import static ").append(FilePath.MARKED_STATEMENT_METHOD_IMPORT).append(";\n");

        // Extract class data
        List<ClassData> classDataArr = new ArrayList<>();
        ASTVisitor classVisitor = new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                classDataArr.add(new ClassData(node));
                return true;
            }
        };
        compilationUnit.accept(classVisitor);

        ClassData classData = classDataArr.get(0);

        result.append("public ")
                .append(classData.getTypeOfClass()).append(" ")
                .append(classData.getClassName());

        // Extensions
        if (classData.getSuperClassName() != null) {
            result.append(" extends ").append(classData.getSuperClassName());
        }

        // Implementations
        if (classData.getSuperInterfaceName() != null) {
            result.append(" implements ");
            List<String> interfaceList = classData.getSuperInterfaceName();
            for (int i = 0; i < interfaceList.size(); i++) {
                result.append(interfaceList.get(i));
                if (i != interfaceList.size() - 1) {
                    result.append(", ");
                }
            }
        }

        result.append(" {\n");

        result.append(classData.getFields());

        // Process methods
        List<ASTNode> methods = new ArrayList<>();
        ASTVisitor methodsVisitor = new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                methods.addAll(Arrays.asList(node.getMethods()));
                return true;
            }
        };
        compilationUnit.accept(methodsVisitor);
        informationOfMethods.clear();
        for (ASTNode astNode : methods) {
            totalFunctionStatement = 0;
            totalFunctionBranch = 0;
            MethodDeclaration methodDeclaration = (MethodDeclaration) astNode;

            if (!methodDeclaration.isConstructor()) {
                result.append(createCloneMethod(methodDeclaration, coverage));
            } else {
                result.append(methodDeclaration);
            }
            informationOfMethods.put(methodDeclaration,
                    Map.of("TotalStatement", totalFunctionStatement,
                            "TotalBranch", totalFunctionBranch));
        }

        result.append(createTotalClassStatementVariable(classData));
        result.append("}");

        return result.toString();
    }

    public static void regenerateCloneFromCompilationUnit(CompilationUnit compilationUnit,
                                                          String fileName,
                                                          ASTHelper.Coverage coverage) throws Exception {
        if (compilationUnit == null) {
            throw new IllegalArgumentException("CompilationUnit cannot be null");
        }
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("FileName cannot be null or empty");
        }

        // Ensure the file has .java extension
        if (!fileName.endsWith(".java")) {
            fileName = fileName + ".java";
        }

        createFile(Path.of(FilePath.JCIA_PROJECT_ROOT_PATH, FilePath.PATH_TO_CLONED_PROJECT).toString(), fileName);
        String sourceCode = createCloneSourceCode(compilationUnit, coverage);
        String filePath = Path.of(FilePath.JCIA_PROJECT_ROOT_PATH, FilePath.PATH_TO_CLONED_PROJECT, fileName).toString();
        writeDataToFile(sourceCode, filePath);

        try {
            Compiler.getInstance().compileJavaFile(filePath, FilePath.PATH_TO_MAVEN_TARGET_CLASSES);
        } catch (RuntimeException e) {
            throw new Exception("Compilation failed for regenerated file: " + filePath, e);
        }

    }

    /**
     * Creates a cloned method with instrumentation.
     */
    private static String createCloneMethod(MethodDeclaration method, ASTHelper.Coverage coverage) {
        StringBuilder cloneMethod = new StringBuilder();
        List<ASTNode> modifiers = method.modifiers();
        for (ASTNode modifier : modifiers) {
            if (modifier.toString().equals("private")) {
                cloneMethod.append("public").append(" ");
                continue;
            }
            cloneMethod.append(modifier).append(" ");
        }

        cloneMethod.append(method.getReturnType2() != null ? method.getReturnType2() : "")
                .append(" ").append(method.getName()).append("(");
        List<ASTNode> parameters = method.parameters();
        for (int i = 0; i < parameters.size(); i++) {
            cloneMethod.append(parameters.get(i));
            if (i != parameters.size() - 1)
                cloneMethod.append(", ");
        }
        cloneMethod.append(") {\n");
        cloneMethod.append(generateCodeForBlock(method.getBody(), coverage)).append("\n");
        cloneMethod.append("}\n");
        return cloneMethod.toString();
    }

    /**
     * Generates code for a block statement.
     */
    private static String generateCodeForBlock(Block block, ASTHelper.Coverage coverage) {
        StringBuilder result = new StringBuilder();

        result.append("{\n");
        if (block != null) {
            List<ASTNode> statements = block.statements();
            for (int i = 0; i < statements.size(); i++) {
                result.append(generateCodeForOneStatement(statements.get(i), ";", coverage));
            }
        }
        result.append("}\n");

        return result.toString();
    }

    /**
     * Generates code for one statement with instrumentation.
     */
    private static String generateCodeForOneStatement(ASTNode statement, String markMethodSeparator,
            ASTHelper.Coverage coverage) {
        if (statement == null) {
            return "";
        }

        if (statement instanceof Block) {
            return generateCodeForBlock((Block) statement, coverage);
        } else if (statement instanceof IfStatement) {
            return generateCodeForIfStatement((IfStatement) statement, coverage);
        } else if (statement instanceof ForStatement) {
            return generateCodeForForStatement((ForStatement) statement, coverage);
        } else if (statement instanceof WhileStatement) {
            return generateCodeForWhileStatement((WhileStatement) statement, coverage);
        } else if (statement instanceof DoStatement) {
            return generateCodeForDoStatement((DoStatement) statement, coverage);
        } else if (isReturnWithConditionalExpression(statement)) {
            return generateCodeForReturnConditionalExpression((ReturnStatement) statement, coverage);
        } else if (isAssignmentWithConditionalExpression(statement)) {
            return generateCodeForAssignmentConditionalExpression((ExpressionStatement) statement, coverage);
        } else if (hasTernaryVariableDeclarationFragment(statement)) {
            return generateCodeForVariableDeclarationConditionalExpression((VariableDeclarationStatement) statement, coverage);
        } else {
            return generateCodeForNormalStatement(statement, markMethodSeparator);
        }
    }

    private static boolean isReturnWithConditionalExpression(ASTNode statement) {
        if (!(statement instanceof ReturnStatement)) {
            return false;
        }

        ReturnStatement returnStatement = (ReturnStatement) statement;
        return getConditionalExpression(returnStatement.getExpression()) != null;
    }

    private static String generateCodeForReturnConditionalExpression(ReturnStatement returnStatement,
                                                                     ASTHelper.Coverage coverage) {
        ConditionalExpression conditionalExpression = getConditionalExpression(returnStatement.getExpression());
        if (conditionalExpression == null) {
            return generateCodeForNormalStatement(returnStatement, ";");
        }
        AST ast = returnStatement.getAST();
        StringBuilder result = new StringBuilder();

        if (hasNestedTernary(returnStatement.getExpression())) {
            return convertReturnNestedTernary(returnStatement, coverage);
        }

        result.append("if (")
                .append(generateCodeForCondition(conditionalExpression.getExpression(), coverage))
                .append(")\n");
        result.append("{\n");

        ReturnStatement thenReturn = ast.newReturnStatement();
        thenReturn.setExpression((Expression) ASTNode.copySubtree(ast, conditionalExpression.getThenExpression()));
        result.append(generateCodeForNormalStatement(thenReturn, ";"));

        result.append("}\n");
        result.append("else {\n");

        ReturnStatement elseReturn = ast.newReturnStatement();
        elseReturn.setExpression((Expression) ASTNode.copySubtree(ast, conditionalExpression.getElseExpression()));
        result.append(generateCodeForNormalStatement(elseReturn, ";"));

        result.append("}\n");

        return result.toString();
    }

    private static String convertReturnNestedTernary(ReturnStatement returnStatement, ASTHelper.Coverage coverage) {
        Expression expr = returnStatement.getExpression();
        ConditionalExpression ce = getConditionalExpression(expr);
        if (ce == null) {
            return generateCodeForNormalStatement(returnStatement, ";");
        }

        AST ast = returnStatement.getAST();
        StringBuilder result = new StringBuilder();

        result.append("if (")
                .append(generateCodeForCondition(ce.getExpression(), coverage))
                .append(")\n");
        result.append("{\n");

        Expression thenExpr = ce.getThenExpression();
        ConditionalExpression nestedThen = getConditionalExpression(thenExpr);
        if (nestedThen != null) {
            ReturnStatement nestedReturn = ast.newReturnStatement();
            nestedReturn.setExpression((Expression) ASTNode.copySubtree(ast, nestedThen));
            result.append(convertReturnNestedTernary(nestedReturn, coverage));
            result.append("}\n");
        } else {
            ReturnStatement thenReturn = ast.newReturnStatement();
            thenReturn.setExpression((Expression) ASTNode.copySubtree(ast, thenExpr));
            result.append(generateCodeForNormalStatement(thenReturn, ";"));
            result.append("}\n");
        }
        
        result.append("else {\n");

        Expression elseExpr = ce.getElseExpression();
        ConditionalExpression nestedElse = getConditionalExpression(elseExpr);
        if (nestedElse != null) {
            ReturnStatement nestedReturn = ast.newReturnStatement();
            nestedReturn.setExpression((Expression) ASTNode.copySubtree(ast, nestedElse));
            result.append(convertReturnNestedTernary(nestedReturn, coverage));
            result.append("}\n");
        } else {
            ReturnStatement elseReturn = ast.newReturnStatement();
            elseReturn.setExpression((Expression) ASTNode.copySubtree(ast, elseExpr));
            result.append(generateCodeForNormalStatement(elseReturn, ";"));
            result.append("}\n");
        }

        return result.toString();
    }

    private static boolean isAssignmentWithConditionalExpression(ASTNode statement) {
        if (!(statement instanceof ExpressionStatement)) {
            return false;
        }

        Expression expression = ((ExpressionStatement) statement).getExpression();
        if (!(expression instanceof Assignment)) {
            return false;
        }

        Assignment assignment = (Assignment) expression;
        //TODO: handle simple
        if (!(assignment.getLeftHandSide() instanceof SimpleName)) {
            return false;
        }

        return getConditionalExpression(assignment.getRightHandSide()) != null;
    }

    private static String generateCodeForAssignmentConditionalExpression(ExpressionStatement statement,
                                                                         ASTHelper.Coverage coverage) {
        Assignment assignment = (Assignment) statement.getExpression();
        ConditionalExpression conditionalExpression = getConditionalExpression(assignment.getRightHandSide());
        if (conditionalExpression == null) {
            return generateCodeForNormalStatement(statement, ";");
        }

        if (hasNestedTernary(assignment.getRightHandSide())) {
            return convertAssignmentNestedTernary(assignment, coverage);
        }

        StringBuilder result = new StringBuilder();

        result.append("if (")
                .append(generateCodeForCondition(conditionalExpression.getExpression(), coverage))
                .append(")\n");
        result.append("{\n");

        ExpressionStatement thenAssignment = createAssignmentStatement(
                assignment.getAST(),
                assignment.getLeftHandSide(),
                assignment.getOperator(),
                conditionalExpression.getThenExpression());
        result.append(generateCodeForNormalStatement(thenAssignment, ";"));

        result.append("}\n");
        result.append("else {\n");

        ExpressionStatement elseAssignment = createAssignmentStatement(
                assignment.getAST(),
                assignment.getLeftHandSide(),
                assignment.getOperator(),
                conditionalExpression.getElseExpression());
        result.append(generateCodeForNormalStatement(elseAssignment, ";"));

        result.append("}\n");

        return result.toString();
    }

    private static String convertAssignmentNestedTernary(Assignment assignment, ASTHelper.Coverage coverage) {
        ConditionalExpression ce = getConditionalExpression(assignment.getRightHandSide());
        if (ce == null) {
            return generateCodeForNormalStatement(
                    assignment.getAST().newExpressionStatement(assignment), ";");
        }

        AST ast = assignment.getAST();
        SimpleName leftHandSide = (SimpleName) assignment.getLeftHandSide();
        StringBuilder result = new StringBuilder();

        result.append("if (")
                .append(generateCodeForCondition(ce.getExpression(), coverage))
                .append(")\n");
        result.append("{\n");

        ConditionalExpression nestedThen = getConditionalExpression(ce.getThenExpression());
        if (nestedThen != null) {
            result.append(convertAssignmentNestedTernary(
                    createAssignment(ast, leftHandSide, assignment.getOperator(), nestedThen.getThenExpression()),
                    coverage));
            result.append("}\n");
            result.append("else {\n");
            result.append(generateCodeForNormalStatement(
                    createAssignmentStatement(ast, leftHandSide, Assignment.Operator.ASSIGN, nestedThen.getElseExpression()), ";"));
            result.append("}\n");
        } else {
            ExpressionStatement thenAssign = createAssignmentStatement(
                    ast, leftHandSide, assignment.getOperator(), ce.getThenExpression());
            result.append(generateCodeForNormalStatement(thenAssign, ";"));
            result.append("}\n");
        }


        result.append("else {\n");

        ConditionalExpression nestedElse = getConditionalExpression(ce.getElseExpression());
        if (nestedElse != null) {
            result.append(convertAssignmentNestedTernary(
                    createAssignment(ast, leftHandSide, assignment.getOperator(), nestedElse.getThenExpression()),
                    coverage));
            result.append("}\n");
        } else {
            ExpressionStatement elseAssign = createAssignmentStatement(
                    ast, leftHandSide, assignment.getOperator(), ce.getElseExpression());
            result.append(generateCodeForNormalStatement(elseAssign, ";"));
            result.append("}\n");
        }

        return result.toString();
    }

    private static Assignment createAssignment(AST ast, SimpleName leftHandSide, Assignment.Operator operator, Expression rightHandSide) {
        Assignment assign = ast.newAssignment();
        assign.setLeftHandSide((Expression) ASTNode.copySubtree(ast, leftHandSide));
        assign.setOperator(operator);
        assign.setRightHandSide((Expression) ASTNode.copySubtree(ast, rightHandSide));
        return assign;
    }

    private static boolean hasTernaryVariableDeclarationFragment(ASTNode statement) {
        if (!(statement instanceof VariableDeclarationStatement)) {
            return false;
        }

        VariableDeclarationStatement declarationStatement = (VariableDeclarationStatement) statement;
        @SuppressWarnings("unchecked")
        List<VariableDeclarationFragment> fragments = declarationStatement.fragments();

        for (VariableDeclarationFragment fragment : fragments) {
            if (getConditionalExpression(fragment.getInitializer()) != null) {
                return true;
            }
        }
        return false;
    }

    private static String generateCodeForVariableDeclarationConditionalExpression(
            VariableDeclarationStatement declarationStatement,
            ASTHelper.Coverage coverage) {
        StringBuilder result = new StringBuilder();
        AST ast = declarationStatement.getAST();

        @SuppressWarnings("unchecked")
        List<VariableDeclarationFragment> fragments = declarationStatement.fragments();

        for (VariableDeclarationFragment fragment : fragments) {
            Expression initializer = fragment.getInitializer();
            ConditionalExpression conditionalExpression = getConditionalExpression(initializer);

            if (conditionalExpression != null && canSafelySplitTernaryDeclarationFragment(declarationStatement)) {

                if (hasNestedTernary(initializer)) {
                    result.append(convertVariableDeclarationNestedTernary(declarationStatement, fragment, coverage));
                } else {
                    VariableDeclarationStatement declarationOnly = createSingleFragmentDeclaration(
                            declarationStatement,
                            fragment,
                            false);
                    result.append(generateCodeForNormalStatement(declarationOnly, ";"));

                    result.append("if (")
                            .append(generateCodeForCondition(conditionalExpression.getExpression(), coverage))
                            .append(")\n");
                    result.append("{\n");

                    ExpressionStatement thenAssign = createAssignmentStatement(
                            ast,
                            fragment.getName(),
                            Assignment.Operator.ASSIGN,
                            conditionalExpression.getThenExpression());
                    result.append(generateCodeForNormalStatement(thenAssign, ";"));

                    result.append("}\n");
                    result.append("else {\n");

                    ExpressionStatement elseAssign = createAssignmentStatement(
                            ast,
                            fragment.getName(),
                            Assignment.Operator.ASSIGN,
                            conditionalExpression.getElseExpression());
                    result.append(generateCodeForNormalStatement(elseAssign, ";"));

                    result.append("}\n");
                }
            } else {
                VariableDeclarationStatement keptDeclaration = createSingleFragmentDeclaration(
                        declarationStatement,
                        fragment,
                        true);
                result.append(generateCodeForNormalStatement(keptDeclaration, ";"));
            }
        }

        return result.toString();
    }

    private static String convertVariableDeclarationNestedTernary(
            VariableDeclarationStatement declarationStatement,
            VariableDeclarationFragment fragment,
            ASTHelper.Coverage coverage) {
        Expression initializer = fragment.getInitializer();
        ConditionalExpression ce = getConditionalExpression(initializer);
        if (ce == null) {
            return generateCodeForNormalStatement(declarationStatement, ";");
        }

        AST ast = declarationStatement.getAST();
        SimpleName varName = fragment.getName();
        StringBuilder result = new StringBuilder();

        VariableDeclarationStatement declarationOnly = createSingleFragmentDeclaration(
                declarationStatement,
                fragment,
                false);
        result.append(generateCodeForNormalStatement(declarationOnly, ";"));

        result.append("if (")
                .append(generateCodeForCondition(ce.getExpression(), coverage))
                .append(")\n");
        result.append("{\n");

        ConditionalExpression nestedThen = getConditionalExpression(ce.getThenExpression());
        if (nestedThen != null) {
            result.append(convertVariableDeclarationNestedTernaryTwoLevel(
                    ast, varName, nestedThen, coverage));
        } else {
            ExpressionStatement thenAssign = createAssignmentStatement(
                    ast, varName, Assignment.Operator.ASSIGN, ce.getThenExpression());
            result.append(generateCodeForNormalStatement(thenAssign, ";"));
        }

        result.append("}\n");
        result.append("else {\n");

        ConditionalExpression nestedElse = getConditionalExpression(ce.getElseExpression());
        if (nestedElse != null) {
            result.append(convertVariableDeclarationNestedTernaryTwoLevel(
                    ast, varName, nestedElse, coverage));
        } else {
            ExpressionStatement elseAssign = createAssignmentStatement(
                    ast, varName, Assignment.Operator.ASSIGN, ce.getElseExpression());
            result.append(generateCodeForNormalStatement(elseAssign, ";"));
        }

        result.append("}\n");

        return result.toString();
    }

    private static String convertVariableDeclarationNestedTernaryTwoLevel(
            AST ast, SimpleName varName, ConditionalExpression ce, ASTHelper.Coverage coverage) {
        StringBuilder result = new StringBuilder();

        result.append("if (")
                .append(generateCodeForCondition(ce.getExpression(), coverage))
                .append(")\n");
        result.append("{\n");

        ConditionalExpression nestedThen = getConditionalExpression(ce.getThenExpression());
        if (nestedThen != null) {
            result.append(convertVariableDeclarationNestedTernaryTwoLevel(
                    ast, varName, nestedThen, coverage));
            result.append("}\n");
            result.append("else {\n");
            ExpressionStatement elseAssign = createAssignmentStatement(
                    ast, varName, Assignment.Operator.ASSIGN, nestedThen.getElseExpression());
            result.append(generateCodeForNormalStatement(elseAssign, ";"));
            result.append("}\n");
        } else {
            ExpressionStatement thenAssign = createAssignmentStatement(
                    ast, varName, Assignment.Operator.ASSIGN, ce.getThenExpression());
            result.append(generateCodeForNormalStatement(thenAssign, ";"));
            result.append("}\n");
        }

        result.append("else {\n");

        ConditionalExpression nestedElse = getConditionalExpression(ce.getElseExpression());
        if (nestedElse != null) {
            result.append(convertVariableDeclarationNestedTernaryTwoLevel(
                    ast, varName, nestedElse, coverage));
            result.append("}\n");
        } else {
            ExpressionStatement elseAssign = createAssignmentStatement(
                    ast, varName, Assignment.Operator.ASSIGN, ce.getElseExpression());
            result.append(generateCodeForNormalStatement(elseAssign, ";"));
            result.append("}\n");
        }


        return result.toString();
    }

    private static VariableDeclarationStatement createSingleFragmentDeclaration(
            VariableDeclarationStatement template,
            VariableDeclarationFragment fragment,
            boolean keepInitializer) {
        AST ast = template.getAST();

        VariableDeclarationFragment fragmentCopy =
                (VariableDeclarationFragment) ASTNode.copySubtree(ast, fragment);
        if (!keepInitializer) {
            fragmentCopy.setInitializer(null);
        }

        VariableDeclarationStatement singleDeclaration = ast.newVariableDeclarationStatement(fragmentCopy);
        singleDeclaration.setType((Type) ASTNode.copySubtree(ast, template.getType()));

        @SuppressWarnings("unchecked")
        List<IExtendedModifier> templateModifiers = template.modifiers();
        @SuppressWarnings("unchecked")
        List<IExtendedModifier> singleModifiers = singleDeclaration.modifiers();
        for (IExtendedModifier modifier : templateModifiers) {
            singleModifiers.add((IExtendedModifier) ASTNode.copySubtree(ast, (ASTNode) modifier));
        }

        return singleDeclaration;
    }

    private static ExpressionStatement createAssignmentStatement(
            AST ast,
            Expression leftHandSide,
            Assignment.Operator operator,
            Expression rightHandSide) {
        Assignment assignment = ast.newAssignment();
        assignment.setLeftHandSide((Expression) ASTNode.copySubtree(ast, leftHandSide));
        assignment.setOperator(operator);
        assignment.setRightHandSide((Expression) ASTNode.copySubtree(ast, rightHandSide));
        return ast.newExpressionStatement(assignment);
    }

    private static ConditionalExpression getConditionalExpression(Expression expression) {
        Expression unwrappedExpression = unwrapParenthesizedExpression(expression);
        if (unwrappedExpression instanceof ConditionalExpression) {
            return (ConditionalExpression) unwrappedExpression;
        }
        return null;
    }

    private static Expression unwrapParenthesizedExpression(Expression expression) {
        Expression unwrappedExpression = expression;
        while (unwrappedExpression instanceof ParenthesizedExpression) {
            unwrappedExpression = ((ParenthesizedExpression) unwrappedExpression).getExpression();
        }
        return unwrappedExpression;
    }

    private static boolean hasNestedTernary(Expression expr) {
        Expression unwrapped = unwrapParenthesizedExpression(expr);
        if (unwrapped instanceof ConditionalExpression) {
            ConditionalExpression ce = (ConditionalExpression) unwrapped;
            return getConditionalExpression(ce.getThenExpression()) != null
                    || getConditionalExpression(ce.getElseExpression()) != null;
        }
        return false;
    }

    private static boolean canSafelySplitTernaryDeclarationFragment(VariableDeclarationStatement declarationStatement) {
        if (isFinalDeclaration(declarationStatement)) {
            return false;
        }
        return !isVarType(declarationStatement.getType());
    }

    private static boolean isFinalDeclaration(VariableDeclarationStatement declarationStatement) {
        @SuppressWarnings("unchecked")
        List<IExtendedModifier> modifiers = declarationStatement.modifiers();
        for (IExtendedModifier modifier : modifiers) {
            if (modifier instanceof Modifier && ((Modifier) modifier).isFinal()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isVarType(Type type) {
        if (!(type instanceof SimpleType)) {
            return false;
        }
        return "var".equals(((SimpleType) type).getName().getFullyQualifiedName());
    }

    /**
     * Generates code for an if statement with instrumentation.
     */
    private static String generateCodeForIfStatement(IfStatement ifStatement,
            ASTHelper.Coverage coverage) {
        StringBuilder result = new StringBuilder();

        result.append("if (").append(generateCodeForCondition(ifStatement.getExpression(), coverage))
                .append(")\n");
        result.append("{\n");
        result.append(generateCodeForOneStatement(ifStatement.getThenStatement(), ";", coverage));
        result.append("}\n");

        String elseCode = generateCodeForOneStatement(ifStatement.getElseStatement(), ";", coverage);
        if (!elseCode.equals("")) {
            result.append("else {\n").append(elseCode).append("}\n");
        }

        return result.toString();
    }

    /**
     * Generates code for a for statement with instrumentation.
     */
    private static String generateCodeForForStatement(ForStatement forStatement,
            ASTHelper.Coverage coverage) {
        StringBuilder result = new StringBuilder();

        // Initializers
        List<ASTNode> initializers = forStatement.initializers();
        for (ASTNode initializer : initializers) {
            result.append(generateCodeForMarkMethod(initializer, ";"));
        }
        result.append("for (");
        for (int i = 0; i < initializers.size(); i++) {
            result.append(initializers.get(i));
            if (i != initializers.size() - 1)
                result.append(", ");
        }

        // Condition
        result.append("; ");
        result.append(generateCodeForCondition(forStatement.getExpression(), coverage));

        // Updaters
        result.append("; ");
        List<ASTNode> updaters = forStatement.updaters();
        for (int i = 0; i < updaters.size(); i++) {
            result.append(generateCodeForOneStatement(updaters.get(i), ",", coverage));
            if (i != updaters.size() - 1)
                result.append(", ");
        }

        // Body
        result.append(") {\n");
        result.append(generateCodeForOneStatement(forStatement.getBody(), ";", coverage));
        result.append("}\n");

        return result.toString();
    }

    /**
     * Generates code for a while statement with instrumentation.
     */
    private static String generateCodeForWhileStatement(WhileStatement whileStatement,
            ASTHelper.Coverage coverage) {
        StringBuilder result = new StringBuilder();

        result.append("while (");
        result.append(generateCodeForCondition(whileStatement.getExpression(), coverage));
        result.append(") {\n");

        result.append(generateCodeForOneStatement(whileStatement.getBody(), ";", coverage));
        result.append("}\n");

        return result.toString();
    }

    /**
     * Generates code for a do-while statement with instrumentation.
     */
    private static String generateCodeForDoStatement(DoStatement doStatement,
            ASTHelper.Coverage coverage) {
        StringBuilder result = new StringBuilder();

        result.append("do {");
        result.append(generateCodeForOneStatement(doStatement.getBody(), ";", coverage));
        result.append("}\n");

        result.append("while (");
        result.append(generateCodeForCondition(doStatement.getExpression(), coverage));
        result.append(");\n");

        return result.toString();
    }

    /**
     * Generates code for a normal statement with markOneStatement method.
     */
    private static String generateCodeForNormalStatement(ASTNode statement,
            String markMethodSeparator) {
        StringBuilder result = new StringBuilder();

        result.append(generateCodeForMarkMethod(statement, markMethodSeparator));
        result.append(statement);

        return result.toString();
    }

    /**
     * Generates code for the markOneStatement method call.
     */
    private static String generateCodeForMarkMethod(ASTNode statement, String markMethodSeparator) {
        StringBuilder result = new StringBuilder();

        String stringStatement = statement.toString();
        StringBuilder newStatement = new StringBuilder();

        // Escape special characters for string literal
        for (int i = 0; i < stringStatement.length(); i++) {
            char charAt = stringStatement.charAt(i);

            if (charAt == '\n') {
                newStatement.append("\\n");
                continue;
            } else if (charAt == '"') {
                newStatement.append("\\").append('"');
                continue;
            } else if (i != stringStatement.length() - 1 && charAt == '\\' &&
                    stringStatement.charAt(i + 1) == 'n') {
                newStatement.append("\" + \"").append("\\n").append("\" + \"");
                i++;
                continue;
            }

            newStatement.append(charAt);
        }

        int position = statement.getStartPosition();
        result.append("markOneStatement(\"").append(newStatement)
                .append("\", false, false, ").append(position).append(')')
                .append(markMethodSeparator).append("\n");
        totalFunctionStatement++;
        totalClassStatement++;

        return result.toString();
    }

    /**
     * Generates code for a condition with instrumentation based on coverage type.
     */
    private static String generateCodeForCondition(Expression condition,
            ASTHelper.Coverage coverage) {
        if (coverage == ASTHelper.Coverage.MCDC) {
            return generateCodeForConditionForMCDCCoverage(condition);
        } else if (coverage == ASTHelper.Coverage.BRANCH ||
                coverage == ASTHelper.Coverage.STATEMENT) {
            return generateCodeForConditionForBranchAndStatementCoverage(condition);
        } else {
            throw new RuntimeException("Invalid coverage!");
        }
    }

    /**
     * Generates code for condition with branch/statement coverage.
     */
    private static String generateCodeForConditionForBranchAndStatementCoverage(Expression condition) {
        totalFunctionStatement++;
        totalClassStatement++;
        totalFunctionBranch += 2;
        int position = condition.getStartPosition();
        return "((" + condition + ") && markOneStatement(\"" + condition + "\", true, false, " +
                position + "))" +
                " || markOneStatement(\"" + condition + "\", false, true, " + position + ")";
    }

    /**
     * Generates code for condition with MC/DC coverage.
     */
    private static String generateCodeForConditionForMCDCCoverage(Expression condition) {
        StringBuilder result = new StringBuilder();

        if (condition instanceof InfixExpression &&
                isSeparableOperator(((InfixExpression) condition).getOperator())) {
            InfixExpression infixCondition = (InfixExpression) condition;

            result.append("(").append(generateCodeForConditionForMCDCCoverage(
                    infixCondition.getLeftOperand()))
                    .append(") ").append(infixCondition.getOperator()).append(" (");
            result.append(generateCodeForConditionForMCDCCoverage(
                    infixCondition.getRightOperand())).append(")");

            List<ASTNode> extendedOperands = infixCondition.extendedOperands();
            for (ASTNode operand : extendedOperands) {
                result.append(" ").append(infixCondition.getOperator()).append(" ");
                result.append("(").append(generateCodeForConditionForMCDCCoverage(
                        (Expression) operand)).append(")");
            }
        } else {
            totalFunctionStatement++;
            totalClassStatement++;
            totalFunctionBranch += 2;
            int position = condition.getStartPosition();
            result.append("((").append(condition).append(") && markOneStatement(\"").append(condition)
                    .append("\", true, false, ").append(position).append("))");
            result.append(" || markOneStatement(\"").append(condition).append("\", false, true, ")
                    .append(position).append(")");
        }

        return result.toString();
    }

    /**
     * Checks if an operator is separable for MC/DC coverage.
     */
    private static boolean isSeparableOperator(InfixExpression.Operator operator) {
        return operator.equals(InfixExpression.Operator.CONDITIONAL_OR) ||
                operator.equals(InfixExpression.Operator.OR) ||
                operator.equals(InfixExpression.Operator.CONDITIONAL_AND) ||
                operator.equals(InfixExpression.Operator.AND);
    }

    /**
     * Reformats a variable name by removing special characters.
     */
    private static String reformatVariableName(String name) {
        return name.replace(" ", "").replace(".", "")
                .replace("[", "").replace("]", "")
                .replace("<", "").replace(">", "")
                .replace(",", "");
    }

    /**
     * Creates a total class statement variable declaration.
     */
    private static String createTotalClassStatementVariable(ClassData classData) {
        StringBuilder result = new StringBuilder();
        result.append(classData.getClassName()).append("TotalStatement");
        return "public static final int ".concat(reformatVariableName(result.toString()))
                .concat(" = " + totalClassStatement + ";\n");
    }

    /**
     * Writes data to a file.
     */
    private static void writeDataToFile(String data, String path) {
        try {
            FileWriter writer = new FileWriter(path);
            writer.write(data + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads package declaration from a Java file.
     */
    private static String readPackageDecl(Path file) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("//") || line.startsWith("/*") || line.isEmpty())
                    continue;
                if (line.startsWith("package ")) {
                    int semi = line.indexOf(';');
                    if (semi > 0) {
                        return line.substring("package ".length(), semi).trim();
                    }
                }
                if (line.startsWith("class ") || line.startsWith("interface ")
                        || line.startsWith("enum ") || line.startsWith("@interface ")) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Finds the common prefix of two paths.
     */
    private static Path commonPrefix(Path a, Path b) {
        a = a.toAbsolutePath().normalize();
        b = b.toAbsolutePath().normalize();

        if (a.getRoot() == null || b.getRoot() == null || !Objects.equals(a.getRoot(), b.getRoot()))
            return null;

        int n = Math.min(a.getNameCount(), b.getNameCount());
        Path res = a.getRoot();
        for (int i = 0; i < n; i++) {
            if (!a.getName(i).equals(b.getName(i)))
                break;
            res = res.resolve(a.getName(i).toString());
        }
        return res;
    }
}
