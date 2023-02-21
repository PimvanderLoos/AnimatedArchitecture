package nl.pim16aap2.animatedarchitecture.spigot.core.comands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.context.CommandContext;
import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

public class DirectionArgument extends CommandArgument<ICommandSender, MovementDirection>
{
    @lombok.Builder
    public DirectionArgument(
        boolean required, String name, @Nullable String defaultValue,
        @Nullable BiFunction<CommandContext<ICommandSender>, String, List<String>> suggestionsProvider,
        @Nullable ArgumentDescription defaultDescription, DirectionParser parser)
    {
        super(required, name, parser, Objects.requireNonNullElse(defaultValue, ""), MovementDirection.class,
              suggestionsProvider, Objects.requireNonNullElse(defaultDescription, ArgumentDescription.empty()));
    }
}
