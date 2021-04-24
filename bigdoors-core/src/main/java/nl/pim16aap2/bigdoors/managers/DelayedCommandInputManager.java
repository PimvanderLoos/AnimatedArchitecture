package nl.pim16aap2.bigdoors.managers;

import lombok.NonNull;
import lombok.val;
import nl.pim16aap2.bigdoors.commands.BaseCommand;
import nl.pim16aap2.bigdoors.commands.DelayedCommandInputRequest;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a manager for outstanding {@link DelayedCommandInputRequest}s for {@link BaseCommand}s.
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
     * Deregisters all registered {@link DelayedCommandInputRequest}s for an {@link ICommandSender}.
     *
     * @param commandSender The {@link ICommandSender} for which to deregister the input requests.
     * @return True if a {@link DelayedCommandInputRequest} was previously registered for the {@link ICommandSender}.
     * When nothing was registered and nothing was removed, this method will return false.
     */
    public void deregisterAll(final @NonNull ICommandSender commandSender)
    {
        requests.remove(commandSender);
    }

    /**
     * Deregisters and cancels all registered {@link DelayedCommandInputRequest}s for an {@link ICommandSender}.
     *
     * @param commandSender The {@link ICommandSender} for which to deregister the input requests.
     */
    public void cancelAll(final @NonNull ICommandSender commandSender)
    {
        requests.computeIfPresent(commandSender, (k, v) ->
        {
            v.cancel();
            return null;
        });
    }

    /**
     * Deregisters a registered {@link DelayedCommandInputRequest} for an {@link ICommandSender} if the registered input
     * request 1) exists and 2) is the same (reference equality) as the provided input request.
     * <p>
     * This method is useful if the goal is to remove the exact request and not any new requests that may have
     * overridden it.
     *
     * @param commandSender              The {@link ICommandSender} for which to deregister the input request.
     * @param delayedCommandInputRequest The {@link DelayedCommandInputRequest} instance to compare any registered
     *                                   requests to. If the reference of the registered request and this one are the
     *                                   same, it will be deregistered.
     */
    public void deregister(final @NonNull ICommandSender commandSender,
                           final @NonNull DelayedCommandInputRequest<?> delayedCommandInputRequest)
    {
        requests.computeIfPresent(commandSender,
                                  (sender, request) -> request == delayedCommandInputRequest ? null : request);
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
