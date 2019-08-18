package nl.pim16aap2.bigdoors.toolusers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
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

    public PowerBlockRelocator(final @NotNull BigDoors plugin, final @NotNull Player player, final long doorUID)
    {
        super(plugin, player);
        this.doorUID = doorUID;
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
            plugin.getDatabaseManager().updatePowerBlockLoc(doorUID, newLoc);
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
