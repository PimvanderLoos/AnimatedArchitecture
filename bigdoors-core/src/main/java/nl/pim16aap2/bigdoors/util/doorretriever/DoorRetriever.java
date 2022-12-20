package nl.pim16aap2.bigdoors.util.doorretriever;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DoorSpecificationManager;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.delayedinput.DelayedDoorSpecificationInputRequest;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Represents a way to retrieve a door. It may be referenced by its name, its UID, or the object itself.
 *
 * @author Pim
 */
public sealed abstract class DoorRetriever
{
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
    public abstract CompletableFuture<Optional<AbstractDoor>> getDoor();

    /**
     * Gets the door that is referenced by this {@link DoorRetriever} and owned by the provided player if exactly 1 door
     * matches the description.
     * <p>
     * In case the door is referenced by its name, there may be more than one match (names are not unique). When this
     * happens, no doors are returned.
     *
     * @param player
     *     The {@link IPPlayer} that owns the door.
     * @return The {@link AbstractDoor} if it can be found.
     */
    public abstract CompletableFuture<Optional<AbstractDoor>> getDoor(IPPlayer player);

    /**
     * Gets the door referenced by this {@link DoorRetriever}.
     * <p>
     * If the {@link ICommandSender} is a player, see {@link #getDoor(IPPlayer)}, otherwise see {@link #getDoor()}.
     *
     * @param commandSender
     *     The {@link ICommandSender} for whom to retrieve the doors.
     * @return The door referenced by this {@link DoorRetriever}.
     */
    public CompletableFuture<Optional<AbstractDoor>> getDoor(ICommandSender commandSender)
    {
        if (commandSender instanceof IPPlayer player)
            return getDoor(player);
        return getDoor();
    }

    /**
     * Attempts to retrieve a door from its specification (see {@link #getDoor(IPPlayer)}).
     * <p>
     * If more than 1 match was found, the player will be asked to specify which one they asked for specifically.
     * <p>
     * The amount of time to wait (when required) is determined by {@link IConfigLoader#specificationTimeout()}.
     * <p>
     * See {@link DelayedDoorSpecificationInputRequest}.
     *
     * @param player
     *     The player for whom to get the door.
     * @return The door as specified by this {@link DoorRetriever} and with user input in case more than one match was
     * found.
     */
    // TODO: Implement the interactive system.
    public CompletableFuture<Optional<AbstractDoor>> getDoorInteractive(IPPlayer player)
    {
        return getDoor(player);
    }

    /**
     * Gets all doors referenced by this {@link DoorRetriever}.
     *
     * @return All doors referenced by this {@link DoorRetriever}.
     */
    public CompletableFuture<List<AbstractDoor>> getDoors()
    {
        return optionalToList(getDoor());
    }

    /**
     * Gets all doors referenced by this {@link DoorRetriever} where the provided player is a (co)owner of with any
     * permission level.
     *
     * @param player
     *     The {@link IPPlayer} that owns all matching doors.
     * @return All doors referenced by this {@link DoorRetriever}.
     */
    public CompletableFuture<List<AbstractDoor>> getDoors(IPPlayer player)
    {
        return optionalToList(getDoor(player));
    }

    /**
     * Gets all doors referenced by this {@link DoorRetriever}.
     * <p>
     * If the {@link ICommandSender} is a player, see {@link #getDoors(IPPlayer)}, otherwise see {@link #getDoors()}.
     *
     * @param commandSender
     *     The {@link ICommandSender} for whom to retrieve the doors.
     * @return The doors referenced by this {@link DoorRetriever}.
     */
    public CompletableFuture<List<AbstractDoor>> getDoors(ICommandSender commandSender)
    {
        if (commandSender instanceof IPPlayer player)
            return getDoors(player);
        return getDoors();
    }

