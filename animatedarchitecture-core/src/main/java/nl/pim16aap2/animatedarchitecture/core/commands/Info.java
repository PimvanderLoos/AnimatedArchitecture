package nl.pim16aap2.animatedarchitecture.core.commands;

import com.google.common.flogger.StackSize;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.HighlightedBlockSpawner;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyValuePair;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.text.Text;
import nl.pim16aap2.animatedarchitecture.core.text.TextArgument;
import nl.pim16aap2.animatedarchitecture.core.text.TextArgumentFactory;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Represents the information command that provides the issuer with more information about the structure.
 */
@ToString
@Flogger
public class Info extends StructureTargetCommand
{
    private final HighlightedBlockSpawner glowingBlockSpawner;

    @AssistedInject
    Info(
        @Assisted ICommandSender commandSender,
        @Assisted StructureRetriever structureRetriever,
        ILocalizer localizer,
        ITextFactory textFactory,
        HighlightedBlockSpawner glowingBlockSpawner)
    {
        super(commandSender, localizer, textFactory, structureRetriever, StructureAttribute.INFO);
        this.glowingBlockSpawner = glowingBlockSpawner;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.INFO;
    }

    @Override
    protected CompletableFuture<?> performAction(AbstractStructure structure)
    {
        final StructureSnapshot snapshot = structure.getSnapshot();
        try
        {
            sendInfoMessage(snapshot);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to send info message to command sender: %s", getCommandSender());
        }
        try
        {
            highlightBlocks(snapshot);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to highlight blocks for command sender: %s", getCommandSender());
        }
        return CompletableFuture.completedFuture(null);
    }

    private void decorateHeader(StructureSnapshot structure, Text text)
    {
        text.append(
                localizer.getMessage("commands.info.output.header"),
                TextType.INFO,
                arg -> arg.highlight(localizer.getStructureType(structure)),
                arg -> arg.highlight(structure.getNameAndUid()))
            .append('\n');
    }

    private void decoratePrimeOwner(StructureSnapshot structure, Text text)
    {
        final boolean hasCreatorAccess = hasCreatorAccess(structure, getCommandSender());
        final String primeOwner = structure.getPrimeOwner().playerData().getName();

        final Function<TextArgumentFactory, TextArgument> argumentFunction =
            hasCreatorAccess ?
                arg -> arg.highlight(primeOwner) :
                arg -> arg.info(primeOwner);

        text.append(
                localizer.getMessage("commands.info.output.prime_owner"),
                TextType.INFO,
                argumentFunction.apply(text.getTextArgumentFactory()))
            .append('\n');
    }

    private void decorateCurrentPlayerAccess(StructureSnapshot structure, Text text)
    {
        final boolean hasCreatorAccess = hasCreatorAccess(structure, getCommandSender());
        if (hasCreatorAccess)
            return;

        final var player = getCommandSender().getPlayer().orElseThrow();
        final var currentOwner = structure.getOwner(player).orElseThrow();
        final var accessLevel = currentOwner.permission();

        text.append(
                localizer.getMessage("commands.info.output.your_access_level"),
                TextType.INFO,
                arg -> arg.highlight(localizer.getMessage(accessLevel.getTranslationKey())))
            .append('\n');
    }

    /**
     * Decorates the owner of the structure.
     * <p>
     * There are several cases to consider:
     * <ul>
     *     <li>The command sender is the owner of the structure.</li>
     *     <li>The command sender is a co-owner of the structure.</li>
     *     <li>The command sender is not the owner of the structure.</li>
     *     <li>The command sender is not a player</li>
     * </ul>
     *
     * @param structure
     *     The structure to decorate the owner for.
     * @param text
     *     The {@link Text} object to append the owner information to.
     */
    private void decorateOwner(StructureSnapshot structure, Text text)
    {
        // If the command sender is either not a player or the original creator,
        // we only print the line `Creator: <creator>`
        // Otherwise, we print the creator line and the line `Your Access: <access>`
        decoratePrimeOwner(structure, text);
        decorateCurrentPlayerAccess(structure, text);
    }

    private void decorateLocation(StructureSnapshot structure, Text text)
    {
        final Vector3Di min = structure.getMinimum();
        final Vector3Di max = structure.getMaximum();
        text.append(
                localizer.getMessage("commands.info.output.location"),
                TextType.INFO,
                arg -> arg.highlight(String.format("%d %d %d", min.x(), min.y(), min.z())),
                arg -> arg.highlight(String.format("%d %d %d", max.x(), max.y(), max.z())))
            .append('\n');
    }

    private void decorateOpenStatus(StructureSnapshot structure, Text text)
    {
        final @Nullable Boolean isOpen = structure.getPropertyValue(Property.OPEN_STATUS).value();
        if (isOpen == null)
            return;

        final String localizedOpenStatus =
            localizer.getMessage(isOpen ? "constants.open_status.open" : "constants.open_status.closed");
        final String oppositeLocalizedOpenStatus =
            localizer.getMessage(isOpen ? "constants.open_status.closed" : "constants.open_status.open");

        final var openStatusArgument = text.getTextArgumentFactory().clickable(
            localizedOpenStatus,
            String.format(
                "/animatedarchitecture setopenstatus %s %d true",
                oppositeLocalizedOpenStatus,
                structure.getUid())
        );

        text.append(localizer.getMessage("commands.info.output.open_status"), TextType.INFO, openStatusArgument)
            .append('\n');
    }

