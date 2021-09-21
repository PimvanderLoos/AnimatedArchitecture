package nl.pim16aap2.bigdoors.util.doorretriever;

import nl.pim16aap2.bigdoors.api.IConfigLoader;
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

    @Inject
    public DoorRetrieverFactory(DatabaseManager databaseManager, IConfigLoader config,
                                DoorSpecificationManager doorSpecificationManager)
    {
        this.databaseManager = databaseManager;
        this.config = config;
        this.doorSpecificationManager = doorSpecificationManager;
    }

    /**
     * Creates a new {@link DoorRetrieverFactory} from its ID.
     *
     * @param doorID
     *     The identifier (name or UID) of the door.
     * @return The new {@link DoorRetrieverFactory}.
     */
    public DoorRetriever of(String doorID)
    {
        final OptionalLong doorUID = Util.parseLong(doorID);
        return doorUID.isPresent() ?
               new DoorRetriever.DoorUIDRetriever(databaseManager, doorUID.getAsLong()) :
               new DoorRetriever.DoorNameRetriever(databaseManager, config, doorSpecificationManager, doorID);
    }

    /**
     * Creates a new {@link DoorRetrieverFactory} from its UID.
     *
     * @param doorUID
     *     The UID of the door.
     * @return The new {@link DoorRetrieverFactory}.
     */
    public DoorRetriever of(long doorUID)
    {
        return new DoorRetriever.DoorUIDRetriever(databaseManager, doorUID);
    }

    /**
     * Creates a new {@link DoorRetrieverFactory} from the door object itself.
     *
     * @param door
     *     The door object itself.
     * @return The new {@link DoorRetrieverFactory}.
     */
    public DoorRetriever of(AbstractDoor door)
    {
        return DoorRetrieverFactory.ofDoor(door);
    }

    /**
     * Creates a new {@link DoorRetrieverFactory} from a door that is being retrieved.
     *
     * @param door
     *     The door that is being retrieved.
     * @return The new {@link DoorRetrieverFactory}.
     */
    public DoorRetriever of(CompletableFuture<Optional<AbstractDoor>> door)
    {
        return DoorRetrieverFactory.ofDoor(door);
    }

    /**
     * Creates a new {@link DoorRetrieverFactory} from the door object itself.
     *
     * @param door
     *     The door object itself.
     * @return The new {@link DoorRetrieverFactory}.
     */
    public static DoorRetriever ofDoor(@Nullable AbstractDoor door)
    {
        return new DoorRetriever.DoorObjectRetriever(door);
    }

    /**
     * Creates a new {@link DoorRetrieverFactory} from a door that is still being retrieved.
     *
     * @param door
     *     The future door.
     * @return The new {@link DoorRetrieverFactory}.
     */
    public static DoorRetriever ofDoor(CompletableFuture<Optional<AbstractDoor>> door)
    {
        return new DoorRetriever.FutureDoorRetriever(door);
    }

    /**
     * Creates a new {@link DoorRetrieverFactory} from a list of doors.
     *
     * @param doors
     *     The doors.
     * @return The new {@link DoorRetrieverFactory}.
     */
    public static DoorRetriever ofDoors(List<AbstractDoor> doors)
    {
        return new DoorRetriever.DoorListRetriever(doors);
    }
}
