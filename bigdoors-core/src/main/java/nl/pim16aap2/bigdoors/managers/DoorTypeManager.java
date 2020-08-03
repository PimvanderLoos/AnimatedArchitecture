package nl.pim16aap2.bigdoors.managers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

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
    private final Map<DoorType, DoorTypeInfo> doorTypesToID = new ConcurrentHashMap<>();
    @NotNull
    private final Map<Long, DoorType> doorTypesFromID = new ConcurrentHashMap<>();

    private DoorTypeManager()
    {
    }

    /**
     * Gets all {@link DoorType}s that are currently registered.
     *
     * @return All {@link DoorType}s that are currently registered.
     */
    public Set<DoorType> getRegisteredDoorTypes()
    {
        return Collections.unmodifiableSet(doorTypesToID.keySet());
    }

    /**
     * Gets all {@link DoorType}s that are currently enabled.
     *
     * @return All {@link DoorType}s that are currently enabled.
     */
    @NotNull
    public List<DoorType> getEnabledDoorTypes()
    {
        final List<DoorType> enabledDoorTypes = new ArrayList<>();
        for (final Map.Entry<DoorType, DoorTypeInfo> doorType : doorTypesToID.entrySet())
            if (doorType.getValue().status)
                enabledDoorTypes.add(doorType.getKey());
        return enabledDoorTypes;
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
    public Optional<DoorType> getDoorTypeID(final long doorTypeID)
    {
        return Optional.ofNullable(doorTypesFromID.get(doorTypeID));
    }

    /**
     * Obtains the ID of a {@link DoorType}.
     *
     * @param doorType The {@link Class} of the {@link DoorType}.
     * @return An optional that contains the ID of the {@link DoorType} if it is registered.
     */
    public Optional<Long> getDoorTypeID(final @NotNull DoorType doorType)
    {
        final @Nullable DoorTypeInfo info = doorTypesToID.get(doorType);
        return info == null ? Optional.empty() : Optional.of(info.id);
    }

    /**
     * Checks if a {@link DoorType} is enabled or not. Disabled {@link DoorType}s cannot be toggled or created.
     * <p>
     * {@link DoorType}s that are not registered, are disabled by definition.
     *
     * @param doorType The {@link DoorType} to check.
     * @return True if this {@link DoorType} is both registered and enabled.
     */
    public boolean isDoorTypeEnabled(final @NotNull DoorType doorType)
    {
        final @Nullable DoorTypeInfo info = doorTypesToID.get(doorType);
        return info != null && info.status;
    }

    /**
     * Registers a {@link DoorType}.
     *
     * @param doorType The {@link DoorType} to register.
     * @return True if registration was successful.
     */
    public CompletableFuture<Boolean> registerDoorType(final @NotNull DoorType doorType)
    {
        return registerDoorType(doorType, true);
    }

    /**
     * Registers a {@link DoorType}.
     *
     * @param doorType  The {@link DoorType} to register.
     * @param isEnabled Whether or not this {@link DoorType} should be enabled or not. Default = true.
     * @return True if registration was successful.
     */
    public CompletableFuture<Boolean> registerDoorType(final @NotNull DoorType doorType, final boolean isEnabled)
    {
        CompletableFuture<Long> registrationResult = BigDoors.get().getDatabaseManager().registerDoorType(doorType);
        return registrationResult.handle(
            (doorTypeID, throwable) ->
            {
                if (doorTypeID < 1)
                    return false;
                doorTypesToID.put(doorType, new DoorTypeInfo(doorTypeID, isEnabled));
                doorTypesFromID.put(doorTypeID, doorType);
                return true;
            });
    }

    /**
     * Changes the status of a {@link DoorType}. If disabled, this type cannot be toggled or created.
     *
     * @param doorType  The {@link DoorType} to enabled or disable.
     * @param isEnabled True to enabled this {@link DoorType} (default), or false to disable it.
     */
    public void setDoorTypeEnabled(final @NotNull DoorType doorType, final boolean isEnabled)
    {
        final @Nullable DoorTypeInfo info = doorTypesToID.get(doorType);
        if (info != null)
            info.status = isEnabled;
    }

    /**
     * Describes the ID value and the enabled status of a {@link DoorType}.
     *
     * @author Pim
     */
    private static final class DoorTypeInfo
    {
        private final long id;
        private boolean status;

        public DoorTypeInfo(final long id, final boolean status)
        {
            this.id = id;
            this.status = status;
        }
    }
}
