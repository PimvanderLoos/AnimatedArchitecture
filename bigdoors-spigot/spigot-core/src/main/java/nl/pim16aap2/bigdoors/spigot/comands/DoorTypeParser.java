package nl.pim16aap2.bigdoors.spigot.comands;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

public class DoorTypeParser implements ArgumentParser<ICommandSender, DoorType>, IRestartable
{
    private final DoorTypeManager doorTypeManager;
    private final ILocalizer localizer;
    private Map<String, DoorType> suggestions = Collections.emptyMap();

    @Inject DoorTypeParser(RestartableHolder restartableHolder, DoorTypeManager doorTypeManager, ILocalizer localizer)
    {
        restartableHolder.registerRestartable(this);
        this.doorTypeManager = doorTypeManager;
        this.localizer = localizer;
    }

    @Override
    public ArgumentParseResult<DoorType> parse(
        CommandContext<ICommandSender> commandContext, Queue<String> inputQueue)
    {
        final @Nullable String input = inputQueue.peek();
        final @Nullable DoorType doorType = input == null ? null : suggestions.get(input.toLowerCase(Locale.ROOT));

        if (doorType == null)
            return ArgumentParseResult.failure(
                new IllegalStateException("Failed to parse door type from input: '" + input + "'"));

        inputQueue.remove();
        return ArgumentParseResult.success(doorType);
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
        final Collection<DoorType> types = doorTypeManager.getEnabledDoorTypes();
        this.suggestions = new LinkedHashMap<>(2 * types.size());
        types.forEach(type ->
                      {
                          suggestions.put(type.getFullName(), type);
                          suggestions.put(localizer.getMessage(type.getLocalizationKey()), type);
                      });
    }

    @Override
    public synchronized void shutDown()
    {
        suggestions = Collections.emptyMap();
    }
}

