import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.*;

public class ClientProcessorTest {

    private static final String TEST_DIR = "existingFilesTest";
    private static final char EOF = '%';
    public Socket mockSocket;
    public ServerSocket mockServerSocket;
    public DataInputStream inputStream;
    public DataOutputStream outputStream;
    public ClientProcessor processor;

    @Before
    public void init() throws IOException {
        mockSocket = mock(Socket.class);
        mockServerSocket = mock(ServerSocket.class);
        final SocketAddress mockSocketAddress = mock(SocketAddress.class);

        when(mockSocketAddress.toString()).thenReturn("Test.IP");
        when(mockServerSocket.accept()).thenReturn(mockSocket);
        when(mockSocket.getRemoteSocketAddress()).thenReturn(mockSocketAddress);
        when(mockSocket.getPort()).thenReturn(10000);

        processor = new ClientProcessor(mockServerSocket.accept()); // This line needs to be repeated in certain tests
    }

    @Test
    public void testGetExistingFilenamesSuccess() throws IOException {
        final String pathToTestFolder = this
                .getClass()
                .getResource(TEST_DIR)
                .toString()
                .replaceFirst("file:", "");

        final String[] existingFiles = processor.getExistingFilenames(pathToTestFolder);
        Arrays.sort(existingFiles);

        assertEquals(3, existingFiles.length);
        assertEquals("test1.png", existingFiles[0]);
        assertEquals("test2.jpg", existingFiles[1]);
        assertEquals("test3.jpeg", existingFiles[2]);
    }

    @Test(expected = IOException.class)
    public void testGetExistingFilenamesBadDirectory() throws IOException {
        processor.getExistingFilenames("/random-incorrect-directory");
    }

    @Test
    public void testCreateSimilarFilename() {
        final Set<String> filenames = ImmutableSet.of("test.png", "testo.png", "testo-1.png", "testy1.png", "tst.jpg");

        // Test the increments
        assertEquals("test-1.png", processor.createIncrementedFilename("test.png", filenames));
        assertEquals("testo-2.png", processor.createIncrementedFilename("testo.png", filenames));
        assertEquals("testy1-1.png", processor.createIncrementedFilename("testy1.png", filenames));

        // Same body, but different file extension
        assertEquals("tst.png", processor.createIncrementedFilename("tst.png", filenames));
    }

    @Test
    public void testValidTransferRequest() {
        final String[] validTest1 = {"test.png", "12345", "192.42.123.24"};
        final String[] validTest2 = {"test.png", "12345", "2001:db8:1234:0000:0000:0000:0000:0000"};
        final String[] invalidTest1 = {"test.png", "12345f", "2001:db8:1234:0000:0000:0000:0000:0000"};
        final String[] invalidTest2 = {"test.png", "12345", "999.12.32.43"};
        assertTrue(processor.isValidTransferRequest(validTest1));
        assertTrue(processor.isValidTransferRequest(validTest2));
        assertFalse(processor.isValidTransferRequest(invalidTest1));
        assertFalse(processor.isValidTransferRequest(invalidTest2));
    }

    @Test
    public void testObtainMetadataSuccess() throws IOException {
        final String testMetadata = "filename.png/12345/192.142.23.12%";
        final String[] expected = {"filename.png", "12345", "192.142.23.12"};

        // Java is Big Endian, so using 'regular' UTF-16 will include a BOM at beginning
        final InputStream is = new ByteArrayInputStream(StandardCharsets.UTF_16BE.encode(testMetadata).array());
        inputStream = new DataInputStream(is);

        // The following block will break if moved to @Before section, the socket's inputStream will be null
        when(mockSocket.getInputStream()).thenReturn(inputStream);
        processor = new ClientProcessor(mockServerSocket.accept());

        assertArrayEquals(expected, processor.obtainMetadata());
    }

    @Test(expected = EOFException.class)
    public void testObtainMetadataNoEOF() throws IOException {
        final String testMetadata = "filename.png/12345/192.142.23.12";
        final String[] expected = {"filename.png", "12345", "192.142.23.12"};

        // Java is Big Endian, so using 'regular' UTF-16 will include a BOM at beginning
        final InputStream is = new ByteArrayInputStream(StandardCharsets.UTF_16BE.encode(testMetadata).array());
        inputStream = new DataInputStream(is);

        // The following block will break if moved to @Before section, the socket's inputStream will be null
        when(mockSocket.getInputStream()).thenReturn(inputStream);
        processor = new ClientProcessor(mockServerSocket.accept());

        // Trigger exception
        processor.obtainMetadata();
    }
}