    private void decorateOpenDirection(StructureSnapshot structure, Text text)
    {
        final var argument = text.getTextArgumentFactory().clickable(
            localizer.getMessage(structure.getOpenDir().getLocalizationKey()),
            String.format(
                "/animatedarchitecture setopendirection %s %d true",
                localizer.getMessage(structure.getCycledOpenDirection().getLocalizationKey()),
                structure.getUid())
        );

        text.append(localizer.getMessage("commands.info.output.open_direction"), TextType.INFO, argument).append('\n');
    }

    private void decorateLockedStatus(StructureSnapshot structure, Text text)
    {
        final String localizationKey =
            structure.isLocked() ? "constants.locked_status.locked" : "constants.locked_status.unlocked";

        final var argument = text.getTextArgumentFactory().clickable(
            localizer.getMessage(localizationKey),
            String.format(
                "/animatedarchitecture lock %s %d true",
                !structure.isLocked(),
                structure.getUid())
        );

        text.append(localizer.getMessage("commands.info.output.locked_status"), TextType.INFO, argument).append('\n');
    }

    private void decorateBlocksToMove(StructureSnapshot structure, Text text)
    {
        final var value = structure.getPropertyValue(Property.BLOCKS_TO_MOVE);
        final @Nullable Integer blocksToMove = value.value();
        if (blocksToMove == null)
            return;

        text.append(
                localizer.getMessage("commands.info.output.blocks_to_move"),
                TextType.INFO,
                arg -> arg.highlight(blocksToMove))
            .append('\n');
    }

    private void decoratePowerBlock(StructureSnapshot structure, Text text)
    {
        final Vector3Di loc = structure.getPowerBlock();
        text.append(
                localizer.getMessage("commands.info.output.power_block_location"),
                TextType.INFO,
                arg -> arg.highlight(String.format("%d %d %d", loc.x(), loc.y(), loc.z())))
            .append('\n');
    }

    /**
     * Decorates a property of the structure.
     *
     * @param text
     *     The {@link Text} object to append the property to.
     * @param propertyValuePair
     *     The property-value pair to decorate.
     */
    // TODO: Remove this placeholder method and implement a decorator pattern for properties.
    private void decorateProperty(Text text, PropertyValuePair<?> propertyValuePair)
    {
        // These properties are handled separately.
        if (propertyValuePair.property().equals(Property.OPEN_STATUS) ||
            propertyValuePair.property().equals(Property.BLOCKS_TO_MOVE))
            return;

        text.append(
                propertyValuePair.property().getFullKey() + ": {0}",
                TextType.INFO,
                arg -> arg.highlight(Objects.toString(propertyValuePair.propertyValue())))
            .append('\n');
    }

    /**
     * Decorates the properties of the structure.
     * <p>
     * The properties that are handled separately are ignored (i.e. {@link Property#OPEN_STATUS} and
     * {@link Property#BLOCKS_TO_MOVE}).
     *
     * @param structure
     *     The structure to decorate the properties for.
     * @param text
     *     The {@link Text} object to append the properties to.
     */
    private void decorateProperties(StructureSnapshot structure, Text text)
    {
        structure
            .getPropertyContainerSnapshot()
            .forEach(entry -> decorateProperty(text, entry));
    }

    // TODO: Implement a decorator pattern properties.
    protected void sendInfoMessage(StructureSnapshot structure)
    {
        final Text output = textFactory.newText();

        decorateHeader(structure, output);
        decorateOwner(structure, output);
        decorateLocation(structure, output);
        decorateOpenStatus(structure, output);
        decorateOpenDirection(structure, output);
        decorateLockedStatus(structure, output);
        decorateBlocksToMove(structure, output);
        decoratePowerBlock(structure, output);

        decorateProperties(structure, output);

        getCommandSender().sendMessage(output);
    }

    protected void highlightBlocks(StructureSnapshot structure)
    {
        if (!(getCommandSender() instanceof IPlayer player))
        {
            // Most parts of the command can be handled for any type of command sender, so this is not an error.
            log.atFinest().withStackTrace(StackSize.FULL).log(
                "Not highlighting blocks for non-player command sender '%s'.",
                getCommandSender()
            );
            return;
        }
        glowingBlockSpawner.spawnHighlightedBlocks(structure, player, Duration.ofSeconds(3));
    }

    /**
     * Checks if the command sender has creator access to the structure.
     * <p>
     * A command sender has creator access if:
     * <ul>
     *     <li>The command sender is a player and the prime creator of the structure.</li>
     *     <li>The command sender is not a player (i.e. the server/command block.)</li>
     * <ul>
     *
     * @param structure
     *     The structure to check against.
     * @param commandSender
     *     The command sender to check against.
     * @return {@code true} if the command sender has creator access to the structure.
     */
    private static boolean hasCreatorAccess(StructureSnapshot structure, ICommandSender commandSender)
    {
        return commandSender
            .getPlayer()
            .map(player -> structure.getPrimeOwner().matches(player))
            .orElse(true);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link Info} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for retrieving the structure info and the receiver of the
         *     structure's information.
         * @param structureRetriever
         *     A {@link StructureRetrieverFactory} representing the {@link AbstractStructure} for which the information
         *     will be retrieved.
         * @return See {@link BaseCommand#run()}.
         */
        Info newInfo(ICommandSender commandSender, StructureRetriever structureRetriever);
    }
}
