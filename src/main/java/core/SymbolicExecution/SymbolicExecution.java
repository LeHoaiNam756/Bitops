package core.SymbolicExecution;

import com.microsoft.z3.*;
import core.SymbolicExecution.AstNode.Expression.Array.ArrayNode;
import core.SymbolicExecution.AstNode.Expression.ExpressionNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNumberNode;
import core.SymbolicExecution.Variable.PrimitiveVariable;
import org.eclipse.jdt.core.dom.*;
import core.CFG.CfgBoolExprNode;
import core.CFG.CfgNode;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralBooleanNode;
import core.SymbolicExecution.AstNode.Expression.Name.SimpleNameNode;
import core.SymbolicExecution.AstNode.Expression.Operation.OperationExpressionNode;
import core.SymbolicExecution.AstNode.Expression.Operation.PrefixExpressionNode;
import core.SymbolicExecution.Variable.ArrayVariable;
import core.SymbolicExecution.Variable.Variable;
import core.TestGeneration.path.FindPath.PathNode;
import core.utils.Setup;
import org.eclipse.jdt.core.dom.AST;

import java.util.*;

public class SymbolicExecution {
    private List<ASTNode> parameterList;
    private Class<?>[] parameterClasses;
    // Kept for backward compatibility but no longer used for determining ordering
    private final LinkedHashSet<Expr<?>> paramZ3ExprList = new LinkedHashSet<>();
    private List<PathNode> testPath;
    private MemoryModel memoryModel;
    private Context ctx;
    private Model model;
    // TODO: This is a temporary solution to track whether the current
    //  AST node being executed is related to any parameter.
    public static boolean isRelatedToParameter;

    public SymbolicExecution(List<ASTNode> parameterList, List<PathNode> testPath) {
        this.parameterList = parameterList;
        this.testPath = testPath;
    }

    public void execute() {
        memoryModel = new MemoryModel();
        CfgNode currentNode;
        BoolExpr finalZ3Expression = null;

        HashMap<String, String> cfg = new HashMap<>();
        cfg.put("model", "true");
        ctx = new Context(cfg);
        executeParameters(ctx);

        for (PathNode pathNode : testPath) {
            currentNode = pathNode.node;
            ASTNode astNode = currentNode.getAst();
            if (astNode == null) {
                continue;
            }

            isRelatedToParameter = false;
            AstNode rewriteAstNode = AstNode.executeASTNode(astNode, memoryModel);
            if (currentNode instanceof CfgBoolExprNode && isRelatedToParameter) {
                if (!pathNode.decision) {
                    PrefixExpressionNode prefixExpressionNode = new PrefixExpressionNode();
                    prefixExpressionNode.setOperator(PrefixExpression.Operator.NOT);
                    prefixExpressionNode.setOperand(rewriteAstNode);
                    rewriteAstNode = PrefixExpressionNode.executePrefixExpressionNode(prefixExpressionNode,
                            memoryModel);
                }

                if (rewriteAstNode instanceof LiteralBooleanNode) {
                    if (!((LiteralBooleanNode) rewriteAstNode).isValue()) {
                        finalZ3Expression = ctx.mkFalse();
                        break;
                    }
                }

                Expr<?> z3Expr = OperationExpressionNode.convertAstNodeToZ3Expr(rewriteAstNode, ctx, memoryModel);
                BoolExpr constraint;
                if (z3Expr instanceof BoolExpr) {
                    constraint = (BoolExpr) z3Expr;
                } else if (z3Expr instanceof BitVecExpr) {
                    constraint = ctx.mkEq(z3Expr, ctx.mkBV(1, ((BitVecExpr) z3Expr).getSortSize()));
                } else {
                    throw new RuntimeException("Z3 expression is not a boolean expression: " + z3Expr);
                }

                if (finalZ3Expression == null) {
                    finalZ3Expression = constraint;
                } else {
                    finalZ3Expression = ctx.mkAnd(finalZ3Expression, constraint);
                }
            }
        }

        model = createModel(ctx, finalZ3Expression);
        System.out.println(finalZ3Expression);
    }

