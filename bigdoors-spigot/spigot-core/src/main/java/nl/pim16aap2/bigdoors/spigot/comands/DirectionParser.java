package nl.pim16aap2.bigdoors.spigot.comands;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.MovementDirection;
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
 * Represents an argument parser that can be used to retrieve and suggest movable types.
 *
 * @author Pim
 */
public class DirectionParser implements ArgumentParser<ICommandSender, MovementDirection>, IRestartable
{
    private final ToolUserManager toolUserManager;
    private final ILocalizer localizer;
    private final Map<String, MovementDirection> suggestions = new LinkedHashMap<>(MovementDirection.values().length);
    private final Map<MovementDirection, String> invertedSuggestions = new EnumMap<>(MovementDirection.class);

    @Inject DirectionParser(RestartableHolder restartableHolder, ToolUserManager toolUserManager, ILocalizer localizer)
    {
        restartableHolder.registerRestartable(this);
        this.toolUserManager = toolUserManager;
        this.localizer = localizer;
    }

    private void fillSuggestions()
    {
        for (final MovementDirection movementDirection : MovementDirection.values())
        {
            final String name = movementDirection.name().toLowerCase(Locale.ROOT);
            final String localized = localizer.getMessage("constants.rotate_direction." + name);
            suggestions.put(localized.toLowerCase(Locale.ROOT), movementDirection);
            invertedSuggestions.put(movementDirection, localized);
        }
    }

    @Override
    public ArgumentParseResult<MovementDirection> parse(
        CommandContext<ICommandSender> commandContext, Queue<String> inputQueue)
    {
        final @Nullable String input = inputQueue.peek();
        final @Nullable MovementDirection direction =
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
        final String lowerCaseInput = input.toLowerCase(Locale.ROOT);
        return suggestionsStream.filter(val -> val.startsWith(lowerCaseInput)).toList();
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

    @Override
    public void initialize()
    {
        fillSuggestions();
    }

    @Override
    public void shutDown()
    {
        suggestions.clear();
        invertedSuggestions.clear();
    }
}

