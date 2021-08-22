package nl.pim16aap2.bigdoors.api;

import nl.pim16aap2.bigdoors.doortypes.DoorType;

import java.util.OptionalDouble;

public interface IEconomyManager
{
    /**
     * Buys a door for a player.
     *
     * @param player
     *     The player whose bank account to use.
     * @param world
     *     The world the door is in.
     * @param type
     *     The {@link DoorType} of the door.
     * @param blockCount
     *     The number of blocks in the door.
     * @return True if the player bought the door successfully.
     */
    boolean buyDoor(IPPlayer player, IPWorld world, DoorType type, int blockCount);

    /**
     * Gets the price of {@link DoorType} for a specific number of blocks.
     *
     * @param type
     *     The {@link DoorType}.
     * @param blockCount
     *     The number of blocks.
     * @return The price of this {@link DoorType} with this number of blocks.
     */
    OptionalDouble getPrice(DoorType type, int blockCount);

    /**
     * Checks if the economy manager is enabled.
     *
     * @return True if the economy manager is enabled.
     */
    boolean isEconomyEnabled();
}
