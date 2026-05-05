package core.CFG.Utils;

import core.utils.AstIdGenerator;
import org.eclipse.jdt.core.dom.*;

import java.util.*;

public final class TernarySplitHelper {
    private TernarySplitHelper() {}

    public enum PartKind {
        CONDITION,
        THEN_STATEMENT,
        ELSE_STATEMENT
    }

    public static final class TernaryPart {
        private final PartKind kind;
        private final ASTNode originalPart;
        private final ASTNode convertedPart;

        public TernaryPart(PartKind kind, ASTNode originalPart, ASTNode convertedPart) {
            this.kind = kind;
            this.originalPart = originalPart;
            this.convertedPart = convertedPart;
        }

        public PartKind getKind() {
            return kind;
        }

        public ASTNode getOriginalPart() {
            return originalPart;
        }

        public ASTNode getConvertedPart() {
            return convertedPart;
        }
    }

    public static final class TernarySplitResult {
        private final ASTNode originalStatement;
        private final ASTNode normalizedStatement;
        private final ConditionalExpression originalConditionalExpression;
        private final IfStatement convertedIfStatement;
        private final Statement convertedThenStatement;
        private final Statement convertedElseStatement;
        private final List<Statement> prefixStatements;
        private final List<TernaryPart> parts;
        private final Map<String, String> originalPartIdToConvertedPartId;

        public TernarySplitResult(
                ASTNode originalStatement,
                ASTNode normalizedStatement,
                ConditionalExpression originalConditionalExpression,
                IfStatement convertedIfStatement,
                Statement convertedThenStatement,
                Statement convertedElseStatement,
                List<Statement> prefixStatements,
                List<TernaryPart> parts,
                Map<String, String> originalPartIdToConvertedPartId) {
            this.originalStatement = originalStatement;
            this.normalizedStatement = normalizedStatement;
            this.originalConditionalExpression = originalConditionalExpression;
            this.convertedIfStatement = convertedIfStatement;
            this.convertedThenStatement = convertedThenStatement;
            this.convertedElseStatement = convertedElseStatement;
            this.prefixStatements = prefixStatements;
            this.parts = parts;
            this.originalPartIdToConvertedPartId = originalPartIdToConvertedPartId;
        }

        public ASTNode getOriginalStatement() {
            return originalStatement;
        }

        public ASTNode getNormalizedStatement() {
            return normalizedStatement;
        }

        public ConditionalExpression getOriginalConditionalExpression() {
            return originalConditionalExpression;
        }

        public IfStatement getConvertedIfStatement() {
            return convertedIfStatement;
        }

        public Statement getConvertedThenStatement() {
            return convertedThenStatement;
        }

        public Statement getConvertedElseStatement() {
            return convertedElseStatement;
        }

        public List<Statement> getPrefixStatements() {
            return prefixStatements;
        }

        public List<TernaryPart> getParts() {
            return parts;
        }

        public Map<String, String> getOriginalPartIdToConvertedPartId() {
            return originalPartIdToConvertedPartId;
        }

        public String getConvertedIdForOriginalCondition() {
            return originalPartIdToConvertedPartId.get(
                    AstIdGenerator.buildSignature(originalConditionalExpression.getExpression()));
        }

        public String getConvertedIdForOriginalThen() {
            return originalPartIdToConvertedPartId.get(
                    AstIdGenerator.buildSignature(originalConditionalExpression.getThenExpression()));
        }

        public String getConvertedIdForOriginalElse() {
            return originalPartIdToConvertedPartId.get(
                    AstIdGenerator.buildSignature(originalConditionalExpression.getElseExpression()));
        }
    }

