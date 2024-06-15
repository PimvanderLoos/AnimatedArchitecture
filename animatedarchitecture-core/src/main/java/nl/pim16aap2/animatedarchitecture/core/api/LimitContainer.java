package nl.pim16aap2.animatedarchitecture.core.api;

import nl.pim16aap2.animatedarchitecture.core.util.Limit;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;

/**
 * A record that contains the {@link Limit}s for a player.
 */
public record LimitContainer(
    OptionalInt structureSizeLimit,
    OptionalInt structureCountLimit,
    OptionalInt powerBlockDistanceLimit,
    OptionalInt blocksToMoveLimit)
{
    public LimitContainer(
        @Nullable Integer structureSizeLimit,
        @Nullable Integer structureCountLimit,
        @Nullable Integer powerBlockDistanceLimit,
        @Nullable Integer blocksToMoveLimit)
    {
        this(
            ofNullable(structureSizeLimit),
            ofNullable(structureCountLimit),
            ofNullable(powerBlockDistanceLimit),
            ofNullable(blocksToMoveLimit)
        );
    }

    public LimitContainer(
        int structureSizeLimit,
        int structureCountLimit,
        int powerBlockDistanceLimit,
        int blocksToMoveLimit)
    {
        this(
            OptionalInt.of(structureSizeLimit),
            OptionalInt.of(structureCountLimit),
            OptionalInt.of(powerBlockDistanceLimit),
            OptionalInt.of(blocksToMoveLimit)
        );
    }

    /**
     * Gets the limit for the specified {@link Limit}.
     *
     * @param limit
     *     The {@link Limit} to get the limit for.
     * @return The limit for the specified {@link Limit}.
     */
    public OptionalInt getLimit(Limit limit)
    {
        return switch (limit)
        {
            case STRUCTURE_SIZE -> structureSizeLimit;
            case STRUCTURE_COUNT -> structureCountLimit;
            case POWERBLOCK_DISTANCE -> powerBlockDistanceLimit;
            case BLOCKS_TO_MOVE -> blocksToMoveLimit;
        };
    }

    /**
     * Creates a {@link LimitContainer} from the given {@link IPlayerDataContainer}.
     *
     * @param player
     *     The {@link IPlayerDataContainer} to create the {@link LimitContainer} from.
     * @return The created {@link LimitContainer}.
     */
    static LimitContainer of(IPlayerDataContainer player)
    {
        return new LimitContainer(
            player.getLimit(Limit.STRUCTURE_SIZE),
            player.getLimit(Limit.STRUCTURE_COUNT),
            player.getLimit(Limit.POWERBLOCK_DISTANCE),
            player.getLimit(Limit.BLOCKS_TO_MOVE)
        );
    }

    /**
     * Creates a {@link LimitContainer} from the given {@link IPlayer}.
     *
     * @param player
     *     The {@link IPlayer} to create the {@link LimitContainer} from.
     * @return The created {@link LimitContainer}.
     */
    public static LimitContainer of(IPlayer player)
    {
        return of((IPlayerDataContainer) player);
    }

    /**
     * Creates a {@link LimitContainer} from the given {@link PlayerData}.
     *
     * @param playerData
     *     The {@link PlayerData} to create the {@link LimitContainer} from.
     * @return The created {@link LimitContainer}.
     */
    public static LimitContainer of(PlayerData playerData)
    {
        return of((IPlayerDataContainer) playerData);
    }

    /**
     * Creates an {@link OptionalInt} from a nullable {@link Integer}.
     * <p>
     * Works similar to {@link java.util.Optional#ofNullable(Object)}
     *
     * @param value
     *     The nullable {@link Integer} to create the {@link OptionalInt} from.
     * @return The created {@link OptionalInt}. If the value is null, {@link OptionalInt#empty()} is returned.
     * Otherwise, {@link OptionalInt#of(int)} is returned.
     */
    private static OptionalInt ofNullable(@Nullable Integer value)
    {
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }
}
