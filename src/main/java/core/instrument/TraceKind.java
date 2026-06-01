package core.instrument;

/**
 * Classifies what a {@link TracePoint} represents within the CFG.
 *
 * <ul>
 *   <li>{@link #NODE}   – a plain statement node</li>
 *   <li>{@link #COND_T} – the true-branch of a conditional / loop header</li>
 *   <li>{@link #COND_F} – the false-branch of a conditional / loop header</li>
 * </ul>
 */
public enum TraceKind {
    NODE,
    COND_T,
    COND_F
}
