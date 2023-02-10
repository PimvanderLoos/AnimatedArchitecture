package nl.pim16aap2.bigdoors.core.api;

import nl.pim16aap2.bigdoors.core.structuretypes.StructureType;

import java.util.OptionalDouble;

public interface IEconomyManager
{
    /**
     * Buys a structure for a player.
     *
     * @param player
     *     The player whose bank account to use.
     * @param world
     *     The world the structure is in.
     * @param type
     *     The {@link StructureType} of the structure.
     * @param blockCount
     *     The number of blocks in the structure.
     * @return True if the player bought the structure successfully.
     */
    boolean buyStructure(IPlayer player, IWorld world, StructureType type, int blockCount);

    /**
     * Gets the price of {@link StructureType} for a specific number of blocks.
     *
     * @param type
     *     The {@link StructureType}.
     * @param blockCount
     *     The number of blocks.
     * @return The price of this {@link StructureType} with this number of blocks.
     */
    OptionalDouble getPrice(StructureType type, int blockCount);

    /**
     * Checks if the economy manager is enabled.
     *
     * @return True if the economy manager is enabled.
     */
    boolean isEconomyEnabled();
}
