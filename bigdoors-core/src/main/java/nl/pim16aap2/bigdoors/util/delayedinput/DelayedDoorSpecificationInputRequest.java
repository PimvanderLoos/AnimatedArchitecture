package nl.pim16aap2.bigdoors.util.delayedinput;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.Util;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Represents a {@link DelayedInputRequest} to specify which door was meant out of a list of multiple.
 */
public class DelayedDoorSpecificationInputRequest extends DelayedInputRequest<String>
{
    private final @NonNull List<AbstractDoorBase> options;
    private final @NonNull IPPlayer player;

    private DelayedDoorSpecificationInputRequest(final int timeout, final @NonNull List<AbstractDoorBase> options,
                                                 final @NonNull IPPlayer player)
    {
        super(timeout);
        this.options = options;
        this.player = player;
    }

    public static @NonNull Optional<AbstractDoorBase> get(final int timeout,
                                                          final @NonNull List<AbstractDoorBase> options,
                                                          final @NonNull IPPlayer player)
    {
        if (options.size() == 1)
            return Optional.of(options.get(0));
        if (options.isEmpty())
            return Optional.empty();

        final @NonNull Optional<String> specification =
            new DelayedDoorSpecificationInputRequest(timeout, options, player).get();
        final @NonNull OptionalLong uidOpt = Util.parseLong(specification);
        if (!uidOpt.isPresent())
            return Optional.empty();

        final long uid = uidOpt.getAsLong();
        return Util.searchIterable(options, (door) -> door.getDoorUID() == uid);
    }

    @Override
    protected void init()
    {
        throw new NotImplementedException();
    }
}
