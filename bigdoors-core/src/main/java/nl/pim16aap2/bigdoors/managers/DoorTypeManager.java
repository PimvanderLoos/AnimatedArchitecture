package nl.pim16aap2.bigdoors.managers;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.NonFinal;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.util.PLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
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
    private final Map<DoorType, DoorTypeInfo> doorTypeToID = new ConcurrentHashMap<>();
    @NotNull
    private final Map<Long, DoorType> doorTypeFromID = new ConcurrentHashMap<>();
    @NotNull
    private final Map<String, DoorType> doorTypeFromName = new ConcurrentHashMap<>();

    /**
     * Gets all registered AND enabled {@link DoorType}s.
     */
    @Getter(onMethod = @__({@NotNull}))
    private final List<DoorType> sortedDoorTypes = Collections.synchronizedList(new ArrayList<DoorType>()
    {
        @Override
        public boolean add(DoorType doorType)
        {
            super.add(doorType);
            sortedDoorTypes.sort(Comparator.comparing(DoorType::getSimpleName));
            return true;
        }
    });

    private DoorTypeManager()
    {
    }

    /**
     * Gets all {@link DoorType}s that are currently registered.
     *
     * @return All {@link DoorType}s that are currently registered.
     */
    @NotNull
    public Set<DoorType> getRegisteredDoorTypes()
    {
        return Collections.unmodifiableSet(doorTypeToID.keySet());
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
        for (final Map.Entry<DoorType, DoorTypeInfo> doorType : doorTypeToID.entrySet())
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
        return doorTypeToID.containsKey(doorType);
    }

    /**
     * Checks if an {@link DoorType} is enabled.
     *
     * @param doorTypeID The ID of the {@link DoorType} to check.
     * @return True if the {@link DoorType} is enabled, otherwise false.
     */
    public boolean isRegistered(final long doorTypeID)
    {
        return doorTypeFromID.containsKey(doorTypeID);
    }

    /**
     * Obtains the class of an {@link DoorType} as described by its ID.
     *
     * @param doorTypeID The ID of the {@link DoorType}.
     * @return An optional that contains the class of the {@link DoorType} if it is registered.
     */
    @NotNull
    public Optional<DoorType> getDoorType(final long doorTypeID)
    {
        return Optional.ofNullable(doorTypeFromID.get(doorTypeID));
    }

    /**
     * Tries to get a {@link DoorType} from it name as defined by {@link DoorType#getSimpleName()}. This method is
     * case-insensitive.
     *
     * @param typeName The name of the type.
     * @return The {@link DoorType} to retrieve, if possible.
     */
    @NotNull
    public Optional<DoorType> getDoorType(final @NotNull String typeName)
    {
        return Optional.ofNullable(doorTypeFromName.get(typeName.toLowerCase()));
    }

    /**
     * Obtains the ID of a {@link DoorType}.
     *
     * @param doorType The {@link Class} of the {@link DoorType}.
     * @return An optional that contains the ID of the {@link DoorType} if it is registered.
     */
    @NotNull
    public OptionalLong getDoorTypeID(final @NotNull DoorType doorType)
    {
        final @Nullable DoorTypeInfo info = doorTypeToID.get(doorType);
        return info == null ? OptionalLong.empty() : OptionalLong.of(info.id);
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
        final @Nullable DoorTypeInfo info = doorTypeToID.get(doorType);
        return info != null && info.status;
    }

    /**
     * Registers a {@link DoorType}.
     *
     * @param doorType The {@link DoorType} to register.
     * @return True if registration was successful.
     */
    @NotNull
    public CompletableFuture<Boolean> registerDoorType(final @NotNull DoorType doorType)
    {
        return registerDoorType(doorType, true);
    }

    /**
     * Unregisters a door-type. Note that it does <b>NOT</b> remove it or its doors from the database and that after
     * unregistering it, that won't be possible anymore either.
     * <p>
     * Once unregistered, this type will be completely disabled and doors of this type cannot be used for anything.
     *
     * @param doorType The type to unregister.
     */
    public void unregisterDoorType(final @NotNull DoorType doorType)
    {
        final @Nullable DoorTypeInfo doorTypeInfo = doorTypeToID.remove(doorType);
        if (doorTypeInfo == null)
        {
            PLogger.get().warn("Trying to unregister door of type: " + doorType.getSimpleName() + ", but it isn't " +
                                   "registered already!");
            return;
        }
        doorTypeFromID.remove(doorTypeInfo.id);
        doorTypeFromName.remove(doorTypeInfo.name);
        sortedDoorTypes.remove(doorType);
    }

    /**
     * Registers a {@link DoorType}.
     *
     * @param doorType  The {@link DoorType} to register.
     * @param isEnabled Whether or not this {@link DoorType} should be enabled or not. Default = true.
     * @return True if registration was successful.
     */
    @NotNull
    public CompletableFuture<Boolean> registerDoorType(final @NotNull DoorType doorType, final boolean isEnabled)
    {
        CompletableFuture<Long> registrationResult = BigDoors.get().getDatabaseManager().registerDoorType(doorType);
        return registrationResult.handle(
            (doorTypeID, throwable) ->
            {
                if (doorTypeID < 1)
                    return false;
                doorTypeToID.put(doorType, new DoorTypeInfo(doorTypeID, isEnabled, doorType.getSimpleName()));
                doorTypeFromID.put(doorTypeID, doorType);
                doorTypeFromName.put(doorType.getSimpleName(), doorType);
                if (isEnabled)
                    sortedDoorTypes.add(doorType);
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
        final @Nullable DoorTypeInfo info = doorTypeToID.get(doorType);
        if (info != null)
        {
            info.status = isEnabled;
            if (!isEnabled)
                sortedDoorTypes.remove(doorType);
        }
    }

    /**
     * Describes the ID value and the enabled status of a {@link DoorType}.
     *
     * @author Pim
     */
    @Value
    @AllArgsConstructor
    private static class DoorTypeInfo
    {
        long id;
        @NonFinal
        boolean status;
        String name;
    }
}
