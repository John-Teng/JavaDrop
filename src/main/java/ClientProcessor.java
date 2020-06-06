import Model.ProtocolConstants;
import Model.TransferRequest;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.InetAddresses;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.math.NumberUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.net.Socket;

@Log4j2
public class ClientProcessor {
    @Nonnull
    protected final Socket csock;
    @Nullable
    protected DataInputStream in;
    @Nullable
    protected DataOutputStream out;
    @Nullable
    protected FileOutputStream fileOut;

    public ClientProcessor(@Nonnull Socket sock) {
        csock = sock;
        log.debug("Connected to client from IP " + csock.getRemoteSocketAddress().toString() + " Port " + csock.getPort());
        try {
            in = new DataInputStream(new BufferedInputStream(csock.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(csock.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
            log.debug("ClientProcessor could not be initialized with I/O Streams");
            closeConnectionsWithError();
        }
    }

    private void closeConnections() {
        try {
            csock.close();
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (fileOut != null)
                fileOut.close();
        } catch (IOException e) {
            e.printStackTrace();
            log.debug("There was a problem with closing connections");
        }
    }

    @VisibleForTesting
    boolean isValidTransferMetadata(@Nonnull String[] parts) {
        return parts.length == 3
                && NumberUtils.isParsable(parts[1])
                && InetAddresses.isInetAddress(parts[2]);
    }

    @Nonnull
    @VisibleForTesting
    String[] readMetadataPartsFromStream() throws IOException {
        final String metadata = JDLink.readStringFromRemote(in);
        return metadata.split(ProtocolConstants.DELIMITER);
    }

    @VisibleForTesting
    boolean isUserPermissionGranted(@Nonnull final String filename,
                                    @Nonnull final String host,
                                    final long filesize) {
        // TODO show dialog with options and ask user if we should proceed
        return false;
    }

    @VisibleForTesting
    void showErrorDialog() {
        // TODO show dialog telling the user that the transfer/connection failed
    }

    private void closeConnectionsWithError() {
        closeConnections();
        showErrorDialog();
    }

    @Nullable
    @VisibleForTesting
    TransferRequest getTransferRequest() throws IOException {
        final String[] parts = readMetadataPartsFromStream();
        if (!isValidTransferMetadata(parts)) {
            log.error("Received metadata contains error");
            return null;
        }
        final TransferRequest request = new TransferRequest(Long.parseLong(parts[1]), parts[0], parts[2]);
        if (!isUserPermissionGranted(request.getFilename(), request.getHost(), request.getFilesize())) {
            log.info("User has denied permission for file transfer");
            return null;
        }
        return request;
    }

    public void processClient() {
        // check IO Pipe before we attempt
        if (in == null || out == null) {
            closeConnectionsWithError();
            return;
        }
        // TODO unsuccessful completion of this main loop should show error dialog
        try {
            // step 1: parse the filename/filesize(in bytes)/ip metadata from the connection as chars
            final TransferRequest request = getTransferRequest();
            if (request == null) {
                closeConnectionsWithError();
                return;
            }

            // step 2: If valid, respond with "OK"
            JDLink.writeStringToRemote(out, ProtocolConstants.OK_RESPONSE);

            // step 3: read the binary data, write to file stream
            // TODO get the correct directory for where the file should be saved
            final File saveFile = FileUtils.createUniqueFile(request.getFilename(), "/");
            fileOut = new FileOutputStream(saveFile);
            JDLink.readRemoteToFile(in, fileOut, request.getFilesize());

            // step 4: check to see that the stream is closed by the client by returning a -1
            // if this is not the case, then it means the transfer is invalid
            if (in.read() != -1) {
                log.error("Client has not sent over the listed amount of data");
                closeConnectionsWithError();
            }
        } catch (IOException e) {
            e.printStackTrace();
            closeConnectionsWithError();
        } finally {
            closeConnections();
        }
    }
}
