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
import nl.pim16aap2.bigdoors.text.TextType;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetrieverFactory;

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
        getCommandSender().sendMessage(textFactory, TextType.INFO, movable.toString());
        highlightBlocks(movable);
        return CompletableFuture.completedFuture(null);
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
