package nl.pim16aap2.bigdoors.toolusers;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.util.DoorType;
import nl.pim16aap2.bigdoors.util.MyBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;

public abstract class Creator extends ToolUser
{
    protected DoorType type;
    protected String name;
    protected MyBlockFace engineSide = null;
    protected boolean isOpen = false;
    protected Location one, two, engine;
    protected RotateDirection openDir = null;

    protected Creator(BigDoors plugin, Player player, String name, DoorType type)
    {
        super(plugin, player);
        this.name = name;
        one = null;
        two = null;
        engine = null;
        engineSide = null;
        this.type = type;
    }

    // Final cleanup and door creation.
    protected void finishUp(String message)
    {
        if (isReadyToCreateDoor() && !aborting)
        {
            World world     = one.getWorld();
            Location min    = new Location(world, one.getBlockX(), one.getBlockY(), one.getBlockZ());
            Location max    = new Location(world, two.getBlockX(), two.getBlockY(), two.getBlockZ());
            Location engine = new Location(world, this.engine.getBlockX(), this.engine.getBlockY(), this.engine.getBlockZ());
            Location powerB = new Location(world, this.engine.getBlockX(), this.engine.getBlockY() - 1, this.engine.getBlockZ());

            String canBreakBlock = plugin.canBreakBlocksBetweenLocs(player.getUniqueId(), min, max);
            if (canBreakBlock != null)
            {
                Util.messagePlayer(player, messages.getString("CREATOR.GENERAL.NoPermissionHere") + " " + canBreakBlock);
                this.abort(false);
                return;
            }

            Door door = new Door(player.getUniqueId(), world, min, max, engine, name, isOpen, -1, false,
                                 0, type, engineSide, powerB, openDir, -1);

            int doorSize = door.getBlockCount();
            int sizeLimit = Util.getMaxDoorSizeForPlayer(player);

            if (sizeLimit >= 0 && sizeLimit <= doorSize)
                Util.messagePlayer(player, messages.getString("CREATOR.GENERAL.TooManyBlocks") + " " + sizeLimit);
            else if (plugin.getVaultManager().buyDoor(player, type, doorSize))
            {
                plugin.getDatabaseManager().addDoor(door);
                if (message != null)
                    Util.messagePlayer(player, message);
            }
        }
        super.finishUp();
    }

    public final void setName(String newName)
    {
        name = newName;
        triggerGiveTool();
    }

    public final String getName()
    {
        return name;
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
