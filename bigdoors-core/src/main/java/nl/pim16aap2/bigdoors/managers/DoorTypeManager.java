package nl.pim16aap2.bigdoors.managers;

import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.util.Restartable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class manages all {@link DoorType}s. Before a type can be used, it will have to be registered here.
 *
 * @author Pim
 */
public final class DoorTypeManager extends Restartable
{
    @NotNull
    private static final DoorTypeManager INSTANCE = new DoorTypeManager();
    @NotNull
    private final Map<DoorType, DoorRegistrationStatus> doorTypeStatus = new ConcurrentHashMap<>();
    @NotNull
    private final Map<String, DoorType> doorTypeFromName = new ConcurrentHashMap<>();
    private final @NonNull Map<String, DoorType> doorTypeFromFullName = new ConcurrentHashMap<>();

    /**
     * Gets all registered AND enabled {@link DoorType}s.
     */
    @Getter(onMethod = @__({@NotNull}))
    private final List<DoorType> sortedDoorTypes = new CopyOnWriteArrayList<>()
    {
        @Override
        public boolean add(DoorType doorType)
        {
            super.add(doorType);
            sortedDoorTypes.sort(Comparator.comparing(DoorType::getSimpleName));
            return true;
        }
    };

    private DoorTypeManager()
    {
        super(BigDoors.get());
    }

    /**
     * Gets all {@link DoorType}s that are currently registered.
     *
     * @return All {@link DoorType}s that are currently registered.
     */
    public @NotNull Set<DoorType> getRegisteredDoorTypes()
    {
        return Collections.unmodifiableSet(doorTypeStatus.keySet());
    }

    /**
     * Gets all {@link DoorType}s that are currently enabled.
     *
     * @return All {@link DoorType}s that are currently enabled.
     */
    public @NotNull List<DoorType> getEnabledDoorTypes()
    {
        final List<DoorType> enabledDoorTypes = new ArrayList<>();
        for (final Map.Entry<DoorType, DoorRegistrationStatus> doorType : doorTypeStatus.entrySet())
            if (doorType.getValue().status)
                enabledDoorTypes.add(doorType.getKey());
        return enabledDoorTypes;
    }

    /**
     * Obtain the instance of this class.
     *
     * @return The instance of this class.
     */
    public static @NotNull DoorTypeManager get()
    {
        return INSTANCE;
    }

    /**
     * Checks if an {@link DoorType} is enabled.
     *
     * @param doorType The {@link DoorType} to check.
     * @return True if the {@link DoorType} is enabled, otherwise false.
     */
    public boolean isRegistered(final @NotNull DoorType doorType)
    {
        return doorTypeStatus.containsKey(doorType);
    }

    /**
     * Tries to get a {@link DoorType} from it name as defined by {@link DoorType#getSimpleName()}. This method is
     * case-insensitive.
     *
     * @param typeName The name of the type.
     * @return The {@link DoorType} to retrieve, if possible.
     */
    public @NotNull Optional<DoorType> getDoorType(final @NotNull String typeName)
    {
        return Optional.ofNullable(doorTypeFromName.get(typeName.toLowerCase()));
    }

    /**
     * Tries to get a {@link DoorType} from its fully qualified name as defined by {@link DoorType#getSimpleName()}.
     * This method is case-sensitive.
     *
     * @param fullName The fully qualified name of the type.
     * @return The {@link DoorType} to retrieve, if possible.
     */
    public @NonNull Optional<DoorType> getDoorTypeFromFullName(final @NonNull String fullName)
    {
        return Optional.ofNullable(doorTypeFromFullName.get(fullName));
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
        final @Nullable DoorTypeManager.DoorRegistrationStatus info = doorTypeStatus.get(doorType);
        return info != null && info.status;
    }

    /**
     * Registers a {@link DoorType}.
     *
     * @param doorType The {@link DoorType} to register.
     * @return True if registration was successful.
     */
    public void registerDoorType(final @NotNull DoorType doorType)
    {
        registerDoorType(doorType, true);
    }

    /**
     * Registers a {@link DoorType}.
     *
     * @param doorType  The {@link DoorType} to register.
     * @param isEnabled Whether or not this {@link DoorType} should be enabled or not. Default = true.
     */
    public void registerDoorType(final @NotNull DoorType doorType, final boolean isEnabled)
    {
        BigDoors.get().getPLogger().info("Registering door type: " + doorType.toString() + "...");

        doorTypeStatus.put(doorType, new DoorRegistrationStatus(doorType.getFullName(), isEnabled));
        doorTypeFromName.put(doorType.getSimpleName(), doorType);
        doorTypeFromFullName.put(doorType.getFullName(), doorType);

        if (isEnabled)
            sortedDoorTypes.add(doorType);
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
        if (doorTypeStatus.remove(doorType) == null)
        {
            BigDoors.get().getPLogger()
                    .warn("Trying to unregister door of type: " + doorType.getSimpleName() + ", but it isn't " +
                              "registered already!");
            return;
        }
        doorTypeFromName.remove(doorType.getSimpleName());
        doorTypeFromFullName.remove(doorType.getFullName());
        sortedDoorTypes.remove(doorType);
    }

    /**
     * Changes the status of a {@link DoorType}. If disabled, this type cannot be toggled or created.
     *
     * @param doorType  The {@link DoorType} to enabled or disable.
     * @param isEnabled True to enabled this {@link DoorType} (default), or false to disable it.
     */
    public void setDoorTypeEnabled(final @NotNull DoorType doorType, final boolean isEnabled)
    {
        final @Nullable DoorTypeManager.DoorRegistrationStatus info = doorTypeStatus.get(doorType);
        if (info != null)
        {
            info.status = isEnabled;
            if (!isEnabled)
                sortedDoorTypes.remove(doorType);
        }
    }

    /**
     * Registers a list of {@link DoorType}s. See {@link #registerDoorType(DoorType)}.
     *
     * @param doorTypes The list of {@link DoorType}s to register.
     */
    public void registerDoorTypes(final @NotNull List<DoorType> doorTypes)
    {
        doorTypes.forEach(this::registerDoorType);
    }

    @Override
    public void restart()
    {
        shutdown();
    }

    @Override
    public void shutdown()
    {
        doorTypeStatus.clear();
        sortedDoorTypes.clear();
        doorTypeFromName.clear();
        doorTypeFromFullName.clear();
    }

    /**
     * Describes the full name and the enabled status of a {@link DoorType}.
     *
     * @author Pim
     */
    @Value
    private static class DoorRegistrationStatus
    {
        String fullName;
        @NonFinal
        boolean status;
    }
}
