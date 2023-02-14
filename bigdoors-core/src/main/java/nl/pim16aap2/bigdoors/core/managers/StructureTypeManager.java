package nl.pim16aap2.bigdoors.core.managers;

import lombok.Getter;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.bigdoors.core.api.debugging.IDebuggable;
import nl.pim16aap2.bigdoors.core.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.core.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.core.localization.LocalizationManager;
import nl.pim16aap2.bigdoors.core.structures.StructureType;
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
 * This class manages all {@link StructureType}s. Before a type can be used, it will have to be registered here.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
@Singleton
@Flogger
public final class StructureTypeManager extends Restartable implements IDebuggable
{
    private static final boolean DEFAULT_IS_ENABLED = true;

    private final Map<StructureType, StructureRegistrationStatus> structureTypeStatus = new ConcurrentHashMap<>();
    private final Map<String, StructureType> structureTypeFromName = new ConcurrentHashMap<>();
    private final Map<String, StructureType> structureTypeFromFullName = new ConcurrentHashMap<>();
    private final LocalizationManager localizationManager;

    @Inject
    public StructureTypeManager(
        RestartableHolder holder, DebuggableRegistry debuggableRegistry, LocalizationManager localizationManager)
    {
        super(holder);
        this.localizationManager = localizationManager;
        debuggableRegistry.registerDebuggable(this);
    }

    /**
     * All registered AND enabled {@link StructureType}s.
     */
    @Getter
    private final List<StructureType> sortedStructureTypes = new CopyOnWriteArrayList<>()
    {
        @Override
        public boolean add(StructureType structureType)
        {
            super.add(structureType);
            sortedStructureTypes.sort(Comparator.comparing(StructureType::getSimpleName));
            return true;
        }
    };

    /**
     * Gets all {@link StructureType}s that are currently registered.
     *
     * @return All {@link StructureType}s that are currently registered.
     */
    public Set<StructureType> getRegisteredStructureTypes()
    {
        return Collections.unmodifiableSet(structureTypeStatus.keySet());
    }

    private List<StructureType> getStructureTypesWithStatus(boolean status)
    {
        final List<StructureType> enabledStructureTypes = new ArrayList<>();
        for (final Map.Entry<StructureType, StructureRegistrationStatus> structureType : structureTypeStatus.entrySet())
            if (structureType.getValue().status == status)
                enabledStructureTypes.add(structureType.getKey());
        enabledStructureTypes.sort(Comparator.comparing(StructureType::getSimpleName));
        return enabledStructureTypes;
    }

    /**
     * Gets all {@link StructureType}s that are currently enabled.
     *
     * @return All {@link StructureType}s that are currently enabled.
     */
    public List<StructureType> getEnabledStructureTypes()
    {
        return getStructureTypesWithStatus(true);
    }

    /**
     * Gets all {@link StructureType}s that are currently disabled.
     *
     * @return All {@link StructureType}s that are currently disabled.
     */
    public List<StructureType> getDisabledStructureTypes()
    {
        return getStructureTypesWithStatus(false);
    }

    /**
     * Checks if an {@link StructureType} is enabled.
     *
     * @param structureType
     *     The {@link StructureType} to check.
     * @return True if the {@link StructureType} is enabled, otherwise false.
     */
    public boolean isRegistered(StructureType structureType)
    {
        return structureTypeStatus.containsKey(structureType);
    }

    /**
     * Tries to get a {@link StructureType} from it name as defined by {@link StructureType#getSimpleName()}. This
     * method is case-insensitive.
     *
     * @param typeName
     *     The name of the type.
     * @return The {@link StructureType} to retrieve, if possible.
     */
    public Optional<StructureType> getStructureType(String typeName)
    {
        return Optional.ofNullable(structureTypeFromName.get(typeName.toLowerCase(Locale.ENGLISH)));
    }

    /**
     * Tries to get a {@link StructureType} from its fully qualified name as defined by
     * {@link StructureType#getSimpleName()}. This method is case-sensitive.
     *
     * @param fullName
     *     The fully qualified name of the type.
     * @return The {@link StructureType} to retrieve, if possible.
     */
    public Optional<StructureType> getStructureTypeFromFullName(@Nullable String fullName)
    {
        if (fullName == null)
            return Optional.empty();
        return Optional.ofNullable(structureTypeFromFullName.get(fullName));
    }

    /**
     * Checks if a {@link StructureType} is enabled or not. Disabled {@link StructureType}s cannot be toggled or
     * created.
     * <p>
     * {@link StructureType}s that are not registered, are disabled by definition.
     *
     * @param structureType
     *     The {@link StructureType} to check.
     * @return True if this {@link StructureType} is both registered and enabled.
     */
    public boolean isStructureTypeEnabled(StructureType structureType)
    {
        final @Nullable StructureTypeManager.StructureRegistrationStatus info = structureTypeStatus.get(structureType);
        return info != null && info.status;
    }

