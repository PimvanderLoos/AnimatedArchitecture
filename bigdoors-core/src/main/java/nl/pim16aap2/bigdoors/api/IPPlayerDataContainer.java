package nl.pim16aap2.bigdoors.api;

import lombok.NonNull;

import java.util.UUID;

/**
 * Represents a container for player data.
 *
 * @author Pim
 */
interface IPPlayerDataContainer
{
    /**
     * Gets the name of this player.
     *
     * @return The name of this player.
     */
    @NonNull String getName();

    /**
     * Gets the UUID of this player.
     *
     * @return The UUID of this player.
     */
    @NonNull UUID getUUID();

    /**
     * Gets the player as a String in the format '"playerName" (playerUUID)'.
     *
     * @return The player as a String.
     */
    default @NonNull String asString()
    {
        return String.format("\"%s\" (%s)", getName(), getUUID().toString());
    }

    /**
     * Checks if this player has a bypass permission node that allows them to bypass the protection hooks.
     *
     * @return True if this player has permission to bypass the protection hooks.
     */
    boolean hasProtectionBypassPermission();

    /**
     * Gets the limit for the size of doors this player can create/operate based on their permission.
     * <p>
     * Note that this does not take the global limit into account.
     *
     * @return The maximum doorsize this player can make/operate measured in number of blocks.
     */
    int getDoorSizeLimit();

    /**
     * Gets the limit for the number of doors this player can own based on their permission.
     * <p>
     * Note that this does not take the global limit into account.
     *
     * @return The number of doors this player can own.
     */
    int getDoorCountLimit();

    /**
     * Checks if this player is an OP or not.
     *
     * @return True if the player is an OP.
     */
    boolean isOp();

    default @NonNull PPlayerData getPPlayerData()
    {
        return new PPlayerData(getName(), getUUID(), getDoorSizeLimit(), getDoorCountLimit(),
                               isOp(), hasProtectionBypassPermission());
    }
}
