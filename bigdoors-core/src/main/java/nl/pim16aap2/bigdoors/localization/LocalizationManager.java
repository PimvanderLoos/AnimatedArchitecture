package nl.pim16aap2.bigdoors.localization;

import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.restartable.IRestartableHolder;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Represents a manager for the localization system.
 *
 * @author Pim
 */
public final class LocalizationManager extends Restartable implements ILocalizationGenerator
{
    private final @NotNull Path baseDir;
    private final @NotNull String baseName;
    private final @NotNull IConfigLoader configLoader;
    private final long buildID;
    private @NotNull Localizer localizer;
    private final @NotNull LocalizationGenerator baseGenerator;
    private @Nullable LocalizationGenerator patchGenerator;

    public LocalizationManager(@NotNull IRestartableHolder restartableHolder, @NotNull Path baseDir,
                               @NotNull String baseName, @NotNull IConfigLoader configLoader, long buildID)
    {
        super(restartableHolder);
        this.baseDir = baseDir;
        this.baseName = baseName;
        this.configLoader = configLoader;
        this.buildID = buildID;
        localizer = new Localizer(baseDir, baseName);
        localizer.setDefaultLocale(configLoader.locale());
        baseGenerator = new LocalizationGenerator(baseDir, baseName);
    }

    /**
     * Runs a method for the currently installed generators while taking care of restarting the {@link #localizer} when
     * needed.
     */
    private synchronized void runForGenerators(@NotNull Consumer<ILocalizationGenerator> method)
    {
        final Set<String> rootKeys;
        // If the localization system is not patched, shut down the localizer before running the method for the
        // base generator so that the localizer sees the updated generated files. If there ARE patches, we don't need
        // to shut down the localizer just yet, because the localizer doesn't use the base localization files.
        if (patchGenerator == null)
        {
            localizer.shutdown();
            method.accept(baseGenerator);
            rootKeys = baseGenerator.getOutputRootKeys();
        }
        else
        {
            method.accept(baseGenerator);
            localizer.shutdown();
            method.accept(patchGenerator);
            rootKeys = patchGenerator.getOutputRootKeys();
        }

        try
        {
            applyPatches(rootKeys);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        localizer.init();
    }

    synchronized void applyPatches(@NotNull Set<String> rootKeys)
        throws IOException
    {
        final LocalizationPatcher localizationPatcher = new LocalizationPatcher(baseDir, baseName);
        localizationPatcher.updatePatchKeys(rootKeys);
    }

    @Override
    public synchronized void addResources(@NotNull Path path, @Nullable String baseName)
    {
        runForGenerators(generator -> generator.addResources(path, baseName));
    }

    @Override
    public synchronized void addResources(@NotNull List<Path> paths)
    {
        runForGenerators(generator -> generator.addResources(paths));
    }

    @Override
    public synchronized void addResourcesFromClass(@NotNull Class<?> clz, @Nullable String baseName)
    {
        runForGenerators(generator -> generator.addResourcesFromClass(clz, baseName));
    }

    @Override
    public synchronized void addResourcesFromClass(@NotNull List<Class<?>> classes)
    {
        runForGenerators(generator -> generator.addResourcesFromClass(classes));
    }

    @Override
    public synchronized void restart()
    {
        shutdown();
        // TODO: Implement
        localizer.setDefaultLocale(configLoader.locale());
    }

    @Override
    public synchronized void shutdown()
    {
        // TODO: Implement
    }

    public @NotNull ILocalizer getLocalizer()
    {
        return localizer;
    }
}
