package nl.pim16aap2.bigdoors.managers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This class manages all {@link DoorType}s. Before a type can be used, it will have to be registered here.
 *
 * @author Pim
 */
public final class DoorTypeManager
{
    @NotNull
    private static final DoorTypeManager instance = new DoorTypeManager();
    @NotNull
    private final Map<DoorType, Long> doorTypesToID = new HashMap<>();
    @NotNull
    private final Map<Long, DoorType> doorTypesFromID = new HashMap<>();

    private DoorTypeManager()
    {
    }

    /**
     * Obtain the instance of this class.
     *
     * @return The instance of this class.
     */
    @NotNull
    public static DoorTypeManager get()
    {
        return instance;
    }

    /**
     * Checks if an {@link DoorType} is enabled.
     *
     * @param doorType The {@link DoorType} to check.
     * @return True if the {@link DoorType} is enabled, otherwise false.
     */
    public boolean isRegistered(final @NotNull DoorType doorType)
    {
        return doorTypesToID.containsKey(doorType);
    }

    /**
     * Checks if an {@link DoorType} is enabled.
     *
     * @param doorTypeID The ID of the {@link DoorType} to check.
     * @return True if the {@link DoorType} is enabled, otherwise false.
     */
    public boolean isRegistered(final long doorTypeID)
    {
        return doorTypesFromID.containsKey(doorTypeID);
    }

    /**
     * Obtains the class of an {@link DoorType} as described by its ID.
     *
     * @param doorTypeID The ID of the {@link DoorType}.
     * @return An optional that contains the class of the {@link DoorType} if it is registered.
     */
    public Optional<DoorType> getDoorType(final long doorTypeID)
    {
        return Optional.ofNullable(doorTypesFromID.get(doorTypeID));
    }

    /**
     * Obtains the class of an {@link DoorType} as described by its {@link Class}.
     *
     * @param doorType The {@link Class} of the {@link DoorType}.
     * @return An optional that contains the ID of the {@link DoorType} if it is registered.
     */
    public Optional<Long> getDoorType(final @NotNull DoorType doorType)
    {
        return Optional.ofNullable(doorTypesToID.get(doorType));
    }

    /**
     * Registers a {@link DoorType}.
     *
     * @param doorType The {@link DoorType} to register.
     * @return True if registration was successful.
     */
    public boolean registerDoorType(final @NotNull DoorType doorType)
    {
        long doorTypeID = BigDoors.get().getDatabaseManager().registerDoorType(doorType);
        if (doorTypeID < 0)
            return false;

        doorTypesToID.put(doorType, doorTypeID);
        doorTypesFromID.put(doorTypeID, doorType);

        return true;
    }
}
