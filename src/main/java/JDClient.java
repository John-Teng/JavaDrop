import com.google.common.net.InetAddresses;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nonnull;
import java.io.File;

@Log4j2
public class JDClient {
    public static void main(@Nonnull String args[]) {
        // TODO basic args will be in the form: javadrop /usr/test.txt 192.168.12.54

        // Step 1: sanitize user input
        if (args.length != 2) {
            System.out.println("Usage: javadrop filepath IP-destination");
            return;
        }
        final File source = new File(args[0]);
        if (!source.isFile()) {
            System.out.println("Source file is invalid");
            return;
        }
        if (!InetAddresses.isInetAddress(args[1])) {
            System.out.println("Destination IP is invalid");
            return;
        }

        // Step 2: get the file metadata
        final String filename = source.getName(), host = args[1];
        long filesize = source.length();

        // Step 3: establish server handshake
        // Step 4: send over binary data via output stream
    }

}
