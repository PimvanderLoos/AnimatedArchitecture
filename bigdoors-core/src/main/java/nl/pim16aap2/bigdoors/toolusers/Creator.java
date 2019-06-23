package nl.pim16aap2.bigdoors.toolusers;


import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.spigotutil.DoorOwner;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.MyBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;

/**
 * Represents a ToolUser that creates new doors.
 *
 * @author Pim
 * @see ToolUser
 */
public abstract class Creator extends ToolUser
{
    protected DoorType type;
    protected String doorName;
    protected MyBlockFace engineSide = null;
    protected boolean isOpen = false;
    protected Location one, two, engine;
    protected RotateDirection openDir = null;

    protected Creator(BigDoors plugin, Player player, String doorName, DoorType type)
    {
        super(plugin, player);
        doorUID = -1;
        this.doorName = doorName;
        one = null;
        two = null;
        engine = null;
        engineSide = null;
        this.type = type;
    }

    /**
     * Calculate the location of the power block.
     *
     * @param world The world the power block should be in.
     * @return The location of the power block.
     */
    protected Location getPowerBlockLoc(World world)
    {
        return new Location(world, engine.getBlockX(), engine.getBlockY() - 1, engine.getBlockZ());
    }

    /**
     * Set the openDirection of the door.
     * @deprecated This should use {@link nl.pim16aap2.bigdoors.doors.DoorBase#setDefaultOpenDirection()}.
     */
    @Deprecated
    protected abstract void setOpenDirection();

    // Final cleanup and door creation.
    protected void finishUp(String message)
    {
        setOpenDirection();
        if (isReadyToCreateDoor() && !aborting)
        {
            World world     = one.getWorld();
            Location min    = new Location(world, one.getBlockX(), one.getBlockY(), one.getBlockZ());
            Location max    = new Location(world, two.getBlockX(), two.getBlockY(), two.getBlockZ());

            String canBreakBlock = plugin.canBreakBlocksBetweenLocs(player.getUniqueId(), min, max);
            if (canBreakBlock != null)
            {
                SpigotUtil.messagePlayer(player, messages.getString("CREATOR.GENERAL.NoPermissionHere") + " " + canBreakBlock);
                this.abort(false);
                return;
            }

            DoorBase door = type.getNewDoor(plugin, doorUID);
            DoorOwner owner = new DoorOwner(doorUID, player.getUniqueId(), player.getName(), 0);
            door.setName(doorName);
            door.setWorld(world);
            door.setDoorOwner(owner);
            door.setMinimum(min);
            door.setMaximum(max);
            if (engineSide != null)
                door.setEngineSide(engineSide);
            door.setEngineLocation(new Location(world, engine.getBlockX(), engine.getBlockY(), engine.getBlockZ()));
            door.setPowerBlockLocation(getPowerBlockLoc(world));
            door.setAutoClose(-1);
            door.setOpenStatus(isOpen);

            door.setDefaultOpenDirection();

            int doorSize = door.getBlockCount();
            int sizeLimit = SpigotUtil.getMaxDoorSizeForPlayer(player);

            if (sizeLimit >= 0 && sizeLimit <= doorSize)
                SpigotUtil.messagePlayer(player, messages.getString("CREATOR.GENERAL.TooManyBlocks") + " " + sizeLimit);
            else if (plugin.getVaultManager().buyDoor(player, type, doorSize))
            {
                plugin.getDatabaseManager().addDoorBase(door);
                if (message != null)
                    SpigotUtil.messagePlayer(player, message);
            }
        }
        super.finishUp();
    }

    public final void setName(String newName)
    {
        doorName = newName;
        triggerGiveTool();
    }

    public final String getName()
    {
        return doorName;
    }

    // Make sure position "one" contains the minimum values, "two" the maximum
    // values and engine min.Y;
    protected final void minMaxFix()
    {
        int minX = one.getBlockX();
        int minY = one.getBlockY();
        int minZ = one.getBlockZ();
        int maxX = two.getBlockX();
        int maxY = two.getBlockY();
        int maxZ = two.getBlockZ();

        one.setX(minX > maxX ? maxX : minX);
        one.setY(minY > maxY ? maxY : minY);
        one.setZ(minZ > maxZ ? maxZ : minZ);
        two.setX(minX < maxX ? maxX : minX);
        two.setY(minY < maxY ? maxY : minY);
        two.setZ(minZ < maxZ ? maxZ : minZ);
    }

    @Override
    public abstract void selector(Location loc);

    @Override
    protected abstract void triggerGiveTool();

    @Override
    protected abstract void triggerFinishUp();

    // Check if all the variables that cannot be null are not null.
    protected abstract boolean isReadyToCreateDoor();
}
