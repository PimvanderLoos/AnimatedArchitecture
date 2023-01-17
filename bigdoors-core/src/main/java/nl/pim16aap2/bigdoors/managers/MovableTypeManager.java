package nl.pim16aap2.bigdoors.managers;

import lombok.Getter;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.debugging.DebuggableRegistry;
import nl.pim16aap2.bigdoors.api.debugging.IDebuggable;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.localization.LocalizationManager;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.util.SafeStringBuilder;
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

/**
 * This class manages all {@link MovableType}s. Before a type can be used, it will have to be registered here.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
@Singleton
@Flogger
public final class MovableTypeManager extends Restartable implements IDebuggable
{
    private static final boolean DEFAULT_IS_ENABLED = true;

    private final Map<MovableType, MovableRegistrationStatus> movableTypeStatus = new ConcurrentHashMap<>();
    private final Map<String, MovableType> movableTypeFromName = new ConcurrentHashMap<>();
    private final Map<String, MovableType> movableTypeFromFullName = new ConcurrentHashMap<>();
    private final LocalizationManager localizationManager;

    @Inject
    public MovableTypeManager(
        RestartableHolder holder, DebuggableRegistry debuggableRegistry, LocalizationManager localizationManager)
    {
        super(holder);
        this.localizationManager = localizationManager;
        debuggableRegistry.registerDebuggable(this);
    }

    /**
     * All registered AND enabled {@link MovableType}s.
     */
    @Getter
    private final List<MovableType> sortedMovableTypes = new CopyOnWriteArrayList<>()
    {
        @Override
        public boolean add(MovableType movableType)
        {
            super.add(movableType);
            sortedMovableTypes.sort(Comparator.comparing(MovableType::getSimpleName));
            return true;
        }
    };

    /**
     * Gets all {@link MovableType}s that are currently registered.
     *
     * @return All {@link MovableType}s that are currently registered.
     */
    public Set<MovableType> getRegisteredMovableTypes()
    {
        return Collections.unmodifiableSet(movableTypeStatus.keySet());
    }

    private List<MovableType> getMovableTypesWithStatus(boolean status)
    {
        final List<MovableType> enabledMovableTypes = new ArrayList<>();
        for (final Map.Entry<MovableType, MovableRegistrationStatus> movableType : movableTypeStatus.entrySet())
            if (movableType.getValue().status == status)
                enabledMovableTypes.add(movableType.getKey());
        enabledMovableTypes.sort(Comparator.comparing(MovableType::getSimpleName));
        return enabledMovableTypes;
    }

    /**
     * Gets all {@link MovableType}s that are currently enabled.
     *
     * @return All {@link MovableType}s that are currently enabled.
     */
    public List<MovableType> getEnabledMovableTypes()
    {
        return getMovableTypesWithStatus(true);
    }

    /**
     * Gets all {@link MovableType}s that are currently disabled.
     *
     * @return All {@link MovableType}s that are currently disabled.
     */
    public List<MovableType> getDisabledMovableTypes()
    {
        return getMovableTypesWithStatus(false);
    }

    /**
     * Checks if an {@link MovableType} is enabled.
     *
     * @param movableType
     *     The {@link MovableType} to check.
     * @return True if the {@link MovableType} is enabled, otherwise false.
     */
    public boolean isRegistered(MovableType movableType)
    {
        return movableTypeStatus.containsKey(movableType);
    }

    /**
     * Tries to get a {@link MovableType} from it name as defined by {@link MovableType#getSimpleName()}. This method is
     * case-insensitive.
     *
     * @param typeName
     *     The name of the type.
     * @return The {@link MovableType} to retrieve, if possible.
     */
    public Optional<MovableType> getMovableType(String typeName)
    {
        return Optional.ofNullable(movableTypeFromName.get(typeName.toLowerCase(Locale.ENGLISH)));
    }

    /**
     * Tries to get a {@link MovableType} from its fully qualified name as defined by
     * {@link MovableType#getSimpleName()}. This method is case-sensitive.
     *
     * @param fullName
     *     The fully qualified name of the type.
     * @return The {@link MovableType} to retrieve, if possible.
     */
    public Optional<MovableType> getMovableTypeFromFullName(@Nullable String fullName)
    {
        if (fullName == null)
            return Optional.empty();
        return Optional.ofNullable(movableTypeFromFullName.get(fullName));
    }

    /**
     * Checks if a {@link MovableType} is enabled or not. Disabled {@link MovableType}s cannot be toggled or created.
     * <p>
     * {@link MovableType}s that are not registered, are disabled by definition.
     *
     * @param movableType
     *     The {@link MovableType} to check.
     * @return True if this {@link MovableType} is both registered and enabled.
     */
    public boolean isMovableTypeEnabled(MovableType movableType)
    {
        final @Nullable MovableTypeManager.MovableRegistrationStatus info = movableTypeStatus.get(movableType);
        return info != null && info.status;
    }

    private void registerMovableType0(MovableType movableType, boolean isEnabled)
    {
        log.atInfo().log("Registering movable type: %s...", movableType);

        movableTypeStatus.put(movableType, new MovableRegistrationStatus(movableType.getFullName(), isEnabled));
        movableTypeFromName.put(movableType.getSimpleName(), movableType);
        movableTypeFromFullName.put(movableType.getFullName(), movableType);

        if (isEnabled)
            sortedMovableTypes.add(movableType);
    }

    /**
     * Registers a {@link MovableType} with the {@link LocalizationManager}.
     * <p>
     * This ensures any localization stuff that exists in the movable type is properly handled by the localizer.
     *
     * @param types
     *     The type(s) to register with the localization manager.
     */
    private void registerTypeWithLocalizer(List<MovableType> types)
    {
        if (types.isEmpty())
            return;
        final List<Class<?>> classes = new ArrayList<>(types.size());
        for (final MovableType type : types)
            classes.add(type.getMovableClass());
        this.localizationManager.addResourcesFromClass(classes);
    }

    /**
     * Registers a {@link MovableType}.
     *
     * @param movableType
     *     The {@link MovableType} to register.
     * @return True if registration was successful.
     */
    public void registerMovableType(MovableType movableType)
    {
        registerMovableType(movableType, DEFAULT_IS_ENABLED);
    }

    /**
     * Registers a {@link MovableType}.
     *
     * @param movableType
     *     The {@link MovableType} to register.
     * @param isEnabled
     *     Whether this {@link MovableType} should be enabled or not. Default = true.
     */
    public void registerMovableType(MovableType movableType, boolean isEnabled)
    {
        registerTypeWithLocalizer(List.of(movableType));
        registerMovableType0(movableType, isEnabled);
    }

    /**
     * Unregisters a movable type. Note that it does <b>NOT</b> remove it or its movables from the database and that
     * after unregistering it, that won't be possible anymore either.
     * <p>
     * Once unregistered, this type will be completely disabled and movables of this type cannot be used for anything.
     *
     * @param movableType
     *     The type to unregister.
     */
    public void unregisterMovableType(MovableType movableType)
    {
        if (movableTypeStatus.remove(movableType) == null)
        {
            log.atWarning().log("Trying to unregister movable of type: %s, but it isn't registered already!",
                                movableType.getSimpleName());
            return;
        }
        movableTypeFromName.remove(movableType.getSimpleName());
        movableTypeFromFullName.remove(movableType.getFullName());
        sortedMovableTypes.remove(movableType);
    }

    /**
     * Changes the status of a {@link MovableType}. If disabled, this type cannot be toggled or created.
     *
     * @param movableType
     *     The {@link MovableType} to enabled or disable.
     * @param isEnabled
     *     True to enable this {@link MovableType} (default), or false to disable it.
     */
    @SuppressWarnings("unused")
    public void setMovableTypeEnabled(MovableType movableType, boolean isEnabled)
    {
        final @Nullable MovableTypeManager.MovableRegistrationStatus info = movableTypeStatus.get(movableType);
        if (info != null)
        {
            info.status = isEnabled;
            if (!isEnabled)
                sortedMovableTypes.remove(movableType);
        }
    }

    /**
     * Registers a list of {@link MovableType}s. See {@link #registerMovableType(MovableType)}.
     *
     * @param movableTypes
     *     The list of {@link MovableType}s to register.
     */
    public void registerMovableTypes(List<MovableType> movableTypes)
    {
        registerTypeWithLocalizer(movableTypes);
        movableTypes.forEach(movableType -> registerMovableType0(movableType, DEFAULT_IS_ENABLED));
    }

    @Override
    public void shutDown()
    {
        movableTypeStatus.clear();
        sortedMovableTypes.clear();
        movableTypeFromName.clear();
        movableTypeFromFullName.clear();
    }

    @Override
    public void initialize()
    {
        if (this.movableTypeStatus.keySet().isEmpty())
            return;

    }

    @Override
    public String getDebugInformation()
    {
        final SafeStringBuilder sb = new SafeStringBuilder("Registered movable types:\n");
        for (final Map.Entry<MovableType, MovableRegistrationStatus> entry : movableTypeStatus.entrySet())
        {
            final MovableType movableType = entry.getKey();
            sb.append("- ").append(movableType::toString).append(": ");
            if (!entry.getValue().status)
                sb.append("DISABLED");
            else
                sb.append("\n").appendIndented(2, movableType::getMovableSerializer);
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * Describes the full name and the enabled status of a {@link MovableType}.
     *
     * @author Pim
     */
    @Value
    private static class MovableRegistrationStatus
    {
        String fullName;
        @NonFinal
        boolean status;
    }
}
