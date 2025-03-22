package nl.pim16aap2.animatedarchitecture.core.tooluser;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import lombok.experimental.ExtensionMethod;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.managers.PowerBlockManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.text.Text;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutorLocation;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;

import java.util.Collections;
import java.util.List;

/**
 * Represents a type of {@link ToolUser} that tries to find powerblocks based on the locations provided by the user.
 */
@ToString(callSuper = true)
@Flogger
@ExtensionMethod(CompletableFutureExtensions.class)
public class PowerBlockInspector extends ToolUser
{
    @ToString.Exclude
    private final PowerBlockManager powerBlockManager;

    /**
     * Whether this user has the bypass permission.
     * <p>
     * When this is true, the user does not have to be an owner of the structure to retrieve its location.
     */
    private final boolean bypassPermission;

    @AssistedInject
    public PowerBlockInspector(
        ToolUser.Context context, PowerBlockManager powerBlockManager,
        @Assisted IPlayer player, @Assisted boolean bypassPermission)
    {
        super(context, player);
        this.powerBlockManager = powerBlockManager;
        this.bypassPermission = bypassPermission;

        giveTool(
            "tool_user.base.stick_name", "tool_user.powerblock_inspector.stick_lore",
            textFactory.newText()
                .append(localizer.getMessage("tool_user.powerblock_inspector.init"), TextType.INFO)
        );

        init();
    }

    protected boolean inspectLoc(ILocation loc)
    {
        powerBlockManager
            .structuresFromPowerBlockLoc(loc.getPosition(), loc.getWorld())
            .thenAccept(lst ->
            {
                final List<Structure> filtered;
                if (bypassPermission)
                    filtered = lst;
                else
                    filtered = lst.stream().filter(structure -> structure.isOwner(getPlayer())).toList();
                if (filtered.isEmpty())
                    getPlayer().sendError(
                        textFactory,
                        localizer.getMessage("tool_user.power_block_inspected.error.no_structures_found")
                    );
                else
                    sendPowerBlockInfo(getPlayer(), filtered);
            }).handleExceptional(ex ->
                log.atSevere().withCause(ex).log("Failed to inspect location %s for power blocks!", loc.getPosition()));
        return true;
    }

    private void sendPowerBlockInfo(IPlayer player, List<Structure> filtered)
    {
        final Text text = textFactory.newText();
        text.append(localizer.getMessage("tool_user.power_block_inspected.result.header"), TextType.INFO).append('\n');

        for (final Structure structure : filtered)
            text.append(" * ").append(structure.getNameAndUid(), TextType.HIGHLIGHT).append('\n');

        player.sendMessage(text);
    }

    @Override
    protected List<Step> generateSteps()
        throws InstantiationException
    {
        final Step stepBlocksToMove = stepFactory
            .stepName("INSPECT_POWER_BLOCK")
            .messageKey("tool_user.powerblock_inspector.init")
            .stepExecutor(new StepExecutorLocation(this::inspectLoc))
            .waitForUserInput(true)
            .construct();
        return Collections.singletonList(stepBlocksToMove);
    }

    @AssistedFactory
    public interface IFactory
    {
        PowerBlockInspector create(IPlayer player, boolean bypassPermission);

        default PowerBlockInspector create(IPlayer player)
        {
            return create(player, false);
        }
    }
}
