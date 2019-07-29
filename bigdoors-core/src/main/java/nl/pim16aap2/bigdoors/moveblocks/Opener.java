package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.DoorToggleResult;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents a class that can toggle doors.
 */
public abstract class Opener
{
    protected final @NotNull BigDoors plugin;

    protected Opener(final @NotNull BigDoors plugin)
    {
        this.plugin = plugin;
    }

    /**
     * Abort an attempt to toggle a {@link DoorBase}.
     *
     * @param door   The {@link DoorBase}.
     * @param result The reason
     * @return The result.
     */
    protected final @NotNull DoorToggleResult abort(final @NotNull DoorBase door,
                                                    final @NotNull DoorToggleResult result)
    {
        // If the reason the toggle attempt was cancelled was because it was busy, it should obviously
        // not reset the busy status of this door. However, in every other case it should, because the door is
        // registered as busy before all the other checks take place.
        if (!result.equals(DoorToggleResult.BUSY))
            plugin.getDatabaseManager().setDoorAvailable(door.getDoorUID());
        return result;
    }

    /**
     * Checks if the size of a {@link DoorBase} exceeds the global limit.
     *
     * @param door The {@link DoorBase}.
     * @return True if the size of a {@link DoorBase} exceeds the global limit.
     */
    protected final boolean isTooBig(final @NotNull DoorBase door)
    {
        // Make sure the doorSize does not exceed the total doorSize.
        // If it does, open the door instantly.
        int maxDoorSize = plugin.getConfigLoader().maxDoorSize();
        if (maxDoorSize != -1)
            return door.getBlockCount() > maxDoorSize;
        return false;
    }

    /**
     * Parses the result of {@link BigDoors#canBreakBlocksBetweenLocs(UUID, Location, Location)}. If the player is not
     * allowed to break the block(s), they'll receive a message about this.
     *
     * @param door The {@link DoorBase} being opened.
     * @param loc1 The first location of the area to check.
     * @param loc2 The second location of the area to check.
     * @return True if the player is allowed to break the block(s).
     */
    protected boolean canBreakBlocksBetweenLocs(final @NotNull DoorBase door, final @NotNull Location loc1,
                                                final @NotNull Location loc2)
    {
        // If the returned value is an empty Optional, the player is allowed to break blocks.
        return plugin.canBreakBlocksBetweenLocs(door.getPlayerUUID(), loc1, loc2).map(
            PROT ->
            {
                plugin.getPLogger()
                      .warn("Player \"" + door.getPlayerUUID().toString() + "\" is not allowed to open door " +
                                door.getName() + " (" + door.getDoorUID() + ") here! Reason: " + PROT);
                return false;
            }).orElse(true);
    }

    /**
     * Checks if an area is empty. "Empty" here means that there no blocks that are not air or liquid.
     *
     * @param min        The lower bound location of the area.
     * @param max        The upper bound location of the area.
     * @param playerUUID The {@link UUID} of the {@link org.bukkit.entity.Player} to notify of violations. May be null.
     * @return True if the location is not empty.
     */
    boolean isLocationEmpty(final @NotNull Location min, final @NotNull Location max,
                            final @Nullable UUID playerUUID)
    {
        boolean isEmpty = true;
        for (int xAxis = min.getBlockX(); xAxis <= max.getBlockX(); ++xAxis)
            for (int yAxis = min.getBlockY(); yAxis <= max.getBlockY(); ++yAxis)
                for (int zAxis = min.getBlockZ(); zAxis <= max.getBlockZ(); ++zAxis)
                    if (!SpigotUtil.isAirOrLiquid(min.getWorld().getBlockAt(xAxis, yAxis, zAxis)))
                    {
                        if (playerUUID == null)
                            return false;
                        plugin.getGlowingBlockSpawner()
                              .spawnGlowinBlock(playerUUID, min.getWorld().getName(), 10, xAxis, yAxis, zAxis,
                                                ChatColor.RED);
                        isEmpty = false;
                    }
        return isEmpty;
    }

    /**
     * Checks if a {@link DoorBase} is busy and set it to busy if that is the case.
     *
     * @param doorUID The UID of the {@link DoorBase} to check.
     * @return True if already busy.
     */
    private boolean isBusySetIfNot(final long doorUID)
    {
        if (plugin.getDatabaseManager().isDoorBusy(doorUID))
            return true;
        plugin.getDatabaseManager().setDoorBusy(doorUID);
        return false;
    }

