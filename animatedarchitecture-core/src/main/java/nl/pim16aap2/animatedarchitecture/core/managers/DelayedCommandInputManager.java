package nl.pim16aap2.animatedarchitecture.core.managers;

import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.commands.BaseCommand;
import nl.pim16aap2.animatedarchitecture.core.commands.DelayedCommandInputRequest;
import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.util.StringUtil;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a manager for outstanding {@link DelayedCommandInputRequest}s for {@link BaseCommand}s.
 */
@Singleton
public class DelayedCommandInputManager implements IDebuggable
{
    private final Map<ICommandSender, DelayedCommandInputRequest<?>> requests = new ConcurrentHashMap<>();

    @Inject
    public DelayedCommandInputManager(DebuggableRegistry debuggableRegistry)
    {
        debuggableRegistry.registerDebuggable(this);
    }

    /**
     * Registers an input request for a command sender.
     *
     * @param commandSender
     *     The {@link ICommandSender} for which to register the input request.
     * @param inputRequest
     *     The {@link DelayedCommandInputRequest} to register.
     */
    public void register(ICommandSender commandSender, DelayedCommandInputRequest<?> inputRequest)
    {
        final @Nullable var existing = requests.put(commandSender, inputRequest);
        if (existing != null)
            existing.cancel();
    }

    /**
     * Deregisters all registered {@link DelayedCommandInputRequest}s for an {@link ICommandSender}.
     *
     * @param commandSender
     *     The {@link ICommandSender} for which to deregister the input requests.
     */
    public void deregisterAll(ICommandSender commandSender)
    {
        requests.remove(commandSender);
    }

    /**
     * Deregisters and cancels all registered {@link DelayedCommandInputRequest}s for an {@link ICommandSender}.
     *
     * @param commandSender
     *     The {@link ICommandSender} for which to deregister the input requests.
     */
    public void cancelAll(ICommandSender commandSender)
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
     * @param commandSender
     *     The {@link ICommandSender} for which to deregister the input request.
     * @param delayedCommandInputRequest
     *     The {@link DelayedCommandInputRequest} instance to compare any registered requests to. If the reference of
     *     the registered request and this one are the same, it will be deregistered.
     */
    public void deregister(ICommandSender commandSender, DelayedCommandInputRequest<?> delayedCommandInputRequest)
    {
        requests.computeIfPresent(
            commandSender,
            (sender, request) -> request == delayedCommandInputRequest ? null : request);
    }

    /**
     * Gets the {@link DelayedCommandInputRequest} registered for a command sender if one exists.
     *
     * @param commandSender
     *     The {@link ICommandSender} for which to retrieve the input request.
     * @return The {@link DelayedCommandInputRequest} registered for a command sender, if one is registered.
     */
    public Optional<DelayedCommandInputRequest<?>> getInputRequest(ICommandSender commandSender)
    {
        return Optional.ofNullable(requests.get(commandSender));
    }

    @Override
    public @Nullable String getDebugInformation()
    {
        return "Currently active delayed command input requests: " + StringUtil.formatCollection(requests.values());
    }
}
