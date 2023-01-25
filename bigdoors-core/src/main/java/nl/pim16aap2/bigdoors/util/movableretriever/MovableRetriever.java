package nl.pim16aap2.bigdoors.util.movableretriever;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.MovableSpecificationManager;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.delayedinput.DelayedMovableSpecificationInputRequest;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Represents a way to retrieve a movable. It may be referenced by its name, its UID, or the object itself.
 *
 * @author Pim
 */
public sealed abstract class MovableRetriever
{
    /**
     * Checks if the movable that is being retrieved is available.
     *
     * @return True if the movable is available.
     */
    public boolean isAvailable()
    {
        return false;
    }

    /**
     * Gets the movable that is referenced by this {@link MovableRetriever} if exactly 1 movable matches the
     * description.
     * <p>
     * In case the movable is referenced by its name, there may be more than one match (names are not unique). When this
     * happens, no movables are returned.
     *
     * @return The {@link AbstractMovable} if it can be found.
     */
    public abstract CompletableFuture<Optional<AbstractMovable>> getMovable();

    /**
     * Gets the movable that is referenced by this {@link MovableRetriever} and owned by the provided player if exactly
     * 1 movable matches the description.
     * <p>
     * In case the movable is referenced by its name, there may be more than one match (names are not unique). When this
     * happens, no movables are returned.
     *
     * @param player
     *     The {@link IPPlayer} that owns the movable.
     * @return The {@link AbstractMovable} if it can be found.
     */
    public abstract CompletableFuture<Optional<AbstractMovable>> getMovable(IPPlayer player);

    /**
     * Gets the movable referenced by this {@link MovableRetriever}.
     * <p>
     * If the {@link ICommandSender} is a player, see {@link #getMovable(IPPlayer)}, otherwise see
     * {@link #getMovable()}.
     *
     * @param commandSender
     *     The {@link ICommandSender} for whom to retrieve the movables.
     * @return The movable referenced by this {@link MovableRetriever}.
     */
    public CompletableFuture<Optional<AbstractMovable>> getMovable(ICommandSender commandSender)
    {
        if (commandSender instanceof IPPlayer player)
            return getMovable(player);
        return getMovable();
    }

    /**
     * Attempts to retrieve a movable from its specification (see {@link #getMovable(IPPlayer)}).
     * <p>
     * If more than 1 match was found, the player will be asked to specify which one they asked for specifically.
     * <p>
     * The amount of time to wait (when required) is determined by {@link IConfigLoader#specificationTimeout()}.
     * <p>
     * See {@link DelayedMovableSpecificationInputRequest}.
     *
     * @param player
     *     The player for whom to get the movable.
     * @return The movable as specified by this {@link MovableRetriever} and with user input in case more than one match
     * was found.
     */
    // TODO: Implement the interactive system.
    public CompletableFuture<Optional<AbstractMovable>> getMovableInteractive(IPPlayer player)
    {
        return getMovable(player);
    }

    /**
     * Gets all movables referenced by this {@link MovableRetriever}.
     *
     * @return All movables referenced by this {@link MovableRetriever}.
     */
    public CompletableFuture<List<AbstractMovable>> getMovables()
    {
        return optionalToList(getMovable());
    }

    /**
     * Gets all movables referenced by this {@link MovableRetriever} where the provided player is a (co)owner of with
     * any permission level.
     *
     * @param player
     *     The {@link IPPlayer} that owns all matching movables.
     * @return All movables referenced by this {@link MovableRetriever}.
     */
    public CompletableFuture<List<AbstractMovable>> getMovables(IPPlayer player)
    {
        return optionalToList(getMovable(player));
    }

    /**
     * Gets all movables referenced by this {@link MovableRetriever}.
     * <p>
     * If the {@link ICommandSender} is a player, see {@link #getMovables(IPPlayer)}, otherwise see
     * {@link #getMovables()}.
     *
     * @param commandSender
     *     The {@link ICommandSender} for whom to retrieve the movables.
     * @return The movables referenced by this {@link MovableRetriever}.
     */
    public CompletableFuture<List<AbstractMovable>> getMovables(ICommandSender commandSender)
    {
        if (commandSender instanceof IPPlayer player)
            return getMovables(player);
        return getMovables();
    }

    /**
     * Gets a list of (future) movables from an optional one.
     *
     * @param optionalMovable
     *     The (future) optional movable.
     * @return Either an empty list (if the optional was empty) or a singleton list (if the optional was not empty).
     */
    private static CompletableFuture<List<AbstractMovable>> optionalToList(
        CompletableFuture<Optional<AbstractMovable>> optionalMovable)
    {
        return optionalMovable.thenApply(movable -> movable.map(Collections::singletonList)
                                                           .orElseGet(Collections::emptyList));
    }

