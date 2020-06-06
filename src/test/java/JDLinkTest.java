import com.google.common.io.Files;
import org.junit.After;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

public class JDLinkTest {
    private static final String READ_REMOTE_TO_FILE_TEST_DIR = "readRemoteToFileTest";
    private static final String WRITE_FILE_TO_REMOTE_TEST_DIR = "writeFileToRemoteTest";
    private static final String SOURCE_FILE = "source.jpg";
    private static final String NEW_FILE = "new.jpg";

    // We should reserve `in` and `out` to be used as arguments for InputStream and OutputStream,
    // even if they are implemented as FileInputStreams/FileOutputStreams
    public InputStream in;
    public OutputStream out;
    public FileInputStream fileIn;
    public FileOutputStream fileOut;

    @After
    public void cleanup() throws IOException {
        if (in != null)
            in.close();
        if (out != null)
            out.close();
        if (fileIn != null)
            fileIn.close();
        if (fileOut != null)
            fileOut.close();
    }

    @Nonnull
    private File[] createFileComparisonTest(@Nonnull final String dirName) throws IOException {
        final String path = this
                .getClass()
                .getResource(dirName)
                .toString()
                .replaceFirst("file:", "");
        final File source = new File(path + "/" + SOURCE_FILE);
        final File testSave = new File(path + "/" + NEW_FILE);
        testSave.delete();
        if (!testSave.createNewFile()) {
            fail("test file could not be saved");
        }
        return new File[]{source, testSave};
    }

    @Test
    public void testReadRemoteToFile() throws IOException {
        final File[] temp = createFileComparisonTest(READ_REMOTE_TO_FILE_TEST_DIR);
        final File source = temp[0], testSave = temp[1];

        in = new FileInputStream(source);
        fileOut = new FileOutputStream(testSave);
        JDLink.readRemoteToFile(in, fileOut, source.length());

        assertEquals(source.length(), testSave.length());
        assertTrue(Files.equal(source, testSave));
    }

    @Test
    public void testWriteFileToRemote() throws IOException {
        final File[] temp = createFileComparisonTest(WRITE_FILE_TO_REMOTE_TEST_DIR);
        final File source = temp[0], testSave = temp[1];

        fileIn = new FileInputStream(source);
        out = new FileOutputStream(testSave);
        JDLink.writeFileToRemote(fileIn, out, source.length());

        assertEquals(source.length(), testSave.length());
        assertTrue(Files.equal(source, testSave));
    }

    @Test
    public void testReadStringFromRemote() throws IOException {
        final String expected = "Hello World";
        final String test = "Hello World%";
        in = new ByteArrayInputStream(StandardCharsets.UTF_16BE.encode(test).array());
        assertEquals(expected, JDLink.readStringFromRemote(in));
    }

    @Test
    public void testWriteStringToRemote() throws IOException {
        final String test = "Hello World";
        final byte[] expected = StandardCharsets.UTF_16BE.encode(test).array();
        out = new ByteArrayOutputStream();
        JDLink.writeStringToRemote(out, test);
        assertArrayEquals(expected, ((ByteArrayOutputStream) out).toByteArray());
    }
}
