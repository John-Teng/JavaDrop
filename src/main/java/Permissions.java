import javax.annotation.Nonnull;

public class Permissions {
    interface Arbiter {
        void requestPermission(@Nonnull final String filename,
                               @Nonnull final String source,
                               final long filesize,
                               @Nonnull final Requester requester);
    }

    interface Requester {
        void onPermissionGranted();

        void onPermissionRejected();
    }
}
