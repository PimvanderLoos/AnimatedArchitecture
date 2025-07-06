package nl.pim16aap2.animatedarchitecture.core.config;

import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import dagger.Lazy;
import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
    private static final String PATH_VERSION = "version";
    private static final int INITIAL_VERSION = 0;

    private final Path configPath;

    private final YamlConfigurationLoader configLoader;

    @GuardedBy("this")
    private final List<IConfigSection> sections = new ArrayList<>();

    protected AbstractConfig(
        @Named("pluginBaseDirectory") Path baseDir
    )
    {
        this.configPath = baseDir.resolve("config.yaml");
        this.configLoader = initConfig();
    }

    /**
     * Parses the configuration file and applies any necessary transformations.
     * <p>
     * If the configuration file does not exist, it will be created with default values.
     */
    @CheckReturnValue
    protected synchronized final void parseConfig()
    {
        try
        {
            Files.deleteIfExists(configPath);
            final var root = configLoader.load();
            final var result = applyTransformations(root);
            populateDynamicData(result);
            configLoader.save(result);
            printConfig();
            applyResults(root);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to parse configuration file: " + configPath, e);
        }
    }

    /**
     * Applies the results of the config.
     *
     * @param root
     *     The root node to read the results from.
     */
    @GuardedBy("this")
    private void applyResults(CommentedConfigurationNode root)
    {
        this.sections.forEach(section ->
        {
            try
            {
                section.applyResults(root);
            }
            catch (SerializationException exception)
            {
                throw new RuntimeException(
                    "Failed to apply results for section: %s".formatted(section.getSectionTitle()),
                    exception
                );
            }
        });
    }

    /**
     * Gets the header for the configuration file.
     *
     * @return The header text for the configuration file.
     */
    protected String getHeader()
    {
        return """
            Animated Architecture configuration file.
            
            For most options, you can apply your changes using "/animatedarchitecture restart".
            When an option requires a restart, it will be mentioned in the description.
            
            For more information, visit: https://github.com/PimvanderLoos/AnimatedArchitecture
            """;
    }

    /**
     * Adds one or more configuration sections to the configuration.
     *
     * @param sections
     *     The configuration sections to add.
     */
    protected synchronized void addSections(IConfigSection... sections)
    {
        this.sections.addAll(List.of(sections));
    }

    /**
     * Gets the list of configuration sections.
     *
     * @return A list of configuration sections.
     */
    protected synchronized List<IConfigSection> getSections()
    {
        return List.copyOf(this.sections);
    }

    @GuardedBy("this")
    private void populateDynamicData(CommentedConfigurationNode root)
    {
        sections.forEach(section -> section.populateDynamicData(root));
    }

    private YamlConfigurationLoader initConfig()
    {
        return YamlConfigurationLoader.builder()
            .path(configPath)
            .indent(2)
            .nodeStyle(NodeStyle.BLOCK)
            .commentsEnabled(true)
            .defaultOptions(ConfigurationOptions.defaults()
                .header(getHeader()))
            .build();
    }

    @GuardedBy("this")
    private ConfigurationTransformation getInitialTransformations(CommentedConfigurationNode root)
    {
        final var rootPath = root.path();
        final var builder = ConfigurationTransformation.builder();
        final List<IConfigSection> configSections = getSections();

        builder.addAction(rootPath, (path, value) ->
        {
            var newNode = CommentedConfigurationNode.root()
                .act(node ->
                {
                    configSections.forEach(section ->
                    {
                        try
                        {
                            node.node(section.getSectionTitle()).set(section.buildInitialLimitsNode());
                        }
                        catch (Exception exception)
                        {
                            throw new RuntimeException(
                                String.format(
                                    "failed to build initial limits node for section: %s",
                                    section.getSectionTitle()),
                                exception
                            );
                        }
                    });

                    node.node(PATH_VERSION)
                        .comment("""
                            The version of the configuration file.
                            
                            DO NOT EDIT THIS MANUALLY!
                            """)
                        .set(INITIAL_VERSION);
                });
            value.set(newNode);
            return null;
        });
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
    protected void addTransformations(@SuppressWarnings("unused") ConfigurationTransformation.VersionedBuilder builder)
    {
    }

    private ConfigurationTransformation.Versioned createVersionedTransformation(CommentedConfigurationNode root)
    {
        final var builder = ConfigurationTransformation.versionedBuilder()
            .versionKey(PATH_VERSION)
            .addVersion(INITIAL_VERSION, getInitialTransformations(root));

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
            "Configuration file contents:\n--------------------------\n%s\n--------------------------",
            sb.toString()
        );
    }
}
