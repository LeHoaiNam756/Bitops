package core.generation;

import core.parser.JavaProjectParser;
import core.parser.ParseEntry;
import core.parser.ParseResult;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Project {
    private final Path zipPath;
    private final Map<MethodDeclaration, CompilationUnit> MAP_METHODS_2_ROOT_AST;
    private List<ParseEntry> entries;

    public Project(Path zipPath) {
        this.zipPath = zipPath;
        this.MAP_METHODS_2_ROOT_AST = new HashMap<>();
        this.entries = List.of();
        setup();
    }

    private void setup() {
        JavaProjectParser parser = new JavaProjectParser();
        try {
            ParseResult parseResult = parser.parseZip(zipPath);
            this.entries = parseResult.entries();
            mapMethods(parseResult);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse project zip file: " + zipPath, e);
        }
    }

    private void mapMethods(ParseResult parseResult) {
        List<ParseEntry> entries = parseResult.entries();
        for (ParseEntry entry : entries) {
            CompilationUnit cu = entry.compilationUnit();
            cu.accept( new ASTVisitor() {
                @Override
                public boolean visit(MethodDeclaration node) {
                    MAP_METHODS_2_ROOT_AST.put(node, cu);
                    return false;
                }
            });
        }
    }

    public CompilationUnit getRootAST(MethodDeclaration method) {
        return MAP_METHODS_2_ROOT_AST.get(method);
    }

    public List<MethodDeclaration> getMethods() {
        return List.copyOf(MAP_METHODS_2_ROOT_AST.keySet());
    }

    public List<ParseEntry> getEntries() {
        return List.copyOf(entries);
    }


}
