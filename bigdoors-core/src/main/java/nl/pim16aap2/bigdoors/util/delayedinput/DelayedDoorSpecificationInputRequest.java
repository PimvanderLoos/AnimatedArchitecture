package nl.pim16aap2.bigdoors.util.delayedinput;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.managers.DoorSpecificationManager;
import nl.pim16aap2.bigdoors.util.Util;

import java.time.Duration;
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

    private DelayedDoorSpecificationInputRequest(final @NonNull Duration timeout,
                                                 final @NonNull List<AbstractDoorBase> options,
                                                 final @NonNull IPPlayer player)
    {
        super(timeout.toMillis());
        this.options = options;
        this.player = player;
    }

    /**
     * Asks the user to specify which one of multiple doors they want to select.
     * <p>
     * Note that this will block the current thread until either one of the exit conditions is met.
     *
     * @param timeout The amount of time to give the user to provide the required input.
     *                <p>
     *                If the user fails to provide input within this timeout window, an empty result will be returned.
     * @param options The list of options they can choose from.
     * @param player  The player that is asked to make a choice.
     * @return The specified door if the user specified a valid one. Otherwise, an empty Optional.
     */
    public static @NonNull Optional<AbstractDoorBase> get(final @NonNull Duration timeout,
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
        if (uidOpt.isEmpty())
            return Optional.empty();

        final long uid = uidOpt.getAsLong();
        return Util.searchIterable(options, (door) -> door.getDoorUID() == uid);
    }

    @Override
    protected void init()
    {
        DoorSpecificationManager.get().placeRequest(player, this);
        // TODO: Localization
        // TODO: Abstraction. It may be a list and it may specified using a command, but that's not always true.
        //       It may also use a GUI or clickable text or whatever.
        final StringBuilder sb = new StringBuilder("Please specify a door you using \"/bigdoors specify <ID>\"");
        getDoorInfoList(sb);
        player.sendMessage(sb.toString());
    }

    @Override
    protected void cleanup()
    {
        DoorSpecificationManager.get().removeRequest(player);
    }

    private void getDoorInfoList(final @NonNull StringBuilder sb)
    {
        final @NonNull Optional<IPLocation> location = player.getLocation();

        options.forEach(
            door ->
            {
                sb.append("\n")
                  .append(String.format("%d: %s, Creator: %s, World: %s",
                                        door.getDoorUID(), door.getDoorType().getSimpleName(),
                                        door.getPrimeOwner().getPlayer().getName(), door.getWorld().getWorldName()));

                if (location.isEmpty())
                    return;

                final double distance = Util.getDistanceToDoor(location.get(), door);
                if (distance >= 0)
                    sb.append(String.format(", Distance: %.1f", distance));
            });
    }
}
