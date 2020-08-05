import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nonnull;
import java.util.concurrent.CountDownLatch;

@Log4j2
public class JavaFxDialog extends Application {
    private final String filename, source;
    private final long filesize;
    private final Callback callback;

    interface Callback {
        void success();
        void failure();
    }

    static class DialogArbiter implements Permissions.Arbiter {
        @Override
        public void requestPermission(@Nonnull final String filename,
                                      @Nonnull final String source,
                                      final long filesize,
                                      @Nonnull final Permissions.Requester requester) {
            final CountDownLatch latch = new CountDownLatch(1);
            final int[] returnValues = new int[1];
            try {
                new Thread(() -> {
                    log.debug("Spawned thread on: " + Thread.currentThread().getId());
                    Platform.runLater(() -> {
                        new JavaFxDialog(filename, source, filesize, new Callback() {
                            @Override
                            public void success() {
                                returnValues[0] = 1;
                                latch.countDown();
                            }

                            @Override
                            public void failure() {
                                returnValues[0] = 0;
                                latch.countDown();
                            }
                        }).start(new Stage());
                    });
                }).start();
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.debug("Return value: " + returnValues[0]);
        }
    }

    public JavaFxDialog (@Nonnull final String filename,
                         @Nonnull final String source,
                         final long filesize,
                         @Nonnull final Callback callback) {
        this.filename = filename;
        this.source = source;
        this.filesize = filesize;
        this.callback = callback;
    }

    public static void main(String[] args) {
        DialogArbiter d = new DialogArbiter();
        System.out.println("Main thread id: " + Thread.currentThread().getId());
        d.requestPermission("test.png", "192.12.31.1", 123123, new Permissions.Requester() {
            @Override
            public void onPermissionGranted() {
                System.out.println("GRANTED");
                System.out.println("Executing thread id: " + Thread.currentThread().getId());
            }

            @Override
            public void onPermissionRejected() {
                System.out.println("REJECTED");
                System.out.println("Executing thread id: " + Thread.currentThread().getId());
            }
        });
    }

    @Override
    public void start(Stage stage) {
        final Label label = new Label("Accept file transfer of file: " + filename + "(" + filesize + ") bytes from " + source + "?");
        final Button reject = new Button("No");
        reject.setMinWidth(40);
        reject.setMaxWidth(40);
        reject.setOnAction(e -> {
            callback.failure();
            Platform.exit();
        });

        final Button accept = new Button("Yes");
        accept.setMinWidth(40);
        accept.setMaxWidth(40);
        accept.setOnAction(e -> {
            callback.success();
            Platform.exit();
        });

        final HBox buttonLayout = new HBox();
        buttonLayout.getChildren().addAll(accept, reject);
        buttonLayout.setAlignment(Pos.CENTER);
        buttonLayout.setSpacing(20);

        final VBox layout = new VBox();
        layout.getChildren().addAll(label, buttonLayout);
        layout.setAlignment(Pos.CENTER);
        layout.setSpacing(20);

        final Scene scene = new Scene(layout, 150, 100);
        stage.setScene(scene);
        stage.setTitle("JavaDrop");
        stage.show();
        System.out.println("Dialog thread id: " + Thread.currentThread().getId());
    }
}
