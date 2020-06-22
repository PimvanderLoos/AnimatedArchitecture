package nl.pim16aap2.bigdoors.spigot.toolusers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IGlowingBlock;
import nl.pim16aap2.bigdoors.api.PColor;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a user relocating the power block of a {@link AbstractDoorBase}.
 *
 * @author Pim
 **/
public class PowerBlockRelocator extends ToolUser
{
    protected Location newLoc = null;
    @NotNull
    private final Vector3Di oldLoc;
    @NotNull
    private final AbstractDoorBase door;
    @Nullable
    private final IGlowingBlock glowingBlock;

    public PowerBlockRelocator(final @NotNull BigDoorsSpigot plugin, final @NotNull Player player,
                               final @NotNull AbstractDoorBase door, final @NotNull Vector3Di oldLoc,
                               final int time)
    {
        super(plugin, player);
        this.door = door;
        this.oldLoc = oldLoc;
        giveToolToPlayer();

        glowingBlock = plugin.getGlowingBlockSpawner()
                             .spawnGlowinBlock(SpigotAdapter.wrapPlayer(player), player.getWorld().getUID(), time,
                                               door.getPowerBlockLoc().getX(), door.getPowerBlockLoc().getY(),
                                               door.getPowerBlockLoc().getZ(), PColor.GOLD);
    }

    /** {@inheritDoc} */
    @Override
    protected @NotNull String getToolReceivedMessage()
    {
        return messages.getString(Message.CREATOR_PBRELOCATOR_INIT);
    }

    /** {@inheritDoc} */
    @Override
    protected @NotNull String getToolLore()
    {
        return messages.getString(Message.CREATOR_PBRELOCATOR_STICKLORE);
    }

    /** {@inheritDoc} */
    @Override
    protected void triggerFinishUp()
    {
        if (newLoc != null)
        {
            final Vector3Di oldPos = new Vector3Di(oldLoc.getX(), oldLoc.getY(), oldLoc.getZ());
            final Vector3Di newPos = new Vector3Di(newLoc.getBlockX(), newLoc.getBlockY(), newLoc.getBlockZ());
            BigDoors.get().getPowerBlockManager().updatePowerBlockLoc(door, oldPos, newPos);
            SpigotUtil.messagePlayer(player, messages.getString(Message.CREATOR_PBRELOCATOR_SUCCESS));
        }
        finishUp();
    }

    /** {@inheritDoc} */
    @Override
    public void selector(final @NotNull Location loc)
    {
        if (!door.getWorld().getUID().equals(loc.getWorld().getUID()))
        {
            SpigotUtil.messagePlayer(player, messages.getString(Message.CREATOR_PBRELOCATOR_LOCATIONNOTINSAMEWORLD));
            return;
        }
        newLoc = loc;
        setIsDone(true);

        if (glowingBlock != null)
            glowingBlock.kill();

        plugin.getGlowingBlockSpawner()
              .spawnGlowinBlock(pPlayer, loc.getWorld().getUID(), 10, loc.getBlockX(),
                                loc.getBlockY(), loc.getBlockZ(), PColor.GOLD);
    }
}
