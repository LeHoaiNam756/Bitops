# CT4J — Concolic Testing for Java

A concolic (concrete + symbolic) test generation engine for Java. Given a method, it builds a CFG, explores uncovered paths, generates Z3 constraints, and produces concrete test inputs.

---

## Quick orientation

```
ConcolicTesting.java          ← main entry point
├── ProjectParser             ← wraps Eclipse JDT; parses .java source to AST
├── ASTHelper.generateCfg()   ← builds CFG from method body
├── FindPath.findPathThrough() ← BFS to find path through uncovered node
└── SymbolicExecution         ← runs symbolic execution on a path → Z3 model → test inputs
```

---

## Entry point

**`core/TestGeneration/ConcolicTesting.java`** — `runConcolicTesting(id, filePath, className, methodName, coverage)`

Algorithm:
1. Parse source with `ProjectParser.loadFile()`
2. Build CFG via `ASTHelper.generateCfg()`
3. Seed with `core.SymbolicExecution.RandomTestData.createRandomTestData()`
4. Loop while uncovered nodes remain:
   - `FindPath.findPathThrough(start, uncoveredNode, exit)` → `List<PathNode>`
   - `new SymbolicExecution(parameterList, testPath).execute()`
   - If UNSAT → mark node fake-visited, continue
   - `getTestInputFromModel(parameterClasses)` → run test, record coverage
5. Return `TestResult` with coverage %

---

## Symbolic execution (`SymbolicExecution.java`)

**Constructor:** `SymbolicExecution(List<ASTNode> parameterList, List<PathNode> testPath)`

- `parameterList` — Eclipse JDT `SingleVariableDeclaration` nodes (method formal params)
- `testPath` — `LinkedList<FindPath.PathNode>` from `FindPath`

**`execute()`** flow:
1. Creates `MemoryModel` and Z3 `Context`
2. `executeParameters(ctx)` — declares params as Z3 symbolic constants
3. For each `PathNode` in `testPath`:
   - `AstNode.executeASTNode(astNode, memoryModel)` — evaluates/rewrites AST subtree
   - For `CfgBoolExprNode` where `isRelatedToParameter == true`: produces `BoolExpr`, optionally negated by `PathNode.decision`, accumulates via `ctx.mkAnd()`
4. `createModel(ctx, finalZ3Expression)` — solves constraints

**`getTestInputFromModel(Class<?>[] parameterClasses)`** — extracts concrete values from Z3 `Model`. Uses `ArrayZ3Parser.parseArrayFromModel()` for array params.

**`createRandomTestData(Class<?>[] parameterClasses)`** — fallback random data bounded by `Setup.*Min/*Max`.

---

## Path representation

**`FindPath.PathNode`** (inner class of `core/TestGeneration/path/FindPath.java`):

```java
public static class PathNode {
    public final CfgNode node;
    public final Boolean decision; // true/false for branches, null for non-branch
}
```

`FindPath.findPathThrough(start, mid, end)` — cycle-aware BFS returning `LinkedList<PathNode>`.

---

## AST node model

All classes live under `core/SymbolicExecution/AstNode/`.

**Dispatch chain:**
```
AstNode.executeASTNode()
├── StatementNode.executeStatement()
│   ├── ExpressionStatement → ExpressionNode
│   ├── VariableDeclarationStatement → VariableDeclarationNode
│   └── ReturnStatement → ExpressionNode
├── ExpressionNode.executeExpression()
│   ├── Literal → LiteralNode
│   ├── Assignment → AssignmentNode
│   ├── Name/FieldAccess → NameNode
│   ├── Operation → OperationExpressionNode
│   │   ├── InfixExpressionNode   (arithmetic, comparison, bitwise, logical)
│   │   ├── PrefixExpressionNode  (++, --, !, +, -, ~)
│   │   ├── PostfixExpressionNode (a++, a--)
│   │   └── ParenthesizedExpressionNode
│   ├── CastExpression → CastExpressionNode
│   ├── MethodInvocation → MethodInvocationNode
│   ├── ArrayAccess → ArrayAccessNode
│   ├── ArrayCreation → ArrayCreationNode
│   └── ArrayInitializer → ArrayInitializerNode
└── VariableDeclarationNode
```

**Z3 conversion entry point:** `ExpressionNode.convertAstNodeToZ3Expr(AstNode, Context, MemoryModel)` — dispatches to per-type `convert*ToZ3Expr()` methods.

### Key node details

| Node | Notes |
|------|-------|
| `InfixExpressionNode` | Concrete literal folding + symbolic Z3 (FP or BV arithmetic) |
| `LiteralNumberNode` | int → `ctx.mkBV(val, 32)`, long → 64-bit BV, double → `ctx.mkFP(val, mkFPSort64())` |
| `LiteralCharacterNode` | `ctx.mkBV(val, 16)` |
| `LiteralStringNode` / `LiteralNullNode` | Z3 conversion throws `UnsupportedOperationException` |
| `SimpleNameNode` | Looks up variable in `MemoryModel`; sets `isRelatedToParameter = true` if param |
| `QualifiedNameNode` | Currently only supports `arr.length` |
| `CastExpressionNode` | FP↔BV coercion via `mkFPToFP`, `mkFPToBV`, `mkSignExt`, `mkZeroExt` |
| `ConditionalExpressionNode` | Z3: `ctx.mkITE(condition, then, else)` with JLS numeric promotion |
| `MethodInvocationNode` | Stubs calls as new symbolic parameters named `method_call_at_<pos>` |
| `AssignmentNode` | Handles compound assignments (`+=`, `-=`, etc.) by rewriting to `InfixExpressionNode` |
| `PostfixExpressionNode` | Z3 conversion returns pre-increment operand expression |

---

## Memory model (`MemoryModel.java`)

