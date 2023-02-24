package nl.pim16aap2.bigdoors.core.localization;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.api.IConfig;
import nl.pim16aap2.bigdoors.core.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.core.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.core.util.MathUtil;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Represents a manager for the localization system.
 *
 * @author Pim
 */
@Singleton
@Flogger
public final class LocalizationManager extends Restartable implements ILocalizationGenerator
{
    private final Path baseDir;
    private final String baseName;
    private final IConfig config;
    private final Localizer localizer;
    private final LocalizationGenerator baseGenerator;
    private @Nullable LocalizationGenerator patchGenerator;

    LocalizationManager(
        RestartableHolder restartableHolder, Path baseDir,
        String baseName, IConfig config, boolean deleteBundleOnStart)
    {
        super(restartableHolder);
        this.baseDir = baseDir;
        this.baseName = baseName;
        this.config = config;
        localizer = new Localizer(baseDir, baseName, deleteBundleOnStart);
        localizer.setDefaultLocale(config.locale());
        baseGenerator = new LocalizationGenerator(baseDir, baseName);
    }

    @Inject
    public LocalizationManager(
        RestartableHolder restartableHolder, @Named("localizationBaseDir") Path baseDir,
        @Named("localizationBaseName") String baseName, IConfig config)
    {
        this(restartableHolder, baseDir, baseName, config, true);
    }

    /**
     * Runs a method for the currently installed generators while taking care of restarting the {@link #localizer} when
     * needed.
     */
    private synchronized void runForGenerators(Consumer<ILocalizationGenerator> method, Supplier<String> desc)
    {
        try
        {
            // If the localization system is not patched, shut down the localizer before running the method for the
            // base generator so that the localizer sees the updated generated files. If there ARE patches, we don't
            // need to shut down the localizer just yet, because the localizer doesn't use the base localization files.
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
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to execute action : '%s'", desc.get());
        }
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
            final LocalizationPatcher localizationPatcher = new LocalizationPatcher(baseDir, baseName);
            localizationPatcher.updatePatchKeys(baseGenerator.getRootKeys());

            final List<LocaleFile> patchFiles = localizationPatcher.getPatchFiles();
            final Map<LocaleFile, Map<String, String>> patches =
                new HashMap<>(MathUtil.ceil(1.25 * patchFiles.size()));
            patchFiles.forEach(localeFile -> patches.put(localeFile, localizationPatcher.getPatches(localeFile)));

            final String patchedName = baseName + "_patched";

            final int patchCount = patches.values().stream().mapToInt(Map::size).sum();
            if (patchCount == 0)
            {
                Files.deleteIfExists(baseDir.resolve(patchedName + ".bundle"));
                return;
            }

            if (patchGenerator == null)
            {
                patchGenerator = new LocalizationGenerator(baseDir, patchedName);
                localizer.updateBundleLocation(baseDir, patchedName);
            }

            // Satisfy NullAway, as it doesn't realize that targetGenerator cannot be null here.
            final LocalizationGenerator targetGenerator = patchGenerator;
            patchGenerator.addResourcesFromZip(baseGenerator.getOutputFile(), baseName);
            patches.forEach((locale, patchMap) -> targetGenerator.applyPatches(locale.locale(), patchMap));
        }
        catch (IOException e)
        {
            log.atSevere().withCause(e).log("Failed to apply localization patches!");
        }
    }

    @Override
    public synchronized void addResources(Path path, @Nullable String baseName)
    {
        runForGenerators(generator -> generator.addResources(path, baseName),
                         () -> "Add resources from path: " + path + ", with baseName: " + baseName);
    }

    @Override
    public synchronized void addResources(List<Path> paths)
    {
        runForGenerators(generator -> generator.addResources(paths),
                         () -> "Add resources from paths: " + paths);
    }

    @Override
    public synchronized void addResourcesFromClass(Class<?> clz, @Nullable String baseName)
    {
        runForGenerators(generator -> generator.addResourcesFromClass(clz, baseName),
                         () -> "Add resources from class: " + clz.getName() + ", with baseName: " + baseName);
    }

    @Override
    public synchronized void addResourcesFromClass(List<Class<?>> classes)
    {
        runForGenerators(generator -> generator.addResourcesFromClass(classes),
                         () -> "Add resources from classes: " + classes.stream().map(Class::getName).toList());
    }

    @Override
    public synchronized void initialize()
    {
        localizer.setDefaultLocale(config.locale());
        applyPatches();
        runForGenerators(generator -> generator.addResourcesFromClass(List.of(LocalizationManager.class)),
                         () -> "Adding resources from LocalizationManager.class.");
        localizer.reInit();
    }

    @Override
    public synchronized void shutDown()
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
