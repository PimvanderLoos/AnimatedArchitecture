package nl.pim16aap2.animatedarchitecture.spigot.core.comands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.util.structureretriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.util.structureretriever.StructureRetrieverFactory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.function.BiFunction;

/**
 * Represents a type of argument in a command used to specify a structure.
 */
public class StructureArgument extends CommandArgument<ICommandSender, StructureRetriever>
{
    @lombok.Builder
    public StructureArgument(
        boolean required, String name, @Nullable String defaultValue,
        @Nullable BiFunction<CommandContext<ICommandSender>, String, List<String>> suggestionsProvider,
        @Nullable ArgumentDescription defaultDescription, boolean asyncSuggestions,
        StructureRetrieverFactory structureRetrieverFactory, PermissionLevel maxPermission)
    {
        super(required, name,
              new StructureArgumentParser(asyncSuggestions, structureRetrieverFactory, maxPermission),
              Objects.requireNonNullElse(defaultValue, ""), StructureRetriever.class, suggestionsProvider,
              Objects.requireNonNullElse(defaultDescription, ArgumentDescription.empty()));
    }

    public static final class StructureArgumentParser implements ArgumentParser<ICommandSender, StructureRetriever>
    {
        private final StructureRetrieverFactory structureRetrieverFactory;
        private final boolean asyncSuggestions;
        private final PermissionLevel maxPermission;

        public StructureArgumentParser(
            boolean asyncSuggestions, StructureRetrieverFactory structureRetrieverFactory,
            PermissionLevel maxPermission)
        {
            this.asyncSuggestions = asyncSuggestions;
            this.structureRetrieverFactory = structureRetrieverFactory;
            this.maxPermission = maxPermission;
        }

        @Override
        public ArgumentParseResult<StructureRetriever> parse(
            CommandContext<ICommandSender> commandContext, Queue<String> inputQueue)
        {
            final @Nullable String input = inputQueue.peek();
            if (input == null || input.isEmpty())
                return ArgumentParseResult.failure(
                    new NoInputProvidedException(StructureArgumentParser.class, commandContext));

            final StructureRetriever result = structureRetrieverFactory.of(input);
            inputQueue.remove();
            return ArgumentParseResult.success(result);
        }

        @Override
        public List<String> suggestions(CommandContext<ICommandSender> commandContext, String input)
        {
            if (input.isBlank())
                return Collections.emptyList();
            return structureRetrieverFactory
                .search(commandContext.getSender(), input, maxPermission)
                .getStructureIdentifiersIfAvailable()
                .<List<String>>map(ArrayList::new)
                .orElse(Collections.emptyList());
        }
    }
}
