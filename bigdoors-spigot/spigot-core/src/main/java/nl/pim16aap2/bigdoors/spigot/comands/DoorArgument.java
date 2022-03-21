package nl.pim16aap2.bigdoors.spigot.comands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.commands.CommandSuggestionCache;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetrieverFactory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Function;

public class DoorArgument extends CommandArgument<ICommandSender, DoorRetriever>
{
    public DoorArgument(boolean required, String name, String defaultValue,
                        @Nullable BiFunction<CommandContext<ICommandSender>, String, List<String>> suggestionsProvider,
                        ArgumentDescription defaultDescription, boolean asyncSuggestions,
                        DoorRetrieverFactory doorRetrieverFactory, int maxPermission)
    {
        super(required, name, new DoorArgument.DoorArgumentParser(asyncSuggestions, doorRetrieverFactory,
                                                                  maxPermission),
              defaultValue, DoorRetriever.class, suggestionsProvider, defaultDescription);
    }

    public static final class DoorArgumentParser implements ArgumentParser<ICommandSender, DoorRetriever>
    {
        private final CommandSuggestionCache cache;
        private final boolean asyncSuggestions;
        private final DoorRetrieverFactory doorRetrieverFactory;
        private final int maxPermission;

        public DoorArgumentParser(boolean asyncSuggestions,
                                  DoorRetrieverFactory doorRetrieverFactory, int maxPermission)
        {
            this.cache = new CommandSuggestionCache();
            this.asyncSuggestions = asyncSuggestions;
            this.doorRetrieverFactory = doorRetrieverFactory;
            this.maxPermission = maxPermission;
        }

        @Override
        public ArgumentParseResult<DoorRetriever> parse(CommandContext<ICommandSender> commandContext,
                                                        Queue<String> inputQueue)
        {
            final @Nullable String input = inputQueue.peek();
            if (input == null)
                return ArgumentParseResult.failure(new NoInputProvidedException(DoorArgument.DoorArgumentParser.class,
                                                                                commandContext));

            final DoorRetriever result = doorRetrieverFactory.of(input);
            inputQueue.remove();
            return ArgumentParseResult.success(result);
        }

        @Override
        public List<String> suggestions(CommandContext<ICommandSender> commandContext, String input)
        {
            return cache.getDelayedTabCompleteOptions(commandContext.getSender(), input,
                                                      () -> getDoorNames(commandContext.getSender(), input))
                        .orElse(Collections.emptyList());
        }

        private List<String> getDoorNames(ICommandSender commandSender, String input)
            throws ExecutionException, InterruptedException
        {
            final Function<AbstractDoor, String> mapper = Util.parseLong(input).isPresent() ?
                                                          door -> Long.toString(door.getDoorUID()) :
                                                          AbstractDoor::getName;

            final DoorRetriever retriever = doorRetrieverFactory.of(input);
            final CompletableFuture<List<String>> names;

            if (commandSender instanceof IPPlayer player)
            {
                names = retriever.getDoors(player).thenApply(
                    doorList ->
                    {
                        final ArrayList<String> ret = new ArrayList<>(doorList.size());
                        for (final var door : doorList)
                        {
                            final int permission =
                                door.getDoorOwner(player).map(DoorOwner::permission).orElse(Integer.MAX_VALUE);
                            if (permission <= maxPermission)
                                ret.add(mapper.apply(door));
                        }
                        return ret;
                    });
            }
            else
                names = retriever.getDoors().thenApply(doorList -> doorList.stream().map(mapper).toList());
            return names.get();
        }
    }
}
