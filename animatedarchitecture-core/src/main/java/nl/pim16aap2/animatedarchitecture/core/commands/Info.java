package nl.pim16aap2.animatedarchitecture.core.commands;

import com.google.common.flogger.StackSize;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.GlowingBlockSpawner;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.util.structureretriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.util.structureretriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.text.Text;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;

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
    private final GlowingBlockSpawner glowingBlockSpawner;

    @AssistedInject //
    Info(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        @Assisted StructureRetriever structureRetriever, GlowingBlockSpawner glowingBlockSpawner)
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
        sendInfoMessage(structure);
        highlightBlocks(structure);
        return CompletableFuture.completedFuture(null);
    }

    protected void sendInfoMessage(AbstractStructure structure)
    {
        final Cuboid cuboid = structure.getCuboid();
        final Vector3Di min = cuboid.getMin();
        final Vector3Di max = cuboid.getMax();

        final Text output = textFactory.newText();

        // TODO: Localization
        output.append(localizer.getStructureType(structure.getType()), TextType.HIGHLIGHT)
              .append(" '", TextType.INFO).append(structure.getNameAndUid(), TextType.HIGHLIGHT)
              .append("' at [", TextType.INFO)
              .append(String.format("%d %d %d", min.x(), min.y(), min.z()), TextType.HIGHLIGHT)
              .append(" ; ", TextType.INFO)
              .append(String.format("%d %d %d", max.x(), max.y(), max.z()), TextType.HIGHLIGHT)
              .append("]\n", TextType.INFO)

              .append("It is currently ", TextType.INFO)
              .append(localizer.getMessage(structure.isOpen() ?
                                           "constants.open_status.open" : "constants.open_status.closed"))
              .append('\n')

              .append("Its open direction is: ", TextType.INFO)
              .append(localizer.getMessage(structure.getOpenDir().getLocalizationKey()), TextType.HIGHLIGHT)
              .append('\n')

              .append("It is ", TextType.INFO)
              .append(localizer.getMessage(structure.isLocked() ?
                                           "constants.locked_status.locked" : "constants.locked_status.unlocked"));


        getCommandSender().sendMessage(output);
    }

    protected void highlightBlocks(AbstractStructure structure)
    {
        if (!(getCommandSender() instanceof IPlayer player))
        {
            log.atSevere().withStackTrace(StackSize.FULL).log("Non-player command sender tried to highlight blocks!");
            return;
        }
        glowingBlockSpawner.spawnGlowingBlocks(structure, player, Duration.ofSeconds(3));
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
