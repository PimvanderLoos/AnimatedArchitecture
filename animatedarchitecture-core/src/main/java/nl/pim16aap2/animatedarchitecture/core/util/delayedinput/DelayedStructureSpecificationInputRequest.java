package nl.pim16aap2.animatedarchitecture.core.util.delayedinput;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.ExtensionMethod;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IConfig;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.localization.PersonalizedLocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureSpecificationManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.text.Text;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.util.CollectionsUtil;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;

import javax.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a {@link DelayedInputRequest} to specify which structure was meant out of a list of multiple.
 */
@Flogger
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@ExtensionMethod(CompletableFutureExtensions.class)
public final class DelayedStructureSpecificationInputRequest extends DelayedInputRequest<String>
{
    private final IPlayer player;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final StructureSpecificationManager structureSpecificationManager;

    private final PersonalizedLocalizer localizer;

    private final List<Structure> options;

    private DelayedStructureSpecificationInputRequest(
        Duration timeout,
        List<Structure> options,
        IPlayer player,
        IExecutor executor,
        StructureSpecificationManager structureSpecificationManager)
    {
        super(timeout, executor);
        this.options = options;
        this.player = player;
        this.localizer = player.getPersonalizedLocalizer();
        this.structureSpecificationManager = structureSpecificationManager;
        init();
    }

    private void init()
    {
        structureSpecificationManager.placeRequest(player, this);

        final Text text = player.newText();
        text.append(localizer.getMessage("input_request.specify_structure.header"), TextType.INFO);

        getStructureInfoList(text);

        player.sendMessage(text);
    }

    private Optional<Structure> parseInput(Optional<String> input)
    {
        final OptionalLong uidOpt = MathUtil.parseLong(input);
        if (uidOpt.isEmpty())
            return Optional.empty();

        final long uid = uidOpt.getAsLong();
        return CollectionsUtil.searchIterable(options, structure -> structure.getUid() == uid);
    }

    /**
     * Retrieves the structure that the user specified.
     *
     * @return The structure that the user specified.
     */
    public CompletableFuture<Optional<Structure>> get()
    {
        return super
            .getInputResult()
            .thenApply(this::parseInput)
            .withExceptionContext("Get specified structure from request %s", this);
    }

    @Override
    protected void cleanup()
    {
        structureSpecificationManager.cancelRequest(player);
    }

    private void appendStructureInfo(Text text, Structure structure, Optional<ILocation> location)
    {
        final long distance = location
            .map(loc -> Math.round(structure.getCuboid().getCenter().getDistance(loc)))
            .orElse(-1L);

        final String cmd = "/animatedarchitecture specify " + structure.getUid();
        final String info = localizer.getMessage("input_request.specify_structure.structure_option.info");

        text.append("\n * ", TextType.INFO).append(
            localizer.getMessage("input_request.specify_structure.structure_option"),
            text.getTextComponentFactory().newClickableTextComponent(TextType.CLICKABLE, cmd, info),
            arg -> arg.clickable(structure.getUid(), TextType.CLICKABLE_CONFIRM, cmd, info),
            arg -> arg.clickable(structure.getType().getSimpleName(), cmd, info),
            arg -> arg.clickable(structure.getName(), TextType.CLICKABLE_CONFIRM, cmd, info),
            arg -> arg.clickable(distance, TextType.HIGHLIGHT, cmd, info)
        );
    }

    private void getStructureInfoList(Text text)
    {
        final Optional<ILocation> location = player.getLocation();
        options.forEach(structure -> appendStructureInfo(text, structure, location));
    }

    @AllArgsConstructor(onConstructor = @__(@Inject))
    public static final class Factory
    {
        private final IExecutor executor;
        private final IConfig config;
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
        public CompletableFuture<Optional<Structure>> get(
            Duration timeout,
            List<Structure> options,
            IPlayer player)
        {
            if (options.size() == 1)
                return CompletableFuture.completedFuture(Optional.of(options.getFirst()));
            if (options.isEmpty())
                return CompletableFuture.completedFuture(Optional.empty());

            return new DelayedStructureSpecificationInputRequest(
                timeout,
                options,
                player,
                executor,
                structureSpecificationManager
            )
                .get()
                .withExceptionContext(
                    "Get structure specification from player %s for options %s",
                    player.getName(),
                    options
                );
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
        public CompletableFuture<Optional<Structure>> get(List<Structure> options, IPlayer player)
        {
            return get(Duration.ofSeconds(config.specificationTimeout()), options, player);
        }
    }
}
