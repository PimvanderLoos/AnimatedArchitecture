package nl.pim16aap2.bigdoors.util.delayedinput;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.Util;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Represents a {@link DelayedInputRequest} to specify which door was meant out of a list of multiple.
 *
 * @author Pim
 */
public class DelayedDoorSpecificationInputRequest extends DelayedInputRequest<String>
{
    private final @NonNull List<AbstractDoorBase> options;
    private final @NonNull IPPlayer player;

    private DelayedDoorSpecificationInputRequest(final long timeout, final @NonNull List<AbstractDoorBase> options,
                                                 final @NonNull IPPlayer player)
    {
        super(timeout);
        this.options = options;
        this.player = player;
    }

    /**
     * Asks the user to specify which one of multiple doors they want to select.
     *
     * @param timeout The amount of time (in ms) to give the user to provide the required input.
     *                <p>
     *                If the user fails to provide input within this timeout window, an empty result will be returned.
     * @param options The list of options they can choose from.
     * @param player  The player that is asked to make a choice.
     * @return The specified door if the user specified a valid one. Otherwise, an empty Optional.
     */
    public static @NonNull Optional<AbstractDoorBase> get(final long timeout,
                                                          final @NonNull List<AbstractDoorBase> options,
                                                          final @NonNull IPPlayer player)
    {
        if (options.size() == 1)
            return Optional.of(options.get(0));
        if (options.isEmpty())
            return Optional.empty();

        final @NonNull Optional<String> specification =
            new DelayedDoorSpecificationInputRequest(timeout, options, player).waitForInput();
        final @NonNull OptionalLong uidOpt = Util.parseLong(specification);
        if (!uidOpt.isPresent())
            return Optional.empty();

        final long uid = uidOpt.getAsLong();
        return Util.searchIterable(options, (door) -> door.getDoorUID() == uid);
    }

    @Override
    protected void init()
    {
        throw new UnsupportedOperationException("DelayedDoorSpecificationInputRequest is not fully implemented yet!");
    }

    @Override
    protected void cleanup()
    {
        throw new UnsupportedOperationException("DelayedDoorSpecificationInputRequest is not fully implemented yet!");
    }
}