    private Model createModel(Context ctx, BoolExpr f) {
        if (f == null) {
            throw new IllegalArgumentException("Expr cannot be null");
        }
        
        Solver solver = ctx.mkSolver();
        solver.add(f);
        
        Status satisfaction = solver.check();
        
        if (satisfaction == Status.SATISFIABLE) {
            return solver.getModel();
        } else if (satisfaction == Status.UNSATISFIABLE) {
            String reason = solver.getReasonUnknown();
            throw new RuntimeException("Constraints are unsatisfiable" + (reason != null ? ": " + reason : ""));
        } else {
            String reason = solver.getReasonUnknown();
            throw new RuntimeException("Solver could not determine satisfiability (UNKNOWN)" + (reason != null ? ": "
                    + reason : ""));
        }
    }

    private void executeParameters(Context ctx) {
        for (ASTNode astNode : parameterList) {
            AstNode.executeASTNode(astNode, memoryModel);
            
            if (astNode instanceof SingleVariableDeclaration) {
                SingleVariableDeclaration svd = (SingleVariableDeclaration) astNode;
                String paramName = svd.getName().getIdentifier();
                Type paramType = svd.getType();
                if (paramType instanceof PrimitiveType) {
                    SimpleNameNode simpleNameNode = SimpleNameNode.of(paramName);
                    memoryModel.assignVariable(paramName, simpleNameNode);
                } else if (paramType instanceof ArrayType) {
                    ArrayNode arrayNode = new ArrayNode(paramName);
                    memoryModel.assignVariable(paramName, arrayNode);
                }
                
                Variable variable = memoryModel.getVariable(paramName);
                if (variable != null) {
                    Expr<?> paramExpr = variable.createZ3Expr(ctx);
                    if (variable instanceof ArrayVariable) {
                        AST ast = astNode.getAST();
                        PrimitiveType intType = ast.newPrimitiveType(PrimitiveType.INT);
                        PrimitiveVariable arrLenVar = new PrimitiveVariable(intType, paramName + "_len");
                        Expr<?> arrLenExpr = arrLenVar.createZ3Expr(ctx);
                        SimpleNameNode arrLenSimpleNameNode = SimpleNameNode.of(paramName + "_len");
                        memoryModel.declareVariable(arrLenVar, arrLenSimpleNameNode);
                        arrLenVar.setParameter(true);
                    }
                    variable.setParameter(true);
                    // paramZ3ExprList is no longer used to determine ordering, but we keep it
                    // populated for potential diagnostic or backward-compat uses.
                    paramZ3ExprList.add(paramExpr);
                }
            }
        }
    }


    public static Object[] createRandomTestData(Class<?>[] parameterClasses) {
        Object[] result = new Object[parameterClasses.length];

        for (int i = 0; i < result.length; i++) {
            result[i] = createRandomVariableData(parameterClasses[i]);
        }

        return result;
    }

    public Object[] getTestInputFromModel(Class<?>[] parameterClasses) {
        this.parameterClasses = parameterClasses;
        return getSolutionFromModel(model, ctx, parameterClasses);
    }

