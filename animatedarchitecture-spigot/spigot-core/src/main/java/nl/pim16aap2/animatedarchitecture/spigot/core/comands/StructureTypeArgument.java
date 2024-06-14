package nl.pim16aap2.animatedarchitecture.spigot.core.comands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.context.CommandContext;
import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Represents a command argument that parses into a {@link StructureType}.
 */
public class StructureTypeArgument extends CommandArgument<ICommandSender, StructureType>
{
    @lombok.Builder
    public StructureTypeArgument(
        boolean required,
        String name,
        @Nullable String defaultValue,
        @Nullable BiFunction<CommandContext<ICommandSender>, String, List<String>> suggestionsProvider,
        @Nullable ArgumentDescription defaultDescription,
        StructureTypeParser parser)
    {
        super(
            required,
            name,
            parser,
            Objects.requireNonNullElse(defaultValue, ""),
            StructureType.class,
            suggestionsProvider,
            Objects.requireNonNullElse(defaultDescription, ArgumentDescription.empty())
        );
    }
}
