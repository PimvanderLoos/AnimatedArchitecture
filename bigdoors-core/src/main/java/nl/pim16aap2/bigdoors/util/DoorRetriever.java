package nl.pim16aap2.bigdoors.util;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.util.delayedinput.DelayedDoorSpecificationInputRequest;

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
    public static @NonNull DoorRetriever of(final @NonNull String doorID)
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
    public static @NonNull DoorRetriever of(final long doorUID)
    {
        return new DoorUIDRetriever(doorUID);
    }

    /**
     * Creates a new {@link DoorRetriever} from the door object itself.
     *
     * @param door The door object itself.
     * @return The new {@link DoorRetriever}.
     */
    public static @NonNull DoorRetriever of(final @NonNull AbstractDoorBase door)
    {
        return new DoorObjectRetriever(door);
    }

    /**
     * Gets the door that is referenced by this {@link DoorRetriever} if exactly 1 door matches the description.
     * <p>
     * In case the door is referenced by its name, there may be more than one match (names are not unique). When this
     * happens, no doors are returned.
     *
     * @return The {@link AbstractDoorBase} if it can be found.
     */
    public abstract @NonNull CompletableFuture<Optional<AbstractDoorBase>> getDoor();

    /**
     * Gets the door that is referenced by this {@link DoorRetriever} and owned by the provided player if exactly 1 door
     * matches the description.
     * <p>
     * In case the door is referenced by its name, there may be more than one match (names are not unique). When this
     * happens, no doors are returned.
     *
     * @param player The {@link IPPlayer} that owns the door.
     * @return The {@link AbstractDoorBase} if it can be found.
     */
    public abstract @NonNull CompletableFuture<Optional<AbstractDoorBase>> getDoor(final @NonNull IPPlayer player);

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
    public @NonNull CompletableFuture<Optional<AbstractDoorBase>> getDoorInteractive(final @NonNull IPPlayer player)
    {
        return getDoor(player);
    }

    /**
     * Gets all doors referenced by this {@link DoorRetriever}.
     *
     * @return All doors referenced by this {@link DoorRetriever}.
     */
    public @NonNull CompletableFuture<List<AbstractDoorBase>> getDoors()
    {
        return optionalToList(getDoor());
    }

    /**
     * Gets all doors referenced by this {@link DoorRetriever} owned by the provided player.
     *
     * @param player The {@link IPPlayer} that owns all matching doors.
     * @return All doors referenced by this {@link DoorRetriever}.
     */
    public @NonNull CompletableFuture<List<AbstractDoorBase>> getDoors(final @NonNull IPPlayer player)
    {
        return optionalToList(getDoor(player));
    }

    /**
     * Gets a list of (future) doors from an optional one.
     *
     * @param optionalDoor The (future) optional door.
     * @return Either an empty list (if the optional was empty) or a singleton list (if the optional was not empty).
     */
    private static CompletableFuture<List<AbstractDoorBase>> optionalToList(
        final @NonNull CompletableFuture<Optional<AbstractDoorBase>> optionalDoor)
    {
        return optionalDoor.thenApply(door -> door.map(Collections::singletonList).orElseGet(Collections::emptyList));
    }

    /**
     * Gets a single (optional/future) door from a list of (future) doors if only 1 door exists in the list.
     *
     * @param list The list of (future) doors.
     * @return An optional (future) {@link AbstractDoorBase} if exactly 1 existed in the list, otherwise an empty
     * optional.
     */
    private static @NonNull CompletableFuture<Optional<AbstractDoorBase>> listToOptional(
        final @NonNull CompletableFuture<List<AbstractDoorBase>> list)
    {
        return list.thenApply(
            doorList ->
            {
                if (doorList.size() == 1)
                    return Optional.of(doorList.get(0));
                return Optional.empty();
            });
    }

    /**
     * Represents a {@link DoorRetriever} that references a door by its name.
     * <p>
     * Because names are not unique, a single name may reference more than 1 door (even for a single player).
     *
     * @author Pim
     */
    @AllArgsConstructor
    private static class DoorNameRetriever extends DoorRetriever
    {
        private final @NonNull String name;

        @Override
        public @NonNull CompletableFuture<Optional<AbstractDoorBase>> getDoor()
        {
            return listToOptional(DatabaseManager.get().getDoors(name));
        }

        @Override
        public @NonNull CompletableFuture<Optional<AbstractDoorBase>> getDoor(final @NonNull IPPlayer player)
        {
            return listToOptional(DatabaseManager.get().getDoors(player, name));
        }

        @Override
        public @NonNull CompletableFuture<List<AbstractDoorBase>> getDoors()
        {
            return DatabaseManager.get().getDoors(name);
        }

        @Override
        public @NonNull CompletableFuture<List<AbstractDoorBase>> getDoors(final @NonNull IPPlayer player)
        {
            return DatabaseManager.get().getDoors(player, name);
        }

        @Override
        public @NonNull CompletableFuture<Optional<AbstractDoorBase>> getDoorInteractive(final @NonNull IPPlayer player)
        {
            return getDoors(player).thenApplyAsync(
                doorList ->
                {
                    if (doorList.size() == 1)
                        return Optional.of(doorList.get(0));
                    if (doorList.isEmpty())
                        return Optional.empty();
                    return DelayedDoorSpecificationInputRequest
                        .get(BigDoors.get().getPlatform().getConfigLoader().specificationTimeout(), doorList, player);
                });
        }
    }

    /**
     * Represents a {@link DoorRetriever} that references a door by its UID.
     * <p>
     * Because the UID is always unique (by definition), this can never reference more than 1 door.
     *
     * @author Pim
     */
    @AllArgsConstructor
    private static class DoorUIDRetriever extends DoorRetriever
    {
        private final long uid;

        @Override
        public @NonNull CompletableFuture<Optional<AbstractDoorBase>> getDoor()
        {
            return DatabaseManager.get().getDoor(uid);
        }

        @Override
        public @NonNull CompletableFuture<Optional<AbstractDoorBase>> getDoor(final @NonNull IPPlayer player)
        {
            return DatabaseManager.get().getDoor(player, uid);
        }
    }

    /**
     * Represents a {@link DoorRetriever} that references a door by the object itself.
     *
     * @author Pim
     */
    @AllArgsConstructor
    private static class DoorObjectRetriever extends DoorRetriever
    {
        private final @NonNull AbstractDoorBase door;

        @Override
        public @NonNull CompletableFuture<Optional<AbstractDoorBase>> getDoor()
        {
            return CompletableFuture.completedFuture(Optional.of(door));
        }

        @Override
        public @NonNull CompletableFuture<Optional<AbstractDoorBase>> getDoor(final @NonNull IPPlayer player)
        {
            return door.getDoorOwner(player).isPresent() ?
                   getDoor() :
                   CompletableFuture.completedFuture(Optional.empty());
        }
    }
}
