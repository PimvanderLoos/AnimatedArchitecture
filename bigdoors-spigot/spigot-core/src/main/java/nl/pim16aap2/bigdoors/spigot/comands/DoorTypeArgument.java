package nl.pim16aap2.bigdoors.spigot.comands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.context.CommandContext;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

public class DoorTypeArgument extends CommandArgument<ICommandSender, DoorType>
{
    @lombok.Builder
    public DoorTypeArgument(
        boolean required, String name, @Nullable String defaultValue,
        @Nullable BiFunction<CommandContext<ICommandSender>, String, List<String>> suggestionsProvider,
        @Nullable ArgumentDescription defaultDescription, DoorTypeParser parser)
    {
        super(required, name, parser, Objects.requireNonNullElse(defaultValue, ""), DoorType.class, suggestionsProvider,
              Objects.requireNonNullElse(defaultDescription, ArgumentDescription.empty()));
    }
}
