import Model.ProtocolConstants;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.Socket;

@Log4j2
public class ServerProcessor {
    private static final int BUFFER_SIZE = 2048;

    private Socket sock;
    private DataOutputStream out;
    private DataInputStream in;
    private FileInputStream fileIn;

    private final File source;
    private final String destination;

    public ServerProcessor(@Nonnull final File source, @Nonnull final String destination) {
        this.source = source;
        this.destination = destination;
        setupConnections();
    }

    private void setupConnections() {
        try {
            sock = new Socket(request.getHost(), ProtocolConstants.PORT); // This should block
            in = new DataInputStream(new BufferedInputStream(sock.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(sock.getOutputStream()));
            fileIn = new FileInputStream(source);
        } catch (IOException e) {
            e.printStackTrace();
            log.debug("Problem with setting up connections");
            closeConnections();
        }
    }

    private void closeConnectionsWithMessage(@Nonnull final String message) {
        System.out.println(message);
        closeConnections();
    }

    private void closeConnections() {
        try {
            if (sock != null)
                sock.close();
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (fileIn != null)
                fileIn.close();
        } catch (IOException e) {
            e.printStackTrace();
            log.debug("There was a problem with closing connections");
        }
    }

    @Nonnull
    private static String generateRequestString(@Nonnull final String filename,
                                                @Nonnull final String host,
                                                long filesize) {
        return new StringBuilder()
                .append(filename)
                .append(ProtocolConstants.DELIMITER)
                .append(filesize)
                .append(ProtocolConstants.DELIMITER)
                .append(host)
                .append(ProtocolConstants.EOF)
                .toString();
    }

    @Nonnull
    private String readResponse() throws IOException {
        final StringBuilder sb = new StringBuilder();
        char c;
        while ((c = in.readChar()) != ProtocolConstants.EOF) {
            sb.append(c);
        }
        return sb.toString();
    }

    private boolean isPipeValid() {
        return in != null && out != null;
    }

    private void writeFileBytesToStream() throws IOException {
        long iterations = source.length() / BUFFER_SIZE;
        if (source.length() % BUFFER_SIZE != 0)
            iterations++;

        final byte[] buffer = new byte[BUFFER_SIZE];
        for (int i = 0; i < iterations; i++) {
            int size = Math.min(in.available(), BUFFER_SIZE);
            fileIn.read(buffer, 0, size);
            out.write(buffer, 0, size);
        }
    }

    public void attemptTransfer() {
        if (!isPipeValid()) {
            closeConnectionsWithMessage("Invalid IO Streams");
            return;
        }
        try {
            // Step 1: Send transfer request
            out.writeChars(generateRequestString(source.getName(), destination, source.length()));

            // Step 2: Wait for OK
            final String response = readResponse();
            if (!ProtocolConstants.OK_RESPONSE.equals(response)) {
                closeConnectionsWithMessage("Receiver has denied transfer request");
                return;
            }

            // Step 3: Write bytes to stream
            writeFileBytesToStream();
            closeConnectionsWithMessage("File data has been sent");
        } catch (IOException e) {
            e.printStackTrace();
            closeConnectionsWithMessage("Transfer attempt has failed due to exception");
        }
    }
}
