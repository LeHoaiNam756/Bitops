# CT4J Package Summary

This document provides a high-level overview of the four key packages in the CT4J (Concolic Testing for Java) project: `model`, `dispatch`, `cfg`, and `simplifier`. These packages form the core pipeline of the symbolic execution engine.

---

---

## 1. `core.SymbolicExecution.model`

### Purpose
The **core symbolic representation layer**. It defines the immutable data structures used to represent symbolic expressions, program state, and the Java type system during concolic execution.

### Location
- **Source:** `src/main/java/core/SymbolicExecution/model/`
- **Types Subpackage:** `src/main/java/core/SymbolicExecution/model/types/`

### Key Components

| Class | Role |
|-------|------|
| **`SymbolicValue`** | Sealed interface — root of the symbolic expression AST. |
| **`SymLiteral`** | Concrete constant values (int, long, float, double, char, boolean, etc.). |
| **`SymVariable`** | Variable references by name. |
| **`SymBinaryOp`** | Binary operations (`+`, `-`, `*`, `/`, `==`, `<`, `&&`, etc.). |
| **`SymUnaryOp`** | Unary operations (`-`, `!`, `++`, `--`, `~`). |
| **`SymITE`** | If-then-else conditional expressions. |
| **`SymFieldAccess`** | Field access with receiver and field name. |
| **`SymArraySelect`** | Array element read (`arr[index]`). |
| **`SymArrayStore`** | Array element write (`arr[index] = value`). |
| **`SymbolicState`** | Immutable snapshot bundling `MemoryModel` + `TypeContext`. Supports `fork()` for branch exploration. |
| **`MemoryModel`** | SSA-style versioned memory (`x_0`, `x_1`, ...). |
| **`TypeContext`** | Stack-based type tracker with primitive type constants. |
| **`SymType`** | Sealed interface for all symbolic types (primitives, objects, arrays, generics, wildcards, etc.). |
| **`SymTypeMap`** | Converts Eclipse JDT `Type` AST nodes into `SymType` equivalents. |

### Design Patterns
- **Immutable Records:** All `SymbolicValue` subtypes are Java `record` classes.
- **Sealed Interfaces:** `SymbolicValue` and `SymType` use sealed interfaces for type-safe pattern matching.
- **SSA (Static Single Assignment):** `MemoryModel` versions variables on every write.

### Relationships
- **Produced by:** `dispatch` package (AST handlers create `SymbolicValue` trees).
- **Consumed by:** `simplifier` package (optimizes `SymbolicValue` trees), `execution` package (encodes to Z3).

---

## 2. `core.SymbolicExecution.dispatch`

### Purpose
The **AST-to-symbolic bridge**. It traverses Eclipse JDT AST nodes and converts them into the internal `SymbolicValue` representation. It is essentially a Chain-of-Responsibility / Visitor pattern implementation.

### Location
- **Source:** `src/main/java/core/SymbolicExecution/dispatch/`
- **Tests:** `src/test/java/core/SymbolicExecution/dispatch/`

### Key Components

| Class | Role |
|-------|------|
| **`AstDispatcher`** | Registry and router. Maintains a list of `AstHandler` instances and delegates `eval(node, state)` to the first handler that `supports()` the node. |
| **`AstHandler`** | Interface: `boolean supports(ASTNode)` and `SymbolicValue eval(ASTNode, SymbolicState, AstDispatcher)`. |
| **`InfixExpressionHandler`** | Binary operations (`+`, `-`, `*`, `/`, `==`, `<`, `&&`, etc.). |
| **`PrefixExpressionHandler`** | Unary prefix (`++x`, `--x`, `!`, `+`, `-`, `~`). |
| **`PostfixExpressionHandler`** | Postfix (`x++`, `x--`). |
| **`AssignmentHandler`** | Assignments including compound (`+=`, `-=`, etc.). |
| **`ConditionalExpressionHandler`** | Ternary (`a ? b : c`). |
| **`CastExpressionHandler`** | Type casts. |
| **`ParenthesizedExpressionHandler`** | Parentheses. |
| **`MethodInvocationHandler`** | Method calls (currently stubbed). |
| **`FieldAccessHandler`** | Explicit field access (`obj.field`). |
| **`QualifiedNameHandler`** | Dotted names (`Math.PI`, `a.b`). |
| **`SimpleNameHandler`** | Variable names, constants, fields. |
| **`ArrayAccessHandler`** | Array indexing. |
| **`ArrayCreationHandler`** | Array literals/creation. |
| **`NumberLiteralHandler`** | Numeric literals with full suffix/type support. |
| **`BooleanLiteralHandler`** | Boolean literals. |
| **`CharacterLiteralHandler`** | Character literals. |
| **`BlockHandler`** | Statement blocks. |
| **`ExpressionStatementHandler`** | Expression statements. |
| **`ReturnStatementHandler`** | Return statements. |
| **`VariableDeclarationStatementHandler`** | Variable declarations. |

### Design Patterns
- **Chain of Responsibility:** `AstDispatcher` iterates handlers until one matches.
- **Visitor Pattern:** Each handler targets a specific JDT AST node type.

### Relationships
- **Produces:** `SymbolicValue` instances for the `model` package.
- **Consumes:** `SymbolicState` (reads/writes `MemoryModel`, uses `TypeContext`).
- **No direct interaction with:** `cfg` or `simplifier` packages.

### Known Issues
- `DefaultAstHandlers.defaultDispatcher()` currently returns `null`.
- `MethodInvocationHandler` is disabled (`supports()` returns `false`).
- `ParenthesizedExpressionHandler` incorrectly uses `expression.getParent()` instead of `expression.getExpression()`.
- `PostfixExpressionHandler` has a tautological condition `(e.getOperator() == e.getOperator())`.

