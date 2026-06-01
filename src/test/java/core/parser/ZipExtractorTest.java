package core.parser;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ZipExtractor} using Mockito and JUnit4.
 * <p>
 * The tests heavily mock file system interactions and the ZIP stream
 * to verify the extraction logic and the zip‑slip protection in
 * complete isolation from any real I/O.
 * </p>
 */
@RunWith(MockitoJUnitRunner.class)
public class ZipExtractorTest {

    @Mock
    private Path mockZipPath;

    @Mock
    private Path mockTargetDir;

    @Mock
    private Path mockCanonicalPath;

    private MockedStatic<Files> filesStaticMock;

    // We will instantiate a fresh mocked construction for each test.
    private MockedConstruction<ZipInputStream> zipInputStreamConstruction;

    @Before
    public void setUpStaticMocks() {
        filesStaticMock = mockStatic(Files.class);
    }

    @After
    public void tearDownStaticMocks() {
        filesStaticMock.close();
        if (zipInputStreamConstruction != null) {
            zipInputStreamConstruction.close();
        }
    }

    // ------------------------------------------------------------------------
    // Normal extraction – both file and directory entries
    // ------------------------------------------------------------------------
    @Test
    public void shouldCreateTargetDirectoryAndExtractEntries() throws Exception {
        // canonical path of target directory
        when(mockTargetDir.toRealPath()).thenReturn(mockCanonicalPath);
        when(mockCanonicalPath.toString()).thenReturn("/secure/target");

        // mock the InputStream returned by Files.newInputStream
        InputStream mockFis = mock(InputStream.class);
        filesStaticMock.when(() -> Files.newInputStream(mockZipPath)).thenReturn(mockFis);

        // set up entries
        ZipEntry fileEntry = mock(ZipEntry.class);
        when(fileEntry.getName()).thenReturn("readme.txt");
        when(fileEntry.isDirectory()).thenReturn(false);

        ZipEntry dirEntry = mock(ZipEntry.class);
        when(dirEntry.getName()).thenReturn("docs/");
        when(dirEntry.isDirectory()).thenReturn(true);

        // capture the mocked ZipInputStream so we can verify interactions
        List<ZipInputStream> captured = new ArrayList<>();
        zipInputStreamConstruction = mockConstruction(ZipInputStream.class,
                (mock, context) -> {
                    captured.add(mock);
                    when(mock.getNextEntry()).thenReturn(fileEntry, dirEntry, null);
                    // Files.copy will call read(byte[], int, int) – return -1 to end stream
                    when(mock.read(any(byte[].class), anyInt(), anyInt())).thenReturn(-1);
                });

        // mock the resolved paths and their normalization / parent
        Path resolvedFile = mock(Path.class);
        when(resolvedFile.toAbsolutePath()).thenReturn(resolvedFile);
        when(resolvedFile.toString()).thenReturn("/secure/target/readme.txt");
        Path fileParent = mock(Path.class);
        when(resolvedFile.getParent()).thenReturn(fileParent);

        Path resolvedDir = mock(Path.class);
        when(resolvedDir.toAbsolutePath()).thenReturn(resolvedDir);
        when(resolvedDir.toString()).thenReturn("/secure/target/docs/");

        when(mockTargetDir.resolve("readme.txt")).thenReturn(resolvedFile);
        when(resolvedFile.normalize()).thenReturn(resolvedFile);

        when(mockTargetDir.resolve("docs/")).thenReturn(resolvedDir);
        when(resolvedDir.normalize()).thenReturn(resolvedDir);

        // static methods without side effects
        filesStaticMock.when(() -> Files.createDirectories(any(Path.class)))
                .thenReturn(mock(Path.class));
        filesStaticMock.when(() -> Files.copy(any(InputStream.class), any(Path.class),
                any(StandardCopyOption.class)))
                .thenReturn(0L);

        // act
        ZipExtractor.extract(mockZipPath, mockTargetDir);

        // verify target directory creation
        filesStaticMock.verify(() -> Files.createDirectories(mockTargetDir));

        // verify file entry processing
        filesStaticMock.verify(() -> Files.createDirectories(fileParent));
        filesStaticMock.verify(() -> Files.copy(any(ZipInputStream.class),
                eq(resolvedFile), eq(StandardCopyOption.REPLACE_EXISTING)));

        // verify directory entry processing (no copy, no getParent)
        filesStaticMock.verify(() -> Files.createDirectories(resolvedDir));
        verify(captured.get(0), times(2)).closeEntry();
    }

    // ------------------------------------------------------------------------
    // Zip‑slip attempt → SecurityException
    // ------------------------------------------------------------------------
    @Test
    public void shouldThrowSecurityExceptionOnZipSlip() throws Exception {
        when(mockTargetDir.toRealPath()).thenReturn(mockCanonicalPath);
        when(mockCanonicalPath.toString()).thenReturn("/secure/target");

        filesStaticMock.when(() -> Files.newInputStream(mockZipPath))
                .thenReturn(mock(InputStream.class));

        ZipEntry slipEntry = mock(ZipEntry.class);
        when(slipEntry.getName()).thenReturn("../../etc/passwd");

        zipInputStreamConstruction = mockConstruction(ZipInputStream.class,
                (mock, context) -> when(mock.getNextEntry()).thenReturn(slipEntry, (ZipEntry) null));

        Path resolvedSlip = mock(Path.class);
        when(resolvedSlip.toAbsolutePath()).thenReturn(resolvedSlip);
        // This path does NOT start with the canonical target
        when(resolvedSlip.toString()).thenReturn("/etc/passwd");

        when(mockTargetDir.resolve("../../etc/passwd")).thenReturn(resolvedSlip);
        when(resolvedSlip.normalize()).thenReturn(resolvedSlip);

        // No need to mock Files.copy – it should never be reached

        assertThrows(SecurityException.class,
                () -> ZipExtractor.extract(mockZipPath, mockTargetDir));

        // Ensure we never tried to create the dangerous path or copy into it
        filesStaticMock.verify(() -> Files.createDirectories(mockTargetDir));
        filesStaticMock.verify(() -> Files.createDirectories(resolvedSlip), never());
        filesStaticMock.verify(
                () -> Files.copy(
                        any(InputStream.class),
                        any(Path.class),
                        any(CopyOption[].class)
                ),
                never()
        );


    }

    // ------------------------------------------------------------------------
    // Empty archive (no entries)
    // ------------------------------------------------------------------------
    @Test
    public void shouldHandleEmptyZip() throws Exception {
        when(mockTargetDir.toRealPath()).thenReturn(mockCanonicalPath);
        when(mockCanonicalPath.toString()).thenReturn("/secure/target");

        filesStaticMock.when(() -> Files.newInputStream(mockZipPath))
                .thenReturn(mock(InputStream.class));

        zipInputStreamConstruction = mockConstruction(ZipInputStream.class,
                (mock, context) -> when(mock.getNextEntry()).thenReturn(null));

        filesStaticMock.when(() -> Files.createDirectories(any(Path.class)))
                .thenReturn(mock(Path.class));

        ZipExtractor.extract(mockZipPath, mockTargetDir);

        filesStaticMock.verify(() -> Files.createDirectories(mockTargetDir));
        filesStaticMock.verify(
                () -> Files.copy(
                        any(InputStream.class),
                        any(Path.class),
                        any(CopyOption[].class)
                ),
                never()
        );

    }
}