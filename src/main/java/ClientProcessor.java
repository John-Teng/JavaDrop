import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.InetAddresses;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.math.NumberUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
public class ClientProcessor {
    private static final String DELIMITER = "\\/";
    private static final char EOF = '%';
    private static final String OK_RESPONSE = "OK";
    private static final int BUFFER_SIZE = 2048; // TODO find a good buffer size

    @Nonnull
    protected final Socket csock;
    @Nullable
    protected DataInputStream in;
    @Nullable
    protected DataOutputStream out;
    @Nullable
    protected FileOutputStream fileStream;

    public ClientProcessor(@Nonnull Socket sock) {
        csock = sock;
        log.debug("Connected to client from IP " + csock.getRemoteSocketAddress().toString() + " Port " + csock.getPort());
        try {
            in = new DataInputStream(new BufferedInputStream(csock.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(csock.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
            log.debug("ClientProcessor could not be initialized with I/O Streams");
            terminateWithError();
        }
    }

    private void closeConnections() {
        try {
            csock.close();
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (fileStream != null)
                fileStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            log.debug("There was a problem with closing connections");
        }
    }

    private boolean isPipeValid() {
        return in != null && out != null;
    }

    @VisibleForTesting
    boolean isValidTransferMetadata(@Nonnull String[] parts) {
        return parts.length == 3
                && NumberUtils.isParsable(parts[1])
                && InetAddresses.isInetAddress(parts[2]);
    }

    @Nonnull
    @VisibleForTesting
    String[] readMetadataFromStream() throws IOException {
        final StringBuilder sb = new StringBuilder();
        char c;
        while ((c = in.readChar()) != EOF) {
            sb.append(c);
        }
        return sb.toString().split(DELIMITER);
    }

    @VisibleForTesting
    boolean isUserPermissionGranted(@Nonnull final String filename,
                                    @Nonnull final String host,
                                    final long filesize) {
        // TODO show dialog with options and ask user if we should proceed
        return false;
    }

    void showErrorDialog() {
        // TODO show dialog telling the user that the transfer/connection failed
    }

    private void terminateWithError() {
        closeConnections();
        showErrorDialog();
    }

    @Nonnull
    @VisibleForTesting
    File createUniqueFile(@Nonnull final String filename, @Nonnull final String directory) throws IOException {
        final Set<String> existingNames = Arrays
                .stream(getExistingFilenames(directory))
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        final File saveFile = new File(createIncrementedFilename(filename.toLowerCase(), existingNames));
        if (!saveFile.createNewFile())
            throw new IOException("New file could not be created");
        return saveFile;
    }

    @Nonnull
    @VisibleForTesting
    String createIncrementedFilename(@Nonnull final String originalName, @Nonnull final Set<String> existingNames) {
        int index = originalName.indexOf(".");
        final String body = originalName.substring(0, index), extension = originalName.substring(index);
        String potentialName = body + extension;
        int fileIncrement = 0;
        while (existingNames.contains(potentialName)) {
            fileIncrement++;
            potentialName = body + "-" + fileIncrement + extension;
        }
        return potentialName;
    }

    @Nonnull
    @VisibleForTesting
    String[] getExistingFilenames(@Nonnull final String directory) throws IOException {
        final String[] existingFilenames = new File(directory).list();
        if (existingFilenames == null)
            throw new IOException("Specified save directory could not be located");
        return existingFilenames;
    }

    @Nonnull
    @VisibleForTesting
    void writeToFile(@Nonnull final File saveFile, final long filesize) throws IOException {
        long iterations = filesize / BUFFER_SIZE;
        int remainder = (int) filesize % BUFFER_SIZE;
        if (remainder != 0)
            iterations++;

        byte[] buf = new byte[BUFFER_SIZE];
        fileStream = new FileOutputStream(saveFile);
        // TODO optimize reading and writing to the buffer
        for (int i = 0; i < iterations; i++) {
            int size = i == iterations ? remainder : BUFFER_SIZE;
            in.readFully(buf, 0, size);
            fileStream.write(buf, 0, size);
        }
        fileStream.flush();
        fileStream.close();
    }

    @Nullable
    @VisibleForTesting
    TransferRequest getTransferRequest() throws IOException {
        final String[] parts = readMetadataFromStream();
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
        if (!isPipeValid()) {
            terminateWithError();
            return;
        }
        // TODO unsuccessful completion of this main loop should show error dialog
        try {
            // step 1: parse the filename/filesize(in bytes)/ip metadata from the connection as chars
            final TransferRequest request = getTransferRequest();
            if (request == null) {
                terminateWithError();
                return;
            }

            // step 2: If valid, respond with "OK"
            out.writeChars(OK_RESPONSE);

            // step 3: read the binary data, write to file stream
            // TODO get the correct directory for where the file should be saved
            final File saveFile = createUniqueFile(request.getFilename(), "/");
            writeToFile(saveFile, request.getFilesize());

            // step 4: check to see that the stream is closed by the client by returning a -1
            // if this is not the case, then it means the transfer is invalid
            if (in.read() != -1) {
                log.error("Client has not sent over the listed amount of data");
                terminateWithError();
            }
        } catch (IOException e) {
            e.printStackTrace();
            terminateWithError();
        } finally {
            closeConnections();
        }
    }

    @Getter @Setter @AllArgsConstructor
    static class TransferRequest {
        private long filesize;
        private String filename, host;
    }
}
