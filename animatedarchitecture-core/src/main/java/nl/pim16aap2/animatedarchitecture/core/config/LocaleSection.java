package nl.pim16aap2.animatedarchitecture.core.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Locale;

/**
 * Represents a configuration section for locale settings in Animated Architecture.
 * <p>
 * This section allows the user to configure the locale used by the plugin and whether clients are allowed to use their
 * own locale settings.
 *
 * @param <T>
 *     the type of result this section produces.
 */
public abstract class LocaleSection<T extends IConfigSectionResult> extends ConfigSection<T>
{
    public static final String SECTION_TITLE = "locale";

    public static final String PATH_LOCALE = "locale";
    public static final String PATH_ALLOW_CLIENT_LOCALE = "allow_client_locale";

    public static final Locale DEFAULT_LOCALE = Locale.US;
    public static final boolean DEFAULT_ALLOW_CLIENT_LOCALE = true;

    @Override
    public CommentedConfigurationNode buildInitialLimitsNode()
        throws SerializationException
    {
        return CommentedConfigurationNode.root()
            .comment("Language and locale settings for Animated Architecture.")
            .act(node ->
            {
                addInitialLocale(node.node(PATH_LOCALE));
                addInitialAllowClientLocale(node.node(PATH_ALLOW_CLIENT_LOCALE));
            });
    }

    private void addInitialLocale(CommentedConfigurationNode node)
        throws SerializationException
    {
        node.set(DEFAULT_LOCALE.toString())
            .comment("""
                Determines which locale to use. Defaults to root.
                
                A list of supported locales can be found here:
                https://hosted.weblate.org/projects/AnimatedArchitecture/
                
                For example, to use the Dutch locale, you would set this to 'nl_NL'.
                
                Any strings that are not translated for the chosen locale will default to the root locale (English).
                """);
    }

    private void addInitialAllowClientLocale(CommentedConfigurationNode node)
        throws SerializationException
    {
        node.set(DEFAULT_ALLOW_CLIENT_LOCALE)
            .comment("""
                When enabled, the plugin will allow players to use a different locale than the server.
                
                This is useful for servers with players from different countries.
                """);
    }

    @Override
    public String getSectionTitle()
    {
        return SECTION_TITLE;
    }
}
