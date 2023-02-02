package nl.pim16aap2.bigdoors.util.delayedinput;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.StructureSpecificationManager;
import nl.pim16aap2.bigdoors.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.text.Text;
import nl.pim16aap2.bigdoors.text.TextType;
import nl.pim16aap2.bigdoors.util.Util;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a {@link DelayedInputRequest} to specify which structure was meant out of a list of multiple.
 *
 * @author Pim
 */
public final class DelayedStructureSpecificationInputRequest extends DelayedInputRequest<String>
{
    private final List<AbstractStructure> options;
    private final IPPlayer player;
    @SuppressWarnings({"FieldCanBeLocal", "unused", "PMD.SingularField"})
    private final ILocalizer localizer;
    private final ITextFactory textFactory;
    private final StructureSpecificationManager structureSpecificationManager;

    private DelayedStructureSpecificationInputRequest(
        Duration timeout, List<AbstractStructure> options, IPPlayer player, ILocalizer localizer,
        ITextFactory textFactory, StructureSpecificationManager structureSpecificationManager)
    {
        super(timeout);
        this.options = options;
        this.player = player;
        this.localizer = localizer;
        this.textFactory = textFactory;
        this.structureSpecificationManager = structureSpecificationManager;
        init();
    }

    private void init()
    {
        structureSpecificationManager.placeRequest(player, this);
        // TODO: Localization
        // TODO: Abstraction. It may be a list and it may specified using a command, but that's not always true.
        //       It may also use a GUI or clickable text or whatever.
        final Text text = textFactory.newText();
        text.append("Please specify a structure you using \"", TextType.INFO)
            .append("/BigDoors specify <ID>", TextType.HIGHLIGHT)
            .append("\"", TextType.INFO);

        getStructureInfoList(text);
        player.sendMessage(text);
    }

    /**
     * Asks the user to specify which one of multiple structures they want to select.
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
     * @return The specified structure if the user specified a valid one. Otherwise, an empty Optional.
     */
    public static CompletableFuture<Optional<AbstractStructure>> get(
        Duration timeout, List<AbstractStructure> options, IPPlayer player, ILocalizer localizer,
        ITextFactory textFactory, StructureSpecificationManager structureSpecificationManager)
    {
        if (options.size() == 1)
            return CompletableFuture.completedFuture(Optional.of(options.get(0)));
        if (options.isEmpty())
            return CompletableFuture.completedFuture(Optional.empty());

        return new DelayedStructureSpecificationInputRequest(
            timeout, options, player, localizer, textFactory, structureSpecificationManager).getInputResult().thenApply(
            input ->
            {
                final OptionalLong uidOpt = Util.parseLong(input);
                if (uidOpt.isEmpty())
                    return Optional.empty();

                final long uid = uidOpt.getAsLong();
                return Util.searchIterable(options, structure -> structure.getUid() == uid);
            });
    }

    @Override
    protected void cleanup()
    {
        structureSpecificationManager.cancelRequest(player);
    }

    private void getStructureInfoList(Text text)
    {
        final Optional<IPLocation> location = player.getLocation();

        options.forEach(
            structure ->
            {
                text.append("\n")
                    .append(Long.toString(structure.getUid()), TextType.HIGHLIGHT)
                    .append(": ", TextType.INFO)
                    .append(structure.getType().getSimpleName(), TextType.HIGHLIGHT)
                    .append(", Creator: ", TextType.INFO)
                    .append(structure.getPrimeOwner().pPlayerData().getName(), TextType.HIGHLIGHT)
                    .append(", World: ", TextType.INFO)
                    .append(structure.getWorld().worldName(), TextType.HIGHLIGHT);

                if (location.isEmpty())
                    return;

                final double distance = Util.getDistanceToStructure(location.get(), structure);
                if (distance >= 0)
                    text.append(", Distance: ", TextType.INFO)
                        .append(String.format("%.1f", distance), TextType.HIGHLIGHT);
            });
    }
}
