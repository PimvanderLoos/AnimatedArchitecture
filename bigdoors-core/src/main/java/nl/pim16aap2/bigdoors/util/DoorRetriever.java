package nl.pim16aap2.bigdoors.util;

import lombok.AllArgsConstructor;
import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.util.delayedinput.DelayedDoorSpecificationInputRequest;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a way to retrieve a door. It may be referenced by its name, its UID, or the object itself.
 *
 * @author Pim
 */
public abstract class DoorRetriever
{
    /**
     * Creates a new {@link DoorRetriever} from its ID.
     *
     * @param doorID The identifier (name or UID) of the door.
     * @return The new {@link DoorRetriever}.
     */
    public static @NotNull DoorRetriever of(final @NotNull String doorID)
    {
        final OptionalLong doorUID = Util.parseLong(doorID);
        return doorUID.isPresent() ?
               new DoorUIDRetriever(doorUID.getAsLong()) :
               new DoorNameRetriever(doorID);
    }

    /**
     * Creates a new {@link DoorRetriever} from its UID.
     *
     * @param doorUID The UID of the door.
     * @return The new {@link DoorRetriever}.
     */
    public static @NotNull DoorRetriever of(final long doorUID)
    {
        return new DoorUIDRetriever(doorUID);
    }

    /**
     * Creates a new {@link DoorRetriever} from the door object itself.
     *
     * @param door The door object itself.
     * @return The new {@link DoorRetriever}.
     */
    public static @NotNull DoorRetriever of(final @NotNull AbstractDoor door)
    {
        return new DoorObjectRetriever(door);
    }

    /**
     * Checks if the door that is being retrieved is available.
     *
     * @return True if the door is available.
     */
    public boolean isAvailable()
    {
        return false;
    }

    /**
     * Gets the door that is referenced by this {@link DoorRetriever} if exactly 1 door matches the description.
     * <p>
     * In case the door is referenced by its name, there may be more than one match (names are not unique). When this
     * happens, no doors are returned.
     *
     * @return The {@link AbstractDoor} if it can be found.
     */
    public abstract @NotNull CompletableFuture<Optional<AbstractDoor>> getDoor();

    /**
     * Gets the door that is referenced by this {@link DoorRetriever} and owned by the provided player if exactly 1 door
     * matches the description.
     * <p>
     * In case the door is referenced by its name, there may be more than one match (names are not unique). When this
     * happens, no doors are returned.
     *
     * @param player The {@link IPPlayer} that owns the door.
     * @return The {@link AbstractDoor} if it can be found.
     */
    public abstract @NotNull CompletableFuture<Optional<AbstractDoor>> getDoor(final @NotNull IPPlayer player);

    /**
     * Attempts to retrieve a door from its specification (see {@link #getDoor(IPPlayer)}).
     * <p>
     * If more than 1 match was found, the player will be asked to specify which one they asked for specifically.
     * <p>
     * The amount of time to wait (when required) is determined by {@link IConfigLoader#specificationTimeout()}.
     * <p>
     * See {@link DelayedDoorSpecificationInputRequest}.
     *
     * @param player The player for whom to get the door.
     * @return The door as specified by this {@link DoorRetriever} and with user input in case more than one match was
     * found.
     */
    public @NotNull CompletableFuture<Optional<AbstractDoor>> getDoorInteractive(final @NotNull IPPlayer player)
    {
        return getDoor(player);
    }

    /**
     * Gets all doors referenced by this {@link DoorRetriever}.
     *
     * @return All doors referenced by this {@link DoorRetriever}.
     */
    public @NotNull CompletableFuture<List<AbstractDoor>> getDoors()
    {
        return optionalToList(getDoor());
    }

    /**
     * Gets all doors referenced by this {@link DoorRetriever} where the provided player is a (co)owner of with any
     * permission level.
     *
     * @param player The {@link IPPlayer} that owns all matching doors.
     * @return All doors referenced by this {@link DoorRetriever}.
     */
    public @NotNull CompletableFuture<List<AbstractDoor>> getDoors(final @NotNull IPPlayer player)
    {
        return optionalToList(getDoor(player));
    }

    /**
     * Gets a list of (future) doors from an optional one.
     *
     * @param optionalDoor The (future) optional door.
     * @return Either an empty list (if the optional was empty) or a singleton list (if the optional was not empty).
     */
    private static CompletableFuture<List<AbstractDoor>> optionalToList(
        final @NotNull CompletableFuture<Optional<AbstractDoor>> optionalDoor)
    {
        return optionalDoor.thenApply(door -> door.map(Collections::singletonList).orElseGet(Collections::emptyList))
                           .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
    }

