package nl.pim16aap2.bigdoors.managers;

import lombok.Getter;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.debugging.DebugReporter;
import nl.pim16aap2.bigdoors.api.debugging.IDebuggable;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.util.SafeStringBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

/**
 * This class manages all {@link DoorType}s. Before a type can be used, it will have to be registered here.
 *
 * @author Pim
 */
@Singleton
@Flogger
public final class DoorTypeManager extends Restartable implements IDebuggable
{
    private final Map<DoorType, DoorRegistrationStatus> doorTypeStatus = new ConcurrentHashMap<>();
    private final Map<String, DoorType> doorTypeFromName = new ConcurrentHashMap<>();
    private final Map<String, DoorType> doorTypeFromFullName = new ConcurrentHashMap<>();

    @Inject
    public DoorTypeManager(RestartableHolder holder, DebugReporter debugReporter)
    {
        super(holder);
        debugReporter.registerDebuggable(this);
    }

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

    /**
     * Gets all {@link DoorType}s that are currently registered.
     *
     * @return All {@link DoorType}s that are currently registered.
     */
    public Set<DoorType> getRegisteredDoorTypes()
    {
        return Collections.unmodifiableSet(doorTypeStatus.keySet());
    }

    private List<DoorType> getDoorTypesWithStatus(boolean status)
    {
        final List<DoorType> enabledDoorTypes = new ArrayList<>();
        for (final Map.Entry<DoorType, DoorRegistrationStatus> doorType : doorTypeStatus.entrySet())
            if (doorType.getValue().status == status)
                enabledDoorTypes.add(doorType.getKey());
        return enabledDoorTypes;
    }

    /**
     * Gets all {@link DoorType}s that are currently enabled.
     *
     * @return All {@link DoorType}s that are currently enabled.
     */
    public List<DoorType> getEnabledDoorTypes()
    {
        return getDoorTypesWithStatus(true);
    }

    /**
     * Gets all {@link DoorType}s that are currently disabled.
     *
     * @return All {@link DoorType}s that are currently disabled.
     */
    public List<DoorType> getDisabledDoorTypes()
    {
        return getDoorTypesWithStatus(false);
    }

    /**
     * Checks if an {@link DoorType} is enabled.
     *
     * @param doorType
     *     The {@link DoorType} to check.
     * @return True if the {@link DoorType} is enabled, otherwise false.
     */
    public boolean isRegistered(DoorType doorType)
    {
        return doorTypeStatus.containsKey(doorType);
    }

    /**
     * Tries to get a {@link DoorType} from it name as defined by {@link DoorType#getSimpleName()}. This method is
     * case-insensitive.
     *
     * @param typeName
     *     The name of the type.
     * @return The {@link DoorType} to retrieve, if possible.
     */
    public Optional<DoorType> getDoorType(String typeName)
    {
        return Optional.ofNullable(doorTypeFromName.get(typeName.toLowerCase(Locale.ENGLISH)));
    }

    /**
     * Tries to get a {@link DoorType} from its fully qualified name as defined by {@link DoorType#getSimpleName()}.
     * This method is case-sensitive.
     *
     * @param fullName
     *     The fully qualified name of the type.
     * @return The {@link DoorType} to retrieve, if possible.
     */
    public Optional<DoorType> getDoorTypeFromFullName(String fullName)
    {
        return Optional.ofNullable(doorTypeFromFullName.get(fullName));
    }

    /**
     * Checks if a {@link DoorType} is enabled or not. Disabled {@link DoorType}s cannot be toggled or created.
     * <p>
     * {@link DoorType}s that are not registered, are disabled by definition.
     *
     * @param doorType
     *     The {@link DoorType} to check.
     * @return True if this {@link DoorType} is both registered and enabled.
     */
    public boolean isDoorTypeEnabled(DoorType doorType)
    {
        final @Nullable DoorTypeManager.DoorRegistrationStatus info = doorTypeStatus.get(doorType);
        return info != null && info.status;
    }

    /**
     * Registers a {@link DoorType}.
     *
     * @param doorType
     *     The {@link DoorType} to register.
     * @return True if registration was successful.
     */
    public void registerDoorType(DoorType doorType)
    {
        registerDoorType(doorType, true);
    }

    /**
     * Registers a {@link DoorType}.
     *
     * @param doorType
     *     The {@link DoorType} to register.
     * @param isEnabled
     *     Whether this {@link DoorType} should be enabled or not. Default = true.
     */
    public void registerDoorType(DoorType doorType, boolean isEnabled)
    {
        log.at(Level.INFO).log("Registering door type: %s...", doorType);

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
     * @param doorType
     *     The type to unregister.
     */
    public void unregisterDoorType(DoorType doorType)
    {
        if (doorTypeStatus.remove(doorType) == null)
        {
            log.at(Level.WARNING).log("Trying to unregister door of type: %s, but it isn't registered already!",
                                      doorType.getSimpleName());
            return;
        }
        doorTypeFromName.remove(doorType.getSimpleName());
        doorTypeFromFullName.remove(doorType.getFullName());
        sortedDoorTypes.remove(doorType);
    }

    /**
     * Changes the status of a {@link DoorType}. If disabled, this type cannot be toggled or created.
     *
     * @param doorType
     *     The {@link DoorType} to enabled or disable.
     * @param isEnabled
     *     True to enable this {@link DoorType} (default), or false to disable it.
     */
    @SuppressWarnings("unused")
    public void setDoorTypeEnabled(DoorType doorType, boolean isEnabled)
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
     * @param doorTypes
     *     The list of {@link DoorType}s to register.
     */
    public void registerDoorTypes(List<DoorType> doorTypes)
    {
        doorTypes.forEach(this::registerDoorType);
    }

    @Override
    public void shutDown()
    {
        doorTypeStatus.clear();
        sortedDoorTypes.clear();
        doorTypeFromName.clear();
        doorTypeFromFullName.clear();
    }

    @Override
    public String getDebugInformation()
    {
        final SafeStringBuilder sb = new SafeStringBuilder("Registered door types:\n");
        for (final Map.Entry<DoorType, DoorRegistrationStatus> entry : doorTypeStatus.entrySet())
        {
            final DoorType doorType = entry.getKey();
            sb.append("- ").append(doorType::toString).append(": ");
            if (!entry.getValue().status)
                sb.append("DISABLED");
            else
                sb.append("\n").appendIndented(2, doorType::getDoorSerializer);
            sb.append('\n');
        }
        return sb.toString();
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
