package core.parser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utility that extracts a {@code .zip} archive into a target directory,
 * guarding against zip-slip path traversal attacks.
 */
public final class ZipExtractor {

    private ZipExtractor() {}

    /**
     * Extract {@code zipPath} into {@code targetDir}.
     *
     * @param zipPath   path to the .zip file
     * @param targetDir destination directory (will be created if absent)
     * @throws IOException          on I/O error
     * @throws SecurityException    on zip-slip attempt
     */
    public static void extract(Path zipPath, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        String canonicalTarget = targetDir.toRealPath().toString();

        try (InputStream fis = Files.newInputStream(zipPath);
             ZipInputStream zis = new ZipInputStream(fis)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path resolved = targetDir.resolve(entry.getName()).normalize();

                // Zip-slip guard
                if (!resolved.toAbsolutePath().toString().startsWith(canonicalTarget)) {
                    throw new SecurityException(
                            "Zip slip detected for entry: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(resolved);
                } else {
                    Files.createDirectories(resolved.getParent());
                    Files.copy(zis, resolved, StandardCopyOption.REPLACE_EXISTING);
                }

                zis.closeEntry();
            }
        }
    }
}
