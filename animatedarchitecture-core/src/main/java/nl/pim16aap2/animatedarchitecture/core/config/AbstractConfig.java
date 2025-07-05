package nl.pim16aap2.animatedarchitecture.core.config;

import dagger.Lazy;
import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.IProtectionHookManager;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The base configuration class for Animated Architecture.
 * <p>
 * All shared configuration settings should be defined here. Subclasses can add platform-/version-specific settings.
 */
@ToString
@EqualsAndHashCode
@CustomLog
public abstract class AbstractConfig implements IConfig
{
    private final Path configPath;

    @ToString.Exclude
    private final Lazy<StructureTypeManager> lazyStructureTypeManager;

    @ToString.Exclude
    private final Lazy<IProtectionHookManager> lazyProtectionHookManager;

    private final YamlConfigurationLoader configLoader;

    private final List<IConfigSection> sections = new CopyOnWriteArrayList<>();

    protected AbstractConfig(
        @Named("pluginBaseDirectory") Path baseDir,
        Lazy<StructureTypeManager> structureTypeManager,
        Lazy<IProtectionHookManager> protectionHookManager
    )
    {
        this.configPath = baseDir.resolve("config.yaml");
        this.lazyStructureTypeManager = structureTypeManager;
        this.lazyProtectionHookManager = protectionHookManager;
        this.configLoader = initConfig();
    }

    /**
     * Adds one or more configuration sections to the configuration.
     *
     * @param sections
     *     The configuration sections to add.
     */
    protected void addSections(IConfigSection... sections)
    {
        this.sections.addAll(List.of(sections));
    }

    private YamlConfigurationLoader initConfig()
    {
        return YamlConfigurationLoader.builder()
            .path(configPath)
            .build();
    }

    private ConfigurationTransformation getInitialTransformations(CommentedConfigurationNode root)
    {
        final var rootPath = root.path();
        final var builder = ConfigurationTransformation.builder();
        sections.forEach(section -> builder.addAction(rootPath, section.getInitialTransform()));
        return builder.build();
    }

    /**
     * Adds additional transformations to the configuration.
     * <p>
     * This method can be overridden by subclasses to add their own transformations.
     *
     * @param builder
     *     The builder to add transformations to.
     */
    protected void addTransformations(ConfigurationTransformation.VersionedBuilder builder)
    {
    }

    private ConfigurationTransformation.Versioned createVersionedTransformation(CommentedConfigurationNode root)
    {
        final var builder = ConfigurationTransformation.versionedBuilder()
            .addVersion(0, getInitialTransformations(root));

        addTransformations(builder);

        return builder.build();
    }

    private CommentedConfigurationNode applyTransformations(CommentedConfigurationNode root)
        throws ConfigurateException
    {
        if (root.virtual())
            return root;

        final var transformations = createVersionedTransformation(root);
        final int startVersion = transformations.version(root);
        transformations.apply(root);
        final int endVersion = transformations.version(root);

        if (startVersion != endVersion)
            log.atInfo().log("Updated config schema from %d to %d", startVersion, endVersion);

        return root;
    }

    private void printConfig()
        throws IOException
    {
        final StringBuilder sb = new StringBuilder();
        Files.lines(configPath).forEach(line -> sb.append(line).append(System.lineSeparator()));
        log.atInfo().log(
            "Configuration file contents:\n--------------------------%s\n--------------------------",
            sb.toString()
        );
    }

    /**
     * Parses the configuration file and applies any necessary transformations.
     * <p>
     * If the configuration file does not exist, it will be created with default values.
     */
    protected final void parseConfig()
    {
        try
        {
            Files.deleteIfExists(configPath);
            final var root = configLoader.load();
            final var result = applyTransformations(root);
            configLoader.save(result);
            printConfig();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to parse configuration file: " + configPath, e);
        }
        Runtime.getRuntime().halt(0);
    }
}
