import org.junit.Before;
import org.junit.Test;

import java.io.*;

import static org.mockito.Mockito.mock;
import static org.junit.Assert.*;

public class ServerProcessorTest {
    public ServerProcessor processor;
    
    @Before
    public void init() {
        processor = new ServerProcessor(mock(File.class), "testIP");
    }

    @Test
    public void testGenerateRequestStringSuccess() {
        final String expected = "test.png/123456/192.241.41.16";
        final String filename = "test.png", host = "192.241.41.16";
        long filesize = 123456;

        assertEquals(expected, processor.generateRequestString(filename, host, filesize));
    }

}
