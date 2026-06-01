# Code Review: `core.cfg` package

## `CfgGraph.java`

L18: 🟡 risk: `Node` accepts nullable `ASTNode ast` with no `@Nullable` annotation or javadoc. Mark param `@Nullable` so static analysis catches unchecked derefs in callers.

L47: 🟡 risk: `Edge` constructor accepts raw `int from/to` without validating they exist in `nodes`. Dangling edges are silently allowed. Add existence check or document that builder guarantees validity.

L66-68: 🔵 nit: `nodes`, `outgoing`, `incoming` expose internal mutable state through `getNode`/`outgoing`/`incoming`. Consider returning unmodifiable views or defensive copies.

L79-83: 🟡 risk: `addEdge` creates edges for non-existent node IDs. Verify `nodes.containsKey(from)` and `nodes.containsKey(to)`; throw `IllegalArgumentException` if not.

L85-87: 🟡 risk: `getNode` returns `null` for missing ID. Callers must null-check. Return `Optional<Node>` or document `@Nullable` return.

L89-93: 🔵 nit: `incoming` is a one-liner, `outgoing` is multi-line. Inconsistent formatting. Reformat `incoming` to match `outgoing`.

L93: 🟡 risk: `outgoing(id)` returns mutable internal list for known nodes, immutable `emptyList()` for unknown. Inconsistent mutability. Return `Collections.unmodifiableList` everywhere.

L99-105: 🔵 nit: `edgeCount` is O(edges) on every call. Maintain an `int edgeCount` field incremented in `addEdge` for O(1).

## `CfgGraphBuilder.java`

L19: 🔴 bug: `build(MethodDeclaration)` does not check `method.getBody() == null`. Abstract/interface methods have no body; calling `build` will NPE on L23. Add null guard and throw `IllegalArgumentException`.

L82-85: 🟡 risk: `loopStack.push/pop` around `buildStatement` with no `try/finally`. If AST traversal throws, the stack leaks context and subsequent loop resolution is corrupted. Wrap in `try { ... } finally { loopStack.pop(); }`.

L134-138: 🟡 risk: Same push/pop without `try/finally` in `buildFor`. Wrap in `try/finally`.

L186-190: 🟡 risk: `Objects.requireNonNull(loopStack.peek())` throws raw NPE for `break` outside loop. Replace with `IllegalStateException("break outside loop")`.

L192-196: 🟡 risk: `Objects.requireNonNull(loopStack.peek())` throws raw NPE for `continue` outside loop. Replace with `IllegalStateException("continue outside loop")`.

L60-61: 🔵 nit: `stmt.getExpression().toString()` called twice. Extract local `String condStr = stmt.getExpression().toString();` to avoid double stringify.

L79-80: 🔵 nit: `stmt.getExpression().toString()` called twice in `buildWhile`. Extract local.

L121-123: 🔵 nit: `stmt.getExpression()` accessed twice; `compare.toString()` could be extracted once.

L180-184: 🔵 nit: `buildReturn` passes `stmt.getExpression()` (nullable) to `addNode`. Node stores it without annotation. If callers later dereference `node.getAst()` on a void return, they NPE. Document that `getAst()` may be null for void returns.

## `CfgGraphTest.java`

L7-9: 🔴 bug: Empty test class. No coverage for `addNode`, `addEdge`, `getNode`, `outgoing`, `incoming`, `nodeCount`, `edgeCount`. Add unit tests for the graph data structure before relying on it in builder tests.

## `CfgGraphBuilderTest.java`

L6-10: 🔴 bug: Empty test method `buildsGraphFromMethod`. No assertions, no AST setup, no graph verification. Implement the test or delete the placeholder.

## Package-wide

`Node` / `Edge` / `CfgGraph`: 🔵 nit: No `toString()`, `equals()`, or `hashCode()`. Debugging failing tests or graph dumps is painful. Generate them or add a `toDot()` helper.
