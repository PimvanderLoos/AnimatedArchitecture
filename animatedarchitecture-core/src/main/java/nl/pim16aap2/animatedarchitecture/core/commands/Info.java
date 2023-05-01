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
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.text.Text;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the information command that provides the issuer with more information about the structure.
 *
 * @author Pim
 */
@ToString
@Flogger
public class Info extends StructureTargetCommand
{
    private final HighlightedBlockSpawner glowingBlockSpawner;

    @AssistedInject Info(
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
                localizer.getMessage("commands.info.output.header"), TextType.INFO,
                arg -> arg.highlight(localizer.getStructureType(structure)),
                arg -> arg.highlight(structure.getNameAndUid()))
            .append('\n');
    }

    private void decorateLocation(StructureSnapshot structure, Text text)
    {
        final Vector3Di min = structure.getMinimum();
        final Vector3Di max = structure.getMaximum();
        text.append(
                localizer.getMessage("commands.info.output.location"), TextType.INFO,
                arg -> arg.highlight(String.format("%d %d %d", min.x(), min.y(), min.z())),
                arg -> arg.highlight(String.format("%d %d %d", max.x(), max.y(), max.z())))
            .append('\n');
    }

    private void decorateOpenStatus(StructureSnapshot structure, Text text)
    {
        final String localizedOpenStatus =
            localizer.getMessage(structure.isOpen() ? "constants.open_status.open" : "constants.open_status.closed");
        final String oppositeLocalizedOpenStatus =
            localizer.getMessage(structure.isOpen() ? "constants.open_status.closed" : "constants.open_status.open");

        final var openStatusArgument =
            text.getTextArgumentFactory().clickable(
                localizedOpenStatus,
                String.format(
                    "/animatedarchitecture setopenstatus %s %d true", oppositeLocalizedOpenStatus, structure.getUid()));

        text.append(localizer.getMessage("commands.info.output.open_status"), TextType.INFO, openStatusArgument)
            .append('\n');
    }

    private void decorateOpenDirection(StructureSnapshot structure, Text text)
    {
        final var argument = text.getTextArgumentFactory().clickable(
            localizer.getMessage(structure.getOpenDir().getLocalizationKey()),
            String.format(
                "/animatedarchitecture setopendirection %s %d true",
                localizer.getMessage(structure.getCycledOpenDirection().getLocalizationKey()), structure.getUid()));

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
                !structure.isLocked(), structure.getUid()));

        text.append(localizer.getMessage("commands.info.output.locked_status"), TextType.INFO, argument).append('\n');
    }

    private void decorateBlocksToMove(StructureSnapshot structure, Text text)
    {
        structure.getProperty("blocksToMove").ifPresent(
            blocksToMove ->
                text.append(
                    localizer.getMessage("commands.info.output.blocks_to_move"), TextType.INFO,
                    arg -> arg.highlight(blocksToMove)).append('\n'));
    }

    private void decoratePowerBlock(StructureSnapshot structure, Text text)
    {
        final Vector3Di loc = structure.getPowerBlock();
        text.append(
            localizer.getMessage("commands.info.output.power_block_location"), TextType.INFO,
            arg -> arg.highlight(String.format("%d %d %d", loc.x(), loc.y(), loc.z()))).append('\n');
    }

    protected void sendInfoMessage(StructureSnapshot structure)
    {
        final Text output = textFactory.newText();

        decorateHeader(structure, output);
        decorateLocation(structure, output);
        decorateOpenStatus(structure, output);
        decorateOpenDirection(structure, output);
        decorateLockedStatus(structure, output);
        decorateBlocksToMove(structure, output);
        decoratePowerBlock(structure, output);

        getCommandSender().sendMessage(output);
    }

    protected void highlightBlocks(StructureSnapshot structure)
    {
        if (!(getCommandSender() instanceof IPlayer player))
        {
            log.atSevere().withStackTrace(StackSize.FULL).log("Non-player command sender tried to highlight blocks!");
            return;
        }
        glowingBlockSpawner.spawnHighlightedBlocks(structure, player, Duration.ofSeconds(3));
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
