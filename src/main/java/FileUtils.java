import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class FileUtils {

    @Nonnull
    public static File createUniqueFile(@Nonnull final String filename,
                                        @Nonnull final String directory) throws IOException {
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
    public static String createIncrementedFilename(@Nonnull final String originalName,
                                                   @Nonnull final Set<String> existingNames) {
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
    public static String[] getExistingFilenames(@Nonnull final String directory) throws IOException {
        final String[] existingFilenames = new File(directory).list();
        if (existingFilenames == null)
            throw new IOException("Specified save directory could not be located");
        return existingFilenames;
    }
}
