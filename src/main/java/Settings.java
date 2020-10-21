import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.SystemUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.util.Map;

@Log4j2
public class Settings {
    // Configuration
    private static final String DIVIDER = SystemUtils.IS_OS_WINDOWS ? "\\" : "/";
    private static final String DOWNLOAD_DIR_KEY = "downloadPath";
    private static final String SETTINGS_DIR = System.getProperty("user.home") + DIVIDER + "JavaDrop";
    public static final String SETTINGS_FILE_PATH = SETTINGS_DIR + DIVIDER + "settings.yaml";
    private static final String DOWNLOADS_DIR = SETTINGS_DIR + DIVIDER + "Downloads";

    @VisibleForTesting
    protected static Map<String, String> settings;

    @Nullable
    @VisibleForTesting
    protected static File getSettingsFile(@Nonnull String path) {
        // Retrieve the YAML file, or create it in the default dir
        final File settingsFile = new File(path);
        FileOutputStream fileOut = null;
        try {
            if (settingsFile.createNewFile()) { // Will no-op if file already exists
                fileOut = new FileOutputStream(settingsFile);
                initializeSettingsFile(fileOut);
            }
        } catch (IOException e) {
            e.printStackTrace();
            closeStream(fileOut);
            return null;
        }
        return settingsFile;
    }

    private static void closeStream(@Nullable Closeable stream) {
        try {
            if (stream != null)
                stream.close();
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Error with closing stream");
        }
    }

    private static void initializeSettingsFile(@Nonnull OutputStream out) throws IOException {
        // Need to set YAML formatting options here, otherwise will include "{}"
        final DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        final String initialString = new Yaml(options).dump(
                ImmutableMap.of(DOWNLOAD_DIR_KEY, DOWNLOADS_DIR));
        JDLink.writeStringToRemote(out, initialString);
    }

    public static void loadSettings(@Nonnull String settingsPath) throws FileNotFoundException {
        final File settingsFile = getSettingsFile(settingsPath);
        if (settingsFile == null) {
            throw new FileNotFoundException("Could not find settings file");
        }

        final InputStream inputStream = new FileInputStream(settingsFile);
        settings = (Map<String, String>) new Yaml().load(inputStream);
    }

    @Nullable
    public static String getSetting(@Nonnull String key) {
        return settings.get(key);
    }

    @VisibleForTesting
    protected static void reset() {
        settings = null;
    }
}
