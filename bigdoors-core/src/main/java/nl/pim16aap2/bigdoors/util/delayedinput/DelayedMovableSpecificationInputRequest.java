package nl.pim16aap2.bigdoors.util.delayedinput;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.MovableSpecificationManager;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.text.Text;
import nl.pim16aap2.bigdoors.text.TextType;
import nl.pim16aap2.bigdoors.util.Util;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a {@link DelayedInputRequest} to specify which movable was meant out of a list of multiple.
 *
 * @author Pim
 */
public final class DelayedMovableSpecificationInputRequest extends DelayedInputRequest<String>
{
    private final List<AbstractMovable> options;
    private final IPPlayer player;
    @SuppressWarnings({"FieldCanBeLocal", "unused", "PMD.SingularField"})
    private final ILocalizer localizer;
    private final ITextFactory textFactory;
    private final MovableSpecificationManager movableSpecificationManager;

    private DelayedMovableSpecificationInputRequest(
        Duration timeout, List<AbstractMovable> options, IPPlayer player, ILocalizer localizer,
        ITextFactory textFactory, MovableSpecificationManager movableSpecificationManager)
    {
        super(timeout);
        this.options = options;
        this.player = player;
        this.localizer = localizer;
        this.textFactory = textFactory;
        this.movableSpecificationManager = movableSpecificationManager;
        init();
    }

    private void init()
    {
        movableSpecificationManager.placeRequest(player, this);
        // TODO: Localization
        // TODO: Abstraction. It may be a list and it may specified using a command, but that's not always true.
        //       It may also use a GUI or clickable text or whatever.
        final Text text = textFactory.newText();
        text.append("Please specify a movable you using \"", TextType.INFO)
            .append("/BigDoors specify <ID>", TextType.HIGHLIGHT)
            .append("\"", TextType.INFO);

        getMovableInfoList(text);
        player.sendMessage(text);
    }

    /**
     * Asks the user to specify which one of multiple movables they want to select.
     * <p>
     * Note that this will block the current thread until either one of the exit conditions is met.
     *
     * @param timeout
     *     The amount of time to give the user to provide the input.
     *     <p>
     *     If the user fails to provide input within this timeout window, an empty result will be returned.
     * @param options
     *     The list of options they can choose from.
     * @param player
     *     The player that is asked to make a choice.
     * @return The specified movable if the user specified a valid one. Otherwise, an empty Optional.
     */
    public static CompletableFuture<Optional<AbstractMovable>> get(
        Duration timeout, List<AbstractMovable> options, IPPlayer player, ILocalizer localizer,
        ITextFactory textFactory, MovableSpecificationManager movableSpecificationManager)
    {
        if (options.size() == 1)
            return CompletableFuture.completedFuture(Optional.of(options.get(0)));
        if (options.isEmpty())
            return CompletableFuture.completedFuture(Optional.empty());

        return new DelayedMovableSpecificationInputRequest(
            timeout, options, player, localizer, textFactory, movableSpecificationManager).getInputResult().thenApply(
            input ->
            {
                final OptionalLong uidOpt = Util.parseLong(input);
                if (uidOpt.isEmpty())
                    return Optional.empty();

                final long uid = uidOpt.getAsLong();
                return Util.searchIterable(options, movable -> movable.getUID() == uid);
            });
    }

    @Override
    protected void cleanup()
    {
        movableSpecificationManager.cancelRequest(player);
    }

    private void getMovableInfoList(Text text)
    {
        final Optional<IPLocation> location = player.getLocation();

        options.forEach(
            movable ->
            {
                text.append("\n")
                    .append(Long.toString(movable.getUID()), TextType.HIGHLIGHT)
                    .append(": ", TextType.INFO)
                    .append(movable.getMovableType().getSimpleName(), TextType.HIGHLIGHT)
                    .append(", Creator: ", TextType.INFO)
                    .append(movable.getPrimeOwner().pPlayerData().getName(), TextType.HIGHLIGHT)
                    .append(", World: ", TextType.INFO)
                    .append(movable.getWorld().worldName(), TextType.HIGHLIGHT);

                if (location.isEmpty())
                    return;

                final double distance = Util.getDistanceToMovable(location.get(), movable);
                if (distance >= 0)
                    text.append(", Distance: ", TextType.INFO)
                        .append(String.format("%.1f", distance), TextType.HIGHLIGHT);
            });
    }
}
