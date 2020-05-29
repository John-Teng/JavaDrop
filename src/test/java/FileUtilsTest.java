import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class FileUtilsTest {
    private static final String TEST_DIR = "existingFilesTest";

    @Test
    public void testGetExistingFilenamesSuccess() throws IOException {
        final String pathToTestFolder = this
                .getClass()
                .getResource(TEST_DIR)
                .toString()
                .replaceFirst("file:", "");

        final String[] existingFiles = FileUtils.getExistingFilenames(pathToTestFolder);
        Arrays.sort(existingFiles);
        assertEquals(3, existingFiles.length);
        assertEquals("test1.png", existingFiles[0]);
        assertEquals("test2.jpg", existingFiles[1]);
        assertEquals("test3.jpeg", existingFiles[2]);
    }

    @Test(expected = IOException.class)
    public void testGetExistingFilenamesBadDirectory() throws IOException {
        FileUtils.getExistingFilenames("/random-incorrect-directory");
    }

    @Test
    public void testCreateSimilarFilename() {
        final Set<String> filenames = ImmutableSet.of("test.png", "testo.png", "testo-1.png", "testy1.png", "tst.jpg");

        // Test the increments
        assertEquals("test-1.png", FileUtils.createIncrementedFilename("test.png", filenames));
        assertEquals("testo-2.png", FileUtils.createIncrementedFilename("testo.png", filenames));
        assertEquals("testy1-1.png", FileUtils.createIncrementedFilename("testy1.png", filenames));

        // Same body, but different file extension
        assertEquals("tst.png", FileUtils.createIncrementedFilename("tst.png", filenames));
    }
}
