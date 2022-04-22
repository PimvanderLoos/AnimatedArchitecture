package nl.pim16aap2.bigdoors.spigot.comands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.context.CommandContext;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

public class DirectionArgument extends CommandArgument<ICommandSender, RotateDirection>
{
    @lombok.Builder
    public DirectionArgument(
        boolean required, String name, @Nullable String defaultValue,
        @Nullable BiFunction<CommandContext<ICommandSender>, String, List<String>> suggestionsProvider,
        @Nullable ArgumentDescription defaultDescription, DirectionParser parser)
    {
        super(required, name, parser, Objects.requireNonNullElse(defaultValue, ""), RotateDirection.class,
              suggestionsProvider, Objects.requireNonNullElse(defaultDescription, ArgumentDescription.empty()));
    }
}
