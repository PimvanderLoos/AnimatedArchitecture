package nl.pim16aap2.bigdoors.util.doorretriever;

import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DoorSpecificationManager;
import nl.pim16aap2.bigdoors.util.Util;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the factory for {@link DoorRetriever}s.
 *
 * @author Pim
 */
public final class DoorRetrieverFactory
{
    private final DatabaseManager databaseManager;
    private final IConfigLoader config;
    private final DoorSpecificationManager doorSpecificationManager;
    private final DoorFinderCache doorFinderCache;

    @Inject
    public DoorRetrieverFactory(
        DatabaseManager databaseManager, IConfigLoader config, DoorSpecificationManager doorSpecificationManager,
        DoorFinderCache doorFinderCache)
    {
        this.databaseManager = databaseManager;
        this.config = config;
        this.doorSpecificationManager = doorSpecificationManager;
        this.doorFinderCache = doorFinderCache;
    }

    /**
     * Creates a new {@link DoorRetriever} from its ID.
     *
     * @param doorID
     *     The identifier (name or UID) of the door.
     * @return The new {@link DoorRetriever}.
     */
    public DoorRetriever of(String doorID)
    {
        final OptionalLong doorUID = Util.parseLong(doorID);
        return doorUID.isPresent() ?
               new DoorRetriever.DoorUIDRetriever(databaseManager, doorUID.getAsLong()) :
               new DoorRetriever.DoorNameRetriever(databaseManager, config, doorSpecificationManager, doorID);
    }

    /**
     * Creates a new {@link DoorRetriever} from its UID.
     *
     * @param doorUID
     *     The UID of the door.
     * @return The new {@link DoorRetriever}.
     */
    public DoorRetriever of(long doorUID)
    {
        return new DoorRetriever.DoorUIDRetriever(databaseManager, doorUID);
    }

    /**
     * Creates a new {@link DoorRetriever} from the door object itself.
     *
     * @param door
     *     The door object itself.
     * @return The new {@link DoorRetriever}.
     */
    public DoorRetriever of(AbstractDoor door)
    {
        return DoorRetrieverFactory.ofDoor(door);
    }

    /**
     * Creates a new {@link DoorRetriever} from a door that is being retrieved.
     *
     * @param door
     *     The door that is being retrieved.
     * @return The new {@link DoorRetriever}.
     */
    public DoorRetriever of(CompletableFuture<Optional<AbstractDoor>> door)
    {
        return DoorRetrieverFactory.ofDoor(door);
    }

    /**
     * Gets the {@link DoorFinder} to find doors from partial string matches.
     * <p>
     * If a {@link DoorFinder} already exists for this
     *
     * @param commandSender
     *     The command sender (e.g. player) that is responsible for searching for the door.
     * @param input
     *     The input to use as search query.
     * @param mode
     *     The mode to use for obtaining a {@link DoorFinder} instance. Defaults to {@link DoorFinderMode#USE_CACHE}.
     * @param maxPermission
     *     The maximum permission (inclusive) of the door owner of the doors to find. Does not apply if the command
     *     sender is not a player. Defaults to 0.
     * @return The {@link DoorFinder} instance.
     */
    public DoorFinder search(ICommandSender commandSender, String input, DoorFinderMode mode, int maxPermission)
    {
        return mode == DoorFinderMode.USE_CACHE ?
               doorFinderCache.getDoorFinder(commandSender, input, maxPermission) :
               new DoorFinder(this, databaseManager, commandSender, input, maxPermission);
    }

    /**
     * See {@link #search(ICommandSender, String, DoorFinderMode)}.
     */
    public DoorFinder search(ICommandSender commandSender, String input, int maxPermission)
    {
        return search(commandSender, input, DoorFinderMode.USE_CACHE, maxPermission);
    }

    /**
     * See {@link #search(ICommandSender, String, DoorFinderMode)}.
     */
    public DoorFinder search(ICommandSender commandSender, String input)
    {
        return search(commandSender, input, DoorFinderMode.USE_CACHE);
    }

    /**
     * See {@link #search(ICommandSender, String, DoorFinderMode, int)}.
     */
    public DoorFinder search(ICommandSender commandSender, String input, DoorFinderMode mode)
    {
        return search(commandSender, input, mode, 0);
    }

    /**
     * Creates a new {@link DoorRetriever} from the door object itself.
     *
     * @param door
     *     The door object itself.
     * @return The new {@link DoorRetriever}.
     */
    public static DoorRetriever ofDoor(@Nullable AbstractDoor door)
    {
        return new DoorRetriever.DoorObjectRetriever(door);
    }

    /**
     * Creates a new {@link DoorRetriever} from a door that is still being retrieved.
     *
     * @param door
     *     The future door.
     * @return The new {@link DoorRetriever}.
     */
    public static DoorRetriever ofDoor(CompletableFuture<Optional<AbstractDoor>> door)
    {
        return new DoorRetriever.FutureDoorRetriever(door);
    }

    /**
     * Creates a new {@link DoorRetriever} from a list of doors.
     *
     * @param doors
     *     The doors.
     * @return The new {@link DoorRetriever}.
     */
    public static DoorRetriever ofDoors(List<AbstractDoor> doors)
    {
        return new DoorRetriever.DoorListRetriever(doors);
    }

    /**
     * Creates a new {@link DoorRetriever} from a list of doors.
     *
     * @param doors
     *     The doors.
     * @return The new {@link DoorRetriever}.
     */
    public static DoorRetriever ofDoors(CompletableFuture<List<AbstractDoor>> doors)
    {
        return new DoorRetriever.FutureDoorListRetriever(doors);
    }

    /**
     * Represents different ways a door finder can be instantiated.
     */
    public enum DoorFinderMode
    {
        /**
         * Re-use a cached door finder if possible. If no cached version is available, a new one will be instantiated.
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
