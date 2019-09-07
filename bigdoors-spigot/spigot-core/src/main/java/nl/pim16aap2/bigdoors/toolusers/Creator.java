package nl.pim16aap2.bigdoors.toolusers;


import nl.pim16aap2.bigdoors.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Represents a user creating a {@link DoorBase}.
 *
 * @author Pim
 **/
public abstract class Creator extends ToolUser
{
    protected DoorType type;
    protected String doorName;
    protected PBlockFace engineSide = null;
    protected boolean isOpen = false;
    protected Location one, two, engine;
    /**
     * The openDirection of the door. When set to {@link RotateDirection#NONE}, {@link
     * DoorBase#setDefaultOpenDirection()} is used instead.
     */
    @Nullable
    protected RotateDirection openDirection = RotateDirection.NONE;

    protected Creator(final @NotNull BigDoorsSpigot plugin, final @NotNull Player player,
                      final @Nullable String doorName,
                      final @NotNull DoorType type)
    {
        super(plugin, player);
        doorUID = -1;
        this.doorName = doorName;
        one = null;
        two = null;
        engine = null;
        engineSide = null;
        this.type = type;
        init();
    }

    /**
     * Initializes this {@link Creator}. Sends the appropriate messages to the initiator and potentially gives them a
     * creator tool (if a name was supplied).
     */
    protected final void init()
    {
        SpigotUtil.messagePlayer(player, getInitMessage());
        if (doorName == null)
            SpigotUtil.messagePlayer(player, messages.getString(Message.CREATOR_GENERAL_GIVENAME));
        else
            giveToolToPlayer();
    }

    /**
     * Notifies the {@link Player} that the selected rotation point is not valid.
     */
    protected void sendInvalidRotationMessage()
    {
        SpigotUtil.messagePlayer(player, messages.getString(Message.CREATOR_GENERAL_INVALIDROTATIONPOINT));
    }

    /**
     * Notifies the {@link Player} that the selected point is not valid.
     */
    protected void sendInvalidPointMessage()
    {
        SpigotUtil.messagePlayer(player, messages.getString(Message.CREATOR_GENERAL_INVALIDPOINT));
    }

    /**
     * Parses the result of {@link BigDoorsSpigot#canBreakBlocksBetweenLocs(UUID, Location, Location)} or {@link
     * BigDoorsSpigot#canBreakBlock(UUID, Location)}. If the player is not allowed to break the block(s), they'll
     * receive a message about this.
     *
     * @param canBreakBlock The result of {@link BigDoorsSpigot#canBreakBlocksBetweenLocs(UUID, Location, Location)} or
     *                      {@link BigDoorsSpigot#canBreakBlock(UUID, Location)}
     * @return True if the player is allowed to break the block(s).
     */
    private boolean hasPermission(final @NotNull Optional<String> canBreakBlock)
    {
        return !canBreakBlock.filter(
            P ->
            {
                SpigotUtil.messagePlayer(player, messages.getString(Message.ERROR_NOPERMISSIONFORLOCATION, P));
                plugin.getPLogger().sendMessageToTarget(player, Level.INFO,
                                                        messages.getString(Message.ERROR_NOPERMISSIONFORLOCATION));
                return true;
            }).isPresent();
    }

    /**
     * Send the {@link Message#CREATOR_GENERAL_AREATOOBIG} error message to a player.
     *
     * @param player    The player.
     * @param sizeLimit The size limit that was exceeded.
     */
    private void sendAreaTooBigMessage(final @NotNull Player player, final int sizeLimit)
    {
        SpigotUtil.messagePlayer(player, messages.getString(Message.CREATOR_GENERAL_AREATOOBIG,
                                                            Integer.toString(sizeLimit)));
    }

    /**
     * Constructs a new {@link DoorBase} from the provided data and removes the creator tool from the {@link Player}'s
     * inventory.
     *
     * @param message The message that will be send to the {@link Player} if the {@link DoorBase} was constructed
     *                successfully.
     */
    protected void finishUp(final @Nullable String message)
    {
        if (isReadyToConstructDoor() && !aborting)
        {
            World world = one.getWorld();
            if (world == null)
            {
                IllegalStateException e = new IllegalStateException("World of location one cannot be null!");
                plugin.getPLogger().logException(e);
                throw e;
            }
            Location min = new Location(world, one.getBlockX(), one.getBlockY(), one.getBlockZ());
            Location max = new Location(world, two.getBlockX(), two.getBlockY(), two.getBlockZ());

            if (!hasPermission(plugin.canBreakBlocksBetweenLocs(player.getUniqueId(), min, max)))
            {
                abort(false);
                return;
            }

            DoorOwner owner = new DoorOwner(doorUID, player.getUniqueId(), player.getName(), 0);

            DoorBase.DoorData doorData = new DoorBase.DoorData(min, max, engine, getPowerBlockLoc(world), world,
                                                               isOpen, openDirection);
            DoorBase door = type.getNewDoor(plugin.getPLogger(), doorUID, doorData);
            if (openDirection.equals(RotateDirection.NONE))
                door.setDefaultOpenDirection();

            door.setName(doorName);
            door.setDoorOwner(owner);
            door.setPowerBlockLocation(getPowerBlockLoc(world));
            door.setAutoClose(-1);

            final int doorSize = door.getBlockCount();
            if (plugin.getConfigLoader().maxDoorSize() <= doorSize)
            {
                sendAreaTooBigMessage(player, plugin.getConfigLoader().maxDoorSize());
                super.finishUp();
                return;
            }
            SpigotUtil.getMaxDoorSizeForPlayer(player).whenComplete(
                (sizeLimit, throwable) ->
                {
                    if (sizeLimit >= 0 && sizeLimit <= doorSize)
                        sendAreaTooBigMessage(player, sizeLimit);
                    else if (plugin.getVaultManager().buyDoor(player, type, doorSize))
                    {
                        plugin.getDatabaseManager().addDoorBase(door);
                        if (message != null)
                            SpigotUtil.messagePlayer(player, message);
                        plugin.getGlowingBlockSpawner()
                              .spawnGlowinBlock(player.getUniqueId(), world.getUID(), 30, engine.getBlockX(),
                                                engine.getBlockY(), engine.getBlockZ());
                    }
                });
        }
        super.finishUp();
    }

