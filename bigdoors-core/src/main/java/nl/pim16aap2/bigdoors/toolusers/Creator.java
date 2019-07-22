package nl.pim16aap2.bigdoors.toolusers;


import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
    protected PBlockFace engineSide = null;
    protected boolean isOpen = false;
    protected Location one, two, engine;

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

    protected final void init()
    {
        SpigotUtil.messagePlayer(player, getInitMessage());
        if (doorName == null)
            SpigotUtil.messagePlayer(player, messages.getString(Message.CREATOR_GENERAL_GIVENAME));
        else
            triggerGiveTool();
    }

    protected void sendInvalidRotationMessage()
    {
        SpigotUtil.messagePlayer(player, messages.getString(Message.CREATOR_GENERAL_INVALIDROTATIONPOINT));
    }

    protected void sendInvalidPointMessage()
    {

        SpigotUtil.messagePlayer(player, messages.getString(Message.CREATOR_GENERAL_INVALIDPOINT));
    }

    protected void sendNoPermissionForLocationMessage(String hook)
    {
        SpigotUtil.messagePlayer(player, messages.getString(Message.ERROR_NOPERMISSIONFORLOCATION, hook));
    }


    // Final cleanup and door creation.
    protected void finishUp(String message)
    {
        if (isReadyToCreateDoor() && !aborting)
        {
            World world = one.getWorld();
            Location min = new Location(world, one.getBlockX(), one.getBlockY(), one.getBlockZ());
            Location max = new Location(world, two.getBlockX(), two.getBlockY(), two.getBlockZ());

            String canBreakBlock = plugin.canBreakBlocksBetweenLocs(player.getUniqueId(), min, max);
            if (canBreakBlock != null)
            {
                SpigotUtil.messagePlayer(player,
                                         messages.getString(Message.ERROR_NOPERMISSIONFORLOCATION, canBreakBlock));
                abort(false);
                return;
            }

            DoorBase door = type.getNewDoor(plugin.getPLogger(), doorUID);
            DoorOwner owner = new DoorOwner(doorUID, player.getUniqueId(), player.getName(), 0);
            door.setName(doorName);
            door.setWorld(world);
            door.setDoorOwner(owner);
            door.setMinimum(min);
            door.setMaximum(max);
            if (engineSide != null)
                door.setEngineSide(engineSide);
            door.setEngineLocation(new Location(world, engine.getBlockX(), engine.getBlockY() - 1, engine.getBlockZ()));
            door.setPowerBlockLocation(getPowerBlockLoc(world));
            door.setAutoClose(-1);
            door.setOpenStatus(isOpen);

            door.setDefaultOpenDirection();

            int doorSize = door.getBlockCount();
            int sizeLimit = SpigotUtil.getMaxDoorSizeForPlayer(player);

            if (sizeLimit >= 0 && sizeLimit <= doorSize)
                SpigotUtil.messagePlayer(player, messages.getString(Message.CREATOR_GENERAL_AREATOOBIG,
                                                                    Integer.toString(sizeLimit)));
            else if (plugin.getVaultManager().buyDoor(player, type, doorSize))
            {
                plugin.getDatabaseManager().addDoorBase(door);
                if (message != null)
                    SpigotUtil.messagePlayer(player, message);
                plugin.getGlowingBlockSpawner()
                      .spawnGlowinBlock(player.getUniqueId(), world.getName(), 30, engine.getBlockX(),
                                        engine.getBlockY(), engine.getBlockZ());
            }
        }
        super.finishUp();
    }

    public final String getName()
    {
        return doorName;
    }

    public final void setName(String newName)
    {
        doorName = newName;
        triggerGiveTool();
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

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void selector(Location loc);

    /**
     * Checks if the door to be created has a name already. If not, the player creating the door will receive an
     * instruction message.
     *
     * @return True is the door has a name.
     */
    protected final boolean hasName()
    {
        if (doorName != null)
            return true;
        SpigotUtil.messagePlayer(player, messages.getString(Message.CREATOR_GENERAL_GIVENAME));
        return false;
    }

    /**
     * Checks if the creator has access to break blocks in the given location. If not, the player will receive a message
     * that it is not allowed.
     *
     * @param loc The location to check.
     * @return True if the creator is allowed to break blocks here.
     */
    protected final boolean creatorHasPermissionInLocation(Location loc)
    {
        String canBreakBlock = plugin.canBreakBlock(player.getUniqueId(), loc);
        if (canBreakBlock != null)
        {
            sendNoPermissionForLocationMessage(canBreakBlock);
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void triggerGiveTool()
    {
        giveToolToPlayer(getStickLore().split("\n"),
                         getStickReceived().split("\n"));
        SpigotUtil.messagePlayer(player, getStep1());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void triggerFinishUp()
    {
        finishUp(getSuccessMessage());
    }

    // Check if all the variables that cannot be null are not null.
    protected abstract boolean isReadyToCreateDoor();


    /**
     * Gets the initialization message.
     *
     * @return The initialization message.
     */
    protected abstract @NotNull String getInitMessage();

    /**
     * Gets the success message.
     *
     * @return The success message.
     */
    protected abstract @NotNull String getSuccessMessage();

    /**
     * Gets the lore of the creator stick.
     *
     * @return The lore of the creator stick.
     */
    protected abstract @NotNull String getStickLore();

    /**
     * Gets the message that is sent to the player upon receiving the creator stick.
     *
     * @return The message that is sent to the player upon receiving the creator stick.
     */
    protected abstract @NotNull String getStickReceived();

    /**
     * Gets the message explaining the first step of the creation process.
     *
     * @return The message explaining the first step of the creation process.
     */
    protected abstract @NotNull String getStep1();

    /**
     * Gets the explanation of the second step in the creation process.
     *
     * @return The explanation of the second step in the creation process.
     */
    protected abstract @NotNull String getStep2();

    /**
     * Gets the explanation of the third step in the creation process.
     *
     * @return The explanation of the third step in the creation process.
     */
    protected abstract @NotNull String getStep3();

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
}
