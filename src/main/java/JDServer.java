import lombok.extern.log4j.Log4j2;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.ServerSocket;

@Log4j2
public class JDServer {
    private static final int PORT = 10000;

    public static void main(@Nonnull String args[]) {
        while(true) { // server main loop
            try {
                final ServerSocket ssock = new ServerSocket(PORT);
                log.debug("Listening on port + " + PORT);
                while(true) { // create a new client processor to handle a new client
                    ClientProcessor cp = new ClientProcessor(ssock.accept());
                    cp.processClient();
                }
            } catch (IOException e) {
                e.printStackTrace();
                log.debug("broken connection");
            }
        }
    }

}
