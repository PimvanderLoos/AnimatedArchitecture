package nl.pim16aap2.bigdoors.core.tooluser;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.core.api.ILocation;
import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.core.managers.PowerBlockManager;
import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.text.Text;
import nl.pim16aap2.bigdoors.core.text.TextType;
import nl.pim16aap2.bigdoors.core.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.core.tooluser.step.Step;
import nl.pim16aap2.bigdoors.core.tooluser.stepexecutor.StepExecutorLocation;

import java.util.Collections;
import java.util.List;

/**
 * Represents a type of {@link ToolUser} that tries to find powerblocks based on the locations provided by the user.
 *
 * @author Pim
 */
@ToString
public class PowerBlockInspector extends ToolUser
{
    private final PowerBlockManager powerBlockManager;

    private final ITextFactory textFactory;

    /**
     * Whether this user has the bypass permission.
     * <p>
     * When this is true, the user does not have to be an owner of the structure to retrieve its location.
     */
    private final boolean bypassPermission;

    @AssistedInject
    public PowerBlockInspector(
        ToolUser.Context context, PowerBlockManager powerBlockManager, ITextFactory textFactory,
        @Assisted IPlayer player, @Assisted boolean bypassPermission)
    {
        super(context, player);
        this.powerBlockManager = powerBlockManager;
        this.textFactory = textFactory;
        this.bypassPermission = bypassPermission;
    }

    @Override
    protected void init()
    {
        giveTool("tool_user.base.stick_name", "tool_user.powerblock_inspector.stick_lore",
                 "tool_user.powerblock_inspector.init");
    }

    protected boolean inspectLoc(ILocation loc)
    {
        powerBlockManager.structuresFromPowerBlockLoc(loc.getPosition(), loc.getWorld()).thenAccept(
            lst ->
            {
                System.out.println("Found powerblocks: " + lst);
                final List<AbstractStructure> filtered;
                if (bypassPermission)
                    filtered = lst;
                else
                    filtered = lst.stream().filter(structure -> structure.isOwner(getPlayer())).toList();
                if (filtered.isEmpty())
                    getPlayer().sendError(
                        textFactory, localizer.getMessage("tool_user.power_block_inspected.error.no_structures_found"));
                else
                    sendPowerBlockInfo(getPlayer(), filtered);
            });
        return true;
    }

    private void sendPowerBlockInfo(IPlayer player, List<AbstractStructure> filtered)
    {
        final Text text = textFactory.newText();
        text.append(localizer.getMessage("tool_user.power_block_inspected.result.header"), TextType.INFO).append('\n');

        for (final AbstractStructure structure : filtered)
            text.append(" * ").append(structure.getNameAndUid(), TextType.HIGHLIGHT).append('\n');

        player.sendMessage(text);
    }

    @Override
    protected List<IStep> generateSteps()
        throws InstantiationException
    {
        final Step stepBlocksToMove = stepFactory
            .stepName("INSPECT_POWER_BLOCK")
            .messageKey("tool_user.powerblock_inspector.init")
            .stepExecutor(new StepExecutorLocation(this::inspectLoc))
            .waitForUserInput(true).construct();
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
