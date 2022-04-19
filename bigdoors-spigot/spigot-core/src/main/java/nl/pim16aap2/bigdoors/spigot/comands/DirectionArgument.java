package nl.pim16aap2.bigdoors.spigot.comands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.context.CommandContext;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;

public class DirectionArgument extends CommandArgument<ICommandSender, RotateDirection>
{
    public DirectionArgument(
        boolean required, String name, String defaultValue,
        @Nullable BiFunction<CommandContext<ICommandSender>, String, List<String>> suggestionsProvider,
        ArgumentDescription defaultDescription, DirectionParser parser)
    {
        super(required, name, parser, defaultValue, RotateDirection.class, suggestionsProvider, defaultDescription);
    }
}
