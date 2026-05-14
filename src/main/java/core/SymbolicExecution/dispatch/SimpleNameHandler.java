package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.*;
import org.eclipse.jdt.core.dom.*;

import java.util.Optional;

public class SimpleNameHandler implements AstHandler{
    @Override
    public boolean supports(ASTNode node) {
        return node instanceof SimpleName;
    }

    @Override
    public SymbolicValue eval(ASTNode node, SymbolicState state, AstDispatcher dispatcher) {
        SimpleName name = (SimpleName) node;
        String identifier = name.getIdentifier();

        // 1. Resolve binding first — tells us what kind of name this is
        IBinding binding = name.resolveBinding();

        // 2. Type name — not a value at all (e.g. the "String" in String.valueOf)
        if (binding instanceof ITypeBinding) {
            return new SymVariable("__type_" + identifier);
        }

        // 3. Static constant via binding (same as QualifiedNameHandler path 2a)
        if (binding instanceof IVariableBinding vb
                && vb.isField()
                && isCompileTimeConstant(vb)) {
            Object cv = vb.getConstantValue();
            if (cv instanceof Integer  i) return SymLiteral.of(i.intValue());
            if (cv instanceof Long     l) return SymLiteral.of(l.longValue());
            if (cv instanceof Double   d) return SymLiteral.of(d.doubleValue());
            if (cv instanceof Boolean  b) return SymLiteral.of(b.booleanValue());
            if (cv instanceof Character c) return SymLiteral.of(c.charValue());
            if (cv instanceof Float   f) return SymLiteral.of((float) f.floatValue());
            if (cv instanceof Short    s) return SymLiteral.of((short) s.intValue());
            if (cv instanceof Byte     b) return SymLiteral.of((byte) b.intValue());
        }

        // 4. Known variable — look up current SSA version in state
        Optional<SymbolicValue> stored = state.getMemoryModel().read(identifier);
        if (stored.isPresent()) return stored.get();

        // 5. Field access via implicit "this" — e.g. bare "field" inside a method
        if (binding instanceof IVariableBinding vb && vb.isField()) {
            // model as select(this, fieldName) — same heap model as QualifiedName
            return new SymFieldAccess(
                    new SymVariable("this"),
                    identifier
            );
        }

        // 6. Unknown / unresolved — fresh symbolic variable
        //    covers: parameters not yet written, unresolved bindings, outer scope vars
        return new SymVariable(identifier);
    }

    private boolean isCompileTimeConstant(IVariableBinding vb) {
        int mod = vb.getModifiers();
        return Modifier.isStatic(mod)
                && Modifier.isFinal(mod)
                && vb.getConstantValue() != null;
    }

}