    private void registerStructureType0(StructureType structureType, boolean isEnabled)
    {
        log.atInfo().log("Registering structure type: %s...", structureType.getFullNameWithVersion());

        structureTypeStatus.put(structureType, new StructureRegistrationStatus(structureType.getFullName(), isEnabled));
        structureTypeFromName.put(structureType.getSimpleName(), structureType);
        structureTypeFromFullName.put(structureType.getFullName(), structureType);

        if (isEnabled)
            sortedStructureTypes.add(structureType);
    }

    /**
     * Registers a {@link StructureType} with the {@link LocalizationManager}.
     * <p>
     * This ensures any localization stuff that exists in the structure type is properly handled by the localizer.
     *
     * @param types
     *     The type(s) to register with the localization manager.
     */
    private void registerTypeWithLocalizer(List<StructureType> types)
    {
        if (types.isEmpty())
            return;
        final List<Class<?>> classes = new ArrayList<>(types.size());
        for (final StructureType type : types)
            classes.add(type.getStructureClass());
        this.localizationManager.addResourcesFromClass(classes);
    }

    /**
     * Registers a {@link StructureType}.
     *
     * @param structureType
     *     The {@link StructureType} to register.
     */
    public void registerStructureType(StructureType structureType)
    {
        registerStructureType(structureType, DEFAULT_IS_ENABLED);
    }

    /**
     * Registers a {@link StructureType}.
     *
     * @param structureType
     *     The {@link StructureType} to register.
     * @param isEnabled
     *     Whether this {@link StructureType} should be enabled or not. Default = true.
     */
    public void registerStructureType(StructureType structureType, boolean isEnabled)
    {
        registerTypeWithLocalizer(List.of(structureType));
        registerStructureType0(structureType, isEnabled);
    }

    /**
     * Unregisters a structure type. Note that it does <b>NOT</b> remove it or its structures from the database and that
     * after unregistering it, that won't be possible anymore either.
     * <p>
     * Once unregistered, this type will be completely disabled and structures of this type cannot be used for
     * anything.
     *
     * @param structureType
     *     The type to unregister.
     */
    public void unregisterStructureType(StructureType structureType)
    {
        if (structureTypeStatus.remove(structureType) == null)
        {
            log.atWarning().log("Trying to unregister structure of type: %s, but it isn't registered already!",
                                structureType.getSimpleName());
            return;
        }
        structureTypeFromName.remove(structureType.getSimpleName());
        structureTypeFromFullName.remove(structureType.getFullName());
        sortedStructureTypes.remove(structureType);
    }

    /**
     * Changes the status of a {@link StructureType}. If disabled, this type cannot be toggled or created.
     *
     * @param structureType
     *     The {@link StructureType} to enabled or disable.
     * @param isEnabled
     *     True to enable this {@link StructureType} (default), or false to disable it.
     */
    @SuppressWarnings("unused")
    public void setStructureTypeEnabled(StructureType structureType, boolean isEnabled)
    {
        final @Nullable StructureTypeManager.StructureRegistrationStatus info = structureTypeStatus.get(structureType);
        if (info != null)
        {
            info.status = isEnabled;
            if (!isEnabled)
                sortedStructureTypes.remove(structureType);
        }
    }

    /**
     * Registers a list of {@link StructureType}s. See {@link #registerStructureType(StructureType)}.
     *
     * @param structureTypes
     *     The list of {@link StructureType}s to register.
     */
    public void registerStructureTypes(List<StructureType> structureTypes)
    {
        registerTypeWithLocalizer(structureTypes);
        structureTypes.forEach(structureType -> registerStructureType0(structureType, DEFAULT_IS_ENABLED));
    }

    @Override
    public void shutDown()
    {
        structureTypeStatus.clear();
        sortedStructureTypes.clear();
        structureTypeFromName.clear();
        structureTypeFromFullName.clear();
    }

    @Override
    public void initialize()
    {
        if (this.structureTypeStatus.keySet().isEmpty())
            return;

    }

    @Override
    public String getDebugInformation()
    {
        final SafeStringBuilder sb = new SafeStringBuilder("Registered structure types:\n");
        for (final Map.Entry<StructureType, StructureRegistrationStatus> entry : structureTypeStatus.entrySet())
        {
            final StructureType structureType = entry.getKey();
            sb.append("- ").append(structureType::toString).append(": ");
            if (!entry.getValue().status)
                sb.append("DISABLED");
            else
                sb.append("\n").appendIndented(2, structureType::getStructureSerializer);
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * Describes the full name and the enabled status of a {@link StructureType}.
     *
     * @author Pim
     */
    @Value
    private static class StructureRegistrationStatus
    {
        String fullName;
        @NonFinal
        boolean status;
    }
}
