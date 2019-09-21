package nl.pim16aap2.bigdoors.spigot.toolusers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.PColor;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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

    public PowerBlockRelocator(final @NotNull BigDoorsSpigot plugin, final @NotNull Player player, final long doorUID,
                               final @NotNull Vector3Di oldLoc)
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
            Vector3Di oldPos = new Vector3Di(oldLoc.getX(), oldLoc.getY(), oldLoc.getZ());
            Vector3Di newPos = new Vector3Di(newLoc.getBlockX(), newLoc.getBlockY(), newLoc.getBlockZ());
            BigDoors.get().getPowerBlockManager()
                    .updatePowerBlockLoc(doorUID, newLoc.getWorld().getUID(), oldPos, newPos);
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
        // TODO: Make sure it's in the same world!
        newLoc = loc;
        setIsDone(true);
        plugin.getGlowingBlockSpawner()
              .spawnGlowinBlock(pPlayer, loc.getWorld().getUID(), 10, loc.getBlockX(),
                                loc.getBlockY(), loc.getBlockZ(), PColor.GREEN);
    }
}
