package nl.pim16aap2.bigdoors.commands;

import com.google.common.flogger.StackSize;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.GlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableAttribute;
import nl.pim16aap2.bigdoors.text.Text;
import nl.pim16aap2.bigdoors.text.TextType;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetrieverFactory;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the information command that provides the issuer with more information about the movable.
 *
 * @author Pim
 */
@ToString
@Flogger
public class Info extends MovableTargetCommand
{
    private final GlowingBlockSpawner glowingBlockSpawner;

    @AssistedInject //
    Info(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        @Assisted MovableRetriever movableRetriever, GlowingBlockSpawner glowingBlockSpawner)
    {
        super(commandSender, localizer, textFactory, movableRetriever, MovableAttribute.INFO);
        this.glowingBlockSpawner = glowingBlockSpawner;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.INFO;
    }

    @Override
    protected CompletableFuture<?> performAction(AbstractMovable movable)
    {
        sendInfoMessage(movable);
        highlightBlocks(movable);
        return CompletableFuture.completedFuture(null);
    }

    protected void sendInfoMessage(AbstractMovable movable)
    {
        final Cuboid cuboid = movable.getCuboid();
        final Vector3Di min = cuboid.getMin();
        final Vector3Di max = cuboid.getMax();

        final Text output = textFactory.newText();

        // TODO: Localization
        output.append(localizer.getMovableType(movable.getType()), TextType.HIGHLIGHT)
              .append(" '", TextType.INFO).append(movable.getName(), TextType.HIGHLIGHT)
              .append("' at [", TextType.INFO)
              .append(String.format("%d %d %d", min.x(), min.y(), min.z()), TextType.HIGHLIGHT)
              .append(" ; ", TextType.INFO)
              .append(String.format("%d %d %d", max.x(), max.y(), max.z()), TextType.HIGHLIGHT)
              .append("]\n", TextType.INFO)

              .append("It is currently ", TextType.INFO)
              .append(localizer.getMessage(movable.isOpen() ?
                                           "constants.open_status.open" : "constants.open_status.closed"))
              .append('\n')

              .append("Its open direction is: ", TextType.INFO).append("counter-clockwise\n", TextType.HIGHLIGHT)

              .append("It is ", TextType.INFO)
              .append(localizer.getMessage(movable.isLocked() ?
                                           "constants.locked_status.locked" : "constants.locked_status.unlocked"));


        getCommandSender().sendMessage(output);
    }

    protected void highlightBlocks(AbstractMovable movable)
    {
        if (!(getCommandSender() instanceof IPPlayer player))
        {
            log.atSevere().withStackTrace(StackSize.FULL).log("Non-player command sender tried to highlight blocks!");
            return;
        }
        glowingBlockSpawner.spawnGlowingBlocks(movable, player, Duration.ofSeconds(3));
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link Info} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for retrieving the movable info and the receiver of the movable's
         *     information.
         * @param movableRetriever
         *     A {@link MovableRetrieverFactory} representing the {@link AbstractMovable} for which the information will
         *     be retrieved.
         * @return See {@link BaseCommand#run()}.
         */
        Info newInfo(ICommandSender commandSender, MovableRetriever movableRetriever);
    }
}
