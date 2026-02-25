package core.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CommandLine {
    public static void executeCommand(String command) throws IOException, InterruptedException {
        String os = System.getProperty("os.name").toLowerCase();
        String javaHome = System.getProperty("java.home");
        String binDir = javaHome + File.separator + "bin";

        // Parse command into arguments, respecting quotes.
        List<String> parts = splitCommandPreserveArgs(command);
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("Empty command");
        }

        // If command is "javac" or "java" → replace with full path to the binary.
        String exe = parts.get(0);
        if ("javac".equalsIgnoreCase(exe) || "java".equalsIgnoreCase(exe)) {
            parts.set(0, binDir + File.separator + exe.toLowerCase());
        }

        // Run the process directly without going through an intermediate shell.
        ProcessBuilder builder = new ProcessBuilder(parts);

        builder.redirectErrorStream(true); // gộp stderr vào stdout
        Process process = builder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line); // in ra console
            }
        }

        process.waitFor(30, TimeUnit.SECONDS);
    }

    /**
     * Splits a command string into arguments, treating text inside double quotes
     * as a single argument. Quotes are removed from the resulting tokens.
     */
    private static List<String> splitCommandPreserveArgs(String command) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }
        return result;
    }
}
