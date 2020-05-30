import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.*;

public class ClientProcessorTest {
    private static final String WRITE_STREAM_BYTES_TEST_DIR = "writeStreamBytesToFileTest";
    private static final String WRITE_STREAM_BYTES_SOURCE_FILE = "source.jpg";
    private static final String WRITE_STREAM_BYTES_NEW_FILE = "new.jpg";

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

        // This line needs to be repeated in certain tests
        processor = new ClientProcessor(mockServerSocket.accept());
    }

    @Test
    public void testValidTransferRequest() {
        final String[] validTest1 = {"test.png", "12345", "192.42.123.24"};
        final String[] validTest2 = {"test.png", "12345", "2001:db8:1234:0000:0000:0000:0000:0000"};
        final String[] invalidTest1 = {"test.png", "12345f", "2001:db8:1234:0000:0000:0000:0000:0000"};
        final String[] invalidTest2 = {"test.png", "12345", "999.12.32.43"};
        assertTrue(processor.isValidTransferMetadata(validTest1));
        assertTrue(processor.isValidTransferMetadata(validTest2));
        assertFalse(processor.isValidTransferMetadata(invalidTest1));
        assertFalse(processor.isValidTransferMetadata(invalidTest2));
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

        assertArrayEquals(expected, processor.readMetadataFromStream());
    }

    @Test(expected = EOFException.class)
    public void testObtainMetadataNoEOF() throws IOException {
        final String testMetadata = "filename.png/12345/192.142.23.12";

        // Java is Big Endian, so using 'regular' UTF-16 will include a BOM at beginning
        final InputStream is = new ByteArrayInputStream(StandardCharsets.UTF_16BE.encode(testMetadata).array());
        inputStream = new DataInputStream(is);

        // The following block will break if moved to @Before section, the socket's inputStream will be null
        when(mockSocket.getInputStream()).thenReturn(inputStream);
        processor = new ClientProcessor(mockServerSocket.accept());

        // Trigger exception
        processor.readMetadataFromStream();
    }

    @Test
    public void testWriteStreamBytesToFile() throws IOException {
        final String path = this
                .getClass()
                .getResource(WRITE_STREAM_BYTES_TEST_DIR)
                .toString()
                .replaceFirst("file:", "");
        final File source = new File(path + "/" + WRITE_STREAM_BYTES_SOURCE_FILE);
        final File testSave = new File(path + "/" + WRITE_STREAM_BYTES_NEW_FILE);
        testSave.delete();
        if (!testSave.createNewFile()) {
            fail("test file could not be saved");
        }

        // 1. create a input stream source, maybe from a source txt file
        // so long as inputStream only reads data as binary, this should be ok
        inputStream = new DataInputStream(new FileInputStream(source));

        // 2. feed the input stream to the processor object and try to write to file
        // The following block will break if moved to @Before section, the socket's inputStream will be null
        when(mockSocket.getInputStream()).thenReturn(inputStream);
        processor = new ClientProcessor(mockServerSocket.accept());
        processor.writeStreamBytesToFile(testSave, source.length());

        // 3. assert that the two file values are the same
        assertEquals(source.length(), testSave.length());
        assertTrue(Files.equal(source, testSave));
    }
}
