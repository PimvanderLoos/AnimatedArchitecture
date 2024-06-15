package nl.pim16aap2.animatedarchitecture.spigot.core.comands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.function.BiFunction;

/**
 * An argument that parses a {@link PermissionLevel} from a string.
 */
public class PermissionLevelArgument extends CommandArgument<ICommandSender, PermissionLevel>
{
    /**
     * @param minimumLevel
     *     The minimum permission level (inclusive). Any permission levels lower than this are ignored. When null, no
     *     minimum is used.
     * @param maximumLevel
     *     The maximum permission level (inclusive). Any permission levels higher than this are ignored. When null, no
     *     maximum is used.
     */
    @lombok.Builder
    public PermissionLevelArgument(
        boolean required,
        String name,
        @Nullable String defaultValue,
        @Nullable BiFunction<CommandContext<ICommandSender>, String, List<String>> suggestionsProvider,
        @Nullable ArgumentDescription defaultDescription,
        ILocalizer localizer,
        @Nullable PermissionLevel minimumLevel,
        @Nullable PermissionLevel maximumLevel)
    {
        super(
            required,
            name,
            new PermissionLevelArgumentParser(localizer, minimumLevel, maximumLevel),
            Objects.requireNonNullElse(defaultValue, ""),
            PermissionLevel.class,
            suggestionsProvider,
            Objects.requireNonNullElse(defaultDescription, ArgumentDescription.empty())
        );
    }

    public static final class PermissionLevelArgumentParser implements ArgumentParser<ICommandSender, PermissionLevel>
    {
        private final Map<String, PermissionLevel> suggestions =
            new LinkedHashMap<>(MathUtil.ceil(1.25 * PermissionLevel.getValues().size()));
        private final Map<PermissionLevel, String> invertedSuggestions = new EnumMap<>(PermissionLevel.class);
        private final ILocalizer localizer;

        public PermissionLevelArgumentParser(
            ILocalizer localizer,
            @Nullable PermissionLevel minimumLevel,
            @Nullable PermissionLevel maximumLevel)
        {
            this.localizer = localizer;
            fillSuggestions(minimumLevel, maximumLevel);
        }

        private void fillSuggestions(@Nullable PermissionLevel minimumLevel, @Nullable PermissionLevel maximumLevel)
        {
            for (final PermissionLevel level : PermissionLevel.getValues())
            {
                if (minimumLevel != null && level.isLowerThan(minimumLevel))
                    continue;
                if (maximumLevel != null && maximumLevel.isLowerThan(level))
                    continue;
                final String name = level.name().toLowerCase(Locale.ROOT);
                final String localized = localizer.getMessage("constants.permission_level." + name);

                suggestions.put(localized.toLowerCase(Locale.ROOT), level);
                invertedSuggestions.put(level, localized);
            }
        }

        @Override
        public ArgumentParseResult<PermissionLevel> parse(
            CommandContext<ICommandSender> commandContext,
            Queue<String> inputQueue)
        {
            final @Nullable String input = inputQueue.peek();
            final @Nullable PermissionLevel level =
                input == null ? null : suggestions.get(input.toLowerCase(Locale.ROOT));

            if (level == null)
                return ArgumentParseResult.failure(
                    new IllegalStateException("Failed to parse permission level from input: '" + input + "'"));

            inputQueue.remove();
            return ArgumentParseResult.success(level);
        }

        @Override
        public List<String> suggestions(CommandContext<ICommandSender> commandContext, String input)
        {
            final String lowerCaseInput = input.toLowerCase(Locale.ROOT);
            return invertedSuggestions.values().stream().filter(val -> val.startsWith(lowerCaseInput)).toList();
        }
    }
}