    /**
     * Represents a {@link MovableRetriever} that references a movable by its name.
     * <p>
     * Because names are not unique, a single name may reference more than 1 movable (even for a single player).
     *
     * @author Pim
     */
    @ToString
    @AllArgsConstructor
    @Flogger
    static final class MovableNameRetriever extends MovableRetriever
    {
        @ToString.Exclude
        private final DatabaseManager databaseManager;

        @ToString.Exclude
        private IConfigLoader config;

        @ToString.Exclude
        private MovableSpecificationManager movableSpecificationManager;

        @ToString.Exclude
        private ILocalizer localizer;

        @ToString.Exclude
        private ITextFactory textFactory;

        private final String name;

        @Override
        public CompletableFuture<Optional<AbstractMovable>> getMovable()
        {
            return listToOptional(databaseManager.getMovables(name));
        }

        @Override
        public CompletableFuture<Optional<AbstractMovable>> getMovable(IPPlayer player)
        {
            return listToOptional(databaseManager.getMovables(player, name));
        }

        @Override
        public CompletableFuture<List<AbstractMovable>> getMovables()
        {
            return databaseManager.getMovables(name)
                                  .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
        }

        @Override
        public CompletableFuture<List<AbstractMovable>> getMovables(IPPlayer player)
        {
            return databaseManager.getMovables(player, name)
                                  .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
        }

        @Override
        public CompletableFuture<Optional<AbstractMovable>> getMovableInteractive(IPPlayer player)
        {
            return getMovables(player).thenCompose(
                movablesList ->
                {
                    if (movablesList.size() == 1)
                        return CompletableFuture.completedFuture(Optional.of(movablesList.get(0)));

                    if (movablesList.isEmpty())
                        return CompletableFuture.completedFuture(Optional.empty());

                    final Duration timeOut = Duration.ofSeconds(config.specificationTimeout());
                    return DelayedMovableSpecificationInputRequest.get(timeOut, movablesList, player, localizer,
                                                                       textFactory, movableSpecificationManager);

                }).exceptionally(Util::exceptionallyOptional);
        }

        /**
         * Gets a single (optional/future) movable from a list of (future) movables if only 1 movable exists in the
         * list.
         *
         * @param list
         *     The list of (future) movables.
         * @return An optional (future) {@link AbstractMovable} if exactly 1 existed in the list, otherwise an empty
         * optional.
         */
        private CompletableFuture<Optional<AbstractMovable>> listToOptional(
            CompletableFuture<List<AbstractMovable>> list)
        {
            return list.<Optional<AbstractMovable>>thenApply(
                movablesList ->
                {
                    if (movablesList.size() == 1)
                        return Optional.of(movablesList.get(0));
                    log.atWarning().log("Tried to get 1 movable but received %d!", movablesList.size());
                    return Optional.empty();
                }).exceptionally(Util::exceptionallyOptional);
        }
    }

    /**
     * Represents a {@link MovableRetriever} that references a movable by its UID.
     * <p>
     * Because the UID is always unique (by definition), this can never reference more than 1 movable.
     *
     * @author Pim
     */
    @ToString
    @AllArgsConstructor
    static final class MovableUIDRetriever extends MovableRetriever
    {
        @ToString.Exclude
        private final DatabaseManager databaseManager;

        private final long uid;

        @Override
        public CompletableFuture<Optional<AbstractMovable>> getMovable()
        {
            return databaseManager.getMovable(uid).exceptionally(Util::exceptionallyOptional);
        }

        @Override
        public CompletableFuture<Optional<AbstractMovable>> getMovable(IPPlayer player)
        {
            return databaseManager.getMovable(player, uid).exceptionally(Util::exceptionallyOptional);
        }
    }

    /**
     * Represents a {@link MovableRetriever} that references a movable by the object itself.
     *
     * @author Pim
     */
    @AllArgsConstructor()
    @ToString(doNotUseGetters = true)
    @EqualsAndHashCode(callSuper = false, doNotUseGetters = true)
    static final class MovableObjectRetriever extends MovableRetriever
    {
        private final @Nullable AbstractMovable movable;

        @Override
        public boolean isAvailable()
        {
            return movable != null;
        }

        @Override
        public CompletableFuture<Optional<AbstractMovable>> getMovable()
        {
            return CompletableFuture.completedFuture(Optional.ofNullable(movable));
        }

        @Override
        public CompletableFuture<Optional<AbstractMovable>> getMovable(IPPlayer player)
        {
            return movable != null && movable.isOwner(player) ?
                   getMovable() : CompletableFuture.completedFuture(Optional.empty());
        }
    }

