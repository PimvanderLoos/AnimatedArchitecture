package nl.pim16aap2.bigdoors.core.managers;

import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.core.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.core.util.delayedinput.DelayedInputRequest;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a manager for handling structure specifications requests.
 *
 * @author Pim
 */
@Singleton
public final class StructureSpecificationManager extends Restartable
{
    private final Map<IPlayer, DelayedInputRequest<String>> requests = new ConcurrentHashMap<>();

    @Inject
    public StructureSpecificationManager(RestartableHolder holder)
    {
        super(holder);
    }

    /**
     * Checks if there is an open structure specification request for a player.
     *
     * @param player
     *     The player to check.
     * @return True if there is an open structure specification request for the player.
     */
    @SuppressWarnings("unused")
    public boolean isActive(IPlayer player)
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
    public void placeRequest(IPlayer player, DelayedInputRequest<String> request)
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
    public boolean handleInput(IPlayer player, String input)
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
     * @return True if a request was cancelled.
     */
    public boolean cancelRequest(IPlayer player)
    {
        final @Nullable DelayedInputRequest<String> removed = requests.remove(player);
        if (removed == null)
            return false;
        removed.cancel();
        return true;
    }

    @Override
    public void shutDown()
    {
        requests.values().forEach(DelayedInputRequest::cancel);
        requests.clear();
    }
}
