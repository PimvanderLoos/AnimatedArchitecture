package nl.pim16aap2.bigdoors.spigot.comands;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Represents an argument parser that can be used to retrieve and suggest door types.
 *
 * @author Pim
 */
public class DirectionParser implements ArgumentParser<ICommandSender, RotateDirection>
{
    private final ToolUserManager toolUserManager;
    private final ILocalizer localizer;
    private final Map<String, RotateDirection> suggestions = new LinkedHashMap<>(RotateDirection.values().length);
    private final Map<RotateDirection, String> invertedSuggestions = new EnumMap<>(RotateDirection.class);

    @Inject DirectionParser(ToolUserManager toolUserManager, ILocalizer localizer)
    {
        this.toolUserManager = toolUserManager;
        this.localizer = localizer;
        fillSuggestions();
    }

    private void fillSuggestions()
    {
        for (final RotateDirection rotateDirection : RotateDirection.values())
        {
            final String name = rotateDirection.name().toLowerCase(Locale.ROOT);
            final String localized = localizer.getMessage("constants.rotate_direction." + name);
            suggestions.put(localized.toLowerCase(Locale.ROOT), rotateDirection);
            invertedSuggestions.put(rotateDirection, localized);
        }
    }

    @Override
    public ArgumentParseResult<RotateDirection> parse(
        CommandContext<ICommandSender> commandContext, Queue<String> inputQueue)
    {
        final @Nullable String input = inputQueue.peek();
        final @Nullable RotateDirection direction =
            input == null ? null : suggestions.get(input.toLowerCase(Locale.ROOT));

        if (direction == null)
            return ArgumentParseResult.failure(
                new IllegalStateException("Failed to parse rotation direction from input: '" + input + "'"));

        inputQueue.remove();
        return ArgumentParseResult.success(direction);
    }

    @Override
    public List<String> suggestions(CommandContext<ICommandSender> commandContext, String input)
    {
        final Stream<String> suggestionsStream =
            Objects.requireNonNullElseGet(tryRetrieveGuidedSuggestions(commandContext),
                                          () -> invertedSuggestions.values().stream());
        return suggestionsStream.filter(val -> val.startsWith(input.toLowerCase(Locale.ROOT))).toList();
    }

    private @Nullable Stream<String> tryRetrieveGuidedSuggestions(
        CommandContext<ICommandSender> commandContext)
    {
        final @Nullable UUID uuid = commandContext.getSender().getPlayer().map(IPPlayer::getUUID).orElse(null);
        if (uuid == null)
            return null;

        final @Nullable ToolUser toolUser = toolUserManager.getToolUser(uuid).orElse(null);
        if (!(toolUser instanceof Creator creator))
            return null;

        return creator.getValidOpenDirections().stream().map(invertedSuggestions::get);
    }
}

