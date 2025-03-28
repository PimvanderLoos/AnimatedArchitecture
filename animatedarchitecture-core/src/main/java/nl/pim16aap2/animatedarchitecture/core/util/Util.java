package nl.pim16aap2.animatedarchitecture.core.util;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureConst;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Represents various small and platform-agnostic utility functions.
 */
@Flogger
public final class Util
{
    private static final Map<BlockFace, MovementDirection> TO_MOVEMENT_DIRECTION =
        new EnumMap<>(BlockFace.class);

    private static final Map<MovementDirection, BlockFace> TO_BLOCK_FACE =
        new EnumMap<>(MovementDirection.class);

    static
    {
        for (final var blockFace : BlockFace.values())
        {
            MovementDirection mappedMoveDir; //NOPMD - False positive: https://github.com/pmd/pmd/issues/5046
            try
            {
                mappedMoveDir = MovementDirection.valueOf(blockFace.toString());
            }
            catch (IllegalArgumentException e)
            {
                mappedMoveDir = MovementDirection.NONE;
            }
            TO_MOVEMENT_DIRECTION.put(blockFace, mappedMoveDir);
            TO_BLOCK_FACE.put(mappedMoveDir, blockFace);
        }
    }

    private Util()
    {
    }

    /**
     * Ensures an object is not null.
     * <p>
     * If the object is null after all, a {@link NullPointerException} will be thrown.
     * <p>
     * This is basically the same as {@link Objects#requireNonNull(Object, String)} with the only difference being that
     * this will create a full "must not be null" message for the provided variable name.
     *
     * @param obj
     *     The object to check.
     * @param name
     *     The name of the input object. This is used in the NPE message with the format "{name} must not be null!".
     * @param <T>
     *     The type of the input object.
     * @return The input object, if it is not null.
     *
     * @throws NullPointerException
     *     If the input object to check is null.
     */
    @Contract("null, _ -> fail")
    public static <T> T requireNonNull(@Nullable T obj, String name)
        throws NullPointerException
    {
        //noinspection ConstantConditions
        return Objects.requireNonNull(obj, name + " must not be null!");
    }

    /**
     * Gets a {@link NotNull} value from a {@link Nullable} one, with a provided fallback in case the value is null.
     *
     * @param value
     *     The value that may or may not be null.
     * @param fallback
     *     A {@link Supplier} to supply a fallback to return in case the value is null.
     * @param <T>
     *     The type of the value.
     * @return The value if it is not null, otherwise the fallback.
     */
    public static <T> T valOrDefault(@Nullable T value, Supplier<T> fallback)
    {
        return value == null ? fallback.get() : value;
    }

    /**
     * Gets the {@link MovementDirection} equivalent of a {@link BlockFace} if it exists.
     *
     * @param blockFace
     *     The {@link BlockFace}.
     * @return The {@link MovementDirection} equivalent of a {@link BlockFace} if it exists and otherwise
     * {@link MovementDirection#NONE}.
     */
    public static MovementDirection getMovementDirection(BlockFace blockFace)
    {
        return TO_MOVEMENT_DIRECTION.getOrDefault(blockFace, MovementDirection.NONE);
    }

    /**
     * Gets the {@link BlockFace} equivalent of a {@link MovementDirection} if it exists.
     *
     * @param movementDirection
     *     The {@link MovementDirection}.
     * @return The {@link BlockFace} equivalent of a {@link MovementDirection} if it exists and otherwise
     * {@link BlockFace#NONE}.
     */
    public static BlockFace getBlockFace(MovementDirection movementDirection)
    {
        return TO_BLOCK_FACE.getOrDefault(movementDirection, BlockFace.NONE);
    }

    /**
     * Check if a player has permission to perform an action on a structure.
     * <p>
     * A player has permission to perform an action on a structure if they are an owner of the structure and their
     * permission level is lower than or equal to the permission level of the attribute. See
     * {@link PermissionLevel#isLowerThanOrEquals(PermissionLevel)}.
     * <p>
     * If the provided player is not an owner of the structure, they do not have permission to perform the action.
     *
     * @param uuid
     *     The UUID of the player to check.
     * @param structure
     *     The structure to check.
     * @param attribute
     *     The attribute to check.
     * @return True if the player has permission to perform the action on the structure.
     */
    public static boolean hasPermissionForAction(UUID uuid, IStructureConst structure, StructureAttribute attribute)
    {
        return structure
            .getOwner(uuid)
            .map(structureOwner -> structureOwner.permission().isLowerThanOrEquals(attribute.getPermissionLevel()))
            .orElse(false);
    }

    /**
     * Check if a player has permission to perform an action on a structure.
     * <p>
     * See {@link #hasPermissionForAction(UUID, IStructureConst, StructureAttribute)}.
     *
     * @param player
     *     The player to check.
     * @param structure
     *     The structure to check.
     * @param attribute
     *     The attribute to check.
     * @return True if the player has permission to perform the action on the structure.
     */
    public static boolean hasPermissionForAction(
        IPlayer player,
        IStructureConst structure,
        StructureAttribute attribute)
    {
        return hasPermissionForAction(player.getUUID(), structure, attribute);
    }

    /**
     * Parses the {@link Level} from its name.
     * <p>
     * The strict refers to the aspect that only name matches are allowed. See {@link Level#getName()}. Contrary to
     * {@link Level#parse(String)}, this method won't define new levels from integer inputs.
     *
     * @param logLevelName
     *     The name of the log level.
     * @return The {@link Level} if an exact match could be found, otherwise null.
     */
    public static @Nullable Level parseLogLevelStrict(@Nullable String logLevelName)
    {
        if (logLevelName == null)
            return null;

        final String preparedLogLevelName = logLevelName.toUpperCase(Locale.ENGLISH).strip();
        if (preparedLogLevelName.isBlank())
            return null;

        return switch (preparedLogLevelName)
        {
            case "OFF" -> Level.OFF;
            case "SEVERE" -> Level.SEVERE;
            case "WARNING" -> Level.WARNING;
            case "INFO" -> Level.INFO;
            case "CONFIG" -> Level.CONFIG;
            case "FINE" -> Level.FINE;
            case "FINER" -> Level.FINER;
            case "FINEST" -> Level.FINEST;
            case "ALL" -> Level.ALL;
            default -> null;
        };
    }

    /**
     * Gets the root cause of a {@link Throwable}.
     *
     * @param throwable
     *     The {@link Throwable} to get the root cause of.
     * @return The root cause of the {@link Throwable}.
     */
    public static Throwable getRootCause(Throwable throwable)
    {
        Throwable cause = throwable;
        while (cause.getCause() != null)
            cause = cause.getCause();
        return cause;
    }
}
