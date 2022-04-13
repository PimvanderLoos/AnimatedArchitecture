package nl.pim16aap2.bigdoors.spigot.comands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.context.CommandContext;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;

public class DoorTypeArgument extends CommandArgument<ICommandSender, DoorType>
{
    public DoorTypeArgument(
        boolean required, String name, String defaultValue,
        @Nullable BiFunction<CommandContext<ICommandSender>, String, List<String>> suggestionsProvider,
        ArgumentDescription defaultDescription, DoorTypeParser parser)
    {
        super(required, name, parser, defaultValue, DoorType.class, suggestionsProvider, defaultDescription);
    }
}
