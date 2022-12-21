package nl.pim16aap2.bigdoors.spigot.comands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.doors.PermissionLevel;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetrieverFactory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.function.BiFunction;

/**
 * Represents a type of argument in a command used to specify a door.
 */
public class DoorArgument extends CommandArgument<ICommandSender, DoorRetriever>
{
    @lombok.Builder
    public DoorArgument(
        boolean required, String name, @Nullable String defaultValue,
        @Nullable BiFunction<CommandContext<ICommandSender>, String, List<String>> suggestionsProvider,
        @Nullable ArgumentDescription defaultDescription, boolean asyncSuggestions,
        DoorRetrieverFactory doorRetrieverFactory, PermissionLevel maxPermission)
    {
        super(required, name,
              new DoorArgument.DoorArgumentParser(asyncSuggestions, doorRetrieverFactory, maxPermission),
              Objects.requireNonNullElse(defaultValue, ""), DoorRetriever.class, suggestionsProvider,
              Objects.requireNonNullElse(defaultDescription, ArgumentDescription.empty()));
    }

    public static final class DoorArgumentParser implements ArgumentParser<ICommandSender, DoorRetriever>
    {
        private final DoorRetrieverFactory doorRetrieverFactory;
        private final boolean asyncSuggestions;
        private final PermissionLevel maxPermission;

        public DoorArgumentParser(
            boolean asyncSuggestions, DoorRetrieverFactory doorRetrieverFactory, PermissionLevel maxPermission)
        {
            this.asyncSuggestions = asyncSuggestions;
            this.doorRetrieverFactory = doorRetrieverFactory;
            this.maxPermission = maxPermission;
        }

        @Override
        public ArgumentParseResult<DoorRetriever> parse(
            CommandContext<ICommandSender> commandContext, Queue<String> inputQueue)
        {
            final @Nullable String input = inputQueue.peek();
            if (input == null || input.isEmpty())
                return ArgumentParseResult.failure(new NoInputProvidedException(DoorArgument.DoorArgumentParser.class,
                                                                                commandContext));

            final DoorRetriever result = doorRetrieverFactory.of(input);
            inputQueue.remove();
            return ArgumentParseResult.success(result);
        }

        @Override
        public List<String> suggestions(CommandContext<ICommandSender> commandContext, String input)
        {
            if (input.isBlank())
                return Collections.emptyList();
            return doorRetrieverFactory.search(commandContext.getSender(), input, maxPermission)
                                       .getDoorIdentifiersIfAvailable()
                                       .<List<String>>map(ArrayList::new)
                                       .orElse(Collections.emptyList());
        }
    }
}