    /**
     * Gets a single (optional/future) door from a list of (future) doors if only 1 door exists in the list.
     *
     * @param list The list of (future) doors.
     * @return An optional (future) {@link AbstractDoor} if exactly 1 existed in the list, otherwise an empty optional.
     */
    private static @NotNull CompletableFuture<Optional<AbstractDoor>> listToOptional(
        final @NotNull CompletableFuture<List<AbstractDoor>> list)
    {
        return list.<Optional<AbstractDoor>>thenApply(
            doorList ->
            {
                if (doorList.size() == 1)
                    return Optional.of(doorList.get(0));
                return Optional.empty();
            }).exceptionally(Util::exceptionallyOptional);
    }

    /**
     * Represents a {@link DoorRetriever} that references a door by its name.
     * <p>
     * Because names are not unique, a single name may reference more than 1 door (even for a single player).
     *
     * @author Pim
     */
    @ToString
    @AllArgsConstructor
    private static class DoorNameRetriever extends DoorRetriever
    {
        private final @NotNull String name;

        @Override
        public @NotNull CompletableFuture<Optional<AbstractDoor>> getDoor()
        {
            return listToOptional(BigDoors.get().getDatabaseManager().getDoors(name));
        }

        @Override
        public @NotNull CompletableFuture<Optional<AbstractDoor>> getDoor(final @NotNull IPPlayer player)
        {
            return listToOptional(BigDoors.get().getDatabaseManager().getDoors(player, name));
        }

        @Override
        public @NotNull CompletableFuture<List<AbstractDoor>> getDoors()
        {
            return BigDoors.get().getDatabaseManager().getDoors(name)
                           .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
        }

        @Override
        public @NotNull CompletableFuture<List<AbstractDoor>> getDoors(final @NotNull IPPlayer player)
        {
            return BigDoors.get().getDatabaseManager().getDoors(player, name)
                           .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
        }

        @Override
        public @NotNull CompletableFuture<Optional<AbstractDoor>> getDoorInteractive(final @NotNull IPPlayer player)
        {
            return getDoors(player).thenCompose(
                doorList ->
                {
                    if (doorList.size() == 1)
                        return CompletableFuture.completedFuture(Optional.of(doorList.get(0)));

                    if (doorList.isEmpty())
                        return CompletableFuture.completedFuture(Optional.empty());

                    return DelayedDoorSpecificationInputRequest
                        .get(Duration.ofSeconds(BigDoors.get().getPlatform().getConfigLoader().specificationTimeout()),
                             doorList, player);
                }).exceptionally(Util::exceptionallyOptional);
        }
    }

    /**
     * Represents a {@link DoorRetriever} that references a door by its UID.
     * <p>
     * Because the UID is always unique (by definition), this can never reference more than 1 door.
     *
     * @author Pim
     */
    @ToString
    @AllArgsConstructor
    private static class DoorUIDRetriever extends DoorRetriever
    {
        private final long uid;

        @Override
        public @NotNull CompletableFuture<Optional<AbstractDoor>> getDoor()
        {
            return BigDoors.get().getDatabaseManager().getDoor(uid)
                           .exceptionally(Util::exceptionallyOptional);
        }

        @Override
        public @NotNull CompletableFuture<Optional<AbstractDoor>> getDoor(final @NotNull IPPlayer player)
        {
            return BigDoors.get().getDatabaseManager().getDoor(player, uid)
                           .exceptionally(Util::exceptionallyOptional);
        }
    }

    /**
     * Represents a {@link DoorRetriever} that references a door by the object itself.
     *
     * @author Pim
     */
    @ToString
    @AllArgsConstructor
    private static class DoorObjectRetriever extends DoorRetriever
    {
        private final @NotNull AbstractDoor door;

        @Override
        public @NotNull CompletableFuture<Optional<AbstractDoor>> getDoor()
        {
            return CompletableFuture.completedFuture(Optional.of(door));
        }

        @Override
        public @NotNull CompletableFuture<Optional<AbstractDoor>> getDoor(final @NotNull IPPlayer player)
        {
            return door.getDoorOwner(player).isPresent() ?
                   getDoor() :
                   CompletableFuture.completedFuture(Optional.empty());
        }

        @Override
        public boolean isAvailable()
        {
            return super.isAvailable();
        }
    }
}
