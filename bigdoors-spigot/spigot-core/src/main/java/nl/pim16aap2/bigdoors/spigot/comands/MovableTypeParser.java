package nl.pim16aap2.bigdoors.spigot.comands;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.MovableTypeManager;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

/**
 * Represents an argument parser that can be used to retrieve and suggest movable types.
 *
 * @author Pim
 */
public class MovableTypeParser implements ArgumentParser<ICommandSender, MovableType>, IRestartable
{
    private final MovableTypeManager movableTypeManager;
    private final ILocalizer localizer;
    private Map<String, MovableType> suggestions = Collections.emptyMap();

    @Inject MovableTypeParser(
        RestartableHolder restartableHolder, MovableTypeManager movableTypeManager, ILocalizer localizer)
    {
        restartableHolder.registerRestartable(this);
        this.movableTypeManager = movableTypeManager;
        this.localizer = localizer;
    }

    @Override
    public ArgumentParseResult<MovableType> parse(
        CommandContext<ICommandSender> commandContext, Queue<String> inputQueue)
    {
        final @Nullable String input = inputQueue.peek();
        final @Nullable MovableType movableTypeType =
            input == null ? null : suggestions.get(input.toLowerCase(Locale.ROOT));

        if (movableTypeType == null)
            return ArgumentParseResult.failure(
                new IllegalStateException("Failed to parse movable type from input: '" + input + "'"));

        inputQueue.remove();
        return ArgumentParseResult.success(movableTypeType);
    }

    @Override
    public List<String> suggestions(CommandContext<ICommandSender> commandContext, String input)
    {
        return suggestions.keySet().stream().filter(val -> startsWith(input, val)).toList();
    }

    /**
     * @param base
     *     The base String to use.
     * @param test
     *     The String to compare against the base String.
     * @return True if the test String has the base string as its base.
     */
    static boolean startsWith(String base, String test)
    {
        return test.toLowerCase(Locale.ROOT).startsWith(base.toLowerCase(Locale.ROOT));
    }

    @Override
    public synchronized void initialize()
    {
        final Collection<MovableType> types = movableTypeManager.getEnabledMovableTypes();
        this.suggestions = new LinkedHashMap<>(types.size());
        types.forEach(type -> suggestions.put(format(type), type));
    }

    private String format(MovableType type)
    {
        return localizer.getMessage(type.getLocalizationKey()).toLowerCase(Locale.ENGLISH).replaceAll("\\s+", "");
    }

    @Override
    public synchronized void shutDown()
    {
        suggestions = Collections.emptyMap();
    }
}

