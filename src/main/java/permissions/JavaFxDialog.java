package permissions;

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

@Log4j2
public class JavaFxDialog extends Application {
    // Configuration
    private static final int BUTTON_WIDTH = 60;
    private static final int DIALOG_WIDTH = 300;
    private static final int DIALOG_HEIGHT = 125;
    private static final int BUTTON_SPACING = 30;
    private static final int LAYOUT_SPACING = 10;
    private static final String ACCEPT_BUTTON_TEXT = "Yes";
    private static final String REJECT_BUTTON_TEXT = "No";
    private static final String TITLE = "JavaDrop";

    private final String filename, source;
    private final long filesize;
    private final Callback callback;

    interface Callback {
        void success();

        void failure();
    }

    public JavaFxDialog(@Nonnull final String filename,
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
        boolean permission = d.requestPermission("test.png", "192.12.31.1", 123123, new Permissions.Requester() {
            @Override
            public void onPermissionGranted() {
                System.out.println("GRANTED");
                System.out.println("Permission granted on thread id: " + Thread.currentThread().getId());
            }

            @Override
            public void onPermissionRejected() {
                System.out.println("REJECTED");
                System.out.println("Permission rejected on thread id: " + Thread.currentThread().getId());
            }
        });
        log.debug("Is permission granted: " + permission);
    }

    @Override
    public void start(Stage stage) {
        final Label topLabel = new Label("Accept file transfer of file: " + filename + " (" + (filesize / 1024) + " KB)");
        final Label bottomLabel = new Label("From " + source + "?");
        final Button reject = new Button(REJECT_BUTTON_TEXT);
        reject.setMinWidth(BUTTON_WIDTH);
        reject.setMaxWidth(BUTTON_WIDTH);
        reject.setOnAction(e -> {
            callback.failure();
            Platform.exit();
        });

        final Button accept = new Button(ACCEPT_BUTTON_TEXT);
        accept.setMinWidth(BUTTON_WIDTH);
        accept.setMaxWidth(BUTTON_WIDTH);
        accept.setOnAction(e -> {
            callback.success();
            Platform.exit();
        });

        final HBox buttonLayout = new HBox();
        buttonLayout.getChildren().addAll(accept, reject);
        buttonLayout.setAlignment(Pos.CENTER);
        buttonLayout.setSpacing(BUTTON_SPACING);

        final VBox layout = new VBox();
        layout.getChildren().addAll(topLabel, bottomLabel, buttonLayout);
        layout.setAlignment(Pos.CENTER);
        layout.setSpacing(LAYOUT_SPACING);

        final Scene scene = new Scene(layout, DIALOG_WIDTH, DIALOG_HEIGHT);
        stage.setScene(scene);
        stage.setTitle(TITLE);
        stage.show();
        System.out.println("Dialog thread id: " + Thread.currentThread().getId());
    }
}
