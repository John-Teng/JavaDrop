import com.google.common.net.InetAddresses;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.math.NumberUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.net.Socket;
import java.util.Arrays;

@Log4j2
public class ClientProcessor {
    private static final String DELIMITER = "\\/";
    private static final char EOF = '%';
    private static final String OK_RESPONSE = "OK";
    private static final int BUFFER_SIZE =  2048; // TODO find a good buffer size

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

    private boolean isPipeValid() {
        return in != null && out != null;
    }

    private boolean isValidTransferRequest(@Nonnull String[] parts) {
        return parts.length == 3
                && NumberUtils.isParsable(parts[1])
                && InetAddresses.isInetAddress(parts[2]);
    }

    @Nonnull
    private String[] obtainMetadata() throws IOException {
        final StringBuilder sb = new StringBuilder();
        char c;
        while ((c = in.readChar()) != EOF) {
            sb.append(c);
        }
        return sb.toString().split(DELIMITER);
    }

    private boolean isUserPermissionGranted(@Nonnull final String filename,
                                            @Nonnull final String host,
                                            int filesize) {
        // TODO show dialog with options and ask user if we should proceed
        return false;
    }

    @Nonnull
    private File createUniqueFile(@Nonnull final String originalFilename) throws IOException {
        final String[] existingFilenames = new File("/").list(); // TODO make this the save destination directory, probably want to sanitize this earlier on
        if (existingFilenames == null)
            throw new IOException("Specified save directory could not be located");
        final StringBuilder filenameBuilder = new StringBuilder(originalFilename);

        int fileIncrement = 1;
        Arrays.sort(existingFilenames);
        while (Arrays.binarySearch(existingFilenames, filenameBuilder.toString()) > 0) {
            if (fileIncrement > 1)
                filenameBuilder.deleteCharAt(filenameBuilder.length() - 1);
            filenameBuilder.append(fileIncrement);
            fileIncrement ++;
        }
        final File saveFile = new File(filenameBuilder.toString());
        saveFile.createNewFile();
        return saveFile;
    }

    public void processClient() {
        // check IO Pipe before we attempt
        if (!isPipeValid()) {
            closeConnections();
            return;
        }

        // TODO break each of these steps into individual functions to unit test
        try {
            // step 1: parse the filename/filesize(in bytes)/ip metadata from the connection as chars
            final String[] parts = obtainMetadata();
            if (!isValidTransferRequest(parts)) {
                log.error("Received metadata contains error");
                closeConnections();
                return;
            }
            final String originalFilename = parts[0], host = parts[2];
            final int filesize = Integer.parseInt(parts[1]);
            if (!isUserPermissionGranted(originalFilename, host, filesize)) {
                log.info("User has denied permission for file transfer");
                closeConnections();
                return;
            }

            // step 2: If valid, respond with "OK"
            out.writeChars(OK_RESPONSE);

            // step 3: read the binary data, write to file stream
            long readCount = 0;
            long iterations = filesize / BUFFER_SIZE;
            if (filesize % BUFFER_SIZE != 0)
                iterations ++;

            byte[] buf = new byte[BUFFER_SIZE];
            final File saveFile = createUniqueFile(originalFilename);
            final FileOutputStream fileStream = new FileOutputStream(saveFile);
            // TODO should filestream be declared earlier so that it can properly close upon exception?

            for (int i = 0; i < iterations; i++){
                readCount += Math.min(in.available(), buf.length);
                in.readFully(buf);
                fileStream.write(buf);
                // TODO careful about the very last buffer, it will contain outdated bytes at the end
            }
            fileStream.flush();
            fileStream.close();

            // step 4: check to see that the stream is closed by the client by returning a -1
            // if this is not the case, then it means the transfer is invalid
            if (in.read() != -1) {
                log.error("Client has not sent over the listed amount of data");
                closeConnections();
                return;
            }

        } catch(IOException e) {
            e.printStackTrace();
            closeConnections();
            return;
        }


        closeConnections();
    }
}
