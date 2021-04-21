package nl.pim16aap2.bigdoors.managers;

import lombok.NonNull;
import lombok.val;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.commands.BaseCommand;
import nl.pim16aap2.bigdoors.commands.DelayedCommandInputRequest;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a manager for outstanding {@link DelayedCommandInputRequest}s for {@link BaseCommand}.
 *
 * @author Pim
 */
public class DelayedCommandInputManager
{
    private final @NonNull Map<ICommandSender, DelayedCommandInputRequest<?>> requests = new ConcurrentHashMap<>();

    /**
     * Registers an input request for a command sender.
     *
     * @param commandSender The {@link ICommandSender} for which to register the input request.
     * @param inputRequest  The {@link DelayedCommandInputRequest} to register.
     */
    public void register(final @NonNull ICommandSender commandSender,
                         final @NonNull DelayedCommandInputRequest<?> inputRequest)
    {
        @Nullable val existing = requests.put(commandSender, inputRequest);
        if (existing != null)
            existing.cancel();
    }

    /**
     * Deregisters a registered {@link DelayedCommandInputRequest} for an {@link ICommandSender}.
     *
     * @param commandSender The {@link ICommandSender} for which to deregister the input request.
     * @return True if a {@link DelayedCommandInputRequest} was previously registered for the {@link ICommandSender}.
     * When false, nothing was registered and nothing was removed.
     */
    public boolean deregister(final @NonNull ICommandSender commandSender)
    {
        return requests.remove(commandSender) != null;
    }

    /**
     * Gets the {@link DelayedCommandInputRequest} registered for a command sender if one exists.
     *
     * @param commandSender The {@link ICommandSender} for which to retrieve the input request.
     * @return The {@link DelayedCommandInputRequest} registered for a command sender, if one is registered.
     */
    public @NonNull Optional<DelayedCommandInputRequest<?>> getInputRequest(final @NonNull ICommandSender commandSender)
    {
        return Optional.ofNullable(requests.get(commandSender));
    }
}
