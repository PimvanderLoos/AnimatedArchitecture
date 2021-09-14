package nl.pim16aap2.bigdoors.localization;

import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Represents a manager for the localization system.
 *
 * @author Pim
 */
@Singleton
public final class LocalizationManager extends Restartable implements ILocalizationGenerator
{
    private final Path baseDir;
    private final String baseName;
    private final IConfigLoader configLoader;
    private final IPLogger logger;
    private final Localizer localizer;
    private final LocalizationGenerator baseGenerator;
    private final DoorTypeManager doorTypeManager;
    private @Nullable LocalizationGenerator patchGenerator;

    @Inject
    public LocalizationManager(RestartableHolder restartableHolder, @Named("localizationBaseDir") Path baseDir,
                               @Named("localizationBaseName") String baseName, IConfigLoader configLoader,
                               IPLogger logger, DoorTypeManager doorTypeManager)
    {
        super(restartableHolder);
        this.baseDir = baseDir;
        this.baseName = baseName;
        this.configLoader = configLoader;
        this.logger = logger;
        localizer = new Localizer(baseDir, baseName, logger);
        localizer.setDefaultLocale(configLoader.locale());
        baseGenerator = new LocalizationGenerator(baseDir, baseName, logger);
        this.doorTypeManager = doorTypeManager;
    }

    private void init()
    {
        final List<Class<?>> types = doorTypeManager.getEnabledDoorTypes().stream()
                                                    .map(DoorType::getDoorClass)
                                                    .collect(Collectors.toList());
        addResourcesFromClass(types);
        addResourcesFromClass(List.of(LocalizationManager.class));
    }

    /**
     * Runs a method for the currently installed generators while taking care of restarting the {@link #localizer} when
     * needed.
     */
    private synchronized void runForGenerators(Consumer<ILocalizationGenerator> method)
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
     * @throws IOException
     *     When an I/O error occurred.
     */
    synchronized void applyPatches()
    {
        try
        {
            final LocalizationPatcher localizationPatcher = new LocalizationPatcher(baseDir, baseName, logger);
            final Set<String> rootKeys = (patchGenerator == null ? baseGenerator : patchGenerator).getOutputRootKeys();
            localizationPatcher.updatePatchKeys(rootKeys);

            final List<LocaleFile> patchFiles = localizationPatcher.getPatchFiles();
            final Map<LocaleFile, Map<String, String>> patches = new HashMap<>(patchFiles.size());
            patchFiles.forEach(localeFile -> patches.put(localeFile, localizationPatcher.getPatches(localeFile)));

            final int patchCount = patches.values().stream().mapToInt(Map::size).sum();
            if (patchCount == 0)
                return;

            if (patchGenerator == null)
            {
                patchGenerator = new LocalizationGenerator(baseDir, baseName + "_patched", logger);
                localizer.updateBundleLocation(baseDir, baseName + "_patched");
            }

            // Satisfy NullAway, as it doesn't realize that targetGenerator cannot be null here.
            final LocalizationGenerator targetGenerator = patchGenerator;
            patchGenerator.addResourcesFromZip(baseGenerator.getOutputFile(), baseName);
            patches.forEach((locale, patchMap) -> targetGenerator.applyPatches(locale.locale(), patchMap));
        }
        catch (IOException e)
        {
            logger.logThrowable(e, "Failed to apply localization patches!");
        }
    }

    @Override
    public synchronized void addResources(Path path, @Nullable String baseName)
    {
        runForGenerators(generator -> generator.addResources(path, baseName));
    }

    @Override
    public synchronized void addResources(List<Path> paths)
    {
        runForGenerators(generator -> generator.addResources(paths));
    }

    @Override
    public synchronized void addResourcesFromClass(Class<?> clz, @Nullable String baseName)
    {
        runForGenerators(generator -> generator.addResourcesFromClass(clz, baseName));
    }

    @Override
    public synchronized void addResourcesFromClass(List<Class<?>> classes)
    {
        runForGenerators(generator -> generator.addResourcesFromClass(classes));
    }

    @Override
    public synchronized void restart()
    {
        shutdown();
        localizer.setDefaultLocale(configLoader.locale());
        init();
        applyPatches();
        localizer.reInit();
    }

    @Override
    public synchronized void shutdown()
    {
        localizer.shutdown();
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
    public ILocalizer getLocalizer()
    {
        return localizer;
    }
}
