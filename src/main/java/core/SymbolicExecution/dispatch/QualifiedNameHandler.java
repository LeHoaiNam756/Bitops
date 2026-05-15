package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.*;
import org.eclipse.jdt.core.dom.*;

import java.util.Optional;

public class QualifiedNameHandler implements AstHandler {

    @Override
    public boolean supports(ASTNode node) {
        return node instanceof QualifiedName;
    }

    @Override
    public SymbolicValue eval(ASTNode node, SymbolicState state, AstDispatcher dispatcher) {
       QualifiedName qn = (QualifiedName) node;

        String fullName = qn.getFullyQualifiedName(); // "d.e", "Math.PI", "a.b.c.d"

        // 1. Check state first — maybe it was already written as a flat key
        Optional<SymbolicValue> stored = state.getMemoryModel().read(fullName);
        if (stored.isPresent()) return stored.get();

        // 2. Try to resolve via JDT binding
        IBinding binding = qn.resolveBinding();

        if (binding instanceof IVariableBinding vb) {

            // 2a. Static constant — fold it immediately
            if (vb.isField() && isCompileTimeConstant(vb)) {
                Object cv = vb.getConstantValue();
                if (cv instanceof Integer  i) return SymLiteral.of(i.intValue());
                if (cv instanceof Long     l) return SymLiteral.of(l.longValue());
                if (cv instanceof Double   d) return SymLiteral.of(d.doubleValue());
                if (cv instanceof Float    f) return SymLiteral.of((float) f.floatValue());
                if (cv instanceof Boolean  b) return SymLiteral.of(b.booleanValue());
                if (cv instanceof Character c) return SymLiteral.of(c.charValue());
                if (cv instanceof Byte    b) return SymLiteral.of((byte) b.intValue());
                if (cv instanceof Short s ) return SymLiteral.of((short) s.intValue());
            }

            // 2b. Instance field — model as select on the receiver object
            if (vb.isField()) {
                SymbolicValue receiver = dispatcher.eval(qn.getQualifier(), state);
                String field = qn.getName().getIdentifier();
                return new SymFieldAccess(receiver, field);
            }
        }

        // 3. Binding unavailable (no classpath, generated code, etc.)
        // Fall back: treat the whole dotted name as a flat symbolic variable
        return new SymVariable(fullName);
    }

    private boolean isCompileTimeConstant(IVariableBinding vb) {
        // JDT marks compile-time constants: static final primitive or String
        // with a non-null constant value
        int mod = vb.getModifiers();
        return Modifier.isStatic(mod)
            && Modifier.isFinal(mod)
            && vb.getConstantValue() != null;
    }
}
