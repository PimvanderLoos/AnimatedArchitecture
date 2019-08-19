package nl.pim16aap2.bigdoors.toolusers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.Vector3D;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a user relocating the power block of a {@link DoorBase}.
 *
 * @author Pim
 **/
public class PowerBlockRelocator extends ToolUser
{
    protected Location newLoc = null;
    @NotNull
    private final Location oldLoc;

    public PowerBlockRelocator(final @NotNull BigDoors plugin, final @NotNull Player player, final long doorUID,
                               final @NotNull Location oldLoc)
    {
        super(plugin, player);
        this.doorUID = doorUID;
        this.oldLoc = oldLoc;
        giveToolToPlayer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getToolReceivedMessage()
    {
        return messages.getString(Message.CREATOR_PBRELOCATOR_INIT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getToolLore()
    {
        return messages.getString(Message.CREATOR_PBRELOCATOR_STICKLORE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void triggerFinishUp()
    {
        if (newLoc != null)
        {
            Vector3D oldPos = new Vector3D(oldLoc.getBlockX(), oldLoc.getBlockY(), oldLoc.getBlockZ());
            Vector3D newPos = new Vector3D(newLoc.getBlockX(), newLoc.getBlockY(), newLoc.getBlockZ());
            plugin.getPowerBlockManager().updatePowerBlockLoc(doorUID, newLoc.getWorld().getUID(), oldPos, newPos);
            SpigotUtil.messagePlayer(player, messages.getString(Message.CREATOR_PBRELOCATOR_SUCCESS));
        }
        finishUp();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selector(final @NotNull Location loc)
    {
        newLoc = loc;
        setIsDone(true);
        plugin.getGlowingBlockSpawner()
              .spawnGlowinBlock(getPlayer().getUniqueId(), loc.getWorld().getName(), 10, loc.getBlockX(),
                                loc.getBlockY(), loc.getBlockZ(), ChatColor.GREEN);
    }
}
