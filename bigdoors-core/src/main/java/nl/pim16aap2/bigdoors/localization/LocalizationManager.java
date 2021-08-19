package nl.pim16aap2.bigdoors.localization;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.restartable.IRestartableHolder;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        // If the localization system is not patched, shut down the localizer before running the method for the
        // base generator so that the localizer sees the updated generated files. If there ARE patches, we don't need
        // to shut down the localizer just yet, because the localizer doesn't use the base localization files.
        if (patchGenerator == null)
        {
            localizer.shutdown();
            method.accept(baseGenerator);
        }
        else
        {
            method.accept(baseGenerator);
            localizer.shutdown();
            method.accept(patchGenerator);
        }

        applyPatches();

        localizer.init();
    }

    /**
     * Applies the user-defined patches to the {@link #patchGenerator}. If the {@link #patchGenerator} does not exist
     * yet, a new one will be created.
     *
     * @throws IOException When an I/O error occurred.
     */
    synchronized void applyPatches()
    {
        try
        {
            final LocalizationPatcher localizationPatcher = new LocalizationPatcher(baseDir, baseName);
            final Set<String> rootKeys = (patchGenerator == null ? baseGenerator : patchGenerator).getOutputRootKeys();
            final List<LocaleFile> patchFiles = localizationPatcher.updatePatchKeys(rootKeys);

            final Map<LocaleFile, Map<String, String>> patches = new HashMap<>(patchFiles.size());
            patchFiles.forEach(localeFile -> patches.put(localeFile, localizationPatcher.getPatches(localeFile)));

            final int patchCount = patches.values().stream().mapToInt(Map::size).sum();
            if (patchCount == 0)
                return;

            if (patchGenerator == null)
            {
                patchGenerator = new LocalizationGenerator(baseDir, baseName + "_patched");
                // TODO: Point Localizer to new files.
            }

            // Satisfy NullAway, as it doesn't realize that targetGenerator cannot be null here.
            @NotNull LocalizationGenerator targetGenerator = patchGenerator;
            patchGenerator.addResourcesFromZip(baseGenerator.getOutputFile(), baseName);
            patches.forEach((locale, patchMap) -> targetGenerator.applyPatches(locale.locale(), patchMap));
        }
        catch (IOException e)
        {
            BigDoors.get().getPLogger().logThrowable(e, "Failed to apply localization patches!");
        }
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
        localizer.setDefaultLocale(configLoader.locale());
        applyPatches();
        localizer.reInit();
    }

    @Override
    public synchronized void shutdown()
    {
        localizer.shutdown();
        // TODO: Implement
    }

    /**
     * Checks if the localization system loaded any user-defined patches.
     *
     * @return True if the localization system was patched with user-defined localization values.
     */
    synchronized boolean isPatched()
    {
        return patchGenerator != null;
    }

    /**
     * Gets the {@link ILocalizer} managed by this {@link LocalizationManager}.
     *
     * @return The ILocalizer managed by this LocalizationManager.
     */
    public @NotNull ILocalizer getLocalizer()
    {
        return localizer;
    }
}
