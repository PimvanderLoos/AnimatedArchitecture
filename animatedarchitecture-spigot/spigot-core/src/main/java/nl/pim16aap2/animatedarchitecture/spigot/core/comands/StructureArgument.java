package nl.pim16aap2.animatedarchitecture.spigot.core.comands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureFinder;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;

/**
 * Represents a type of argument in a command used to specify a structure.
 */
@Flogger
public class StructureArgument extends CommandArgument<ICommandSender, StructureRetriever>
{
    @lombok.Builder
    public StructureArgument(
        boolean required,
        String name,
        @Nullable String defaultValue,
        @Nullable BiFunction<CommandContext<ICommandSender>, String, List<String>> suggestionsProvider,
        @Nullable ArgumentDescription defaultDescription,
        boolean asyncSuggestions,
        IExecutor executor,
        StructureRetrieverFactory structureRetrieverFactory,
        PermissionLevel maxPermission)
    {
        super(
            required,
            name,
            new StructureArgumentParser(asyncSuggestions, structureRetrieverFactory, maxPermission, executor),
            Objects.requireNonNullElse(defaultValue, ""),
            StructureRetriever.class,
            suggestionsProvider,
            Objects.requireNonNullElse(defaultDescription, ArgumentDescription.empty())
        );
    }

    public static final class StructureArgumentParser implements ArgumentParser<ICommandSender, StructureRetriever>
    {
        private final StructureRetrieverFactory structureRetrieverFactory;
        private final boolean asyncSuggestions;
        private final PermissionLevel maxPermission;
        private final IExecutor executor;

        public StructureArgumentParser(
            boolean asyncSuggestions,
            StructureRetrieverFactory structureRetrieverFactory,
            PermissionLevel maxPermission,
            IExecutor executor)
        {
            this.asyncSuggestions = asyncSuggestions;
            this.structureRetrieverFactory = structureRetrieverFactory;
            this.maxPermission = maxPermission;
            this.executor = executor;
        }

        @Override
        public ArgumentParseResult<StructureRetriever> parse(
            CommandContext<ICommandSender> commandContext,
            Queue<String> inputQueue)
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

            final var search = structureRetrieverFactory.search(commandContext.getSender(), input, maxPermission);

            if (asyncSuggestions)
                return getAsyncSuggestions(commandContext, input, search);

            return search
                .getStructureIdentifiersIfAvailable()
                .<List<String>>map(ArrayList::new)
                .orElse(Collections.emptyList());
        }

        /**
         * Gets the suggestions for the given input with the assumption that this method is called asynchronously.
         *
         * @param commandContext
         *     The command context.
         * @param input
         *     The input to get suggestions for.
         * @param search
         *     The structure finder to use.
         * @return The suggestions.
         */
        private List<String> getAsyncSuggestions(
            CommandContext<ICommandSender> commandContext, String input, StructureFinder search)
        {
            executor.assertNotMainThread("Async suggestions cannot be retrieved on the main thread!");

            try
            {
                return new ArrayList<>(search.getStructureIdentifiers().get(1, TimeUnit.SECONDS));
            }
            catch (InterruptedException | ExecutionException | TimeoutException e)
            {
                log.atSevere().withCause(e).log(
                    "Failed to get suggestions for structure argument with input '%s' for user: '%s'",
                    input,
                    commandContext.getSender()
                );
                return Collections.emptyList();
            }
        }
    }
}