---

## 3. `core.cfg`

### Purpose
A **next-generation Control Flow Graph (CFG) builder** intended to replace the legacy `core.CFG` package. It parses a Java method body (Eclipse JDT AST) into a directed graph suitable for path analysis.

### Location
- **Source:** `src/main/java/core/cfg/`
- **Tests:** `src/test/java/core/cfg/`

### Key Components

| Class | Role |
|-------|------|
| **`CfgGraph`** | Immutable graph container with inner classes `Node` (id, kind, AST reference, content) and `Edge` (from, to, kind). |
| **`CfgGraphBuilder`** | Entry point: `build(MethodDeclaration)` → `CfgGraph`. Uses internal records `Fragment`, `PendingExit`, `LoopContext`, `ConditionChain`. |
| **`CfgNodeKind`** | Enum: `ENTRY`, `EXIT`, `STMT`, `BRANCH`, `LOOP`, `BLOCK`. |
| **`CfgEdgeKind`** | Enum: `NORMAL`, `TRUE`, `FALSE`. |

### Design Patterns
- **Builder Pattern:** `CfgGraphBuilder` incrementally constructs `CfgGraph`.
- **Wire-up Pattern:** Sub-statements return partial graphs with dangling exits that the caller connects.

### Relationships
- **Feeds into:**: NO 
- **No direct interaction with:** `model`, `dispatch`, or `simplifier` packages..

### Known Issues
- `BLOCK` node kind is declared but never assigned.
- `CfgGraphTest` is completely empty.
- `ASTHelper.generateCfg(MethodDeclaration)` returns `null` (critical integration bug).
- `ConcolicTesting` uses hardcoded node IDs `1, 1` for `PathFinder`.
- No tests for `splitBooleanExpression = true`.
- `switch` statements fall through to `buildPlain` (treated as opaque statements).

---

## 4. `core.SymbolicExecution.simplifier`

### Purpose
A **symbolic expression optimization and canonicalization pipeline**. It reduces the complexity of `SymbolicValue` trees before they are sent to the Z3 SMT solver or stored in path conditions. This reduces solver load, improves cache hit rates, and makes structural equality checks trivial.

### Location
- **Source:** `src/main/java/core/SymbolicExecution/simplifier/`

### Key Components

| Class | Role |
|-------|------|
| **`SymbolicSimplifier`** | Main orchestrator. Performs bottom-up, memoized traversal and applies the strategy pipeline (up to 8 rounds or fixed-point). |
| **`SymValueFactory`** | Hash-consing factory. Guarantees structurally equal expressions share the same Java object reference (turning `O(n)` equality into `O(1)`). |
| **`SimplificationStrategy`** | Functional interface for a single simplification pass. |
| **`ConstantFoldingStrategy`** | Evaluates operations on literal operands at analysis time (e.g., `3 + 4` → `7`). |
| **`IdentityEliminationStrategy`** | Applies algebraic identity laws (e.g., `x + 0 = x`, `x * 0 = 0`). |
| **`DeadConstraintStrategy`** | Classifies boolean constraints as tautology, contradiction, or unknown without calling Z3. |
| **`NormalisationStrategy`** | Canonicalizes expression shape (commutativity ordering, associative flattening, negation normalization). |

### Pipeline Execution Model
1. Recursively simplify children first.
2. Run the strategy pipeline: `ConstantFolding` → `IdentityElimination` → `DeadConstraint` → `Normalisation`.
3. Intern the result via `SymValueFactory.intern()`.
4. Cache the result in an `IdentityHashMap`.

### Design Patterns
- **Strategy Pipeline:** Four interchangeable strategies applied in sequence.
- **Hash-Consing:** `SymValueFactory` uses a `ConcurrentHashMap` for structural interning.
- **Memoization:** Results are cached to avoid redundant simplification.

### Relationships
- **Operates on:** `SymbolicValue` trees from the `model` package.
- **No direct interaction with:** `dispatch` or `cfg` packages.
- **Integration Status:** Currently **unintegrated** into the active symbolic execution flow. Dispatch handlers instantiate `SymbolicValue` records directly with `new`, bypassing `SymValueFactory`.

---

## Architectural Observations

### Two CFG Systems
The codebase currently contains **two competing CFG representations**:

| Aspect | Legacy `core.CFG` | New `core.cfg` |
|--------|-------------------|----------------|
| **Style** | Object-oriented tree/node hierarchy | Graph with integer IDs and edge maps |
| **Key classes** | `CfgNode`, `CfgBoolExprNode`, `CfgBlockNode`, ... | `CfgGraph`, `CfgGraphBuilder` |
| **Used by** | `ConcolicTesting`, `MarkedPath`, `ASTHelper` (main flow) | `PathFinder`, partially wired in `ConcolicTesting` |
| **Branching** | `trueNode`/`falseNode` object references | `TRUE`/`FALSE` edge kinds on integer IDs |
| **Coverage tracking** | Built-in (`isVisited`, `isTrueMarked`, etc.) | Not present (separation of concerns) |

The new `core.cfg` is a cleaner, graph-theoretic refactor designed to separate CFG construction from coverage state and symbolic execution. It is extensively tested but **not yet fully adopted** as the primary CFG.
`core.CFG` is deprecated, 
### Unintegrated Simplifier
The `simplifier` package is mature and well-architected but currently sits outside the active pipeline. A future integration point would be to call `SymbolicSimplifier.simplify()` on `SymbolicValue` trees immediately after dispatch produces them, before they are stored in `MemoryModel` or encoded to Z3.
