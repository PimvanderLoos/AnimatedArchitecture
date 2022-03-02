package nl.pim16aap2.bigdoors.managers;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.util.delayedinput.DelayedInputRequest;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a manager for handling door specifications requests.
 *
 * @author Pim
 */
@Singleton
public final class DoorSpecificationManager extends Restartable
{
    private final Map<IPPlayer, DelayedInputRequest<String>> requests = new ConcurrentHashMap<>();

    @Inject
    public DoorSpecificationManager(RestartableHolder holder)
    {
        super(holder);
    }

    /**
     * Checks if there is an open door specification request for a player.
     *
     * @param player
     *     The player to check.
     * @return True if there is an open door specification request for the player.
     */
    @SuppressWarnings("unused")
    public boolean isActive(IPPlayer player)
    {
        return requests.containsKey(player);
    }

    /**
     * Registers a new delayed input request for the given player.
     * <p>
     * If a request is already active for the given player, the old request will be cancelled and replaced by the new
     * one.
     *
     * @param player
     *     The player to request.
     * @param request
     *     The request.
     */
    public void placeRequest(IPPlayer player, DelayedInputRequest<String> request)
    {
        requests.compute(
            player, (key, value) ->
            {
                // Cancel previous requests if any are still active.
                if (value != null)
                    value.cancel();
                return request;
            });
    }

    /**
     * Handles input for a player.
     *
     * @param player
     *     The player that provided input.
     * @param input
     *     The input to handle.
     * @return False if no request could be found for the player.
     */
    public boolean handleInput(IPPlayer player, String input)
    {
        final @Nullable DelayedInputRequest<String> request = requests.get(player);
        if (request == null)
            return false;

        request.set(input);
        return true;
    }

    /**
     * Cancels an active request for a player.
     *
     * @param player
     *     The player whose requests to cancel.
     */
    public void cancelRequest(IPPlayer player)
    {
        Optional.ofNullable(requests.remove(player)).ifPresent(DelayedInputRequest::cancel);
    }

    @Override
    public void shutDown()
    {
        requests.values().forEach(DelayedInputRequest::cancel);
        requests.clear();
    }
}
