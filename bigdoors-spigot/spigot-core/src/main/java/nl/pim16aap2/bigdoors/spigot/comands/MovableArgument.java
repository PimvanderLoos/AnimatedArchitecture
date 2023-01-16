package nl.pim16aap2.bigdoors.spigot.comands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.movable.PermissionLevel;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetrieverFactory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.function.BiFunction;

/**
 * Represents a type of argument in a command used to specify a movable.
 */
public class MovableArgument extends CommandArgument<ICommandSender, MovableRetriever>
{
    @lombok.Builder
    public MovableArgument(
        boolean required, String name, @Nullable String defaultValue,
        @Nullable BiFunction<CommandContext<ICommandSender>, String, List<String>> suggestionsProvider,
        @Nullable ArgumentDescription defaultDescription, boolean asyncSuggestions,
        MovableRetrieverFactory movableRetrieverFactory, PermissionLevel maxPermission)
    {
        super(required, name,
              new MovableArgumentParser(asyncSuggestions, movableRetrieverFactory, maxPermission),
              Objects.requireNonNullElse(defaultValue, ""), MovableRetriever.class, suggestionsProvider,
              Objects.requireNonNullElse(defaultDescription, ArgumentDescription.empty()));
    }

    public static final class MovableArgumentParser implements ArgumentParser<ICommandSender, MovableRetriever>
    {
        private final MovableRetrieverFactory movableRetrieverFactory;
        private final boolean asyncSuggestions;
        private final PermissionLevel maxPermission;

        public MovableArgumentParser(
            boolean asyncSuggestions, MovableRetrieverFactory movableRetrieverFactory, PermissionLevel maxPermission)
        {
            this.asyncSuggestions = asyncSuggestions;
            this.movableRetrieverFactory = movableRetrieverFactory;
            this.maxPermission = maxPermission;
        }

        @Override
        public ArgumentParseResult<MovableRetriever> parse(
            CommandContext<ICommandSender> commandContext, Queue<String> inputQueue)
        {
            final @Nullable String input = inputQueue.peek();
            if (input == null || input.isEmpty())
                return ArgumentParseResult.failure(
                    new NoInputProvidedException(MovableArgumentParser.class, commandContext));

            final MovableRetriever result = movableRetrieverFactory.of(input);
            inputQueue.remove();
            return ArgumentParseResult.success(result);
        }

        @Override
        public List<String> suggestions(CommandContext<ICommandSender> commandContext, String input)
        {
            if (input.isBlank())
                return Collections.emptyList();
            return movableRetrieverFactory.search(commandContext.getSender(), input, maxPermission)
                                          .getMovableIdentifiersIfAvailable()
                                          .<List<String>>map(ArrayList::new)
                                          .orElse(Collections.emptyList());
        }
    }
}
