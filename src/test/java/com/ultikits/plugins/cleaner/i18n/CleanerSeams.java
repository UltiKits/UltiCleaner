package com.ultikits.plugins.cleaner.i18n;

import com.ultikits.plugins.cleaner.commands.CleanCommand;
import com.ultikits.plugins.cleaner.config.CleanerConfig;
import com.ultikits.plugins.cleaner.config.RemovedConfigKeys;
import com.ultikits.plugins.cleaner.service.CleanerService;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Test support: builds {@link CleanCommand} and calls {@link RemovedConfigKeys#warnAboutLeftovers}
 * through whichever signature the production code declares.
 * <p>
 * Routing their text through the language catalogue gives both a {@link UltiToolsPlugin} parameter.
 * The tests are written once and must compile and run against the code before and after that change
 * (a revert proof restores the old production files and runs these same tests), so they reach the
 * two signatures reflectively: the one taking the plugin when it exists, the old one otherwise.
 */
public final class CleanerSeams {

    private CleanerSeams() {
    }

    /** A {@link CleanCommand} built with the plugin when its constructor takes one. */
    public static CleanCommand command(CleanerService service, CleanerConfig config, UltiToolsPlugin plugin) {
        try {
            try {
                Constructor<CleanCommand> withPlugin =
                        CleanCommand.class.getConstructor(CleanerService.class, CleanerConfig.class, UltiToolsPlugin.class);
                return withPlugin.newInstance(service, config, plugin);
            } catch (NoSuchMethodException e) {
                return CleanCommand.class.getConstructor(CleanerService.class, CleanerConfig.class)
                        .newInstance(service, config);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot construct CleanCommand", e);
        }
    }

    /** {@link RemovedConfigKeys#warnAboutLeftovers}, given the plugin when the method takes one. */
    public static void warnAboutLeftovers(File configFile, Consumer<String> warn, UltiToolsPlugin plugin) {
        try {
            try {
                Method withPlugin = RemovedConfigKeys.class.getMethod("warnAboutLeftovers",
                        File.class, Consumer.class, UltiToolsPlugin.class);
                withPlugin.invoke(null, configFile, warn, plugin);
            } catch (NoSuchMethodException e) {
                RemovedConfigKeys.class.getMethod("warnAboutLeftovers", File.class, Consumer.class)
                        .invoke(null, configFile, warn);
            }
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalStateException(cause);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot call RemovedConfigKeys#warnAboutLeftovers", e);
        }
    }
}
