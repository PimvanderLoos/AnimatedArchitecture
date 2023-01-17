package nl.pim16aap2.bigdoors.spigot.comands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.context.CommandContext;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

public class MovableTypeArgument extends CommandArgument<ICommandSender, MovableType>
{
    @lombok.Builder
    public MovableTypeArgument(
        boolean required, String name, @Nullable String defaultValue,
        @Nullable BiFunction<CommandContext<ICommandSender>, String, List<String>> suggestionsProvider,
        @Nullable ArgumentDescription defaultDescription, MovableTypeParser parser)
    {
        super(required, name, parser, Objects.requireNonNullElse(defaultValue, ""), MovableType.class,
              suggestionsProvider,
              Objects.requireNonNullElse(defaultDescription, ArgumentDescription.empty()));
    }
}
