import Model.ProtocolConstants;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.Socket;

@Log4j2
public class ServerProcessor {
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
            sock = new Socket(destination, ProtocolConstants.PORT); // This should block
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
    @VisibleForTesting
    String generateRequestString(@Nonnull final String filename,
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

    public void attemptTransfer() {
        if (in == null || out == null || fileIn == null) {
            closeConnectionsWithMessage("Invalid IO Streams");
            return;
        }
        try {
            // Step 1: Send transfer request
            JDLink.writeStringToRemote(out, generateRequestString(source.getName(), destination, source.length()));

            // Step 2: Wait for OK
            final String response = JDLink.readStringFromRemote(in);
            if (!ProtocolConstants.OK_RESPONSE.equals(response)) {
                closeConnectionsWithMessage("Receiver has denied transfer request");
                return;
            }

            // Step 3: Write bytes to stream
            JDLink.writeFileToRemote(fileIn, out, source.length());
            closeConnectionsWithMessage("File data has been sent");
        } catch (IOException e) {
            e.printStackTrace();
            closeConnectionsWithMessage("Transfer attempt has failed due to exception");
        }
    }
}