    private Object[] getSolutionFromModel(Model model, Context ctx, Class<?>[] parameterClasses) {
        if (model == null) {
            throw new IllegalArgumentException("Model cannot be null");
        }
        if (ctx == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        if (parameterClasses == null) {
            throw new IllegalArgumentException("Parameter classes cannot be null");
        }
        if (parameterList == null) {
            throw new IllegalStateException("Parameter list is not initialized");
        }
        if (memoryModel == null) {
            throw new IllegalStateException("Memory model is not initialized");
        }
        if (parameterList.size() != parameterClasses.length) {
            throw new IllegalArgumentException("Parameter list size (" + parameterList.size() + 
                ") does not match parameter classes size (" + parameterClasses.length + ")");
        }

        Object[] result = new Object[parameterClasses.length];

        int index = 0;
        for (ASTNode astNode : parameterList) {
            if (!(astNode instanceof SingleVariableDeclaration)) {
                throw new IllegalStateException("Unsupported parameter AST node: " + astNode.getClass());
            }

            SingleVariableDeclaration svd = (SingleVariableDeclaration) astNode;
            String paramName = svd.getName().getIdentifier();

            Variable variable = memoryModel.getVariable(paramName);
            if (variable == null) {
                throw new IllegalStateException("No variable found in memory model for parameter: " + paramName);
            }

            Expr<?> paramExpr = variable.createZ3Expr(ctx);

            if (variable instanceof ArrayVariable) {
                result[index] = extractArrayFromModel(paramExpr, paramName, parameterClasses[index]);
            } else {
                Expr<?> evaluatedResult = model.evaluate(paramExpr, true);
                result[index] = convertEvaluatedResultToJavaType(evaluatedResult, parameterClasses[index]);
            }
            index++;
        }

        return result;
    }

    private Object extractArrayFromModel(Expr<?> paramExpr, String paramName, Class<?> paramClass) {
        AstNode value = memoryModel.accessVariable(paramName);
        if (!(value instanceof ArrayNode)) {
            throw new IllegalStateException("Expected ArraySymbolicRepresent for array variable: " + paramName);
        }
        ArrayNode arrRep = (ArrayNode) value;

        int length;
        AstNode lengthNode = arrRep.getLength();
        if (lengthNode != null) {
            Expr<?> lengthExpr = ExpressionNode.convertAstNodeToZ3Expr(lengthNode, ctx, memoryModel);
            Expr<?> evalLen = model.evaluate(lengthExpr, true);
            if (evalLen instanceof BitVecNum) {
                length = ((BitVecNum) evalLen).getBigInteger().intValue();
            } else {
                throw new RuntimeException("Cannot evaluate array length to concrete value: " + evalLen);
            }
        } else {
            length = arrRep.getElements().size();
        }

        if (length < 0) {
           //TODO: handle for length < 0
           length = 1;
        }

        Expr<?> arrayConst = variableFromName(paramName);
        Sort elementSort = getArrayElementSort(paramExpr.getSort());

        return ArrayZ3Parser.parseArrayFromModel(ctx, model, arrayConst, length, elementSort);
    }

    private Expr<?> variableFromName(String paramName) {
        Variable variable = memoryModel.getVariable(paramName);
        if (variable == null) {
            throw new IllegalStateException("Variable not found: " + paramName);
        }
        return variable.createZ3Expr(ctx);
    }

    private static Sort getArrayElementSort(Sort sort) {
        if (sort instanceof ArraySort) {
            return ((ArraySort<?, ?>) sort).getRange();
        }
        throw new RuntimeException("Expected ArraySort, got: " + sort.getClass());
    }

    private static Object convertEvaluatedResultToJavaType(Expr evaluatedResult, Class<?> parameterClass) {
        String className = parameterClass.getName();

        if (evaluatedResult instanceof BitVecNum) {
            BitVecNum bvNum = (BitVecNum) evaluatedResult;
            java.math.BigInteger val = bvNum.getBigInteger();

            if ("int".equals(className)) {
                return val.intValue();
            } else if ("byte".equals(className)) {
                return val.byteValue();
            } else if ("short".equals(className)) {
                return val.shortValue();
            } else if ("char".equals(className)) {
                return (char) (val.intValue() & 0xFFFF);
            } else if ("long".equals(className)) {
                return val.longValue();
            } else {
                throw new RuntimeException("Unsupported bit vector type: " + className);
            }
        } else if (evaluatedResult instanceof FPNum) {
            FPNum fpNum = (FPNum) evaluatedResult;
            
            if ("float".equals(className)) {
                if (fpNum.isNaN()) {
                    return Float.NaN;
                } else if (fpNum.isInf()) {
                    return fpNum.isNegative() ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
                } else {
                    if (fpNum.toString().contains("/")) {
                        String[] parts = fpNum.toString().split("/");
                        return Float.parseFloat(parts[0]) / Float.parseFloat(parts[1]);
                    }
                    return Float.parseFloat(fpNum.toString());
                }
            } else if ("double".equals(className)) {
                if (fpNum.isNaN()) {
                    return Double.NaN;
                } else if (fpNum.isInf()) {
                    return fpNum.isNegative() ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
                } else {
                    if (fpNum.toString().contains("/")) {
                        String[] parts = fpNum.toString().split("/");
                        return Double.parseDouble(parts[0])
                                / Double.parseDouble(parts[1]);
                    }
                    return Double.parseDouble(fpNum.toString());
                }
            } else {
                throw new RuntimeException("Unsupported floating point type: " + className);
            }
        } else if (evaluatedResult instanceof BoolExpr) {
            BoolExpr boolExpr = (BoolExpr) evaluatedResult;
            if ("boolean".equals(className)) {
                String boolStr = boolExpr.toString();
                if ("true".equals(boolStr)) {
                    return true;
                } else if ("false".equals(boolStr)) {
                    return false;
                } else {
                    return false;
                }
            } else {
                throw new RuntimeException("BoolExpr result for non-boolean type: " + className);
            }
        } else {
            throw new RuntimeException("Unsupported evaluated result type: " + evaluatedResult.getClass() + 
                " for parameter class: " + className);
        }
    }

    private static Object createRandomVariableData(Class<?> parameterClass) {
        if (parameterClass.isPrimitive()) {
            return createRandomPrimitiveVariableData(parameterClass);
        } else if (parameterClass.isArray()) {
            return createRandomArrayVariableData(parameterClass);
        }
        throw new RuntimeException("Unsupported type: " + parameterClass.getName());
    }


    private static Object createRandomArrayVariableData(Class<?> arrayClass) {
        Random random = new Random();
        // Default random length between 1 and 5
        int length = 1 + random.nextInt(5);
        Class<?> component = arrayClass.getComponentType();
        Object array = java.lang.reflect.Array.newInstance(component, length);
        for (int i = 0; i < length; i++) {
            Object elem;
            if (component.isArray()) {
                elem = createRandomArrayVariableData(component);
            } else if (component.isPrimitive()) {
                elem = createRandomPrimitiveVariableData(component);
            } else {
                throw new RuntimeException("Unsupported array component type: " + component.getName());
            }
            java.lang.reflect.Array.set(array, i, elem);
        }
        return array;
    }


    private static Object createRandomPrimitiveVariableData(Class<?> parameterClass) {
        String className = parameterClass.getName();
        Random random = new Random();

        if ("int".equals(className)) {
            int min = Setup.intMin;
            int max = Setup.intMax;
            if (max < min) {
                throw new RuntimeException("Invalid int bounds in Setup: max < min");
            }
            return min + random.nextInt((max - min) + 1);
        } else if ("boolean".equals(className)) {
            return random.nextBoolean();
        } else if ("byte".equals(className)) {
            byte[] bytes = new byte[1];
            random.nextBytes(bytes);
            return bytes[0];
        } else if ("short".equals(className)) {
            return (short) random.nextInt();
        } else if ("char".equals(className)) {
            return (char) random.nextInt();
        } else if ("long".equals(className)) {
            return random.nextLong();
        } else if ("float".equals(className)) {
            float min = Setup.floatMin;
            float max = Setup.floatMax;
            if (max < min) {
                throw new RuntimeException("Invalid float bounds in Setup: max < min");
            }
            return min + random.nextFloat() * (max - min);
        } else if ("double".equals(className)) {
            double min = Setup.doubleMin;
            double max = Setup.doubleMax;
            if (max < min) {
                throw new RuntimeException("Invalid double bounds in Setup: max < min");
            }
            return min + random.nextDouble() * (max - min);
        } else if ("void".equals(className)) {
            return null;
        }
        throw new RuntimeException("Unsupported type: " + className);
    }
}
