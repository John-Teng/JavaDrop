import org.junit.After;
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
    public Socket mockSocket;
    public ServerSocket mockServerSocket;
    public DataInputStream inputStream;
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

    @After
    public void cleanup() throws IOException {
        if (inputStream != null)
            inputStream.close();
    }

    @Test
    public void testValidTransferMetadata() {
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
}
