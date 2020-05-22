import com.google.common.net.InetAddresses;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.math.NumberUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.net.Socket;

@Log4j2
public class ClientProcessor {
    private static final String DELIMITER = "/"; // TODO check if this acts as a regex
    private static final String OK_RESPONSE = "OK";
    @Nonnull
    protected final Socket csock;
    @Nullable
    protected DataInputStream in;
    @Nullable
    protected DataOutputStream out;

    // TODO make this class a singleton
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

    private boolean isIOStreamValid() {
        return in != null && out != null;
    }

    private boolean isValidTransferRequest(@Nonnull String[] parts) {
        return parts.length == 3
                && NumberUtils.isParsable(parts[1])
                && InetAddresses.isInetAddress(parts[2]);
    }

    public void processClient() {
        if (!isIOStreamValid()) {
            closeConnections();
            return;
        }

        // step 1: parse the filename/filesize(in bytes)/ip from the connection as chars
        final StringBuilder sb = new StringBuilder();
        try {
            while (in.available() > 0) {
                sb.append(in.readChar());
            }
        } catch (IOException e) {
            e.printStackTrace();
            closeConnections();
            return;
        }
        final String[] parts = sb.toString().split(DELIMITER);
        if (!isValidTransferRequest(parts)) {
            closeConnections();
            return;
        }
        final String filename = parts[0], host = parts[2];
        final int filesize = Integer.parseInt(parts[1]);
        // TODO Ask user with prompt to request transfer of file


        // step 2: If valid respond with "OK"
        try {
            out.writeChars(OK_RESPONSE);
        } catch (IOException e) {
            e.printStackTrace();
            closeConnections();
            return;
        }

        // step 3: parse the binary data and keep count
        // step 4: once the connection severs, count the binary data to see if complete

        closeConnections();
    }
}
