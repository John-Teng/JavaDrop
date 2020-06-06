import Model.ProtocolConstants;

import javax.annotation.Nonnull;
import java.io.*;

public class JDLink {
    // TODO should this be an object or a static class?
    private static final int BUFFER_SIZE = 2048;

    public static void readRemoteToFile(@Nonnull final InputStream in,
                                        @Nonnull final FileOutputStream out,
                                        final long byteSize) throws IOException {
        inputStreamToOutputStream(in, out, byteSize);
    }

    public static void writeFileToRemote(@Nonnull final FileInputStream in,
                                         @Nonnull final OutputStream out,
                                         final long byteSize) throws IOException {
        inputStreamToOutputStream(in, out, byteSize);
    }

    private static void inputStreamToOutputStream(@Nonnull final InputStream in,
                                                  @Nonnull final OutputStream out,
                                                  final long byteSize) throws IOException {
        long bytesProcessed = 0, read = 0;
        byte[] buffer = new byte[BUFFER_SIZE];
        // TODO optimize reading and writing to the buffer
        while (bytesProcessed < byteSize) {
            read = in.read(buffer);
            out.write(buffer, 0, (int) read);
            bytesProcessed += read;
        }
        out.flush();
    }

    @Nonnull
    public static String readStringFromRemote(@Nonnull final InputStream in) throws IOException {
        final StringBuilder sb = new StringBuilder();
        while (true) {
            int ch1 = in.read();
            int ch2 = in.read();
            if ((ch1 | ch2) < 0)
                throw new EOFException();
            char c = (char) ((ch1 << 8) + (ch2));
            if (c == ProtocolConstants.EOF)
                break;
            sb.append(c);
        }
        // TODO in the future we can just use the default -1 EOF
        return sb.toString();
    }

    public static void writeStringToRemote(@Nonnull final OutputStream out,
                                           @Nonnull final String message) throws IOException {
        for (char c : message.toCharArray()) {
            out.write((c >> 8) & 0xFF);
            out.write((c) & 0xFF);
        }
        out.flush();
    }
}
