import javafx.application.Platform;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nonnull;
import java.util.concurrent.CountDownLatch;

@Log4j2
public class DialogArbiter implements Permissions.Arbiter {
    @Override
    public boolean requestPermission(@Nonnull final String filename,
                                     @Nonnull final String source,
                                     final long filesize,
                                     @Nonnull final Permissions.Requester requester) {
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] permission = new boolean[1];
        try {
            new Thread(() -> {
                log.debug("Spawned UI thread on: " + Thread.currentThread().getId());
                Platform.runLater(() -> {
                    new JavaFxDialog(filename, source, filesize, new JavaFxDialog.Callback() {
                        @Override
                        public void success() {
                            permission[0] = true;
                            latch.countDown();
                        }

                        @Override
                        public void failure() {
                            permission[0] = false;
                            latch.countDown();
                        }
                    }).start(new Stage());
                });
            }).start();
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return permission[0];
    }
}
