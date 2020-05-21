import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.math.NumberUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.net.Socket;

@Log4j2
public class ClientProcessor {
    private static final String DELIMITER = "/"; // TODO check if this acts as a regex
    @Nonnull
    protected final Socket csock;
    @Nullable
    protected DataInputStream in;
    @Nullable
    protected DataOutputStream out;

    public ClientProcessor(@Nonnull Socket sock) {
        csock = sock;
        log.debug("Connected to client from IP " + csock.getRemoteSocketAddress() + " Port " + csock.getPort());
        try {
            in = new DataInputStream(new BufferedInputStream(csock.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(csock.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
            log.debug("ClientProcessor could not be initialized with I/O Streams");
            closeConnections();
        }
    }

    private void closeConnections() {
        try {
            csock.close();
            if (in != null)
                in.close();
            if (out != null)
                out.close();
        } catch (IOException e) {
            e.printStackTrace();
            log.debug("There was a problem with closing connections");
        }
    }

    private boolean validIOStreams() {
        return in != null && out != null;
    }

    private boolean isValidTransferRequest() {
        // step 1: parse the filename/filesize(in bytes)/ip from the connection as chars
        final StringBuilder sb = new StringBuilder();
        try {
            while (in.available() > 0) {
                sb.append(in.readChar());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        final String[] parts = sb.toString().split(DELIMITER);
        if (parts.length != 3) {
            return false;
        }
        final String filename = parts[0], ip = parts[2];
        if (!NumberUtils.isParsable(parts[1]))
            return false;
        final int filesize = Integer.parseInt(parts[1]);

        // send a prompt to the user to see if they would like to accept this file
        return true; // or false based on user's input
    }

    public void processClient() {
        if (!validIOStreams()) {
            closeConnections();
            return;
        }

        if (!isValidTransferRequest()) {
            closeConnections();
            return;
        }
        // step 2: If valid respond with "OK"
        // step 3: parse the binary data and keep count
        // step 4: once the connection severs, count the binary data to see if complete
    }
}
