import org.junit.Test;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class SettingsTest {
    private static final String GET_SETTINGS_FILE_TEST_DIR = "getSettingsFileTest";
    private static final String FILE_NAME = "settings.yaml";

    private String prepareSettingsTest() {
        final String dir = this
                .getClass()
                .getResource(GET_SETTINGS_FILE_TEST_DIR)
                .toString()
                .replaceFirst("file:", "");
        final String pathToFile = dir + "/" + FILE_NAME;
        final File f = new File(pathToFile);
        if (!f.delete()) {
            fail("Settings test could not be prepared - test file could not be cleaned up");
        }
        return pathToFile;
    }

    @Test
    public void getSettingsFile_CreateNewFile() throws IOException {
        // Given
        final String pathToFile = prepareSettingsTest();
        System.out.println(pathToFile);

        // When
        final File testFile = Settings.getSettingsFile(pathToFile);

        // Then
        assert testFile != null;
        assertTrue(testFile.exists());
        assertTrue(FileUtils
                .readFileToString(testFile, StandardCharsets.UTF_16BE)
                .contains("downloadPath:"));
    }

    @Test
    public void getSettingsFile_GetExistingFile() throws IOException {
        // Given
        final String pathToFile = prepareSettingsTest();
        final File existingFile = new File(pathToFile);
        if (!existingFile.createNewFile()) {
            fail("Could not set up GetExistingFile test, existing file could not be created");
        }

        // When
        final File testFile = Settings.getSettingsFile(pathToFile);

        // Then
        assert testFile != null;
        assertTrue(testFile.exists());
        assertEquals(testFile, existingFile);
    }

    @Test
    public void getSettingTest_Success() throws IOException {
        // Given
        final String pathToFile = prepareSettingsTest();
        final File testFile = new File(pathToFile);
        if (!testFile.createNewFile()) {
            fail("Could not set up GetExistingFile test, existing file could not be created");
        }
        final FileOutputStream fout = new FileOutputStream(testFile);
        fout.write("testKey: testValue".getBytes());
        fout.flush();

        // When
        assertNull(Settings.settings);
        Settings.loadSettings(pathToFile);

        // Then
        assertNotNull(Settings.settings);
        assertEquals("testValue", Settings.getSetting("testKey"));

        // Cleanup
        Settings.reset();
    }

    @Test(expected = FileNotFoundException.class)
    public void getSettingTest_Failure() throws FileNotFoundException {
        // Given
        final String badPath = "path/to/nowhere";

        // Then
        Settings.loadSettings(badPath);

        // Cleanup, unreachable
        Settings.reset();
    }

}