    public static Optional<TernarySplitResult> split(ASTNode statement) {
        if (statement == null) {
            return Optional.empty();
        }

        ASTNode normalized = TernaryOperatorsConverter.convertTernaryToIfThenElse(statement);
        IfStatement ifStatement = extractIfStatement(normalized);
        if (ifStatement == null) {
            return Optional.empty();
        }

        ConditionalExpression originalConditional = extractConditionalExpression(statement);
        if (originalConditional == null) {
            return Optional.empty();
        }

        Statement thenStatement = extractSingleStatement(ifStatement.getThenStatement());
        Statement elseStatement = extractSingleStatement(ifStatement.getElseStatement());
        if (thenStatement == null || elseStatement == null) {
            return Optional.empty();
        }

        List<Statement> prefixStatements = extractPrefixStatements(normalized, ifStatement);

        Map<String, String> idMap = new HashMap<>();
        idMap.put(
                AstIdGenerator.buildSignature(originalConditional.getExpression()),
                AstIdGenerator.buildSignature(ifStatement.getExpression()));
        idMap.put(
                AstIdGenerator.buildSignature(originalConditional.getThenExpression()),
                AstIdGenerator.buildSignature(thenStatement));
        idMap.put(
                AstIdGenerator.buildSignature(originalConditional.getElseExpression()),
                AstIdGenerator.buildSignature(elseStatement));

        List<TernaryPart> parts = Arrays.asList(
                new TernaryPart(PartKind.CONDITION,
                        originalConditional.getExpression(), ifStatement.getExpression()),
                new TernaryPart(PartKind.THEN_STATEMENT,
                        originalConditional.getThenExpression(), thenStatement),
                new TernaryPart(PartKind.ELSE_STATEMENT,
                        originalConditional.getElseExpression(), elseStatement)
        );

        return Optional.of(new TernarySplitResult(
                statement,
                normalized,
                originalConditional,
                ifStatement,
                thenStatement,
                elseStatement,
                prefixStatements,
                parts,
                idMap));
    }

    public static ConditionalExpression extractConditionalExpression(ASTNode statement) {
        if (statement instanceof ReturnStatement) {
            return getConditionalExpression(((ReturnStatement) statement).getExpression());
        }
        if (statement instanceof ExpressionStatement) {
            Expression expression = ((ExpressionStatement) statement).getExpression();
            if (expression instanceof Assignment) {
                return getConditionalExpression(((Assignment) expression).getRightHandSide());
            }
        }
        if (statement instanceof VariableDeclarationStatement) {
            @SuppressWarnings("unchecked")
            List<VariableDeclarationFragment> fragments =
                    ((VariableDeclarationStatement) statement).fragments();
            for (VariableDeclarationFragment fragment : fragments) {
                ConditionalExpression ce = getConditionalExpression(fragment.getInitializer());
                if (ce != null) {
                    return ce;
                }
            }
        }
        return null;
    }

    private static ConditionalExpression getConditionalExpression(Expression expression) {
        Expression current = expression;
        while (current instanceof ParenthesizedExpression) {
            current = ((ParenthesizedExpression) current).getExpression();
        }
        return current instanceof ConditionalExpression ? (ConditionalExpression) current : null;
    }

    private static IfStatement extractIfStatement(ASTNode normalized) {
        if (normalized instanceof IfStatement) {
            return (IfStatement) normalized;
        }
        if (normalized instanceof Block) {
            @SuppressWarnings("unchecked")
            List<Statement> statements = ((Block) normalized).statements();
            for (Statement stmt : statements) {
                if (stmt instanceof IfStatement) {
                    return (IfStatement) stmt;
                }
            }
        }
        return null;
    }

    private static Statement extractSingleStatement(Statement statement) {
        if (!(statement instanceof Block)) {
            return statement;
        }
        Block block = (Block) statement;
        @SuppressWarnings("unchecked")
        List<Statement> statements = block.statements();
        if (statements.isEmpty()) {
            return null;
        }
        if (statements.size() == 1) {
            return statements.get(0);
        }
        return block;
    }

    private static List<Statement> extractPrefixStatements(ASTNode normalized, IfStatement ifStatement) {
        if (!(normalized instanceof Block)) {
            return Collections.emptyList();
        }
        @SuppressWarnings("unchecked")
        List<Statement> statements = ((Block) normalized).statements();
        List<Statement> prefix = new ArrayList<>();
        for (Statement stmt : statements) {
            if (stmt == ifStatement) {
                break;
            }
            prefix.add(stmt);
        }
        return prefix;
    }
}