    /**
     * Checks if a {@link DoorBase} can be toggled or not.
     * <p>
     * It checks the following items:
     * <p>
     * - The {@link DoorBase} is not already being animated.
     * <p>
     * - The {@link DoorType} is ensabled.
     * <p>
     * - The {@link DoorBase} is not locked.
     * <p>
     * - All chunks this {@link DoorBase} might interact with are loaded.
     *
     * @param door         The {@link DoorBase}.
     * @param playerToggle Whether this toggle event was requested by a player (as opposed to redstone).
     * @return {@link DoorToggleResult#SUCCESS} if it can be toggled
     */
    protected final @NotNull DoorToggleResult canBeToggled(final @NotNull DoorBase door, final boolean playerToggle)
    {
        if (isBusySetIfNot(door.getDoorUID()))
        {
            if (!playerToggle)
                plugin.getPLogger().warn("Door " + door.getName() + " is not available right now!");
            return DoorToggleResult.BUSY;
        }

        if (door.isLocked())
            return DoorToggleResult.LOCKED;
        if (!DoorType.isEnabled(door.getType()))
            return DoorToggleResult.TYPEDISABLED;

        if (!chunksLoaded(door))
        {
            plugin.getPLogger().warn(ChatColor.RED + "Chunk for door " + door.getName() + " is not loaded!");
            return DoorToggleResult.ERROR;
        }

        return DoorToggleResult.SUCCESS;
    }

    /**
     * Checks if the {@link org.bukkit.Chunk}s a {@link DoorBase} might interact with are loaded. If they aren't loaded
     * try to load them.
     *
     * @param door The {@link DoorBase}.
     * @return True if all {@link org.bukkit.Chunk}s this {@link DoorBase} might interact with are loaded or have been
     * loaded.
     */
    private boolean chunksLoaded(final @NotNull DoorBase door)
    {
        // Return true if the chunk at the max and at the min of the chunks were loaded correctly.
        if (door.getWorld() == null)
        {
            plugin.getPLogger().warn("World is null for door \"" + door.getName() + "\"");
            return false;
        }

        // Try to load doors and return if successful.
        return door.getWorld().getChunkAt(door.getMaximum()).load() &&
            door.getWorld().getChunkAt(door.getMinimum()).isLoaded();
    }

    /**
     * Attempts to open a {@link DoorBase}.
     *
     * @param playerUUID The {@link UUID} of the player initiating the toggle or the original creator if the {@link
     *                   DoorBase} was not toggled by a {@link org.bukkit.entity.Player}.
     * @param door       The {@link DoorBase} to toggle.
     * @param time       The amount of time this {@link DoorBase} will try to use to move. The maximum speed is limited,
     *                   so at a certain point lower values will not increase door speed.
     * @return The result of the action. {@link DoorToggleResult#SUCCESS} is returned if everything went fine.
     */
    @NotNull
    public final DoorToggleResult toggleDoor(final @NotNull UUID playerUUID, final @NotNull DoorBase door,
                                             final double time)
    {
        return toggleDoor(playerUUID, door, time, false, false);
    }


    /**
     * Attempts to open a {@link DoorBase}.
     *
     * @param playerUUID   The {@link UUID} of the player initiating the toggle or the original creator if the {@link
     *                     DoorBase} was not toggled by a {@link org.bukkit.entity.Player}.
     * @param door         The {@link DoorBase} to toggle.
     * @param time         The amount of time this {@link DoorBase} will try to use to move. The maximum speed is
     *                     limited, so at a certain point lower values will not increase door speed.
     * @param instantOpen  If the {@link DoorBase} should be opened instantly (i.e. skip animation) or not.
     * @param playerToggle Whether this toggle event was requested by a player (as opposed to redstone).
     * @return The result of the action. {@link DoorToggleResult#SUCCESS} is returned if everything went fine.
     */
    @NotNull
    public abstract DoorToggleResult toggleDoor(final @NotNull UUID playerUUID, final @NotNull DoorBase door,
                                                final double time, final boolean instantOpen,
                                                final boolean playerToggle);
}