    /**
     * Changes the name that will be given to the object constructed in this {@link Creator}.
     *
     * @param newName The new name.
     */
    public final void setName(final @NotNull String newName)
    {
        doorName = newName;
        giveToolToPlayer();
    }

    /**
     * Makes sure that {@link Creator#one} contains the lowest coordinates and than {@link Creator#two} contains the
     * highest coordinates.
     */
    protected final void minMaxFix()
    {
        int minX = Math.min(one.getBlockX(), two.getBlockX());
        int minY = Math.min(one.getBlockY(), two.getBlockY());
        int minZ = Math.min(one.getBlockZ(), two.getBlockZ());
        int maxX = Math.max(one.getBlockX(), two.getBlockX());
        int maxY = Math.max(one.getBlockY(), two.getBlockY());
        int maxZ = Math.max(one.getBlockZ(), two.getBlockZ());
        one.setX(minX);
        one.setY(minY);
        one.setZ(minZ);
        two.setX(maxX);
        two.setY(maxY);
        two.setZ(maxZ);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void selector(final @NotNull Location loc);

    /**
     * Checks if a {@link Location} is a valid second {@link Location}.
     *
     * @param loc The {@link Location}.
     * @return True if the {@link Location} is valid.
     */
    protected abstract boolean isPosTwoValid(final @NotNull Location loc);

    /**
     * Checks if a {@link Location} is a valid one for the engine.
     *
     * @param loc The {@link Location}.
     * @return True if the {@link Location} is valid as an engine.
     */
    protected abstract boolean isEngineValid(final @NotNull Location loc);

    /**
     * Checks if the {@link DoorBase} to be created has a name already. If not, the player creating the door will
     * receive an instruction message.
     *
     * @return True is the {@link DoorBase} has a name.
     */
    protected final boolean isUnnamed()
    {
        if (doorName != null)
            return false;
        SpigotUtil.messagePlayer(player, messages.getString(Message.CREATOR_GENERAL_GIVENAME));
        return true;
    }

    /**
     * Checks if the creator has access to break blocks in the given location. If not, the player will receive a message
     * that it is not allowed.
     *
     * @param loc The location to check.
     * @return True if the creator is allowed to break blocks here.
     */
    protected final boolean creatorHasPermissionInLocation(final @NotNull Location loc)
    {
        return hasPermission(plugin.canBreakBlock(player.getUniqueId(), loc));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void triggerFinishUp()
    {
        finishUp(getSuccessMessage());
    }

    /**
     * Checks if the {@link Creator} is ready to construct the {@link DoorBase}.
     *
     * @return True if all data needed to construct a {@link DoorBase} is available.
     */
    protected abstract boolean isReadyToConstructDoor();


    /**
     * Gets the initialization message.
     *
     * @return The initialization message.
     */
    @NotNull
    protected abstract String getInitMessage();

    /**
     * Gets the success message.
     *
     * @return The success message.
     */
    @NotNull
    protected abstract String getSuccessMessage();

    /**
     * Gets the lore of the creator stick.
     *
     * @return The lore of the creator stick.
     */
    @NotNull
    protected abstract String getStickLore();

    /**
     * Gets the message that is sent to the player upon receiving the creator stick.
     *
     * @return The message that is sent to the player upon receiving the creator stick.
     */
    @NotNull
    protected abstract String getStickReceived();

    /**
     * Gets the message explaining the first step of the creation process.
     *
     * @return The message explaining the first step of the creation process.
     */
    @NotNull
    protected abstract String getStep1();

    /**
     * Gets the explanation of the second step in the creation process.
     *
     * @return The explanation of the second step in the creation process.
     */
    @NotNull
    protected abstract String getStep2();

    /**
     * Gets the explanation of the third step in the creation process.
     *
     * @return The explanation of the third step in the creation process.
     */
    @NotNull
    protected abstract String getStep3();

    /**
     * Calculate the location of the power block.
     *
     * @param world The world the power block should be in.
     * @return The location of the power block.
     */
    @NotNull
    protected Location getPowerBlockLoc(final @NotNull World world)
    {
        return new Location(world, engine.getBlockX(), engine.getBlockY() - 1, engine.getBlockZ());
    }
}
