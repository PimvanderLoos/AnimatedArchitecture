package nl.pim16aap2.bigdoors.tooluser;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.managers.PowerBlockManager;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.text.Text;
import nl.pim16aap2.bigdoors.text.TextType;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.tooluser.step.Step;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorPLocation;

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
     * When this is true, the user does not have to be an owner of the movable to retrieve its location.
     */
    private final boolean bypassPermission;

    @AssistedInject
    public PowerBlockInspector(
        ToolUser.Context context, PowerBlockManager powerBlockManager, ITextFactory textFactory,
        @Assisted IPPlayer player, @Assisted boolean bypassPermission)
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

    protected boolean inspectLoc(IPLocation loc)
    {
        powerBlockManager.movablesFromPowerBlockLoc(loc.getPosition(), loc.getWorld()).thenAccept(
            lst ->
            {
                System.out.println("Found powerblocks: " + lst);
                final List<AbstractMovable> filtered;
                if (bypassPermission)
                    filtered = lst;
                else
                    filtered = lst.stream().filter(movable -> movable.isOwner(getPlayer())).toList();
                if (filtered.isEmpty())
                    getPlayer().sendError(
                        textFactory, localizer.getMessage("tool_user.power_block_inspected.error.no_movables_found"));
                else
                    sendPowerBlockInfo(getPlayer(), filtered);
            });
        return true;
    }

    private void sendPowerBlockInfo(IPPlayer player, List<AbstractMovable> filtered)
    {
        final Text text = textFactory.newText();
        text.append(localizer.getMessage("tool_user.power_block_inspected.result.header"), TextType.INFO).append('\n');

        for (final AbstractMovable movable : filtered)
            text.append(" * ").append(movable.getNameAndUid(), TextType.HIGHLIGHT).append('\n');

        player.sendMessage(text);
    }

    @Override
    protected List<IStep> generateSteps()
        throws InstantiationException
    {
        final Step stepBlocksToMove = stepFactory
            .stepName("INSPECT_POWER_BLOCK")
            .messageKey("tool_user.powerblock_inspector.init")
            .stepExecutor(new StepExecutorPLocation(this::inspectLoc))
            .waitForUserInput(true).construct();
        return Collections.singletonList(stepBlocksToMove);
    }

    @AssistedFactory
    public interface IFactory
    {
        PowerBlockInspector create(IPPlayer player, boolean bypassPermission);

        default PowerBlockInspector create(IPPlayer player)
        {
            return create(player, false);
        }
    }
}