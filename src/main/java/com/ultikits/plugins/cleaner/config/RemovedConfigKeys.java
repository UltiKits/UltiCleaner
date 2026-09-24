package com.ultikits.plugins.cleaner.config;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Reports configuration keys this module no longer reads but which are still sitting in the
 * operator's own {@code config/cleaner.yml}.
 * <p>
 * Deleting a key from {@link CleanerConfig} stops the framework writing it into a fresh file, but
 * it does nothing to the files already on disk: the framework only ever writes a declared default
 * for a key that is <em>missing</em>, so an existing install keeps the key, keeps whatever value
 * the operator gave it, and gets no indication that the value stopped meaning anything. This class
 * is that indication -- one warning per leftover key, naming the module, the file and the key, and
 * saying where the setting went.
 *
 * @author wisdomme
 * @version 1.0.0
 */
public final class RemovedConfigKeys {

    /**
     * Every key removed from {@code config/cleaner.yml}, mapped to what an operator should be told
     * about it. Insertion order is the order the warnings are emitted in.
     */
    private static final Map<String, String> REMOVED;

    static {
        Map<String, String> removed = new LinkedHashMap<String, String>();
        removed.put("messages.prefix",
                "Nothing ever read this key, so no message was ever prefixed from it and deleting "
                        + "it changes nothing you see. The prefix in a cleanup broadcast is part of "
                        + "that broadcast's own text: edit the 'messages.*' key for the message you "
                        + "want to change (UltiKits/UltiCleaner#18).");
        String chunk = "Chunk unloading was removed: the server engine already unloads idle chunks "
                + "by itself, so this module's version could only ever reach the chunks the server "
                + "deliberately keeps resident. There is no replacement setting "
                + "(UltiKits/UltiCleaner#27).";
        removed.put("chunk.enabled", chunk);
        removed.put("chunk.max-distance", chunk);
        removed.put("chunk.batch-size", chunk);
        removed.put("chunk.timeout", chunk);
        REMOVED = Collections.unmodifiableMap(removed);
    }

    private RemovedConfigKeys() {
        // Utility class
    }

    /**
     * The keys this class knows about, in the order it reports them.
     *
     * @return an unmodifiable map of removed key path to the guidance printed for it
     */
    public static Map<String, String> removedKeys() {
        return REMOVED;
    }

    /**
     * Emit one warning per removed key that is still present in the operator's configuration file.
     * <p>
     * Silent when the file is absent or unreadable -- there is then nothing to report and nothing
     * to be sure of. A parse failure is deliberately not reported here: the framework's own config
     * loading already fails loudly on an unparseable file, and a second message from this check
     * would only add noise to it.
     *
     * @param configFile the operator's {@code config/cleaner.yml}; may be {@code null}
     * @param warn       where to send each warning, normally the module logger's warn method
     */
    public static void warnAboutLeftovers(File configFile, Consumer<String> warn) {
        if (configFile == null || !configFile.isFile()) {
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            return;
        }
        for (Map.Entry<String, String> entry : REMOVED.entrySet()) {
            if (yaml.contains(entry.getKey())) {
                warn.accept("[UltiCleaner] " + configFile.getPath() + " still contains '"
                        + entry.getKey() + "', which this version no longer reads. "
                        + entry.getValue()
                        + " Delete the key from the file to silence this warning.");
            }
        }
    }
}
