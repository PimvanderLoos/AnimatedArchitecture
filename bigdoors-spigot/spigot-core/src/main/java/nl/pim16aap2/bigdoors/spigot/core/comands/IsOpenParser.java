package nl.pim16aap2.bigdoors.spigot.core.comands;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import nl.pim16aap2.bigdoors.core.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.core.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.core.commands.ICommandSender;
import nl.pim16aap2.bigdoors.core.localization.ILocalizer;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

class IsOpenParser implements ArgumentParser<ICommandSender, Boolean>, IRestartable
{
    private final ILocalizer localizer;

    private volatile String localizedOpen = "";
    private volatile String localizedOpenLowerCase = "";
    private volatile String localizedClosed = "";
    private volatile String localizedClosedLowerCase = "";

    @Inject IsOpenParser(RestartableHolder restartableHolder, ILocalizer localizer)
    {
        restartableHolder.registerRestartable(this);
        this.localizer = localizer;
    }

    @Override
    public ArgumentParseResult<Boolean> parse(
        CommandContext<ICommandSender> commandContext, Queue<String> inputQueue)
    {
        final @Nullable String input = inputQueue.peek();
        final @Nullable Boolean result = parseInput(input);

        if (result == null)
            return ArgumentParseResult.failure(
                new IllegalStateException("Failed to parse open status from input: '" + input + "'"));

        inputQueue.remove();
        return ArgumentParseResult.success(result);
    }

    private @Nullable Boolean parseInput(@Nullable String input)
    {
        if (input == null)
            return null;
        final String inputLowerCase = input.toLowerCase(Locale.ROOT);
        if (inputLowerCase.equals(localizedOpenLowerCase))
            return true;
        if (inputLowerCase.equals(localizedClosedLowerCase))
            return false;
        return null;
    }

    @Override
    public List<String> suggestions(CommandContext<ICommandSender> commandContext, String input)
    {
        if (input.isEmpty())
            return List.of(localizedOpen, localizedClosed);

        final String inputLowerCase = input.toLowerCase(Locale.ROOT);
        final List<String> ret = new ArrayList<>(2);
        if (localizedOpenLowerCase.startsWith(inputLowerCase))
            ret.add(localizedOpen);
        if (localizedClosedLowerCase.startsWith(inputLowerCase))
            ret.add(localizedClosed);
        return ret;
    }

    @Override
    public void initialize()
    {
        localizedOpen = localizer.getMessage("constants.open_status.open");
        localizedClosed = localizer.getMessage("constants.open_status.closed");

        localizedOpenLowerCase = localizedOpen.toLowerCase(Locale.ROOT);
        localizedClosedLowerCase = localizedClosed.toLowerCase(Locale.ROOT);
    }

    @Override
    public void shutDown()
    {
    }
}
