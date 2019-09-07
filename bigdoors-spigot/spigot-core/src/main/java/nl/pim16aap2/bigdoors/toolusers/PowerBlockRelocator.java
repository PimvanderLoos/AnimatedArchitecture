package nl.pim16aap2.bigdoors.toolusers;

import nl.pim16aap2.bigdoors.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
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

    public PowerBlockRelocator(final @NotNull BigDoorsSpigot plugin, final @NotNull Player player, final long doorUID,
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
            Vector3Di oldPos = new Vector3Di(oldLoc.getBlockX(), oldLoc.getBlockY(), oldLoc.getBlockZ());
            Vector3Di newPos = new Vector3Di(newLoc.getBlockX(), newLoc.getBlockY(), newLoc.getBlockZ());
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
              .spawnGlowinBlock(getPlayer().getUniqueId(), loc.getWorld().getUID(), 10, loc.getBlockX(),
                                loc.getBlockY(), loc.getBlockZ(), ChatColor.GREEN);
    }
}