    /**
     * Gets a list of (future) doors from an optional one.
     *
     * @param optionalDoor
     *     The (future) optional door.
     * @return Either an empty list (if the optional was empty) or a singleton list (if the optional was not empty).
     */
    private static CompletableFuture<List<AbstractDoor>> optionalToList(
        CompletableFuture<Optional<AbstractDoor>> optionalDoor)
    {
        return optionalDoor.thenApply(door -> door.map(Collections::singletonList)
                                                  .orElseGet(Collections::emptyList));
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
    static final class DoorNameRetriever extends DoorRetriever
    {
        @ToString.Exclude
        private final DatabaseManager databaseManager;

        @ToString.Exclude
        private IConfigLoader config;

        @ToString.Exclude
        private DoorSpecificationManager doorSpecificationManager;

        @ToString.Exclude
        private ILocalizer localizer;

        @ToString.Exclude
        private ITextFactory textFactory;

        private final String name;

        @Override
        public CompletableFuture<Optional<AbstractDoor>> getDoor()
        {
            return listToOptional(databaseManager.getDoors(name));
        }

        @Override
        public CompletableFuture<Optional<AbstractDoor>> getDoor(IPPlayer player)
        {
            return listToOptional(databaseManager.getDoors(player, name));
        }

        @Override
        public CompletableFuture<List<AbstractDoor>> getDoors()
        {
            return databaseManager.getDoors(name)
                                  .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
        }

        @Override
        public CompletableFuture<List<AbstractDoor>> getDoors(IPPlayer player)
        {
            return databaseManager.getDoors(player, name)
                                  .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
        }

        @Override
        public CompletableFuture<Optional<AbstractDoor>> getDoorInteractive(IPPlayer player)
        {
            return getDoors(player).thenCompose(
                doorList ->
                {
                    if (doorList.size() == 1)
                        return CompletableFuture.completedFuture(Optional.of(doorList.get(0)));

                    if (doorList.isEmpty())
                        return CompletableFuture.completedFuture(Optional.empty());

                    final Duration timeOut = Duration.ofSeconds(config.specificationTimeout());
                    return DelayedDoorSpecificationInputRequest.get(timeOut, doorList, player, localizer, textFactory,
                                                                    doorSpecificationManager);

                }).exceptionally(Util::exceptionallyOptional);
        }

        /**
         * Gets a single (optional/future) door from a list of (future) doors if only 1 door exists in the list.
         *
         * @param list
         *     The list of (future) doors.
         * @return An optional (future) {@link AbstractDoor} if exactly 1 existed in the list, otherwise an empty
         * optional.
         */
        private CompletableFuture<Optional<AbstractDoor>> listToOptional(CompletableFuture<List<AbstractDoor>> list)
        {
            return list.<Optional<AbstractDoor>>thenApply(
                doorList ->
                {
                    if (doorList.size() == 1)
                        return Optional.of(doorList.get(0));
                    return Optional.empty();
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
    static final class DoorUIDRetriever extends DoorRetriever
    {
        @ToString.Exclude
        private final DatabaseManager databaseManager;

        private final long uid;

        @Override
        public CompletableFuture<Optional<AbstractDoor>> getDoor()
        {
            return databaseManager.getDoor(uid).exceptionally(Util::exceptionallyOptional);
        }

        @Override
        public CompletableFuture<Optional<AbstractDoor>> getDoor(IPPlayer player)
        {
            return databaseManager.getDoor(player, uid).exceptionally(Util::exceptionallyOptional);
        }
    }

    /**
     * Represents a {@link DoorRetriever} that references a door by the object itself.
     *
     * @author Pim
     */
    @AllArgsConstructor()
    @ToString(doNotUseGetters = true)
    @EqualsAndHashCode(callSuper = false, doNotUseGetters = true)
    static final class DoorObjectRetriever extends DoorRetriever
    {
        private final @Nullable AbstractDoor door;

        @Override
        public boolean isAvailable()
        {
            return door != null;
        }

        @Override
        public CompletableFuture<Optional<AbstractDoor>> getDoor()
        {
            return CompletableFuture.completedFuture(Optional.ofNullable(door));
        }

        @Override
        public CompletableFuture<Optional<AbstractDoor>> getDoor(IPPlayer player)
        {
            return door != null && door.isDoorOwner(player) ?
                   getDoor() : CompletableFuture.completedFuture(Optional.empty());
        }
    }

    /**
     * Represents a {@link DoorRetriever} that references a list of doors by the object themselves.
     *
     * @author Pim
     */
    @ToString
    @AllArgsConstructor()
    @EqualsAndHashCode(callSuper = false, doNotUseGetters = true)
    static final class DoorListRetriever extends DoorRetriever
    {
        private final List<AbstractDoor> doors;

        @Override
        public boolean isAvailable()
        {
            return true;
        }

        @Override
        public CompletableFuture<Optional<AbstractDoor>> getDoor()
        {
            return doors.size() == 1 ?
                   CompletableFuture.completedFuture(Optional.of(doors.get(0))) :
                   CompletableFuture.completedFuture(Optional.empty());
        }

        @Override
        public CompletableFuture<List<AbstractDoor>> getDoors()
        {
            return CompletableFuture.completedFuture(doors);
        }

        private List<AbstractDoor> getDoors0(IPPlayer player)
        {
            final UUID playerUUID = player.getUUID();
            return doors.stream().filter(door -> door.isDoorOwner(playerUUID)).collect(Collectors.toList());
        }

        @Override
        public CompletableFuture<List<AbstractDoor>> getDoors(IPPlayer player)
        {
            return CompletableFuture.completedFuture(getDoors0(player));
        }

        @Override
        public CompletableFuture<Optional<AbstractDoor>> getDoor(IPPlayer player)
        {
            final List<AbstractDoor> ret = getDoors0(player);
            return CompletableFuture.completedFuture(ret.size() == 1 ? Optional.of(ret.get(0)) : Optional.empty());
        }
    }

    /**
     * Represents a {@link DoorRetriever} that references a future list of doors by the object themselves.
     *
     * @author Pim
     */
    @ToString
    @EqualsAndHashCode(callSuper = false, doNotUseGetters = true)
    static final class FutureDoorListRetriever extends DoorRetriever
    {
        private final CompletableFuture<List<AbstractDoor>> doors;

        FutureDoorListRetriever(CompletableFuture<List<AbstractDoor>> doors)
        {
            this.doors = doors.exceptionally(t -> Util.exceptionally(t, Collections.emptyList()));
        }

        @Override
        public boolean isAvailable()
        {
            return doors.isDone();
        }

        @Override
        public CompletableFuture<Optional<AbstractDoor>> getDoor()
        {
            return doors.thenApply(lst -> lst.size() > 0 ? Optional.of(lst.get(0)) : Optional.empty());
        }

        @Override
        public CompletableFuture<List<AbstractDoor>> getDoors()
        {
            return doors;
        }

        private CompletableFuture<List<AbstractDoor>> getDoors0(IPPlayer player)
        {
            final UUID playerUUID = player.getUUID();
            return doors.thenApply(retrieved ->
                                       retrieved.stream().filter(door -> door.isDoorOwner(playerUUID)).toList());
        }

        @Override
        public CompletableFuture<List<AbstractDoor>> getDoors(IPPlayer player)
        {
            return getDoors0(player);
        }

        @Override
        public CompletableFuture<Optional<AbstractDoor>> getDoor(IPPlayer player)
        {
            return getDoors0(player)
                .thenApply(lst -> lst.size() == 1 ? Optional.of(lst.get(0)) : Optional.empty());
        }
    }

    /**
     * Represents a {@link DoorRetriever} that references a future optional door directly.
     *
     * @author Pim
     */
    @ToString
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    static final class FutureDoorRetriever extends DoorRetriever
    {
        private final CompletableFuture<Optional<AbstractDoor>> futureDoor;

        @Override
        public boolean isAvailable()
        {
            return futureDoor.isDone() && !futureDoor.isCancelled() && !futureDoor.isCompletedExceptionally();
        }

        @Override
        public CompletableFuture<Optional<AbstractDoor>> getDoor()
        {
            return futureDoor;
        }

        @Override
        public CompletableFuture<Optional<AbstractDoor>> getDoor(IPPlayer player)
        {
            return futureDoor.thenApply(
                doorOpt ->
                {
                    final boolean playerIsPresent = doorOpt.map(door -> door.isDoorOwner(player)).orElse(false);
                    return playerIsPresent ? doorOpt : Optional.empty();
                });
        }
    }
}

