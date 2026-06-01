package core.instrument;

import core.cfg.ControlFlowGraph;
import core.cfg.Coverage;
import core.parser.ParseEntry;
import core.testpath.BranchCoverageTracker;
import core.testpath.CoverageTracker;
import core.utils.FilePath;
import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public class InstrumentationFactory {
    private final InstrumentationPlanner planner;
    private final SourceEmitter          emitter;

    public record InstrumentationProduct(
            InstrumentationPlan plan,
            CoverageTracker tracker,
            Path instrumentProductPath,
            Path trackPath
    ) {}

    public InstrumentationFactory() {
        this(new InstrumentationPlanner(), new SourceEmitter());
    }

    public InstrumentationFactory(InstrumentationPlanner planner,
                                  SourceEmitter emitter) {
        this.planner   = Objects.requireNonNull(planner);
        this.emitter   = Objects.requireNonNull(emitter);
    }

    public InstrumentationProduct produce(CompilationUnit cu, ControlFlowGraph cfg, Coverage coverage)
            throws Exception {
        InstrumentationPlan plan = planner.plan(cu, cfg, coverage);
        CoverageTracker tracker = coverage == Coverage.STATEMENT
                ? new CoverageTracker(plan.nodeIds())
                : new BranchCoverageTracker(cfg);
        String instrumentedSource = emitter.emit(
                cu, plan, coverage, FilePath.CLONED_PROJECT_ROOT_PACKAGE);
        prepareCloneDirectory();
        String instrumentedProductPath = writeCloneFile(fileNameFor(cu), instrumentedSource);
        try {
            core.utils.Compiler.getInstance().compileJavaFile(
                    instrumentedProductPath, FilePath.PATH_TO_MAVEN_TARGET_CLASSES);
        } catch (RuntimeException e) {
            throw new Exception("Compilation failed for regenerated file: " + instrumentedProductPath, e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new InstrumentationProduct(
                plan, tracker,  Path.of(instrumentedProductPath),
                Path.of(FilePath.JCIA_PROJECT_ROOT_PATH, FilePath.PATH_TO_CLONED_PROJECT, ".ct4j-trace")
        );
    }

    public static void deleteFilesInDirectory(String directoryPath) throws IOException {
        if (Files.exists(Path.of(directoryPath))) {
            FileUtils.cleanDirectory(new File(directoryPath));
        } else {
            FileUtils.forceMkdir(new File(directoryPath));
        }
    }

    private void prepareCloneDirectory() throws IOException {
        deleteFilesInDirectory(FilePath.PATH_TO_CLONED_PROJECT);
    }

    private static String writeCloneFile(String fileName,
                                          String source) throws IOException {
        Path dir  = Path.of(FilePath.JCIA_PROJECT_ROOT_PATH,
                FilePath.PATH_TO_CLONED_PROJECT);
        Files.createDirectories(dir);
        Path file = dir.resolve(fileName);
        Files.writeString(file, source + "\n", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
        return file.toString();
    }

    static String fileNameFor(CompilationUnit cu) {
        IJavaElement javaElement = cu.getJavaElement();
        if (javaElement != null) {
            return javaElement.getElementName();
        }
        Object sourceFileName = cu.getProperty(ParseEntry.SOURCE_FILE_NAME_PROPERTY);
        if (sourceFileName instanceof String fileName && !fileName.isBlank()) {
            return fileName;
        }
        throw new IllegalArgumentException("CompilationUnit does not contain a source file name");
    }
}