    /**
     * Represents a {@link MovableRetriever} that references a list of movables by the object themselves.
     *
     * @author Pim
     */
    @ToString
    @AllArgsConstructor()
    @EqualsAndHashCode(callSuper = false, doNotUseGetters = true)
    @Flogger
    static final class MovableListRetriever extends MovableRetriever
    {
        private final List<AbstractMovable> movables;

        @Override
        public boolean isAvailable()
        {
            return true;
        }

        @Override
        public CompletableFuture<Optional<AbstractMovable>> getMovable()
        {
            if (movables.size() == 1)
                return CompletableFuture.completedFuture(Optional.of(movables.get(0)));

            log.atWarning().log("Tried to get 1 movable but received %d!", movables.size());
            return CompletableFuture.completedFuture(Optional.empty());
        }

        @Override
        public CompletableFuture<List<AbstractMovable>> getMovables()
        {
            return CompletableFuture.completedFuture(movables);
        }

        private List<AbstractMovable> getMovables0(IPPlayer player)
        {
            final UUID playerUUID = player.getUUID();
            return movables.stream().filter(movable -> movable.isOwner(playerUUID)).collect(Collectors.toList());
        }

        @Override
        public CompletableFuture<List<AbstractMovable>> getMovables(IPPlayer player)
        {
            return CompletableFuture.completedFuture(getMovables0(player));
        }

        @Override
        public CompletableFuture<Optional<AbstractMovable>> getMovable(IPPlayer player)
        {
            final List<AbstractMovable> ret = getMovables0(player);

            if (ret.size() == 1)
                return CompletableFuture.completedFuture(Optional.of(ret.get(0)));

            log.atWarning().log("Tried to get 1 movable but received %d!", ret.size());
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    /**
     * Represents a {@link MovableRetriever} that references a future list of movables by the object themselves.
     *
     * @author Pim
     */
    @ToString
    @EqualsAndHashCode(callSuper = false, doNotUseGetters = true)
    @Flogger
    static final class FutureMovableListRetriever extends MovableRetriever
    {
        private final CompletableFuture<List<AbstractMovable>> movables;

        FutureMovableListRetriever(CompletableFuture<List<AbstractMovable>> movables)
        {
            this.movables = movables.exceptionally(t -> Util.exceptionally(t, Collections.emptyList()));
        }

        @Override
        public boolean isAvailable()
        {
            return movables.isDone();
        }

        @Override
        public CompletableFuture<Optional<AbstractMovable>> getMovable()
        {
            return movables.thenApply(
                lst ->
                {
                    if (lst.size() == 1)
                        return Optional.of(lst.get(0));
                    log.atWarning().log("Tried to get 1 movable but received %d!", lst.size());
                    return Optional.empty();
                });
        }

        @Override
        public CompletableFuture<List<AbstractMovable>> getMovables()
        {
            return movables;
        }

        private CompletableFuture<List<AbstractMovable>> getMovables0(IPPlayer player)
        {
            final UUID playerUUID = player.getUUID();
            return movables.thenApply(
                retrieved -> retrieved.stream().filter(movable -> movable.isOwner(playerUUID)).toList());
        }

        @Override
        public CompletableFuture<List<AbstractMovable>> getMovables(IPPlayer player)
        {
            return getMovables0(player);
        }

        @Override
        public CompletableFuture<Optional<AbstractMovable>> getMovable(IPPlayer player)
        {
            return getMovables0(player).thenApply(
                lst ->
                {
                    if (lst.size() == 1)
                        return Optional.of(lst.get(0));
                    log.atWarning().log("Tried to get 1 movable but received %d!", lst.size());
                    return Optional.empty();
                });
        }
    }

    /**
     * Represents a {@link MovableRetriever} that references a future optional movable directly.
     *
     * @author Pim
     */
    @ToString
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    static final class FutureMovableRetriever extends MovableRetriever
    {
        private final CompletableFuture<Optional<AbstractMovable>> futureMovable;

        @Override
        public boolean isAvailable()
        {
            return futureMovable.isDone() && !futureMovable.isCancelled() && !futureMovable.isCompletedExceptionally();
        }

        @Override
        public CompletableFuture<Optional<AbstractMovable>> getMovable()
        {
            return futureMovable;
        }

        @Override
        public CompletableFuture<Optional<AbstractMovable>> getMovable(IPPlayer player)
        {
            return futureMovable.thenApply(
                movableOpt ->
                {
                    final boolean playerIsPresent =
                        movableOpt.map(movable -> movable.isOwner(player)).orElse(false);
                    return playerIsPresent ? movableOpt : Optional.empty();
                });
        }
    }
}

