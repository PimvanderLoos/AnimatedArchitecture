package nl.pim16aap2.animatedarchitecture.spigot.core.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.config.IConfigSectionResult;
import nl.pim16aap2.animatedarchitecture.core.config.LocaleSection;
import nl.pim16aap2.animatedarchitecture.core.localization.LocalizationUtil;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Represents a section in the configuration file that governs locale settings for Spigot.
 */
@AllArgsConstructor
public class LocaleSectionSpigot extends LocaleSection<LocaleSectionSpigot.Result>
{
    @Getter
    private final @Nullable Consumer<Result> resultConsumer;

    @Override
    protected Result getResult(ConfigurationNode sectionNode)
    {
        return new Result(
            getLocale(sectionNode),
            getAllowClientLocale(sectionNode)
        );
    }

    private Locale getLocale(ConfigurationNode sectionNode)
    {
        final String localeStr = sectionNode.node(PATH_LOCALE).getString(DEFAULT_LOCALE.toString());
        return Objects.requireNonNullElse(LocalizationUtil.parseLocale(localeStr), DEFAULT_LOCALE);
    }

    private boolean getAllowClientLocale(ConfigurationNode sectionNode)
    {
        return sectionNode.node(PATH_ALLOW_CLIENT_LOCALE).getBoolean(DEFAULT_ALLOW_CLIENT_LOCALE);
    }

    /**
     * Represents the result of the locale configuration section.
     *
     * @param locale
     *     the locale to be used.
     * @param allowClientLocale
     *     Whether to allow the client to specify its locale.
     */
    public record Result(
        Locale locale,
        boolean allowClientLocale
    ) implements IConfigSectionResult {}
}
