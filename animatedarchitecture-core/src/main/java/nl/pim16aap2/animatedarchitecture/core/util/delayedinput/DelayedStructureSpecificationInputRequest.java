package nl.pim16aap2.animatedarchitecture.core.util.delayedinput;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IConfig;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureSpecificationManager;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.text.Text;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.util.Util;

import javax.inject.Inject;
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
@Flogger
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class DelayedStructureSpecificationInputRequest extends DelayedInputRequest<String>
{
    private final IPlayer player;
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private final ILocalizer localizer;
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private final ITextFactory textFactory;
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private final StructureSpecificationManager structureSpecificationManager;

    private final List<AbstractStructure> options;

    private DelayedStructureSpecificationInputRequest(
        Duration timeout,
        List<AbstractStructure> options,
        IPlayer player, ILocalizer localizer,
        ITextFactory textFactory,
        StructureSpecificationManager structureSpecificationManager)
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

        final Text text = textFactory.newText();
        text.append(localizer.getMessage("input_request.specify_structure.header"), TextType.INFO);

        getStructureInfoList(text);

        player.sendMessage(text);
    }

    private Optional<AbstractStructure> parseInput(Optional<String> input)
    {
        final OptionalLong uidOpt = Util.parseLong(input);
        if (uidOpt.isEmpty())
            return Optional.empty();

        final long uid = uidOpt.getAsLong();
        return Util.searchIterable(options, structure -> structure.getUid() == uid);
    }

    /**
     * Retrieves the structure that the user specified.
     *
     * @return The structure that the user specified.
     */
    public CompletableFuture<Optional<AbstractStructure>> get()
    {
        return super.getInputResult().thenApply(this::parseInput);
    }

    @Override
    protected void cleanup()
    {
        structureSpecificationManager.cancelRequest(player);
    }

    private void appendStructureInfo(Text text, AbstractStructure structure, Optional<ILocation> location)
    {
        final long distance =
            location.map(loc -> Math.round(structure.getCuboid().getCenter().getDistance(loc))).orElse(-1L);

        final String cmd = "/animatedarchitecture specify " + structure.getUid();
        final String info = localizer.getMessage("input_request.specify_structure.structure_option.info");

        text.append("\n * ", TextType.INFO).append(
            localizer.getMessage("input_request.specify_structure.structure_option"),
            text.getTextComponentFactory().newClickableTextComponent(TextType.CLICKABLE, cmd, info),
            arg -> arg.clickable(structure.getUid(), TextType.CLICKABLE_CONFIRM, cmd, info),
            arg -> arg.clickable(structure.getType().getSimpleName(), cmd, info),
            arg -> arg.clickable(structure.getName(), TextType.CLICKABLE_CONFIRM, cmd, info),
            arg -> arg.clickable(distance, TextType.HIGHLIGHT, cmd, info));
    }

    private void getStructureInfoList(Text text)
    {
        final Optional<ILocation> location = player.getLocation();
        options.forEach(structure -> appendStructureInfo(text, structure, location));
    }

    @AllArgsConstructor(onConstructor = @__(@Inject))
    public static final class Factory
    {
        private final IConfig config;
        private final ILocalizer localizer;
        private final ITextFactory textFactory;
        private final StructureSpecificationManager structureSpecificationManager;

        /**
         * Requests the user to specify which structure they want to use out of a list of multiple.
         *
         * @param timeout
         *     The time to wait for the user to specify a structure.
         * @param options
         *     The list of structures to choose from.
         * @param player
         *     The player to request the structure from.
         * @return A {@link CompletableFuture} that will be completed with the structure the user specified. If the user
         * did not specify a structure in time, the future will be completed with {@link Optional#empty()}.
         * <p>
         * If the list of options is empty, the future will be completed with {@link Optional#empty()} and no request
         * will be sent to the user.
         * <p>
         * If the list of options contains only one element, the future will be completed with that element and no
         * request will be sent to the user.
         */
        public CompletableFuture<Optional<AbstractStructure>> get(
            Duration timeout, List<AbstractStructure> options, IPlayer player)
        {
            if (options.size() == 1)
                return CompletableFuture.completedFuture(Optional.of(options.get(0)));
            if (options.isEmpty())
                return CompletableFuture.completedFuture(Optional.empty());

            return new DelayedStructureSpecificationInputRequest(
                timeout, options, player, localizer, textFactory, structureSpecificationManager)
                .get().exceptionally(Util::exceptionallyOptional);
        }

        /**
         * Requests the user to specify which structure they want to use out of a list of multiple.
         * <p>
         * Uses {@link IConfig#specificationTimeout()} for the timeout.
         *
         * @param options
         *     The list of structures to choose from.
         * @param player
         *     The player to request the structure from.
         * @return A {@link CompletableFuture} that will be completed with the structure the user specified. If the user
         * did not specify a structure in time, the future will be completed with {@link Optional#empty()}.
         * <p>
         * If the list of options is empty, the future will be completed with {@link Optional#empty()} and no request
         * will be sent to the user.
         * <p>
         * If the list of options contains only one element, the future will be completed with that element and no
         * request will be sent to the user.
         */
        public CompletableFuture<Optional<AbstractStructure>> get(List<AbstractStructure> options, IPlayer player)
        {
            return get(Duration.ofSeconds(config.specificationTimeout()), options, player);
        }
    }
}
