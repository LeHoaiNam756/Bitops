package core.SymbolicExecution.execution;

import com.microsoft.z3.Context;

import java.util.HashMap;

public class SymbolicContext implements AutoCloseable {
    private final Context ctx;

    public SymbolicContext() {
        HashMap<String, String> cfg = new HashMap<>();
        cfg.put("model", "true");
        ctx = new Context(cfg);
    }

    public Context z3() {
        return ctx;
    }

    @Override
    public void close() {
        ctx.close();
    }
}