```java
HashMap<Variable, AstNode> S
```

- `declareVariable(Variable, AstNode)` — add new binding
- `assignVariable(String name, AstNode)` — mutate existing
- `accessVariable(String name)` → current `AstNode` value
- `getVariable(String name)` → `Variable` (holds name + type + `isParameter` flag)

---

## Variable model

**`Variable`** (abstract) — name, `isParameter`, abstract `getType()`, abstract `createZ3Expr(Context)`

**`PrimitiveVariable`** — maps JDT `PrimitiveType.Code` to Z3 sorts:

| Java type | Z3 sort |
|-----------|---------|
| `byte` | 8-bit BV |
| `char` | 16-bit BV |
| `short` | 16-bit BV |
| `int` | 32-bit BV |
| `long` | 64-bit BV |
| `float` | `mkFPSort32()` |
| `double` | `mkFPSort64()` |
| `boolean` | `mkBoolConst` |

**`ArrayVariable`** — Z3 `ArraySort`: domain = 32-bit BV (index), range = element sort. Supports multi-dimensional arrays.

---

## Utility classes

**`TypedExpr`** — singleton `Map<Expr<?>, JavaType>`. Tracks Java type of each Z3 expression for sign-extension and casting decisions. Enum: `INT, FLOAT, DOUBLE, BOOLEAN, CHAR, STRING, LONG, SHORT, BYTE, OTHER`.

**`ArrayZ3Wrapper`**:
- `convertArrayAccessToZ3()` — `ctx.mkSelect(arrayConst, index)`
- `resolveInitialElements()` — `ctx.mkStore(arrayConst, idx, elemExpr)`

**`ArrayZ3Parser`**:
- `parseArrayFromModel(ctx, model, arrayExpr, length, elementSort)` → concrete Java array
- `parseArrayLength(model, lengthExpr)` → `int`

---

## CFG node types

| Class | Role |
|-------|------|
| `CfgNode` | Base; holds AST ref, before/after links |
| `CfgBoolExprNode` | Branch node; has `trueNode`, `falseNode` |
| `CfgExpressionNode` | Expression statement |
| `CfgNormalStatementNode` | Generic statement |
| `CfgIfStatementNode` | If wrapper |
| `CfgWhileStatementNode` | While wrapper |
| `CfgForStatementNode` | For wrapper |
| `CfgDoWhileStatementNode` | Do-while wrapper |
| `CfgSwitchStatementNode` | Switch wrapper |
| `CfgReturnStatementNode` | Return |
| `CfgBreakStatementNode` / `CfgContinueStatementNode` | Break / continue |
| `CfgBeginBlockNode` / `CfgEndBlockNode` | Block delimiters |
| `CfgBeginWhileNode`, `CfgBeginForNode`, etc. | Loop entry markers |

CFG builder: `core/CFG/Utils/ASTHelper.java` (684 lines) — `generateCfg()`.

---

## Source parsing

**`ProjectParser`** wraps Eclipse JDT `ASTParser` (JLS8, binding resolution enabled).

```java
parser.loadFile(filePath);
CompilationUnit cu = parser.getCompilationUnit();
List<ASTNode> methods = parser.getMethods(); // non-constructor MethodDeclarations
```

There is no custom parser — all AST infrastructure is `org.eclipse.jdt.core.dom.*`.

---

## File map

```
core/
├── TestGeneration/
│   ├── ConcolicTesting.java
│   └── path/FindPath.java
├── SymbolicExecution/
│   ├── SymbolicExecution.java
│   ├── MemoryModel.java
│   ├── TypedExpr.java
│   ├── ArrayZ3Wrapper.java
│   ├── ArrayZ3Parser.java
│   ├── Variable/
│   │   ├── Variable.java
│   │   ├── PrimitiveVariable.java
│   │   └── ArrayVariable.java
│   └── AstNode/
│       ├── AstNode.java
│       ├── VariableDeclarationNode.java
│       ├── Statement/StatementNode.java
│       └── Expression/
│           ├── ExpressionNode.java
│           ├── AssignmentNode.java
│           ├── CastExpressionNode.java
│           ├── ConditionalExpressionNode.java
│           ├── MethodInvocationNode.java
│           ├── Name/
│           │   ├── NameNode.java
│           │   ├── SimpleNameNode.java
│           │   └── QualifiedNameNode.java
│           ├── Operation/
│           │   ├── OperationExpressionNode.java
│           │   ├── InfixExpressionNode.java
│           │   ├── PrefixExpressionNode.java
│           │   ├── PostfixExpressionNode.java
│           │   └── ParenthesizedExpressionNode.java
│           ├── Literal/
│           │   ├── LiteralNode.java
│           │   ├── LiteralNumberNode.java
│           │   ├── LiteralBooleanNode.java
│           │   ├── LiteralCharacterNode.java
│           │   ├── LiteralStringNode.java
│           │   └── LiteralNullNode.java
│           └── Array/
│               ├── ArraySymbolicRepresent.java
│               ├── ArrayAccessNode.java
│               ├── ArrayCreationNode.java
│               └── ArrayInitializerNode.java
└── CFG/
    ├── CfgNode.java
    ├── CfgBoolExprNode.java
    ├── ... (other Cfg* nodes)
    └── Utils/
        ├── ASTHelper.java
        ├── TernarySplitHelper.java
        └── TernaryOperatorsConverter.java
```

---

## Known limitations

- `LiteralStringNode` and `LiteralNullNode` cannot be converted to Z3 expressions (throws `UnsupportedOperationException`)
- `QualifiedNameNode` only handles `arr.length`; other field accesses are unsupported
- Method calls are stubbed (any value); inlined symbolic execution of callees is not supported
- No support for object types, generics, or exceptions in the symbolic model
