import com.google.common.net.InetAddresses;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nonnull;
import java.io.*;

@Log4j2
public class JDClient {
    public static void main(@Nonnull String args[]) {
        // TODO basic args will be in the form: javadrop /usr/test.txt 192.168.12.54

        // Sanitize user input
        if (args.length != 2) {
            System.out.println("Usage: javadrop filepath IP-destination");
            return;
        }
        final File source = new File(args[0]);
        if (!source.isFile()) {
            System.out.println("Source file is invalid");
            return;
        }
        if (!"localhost".equals(args[1]) && !InetAddresses.isInetAddress(args[1])) {
            System.out.println("Destination IP is invalid");
            return;
        }

        final ServerProcessor processor = new ServerProcessor(source, args[1]);
        processor.attemptTransfer();
    }
}
