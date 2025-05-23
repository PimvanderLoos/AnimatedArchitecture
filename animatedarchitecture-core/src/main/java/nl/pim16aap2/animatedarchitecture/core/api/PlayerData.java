package nl.pim16aap2.animatedarchitecture.core.api;

import nl.pim16aap2.animatedarchitecture.core.util.Limit;

import java.util.OptionalInt;
import java.util.UUID;

/**
 * Represents the data of a player.
 *
 * @param uuid
 *     The UUID of the player.
 * @param name
 *     The name of the player.
 * @param limits
 *     The limits of the player.
 * @param isOp
 *     Whether the player is an operator.
 * @param hasProtectionBypassPermission
 *     Whether the player has the permission to bypass protection.
 */
public record PlayerData(
    UUID uuid,
    String name,
    LimitContainer limits,
    boolean isOp,
    boolean hasProtectionBypassPermission
)
    implements IPlayerDataContainer
{
    /**
     * Creates a new {@link PlayerData} instance with the specified parameters.
     *
     * @param uuid
     *     The UUID of the player.
     * @param name
     *     The name of the player.
     * @param limits
     *     The limits of the player.
     * @param permissionsFlag
     *     The permissions flag of the player. See {@link PermissionFlag}. This bit flag is used to set {@link #isOp()}
     *     and {@link #hasProtectionBypassPermission()} from {@link PermissionFlag#OP} and {@link PermissionFlag#BYPASS}
     *     respectively.
     */
    public PlayerData(
        UUID uuid,
        String name,
        LimitContainer limits,
        long permissionsFlag)
    {
        this(
            uuid,
            name,
            limits,
            PermissionFlag.hasFlag(PermissionFlag.OP, permissionsFlag),
            PermissionFlag.hasFlag(PermissionFlag.BYPASS, permissionsFlag)
        );
    }

    /**
     * Gets the limit for the specified {@link Limit}.
     *
     * @param limit
     *     The {@link Limit} to get the limit for.
     * @return The value of the specified {@link Limit} for this player.
     */
    @Override
    public OptionalInt getLimit(Limit limit)
    {
        return limits.getLimit(limit);
    }

    @Override
    public PlayerData getPlayerData()
    {
        return this;
    }

    @Override
    public String getName()
    {
        return name();
    }

    @Override
    public UUID getUUID()
    {
        return uuid;
    }

    @Override
    public boolean hasProtectionBypassPermission()
    {
        return hasProtectionBypassPermission;
    }
}
