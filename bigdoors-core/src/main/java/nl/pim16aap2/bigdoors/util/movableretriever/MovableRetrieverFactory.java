package nl.pim16aap2.bigdoors.util.movableretriever;

import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.MovableSpecificationManager;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.PermissionLevel;
import nl.pim16aap2.bigdoors.util.Util;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the factory for {@link MovableRetriever}s.
 *
 * @author Pim
 */
public final class MovableRetrieverFactory
{
    private final DatabaseManager databaseManager;
    private final IConfigLoader config;
    private final MovableSpecificationManager movableSpecificationManager;
    private final MovableFinderCache movableFinderCache;
    private final ILocalizer localizer;
    private final ITextFactory textFactory;

    @Inject
    public MovableRetrieverFactory(
        DatabaseManager databaseManager, IConfigLoader config, MovableSpecificationManager movableSpecificationManager,
        MovableFinderCache movableFinderCache, ILocalizer localizer, ITextFactory textFactory)
    {
        this.databaseManager = databaseManager;
        this.config = config;
        this.movableSpecificationManager = movableSpecificationManager;
        this.movableFinderCache = movableFinderCache;
        this.localizer = localizer;
        this.textFactory = textFactory;
    }

    /**
     * Creates a new {@link MovableRetriever} from its ID.
     *
     * @param movableID
     *     The identifier (name or UID) of the movable.
     * @return The new {@link MovableRetriever}.
     */
    public MovableRetriever of(String movableID)
    {
        final OptionalLong movableUID = Util.parseLong(movableID);
        return movableUID.isPresent() ?
               new MovableRetriever.MovableUIDRetriever(databaseManager, movableUID.getAsLong()) :
               new MovableRetriever.MovableNameRetriever(databaseManager, config, movableSpecificationManager,
                                                         localizer, textFactory, movableID);
    }

    /**
     * Creates a new {@link MovableRetriever} from its UID.
     *
     * @param movableUID
     *     The UID of the movable.
     * @return The new {@link MovableRetriever}.
     */
    public MovableRetriever of(long movableUID)
    {
        return new MovableRetriever.MovableUIDRetriever(databaseManager, movableUID);
    }

    /**
     * Creates a new {@link MovableRetriever} from the movable object itself.
     *
     * @param movable
     *     The movable object itself.
     * @return The new {@link MovableRetriever}.
     */
    public MovableRetriever of(AbstractMovable movable)
    {
        return MovableRetrieverFactory.ofMovable(movable);
    }

    /**
     * Creates a new {@link MovableRetriever} from a movable that is being retrieved.
     *
     * @param movable
     *     The movable that is being retrieved.
     * @return The new {@link MovableRetriever}.
     */
    public MovableRetriever of(CompletableFuture<Optional<AbstractMovable>> movable)
    {
        return MovableRetrieverFactory.ofMovable(movable);
    }

    /**
     * Gets the {@link MovableFinder} to find movables from partial string matches.
     * <p>
     * If a {@link MovableFinder} already exists for this
     *
     * @param commandSender
     *     The command sender (e.g. player) that is responsible for searching for the movable.
     * @param input
     *     The input to use as search query.
     * @param mode
     *     The mode to use for obtaining a {@link MovableFinder} instance. Defaults to
     *     {@link MovableFinderMode#USE_CACHE}.
     * @param maxPermission
     *     The maximum permission (inclusive) of the movable owner of the movables to find. Does not apply if the
     *     command sender is not a player. Defaults to {@link PermissionLevel#CREATOR}.
     * @return The {@link MovableFinder} instance.
     */
    public MovableFinder search(
        ICommandSender commandSender, String input, MovableFinderMode mode, PermissionLevel maxPermission)
    {
        return mode == MovableFinderMode.USE_CACHE ?
               movableFinderCache.getMovableFinder(commandSender, input, maxPermission) :
               new MovableFinder(this, databaseManager, commandSender, input, maxPermission);
    }

    /**
     * See {@link #search(ICommandSender, String, MovableFinderMode)}.
     */
    public MovableFinder search(ICommandSender commandSender, String input, PermissionLevel maxPermission)
    {
        return search(commandSender, input, MovableFinderMode.USE_CACHE, maxPermission);
    }

    /**
     * See {@link #search(ICommandSender, String, MovableFinderMode)}.
     */
    public MovableFinder search(ICommandSender commandSender, String input)
    {
        return search(commandSender, input, MovableFinderMode.USE_CACHE);
    }

    /**
     * See {@link #search(ICommandSender, String, MovableFinderMode, PermissionLevel)}.
     */
    public MovableFinder search(ICommandSender commandSender, String input, MovableFinderMode mode)
    {
        return search(commandSender, input, mode, PermissionLevel.CREATOR);
    }

    /**
     * Creates a new {@link MovableRetriever} from the movable object itself.
     *
     * @param movable
     *     The movable object itself.
     * @return The new {@link MovableRetriever}.
     */
    public static MovableRetriever ofMovable(@Nullable AbstractMovable movable)
    {
        return new MovableRetriever.MovableObjectRetriever(movable);
    }

    /**
     * Creates a new {@link MovableRetriever} from a movable that is still being retrieved.
     *
     * @param movable
     *     The future movable.
     * @return The new {@link MovableRetriever}.
     */
    public static MovableRetriever ofMovable(CompletableFuture<Optional<AbstractMovable>> movable)
    {
        return new MovableRetriever.FutureMovableRetriever(movable);
    }

    /**
     * Creates a new {@link MovableRetriever} from a list of movables.
     *
     * @param movables
     *     The movables.
     * @return The new {@link MovableRetriever}.
     */
    public static MovableRetriever ofMovables(List<AbstractMovable> movables)
    {
        return new MovableRetriever.MovableListRetriever(movables);
    }

    /**
     * Creates a new {@link MovableRetriever} from a list of movables.
     *
     * @param movables
     *     The movables.
     * @return The new {@link MovableRetriever}.
     */
    public static MovableRetriever ofMovables(CompletableFuture<List<AbstractMovable>> movables)
    {
        return new MovableRetriever.FutureMovableListRetriever(movables);
    }

    /**
     * Represents different ways a movable finder can be instantiated.
     */
    public enum MovableFinderMode
    {
        /**
         * Re-use a cached movable finder if possible. If no cached version is available, a new one will be
         * instantiated.
         */
        USE_CACHE,

        /**
         * Create a new instance of the finder regardless of whether a cached mapping exists.
         * <p>
         * The new instance is not added to the cache.
         */
        NEW_INSTANCE
    }
}
