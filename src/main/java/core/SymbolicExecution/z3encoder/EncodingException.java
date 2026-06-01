package core.SymbolicExecution.z3encoder;

import core.SymbolicExecution.model.SymbolicValue;

/**
 * Thrown when a {@link SymbolicValue} node cannot be encoded into a Z3
 * expression — either because the sort is unresolvable, the operation has
 * no Z3 counterpart, or a type mismatch is detected at encoding time.
 *
 * Carries the offending node for diagnostic purposes.
 */
public final class EncodingException extends RuntimeException {

    private final SymbolicValue offendingNode;

    public EncodingException(String message, SymbolicValue node) {
        super(message + "  [node: " + node + "]");
        this.offendingNode = node;
    }

    public EncodingException(String message, SymbolicValue node, Throwable cause) {
        super(message + "  [node: " + node + "]", cause);
        this.offendingNode = node;
    }

    public SymbolicValue offendingNode() { return offendingNode; }
}
