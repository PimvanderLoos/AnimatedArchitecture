package nl.pim16aap2.animatedarchitecture.core.managers;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import lombok.Getter;
import lombok.Locked;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.localization.LocalizationManager;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.util.SafeStringBuilder;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * This class manages all {@link StructureType}s. Before a type can be used, it will have to be registered here.
 * <p>
 * Structure types can be enabled and disabled. When a type is disabled, it will not be available for use, but it is
 * still registered and can still be used by other types that may depend on it.
 */
@Singleton
@Flogger
@ThreadSafe
public final class StructureTypeManager implements IDebuggable
{
    private static final boolean DEFAULT_IS_ENABLED = true;

    private final LocalizationManager localizationManager;

    /**
     * Private, modifiable list of all {@link StructureType}s that are currently enabled.
     */
    @GuardedBy("$lock")
    private final List<StructureType> enabledStructureTypes0 = new ArrayList<>()
    {
        @Override
        public boolean add(StructureType structureType)
        {
            super.add(structureType);
            enabledStructureTypes0.sort(Comparator.comparing(StructureType::getSimpleName));
            return true;
        }
    };

    /**
     * Private, modifiable list of all {@link StructureType}s that are currently disabled.
     */
    @GuardedBy("$lock")
    private final List<StructureType> disabledStructureTypes0 = new ArrayList<>()
    {
        @Override
        public boolean add(StructureType structureType)
        {
            super.add(structureType);
            disabledStructureTypes0.sort(Comparator.comparing(StructureType::getSimpleName));
            return true;
        }
    };

    /**
     * Gets all {@link StructureType}s that are currently enabled.
     *
     * @return All {@link StructureType}s that are currently enabled.
     */
    @Getter
    private final List<StructureType> enabledStructureTypes = Collections.unmodifiableList(enabledStructureTypes0);

    /**
     * Gets all {@link StructureType}s that are currently disabled.
     *
     * @return All {@link StructureType}s that are currently disabled.
     */
    @Getter
    private final List<StructureType> disabledStructureTypes = Collections.unmodifiableList(disabledStructureTypes0);

    /**
     * Private, modifiable map of all {@link StructureType}s that are currently registered and whether they are
     * enabled.
     */
    @GuardedBy("$lock")
    private final Map<StructureType, Boolean> registeredStructureTypes0 = new IdentityHashMap<>();

    /**
     * Gets all {@link StructureType}s that are currently registered.
     *
     * @return All {@link StructureType}s that are currently registered.
     */
    @Getter
    private final Set<StructureType> registeredStructureTypes = Collections.unmodifiableSet(
        registeredStructureTypes0.keySet());

    @GuardedBy("$lock")
    private final Map<String, StructureType> structureTypeFromFullName = new HashMap<>();

    @Inject
    public StructureTypeManager(DebuggableRegistry debuggableRegistry, LocalizationManager localizationManager)
    {
        this.localizationManager = localizationManager;
        debuggableRegistry.registerDebuggable(this);
    }

    /**
     * Checks if an {@link StructureType} is enabled.
     *
     * @param structureType
     *     The {@link StructureType} to check.
     * @return True if the {@link StructureType} is enabled, otherwise false.
     */
    @Locked.Read
    public boolean isRegistered(StructureType structureType)
    {
        return registeredStructureTypes0.containsKey(structureType);
    }

    /**
     * Tries to get a {@link StructureType} from its fully qualified name as defined by
     * {@link StructureType#getSimpleName()}. This method is case-sensitive.
     *
     * @param fullName
     *     The fully qualified name of the type.
     * @return The {@link StructureType} to retrieve, if possible.
     */
    @Locked.Read
    public Optional<StructureType> getFromFullName(@Nullable String fullName)
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
    @Locked.Read
    public boolean isStructureTypeEnabled(StructureType structureType)
    {
        final @Nullable Boolean result = registeredStructureTypes0.get(structureType);
        return result != null && result;
    }

    @GuardedBy("$lock")
    private void register0(StructureType structureType, boolean isEnabled)
    {
        log.atInfo()
           .log("Registering structure type: '%s'. Enabled: %s", structureType.getFullNameWithVersion(), isEnabled);

        final @Nullable Boolean result = registeredStructureTypes0.put(structureType, isEnabled);
        if (result != null && result == isEnabled)
            return;

        final var from = isEnabled ? disabledStructureTypes0 : enabledStructureTypes0;
        final var to = isEnabled ? enabledStructureTypes0 : disabledStructureTypes0;

        to.add(structureType);

        // If the structure type was already registered, we only need to remove it from the other list.
        if (result != null)
        {
            from.remove(structureType);
            return;
        }

        structureTypeFromFullName.put(structureType.getFullName(), structureType);
    }

    /**
     * Registers a {@link StructureType} with the {@link LocalizationManager}.
     * <p>
     * This ensures any localization stuff that exists in the structure type is properly handled by the localizer.
     *
     * @param types
     *     The type(s) to register with the localization manager.
     */
    @GuardedBy("$lock")
    private void registerWithLocalizer(List<StructureType> types)
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
    @Locked.Write
    public void register(StructureType structureType)
    {
        register(structureType, DEFAULT_IS_ENABLED);
    }

    /**
     * Enables or disables a {@link StructureType}.
     *
     * @param structureType
     *     The {@link StructureType} to enable or disable.
     * @param isEnabled
     *     Whether this {@link StructureType} should be enabled or not.
     */
    @Locked.Write
    public void setEnabledState(StructureType structureType, boolean isEnabled)
    {
        register0(structureType, isEnabled);
    }

    /**
     * Registers a {@link StructureType}.
     *
     * @param structureType
     *     The {@link StructureType} to register.
     * @param isEnabled
     *     Whether this {@link StructureType} should be enabled or not. Default = true.
     */
    @Locked.Write
    public void register(StructureType structureType, boolean isEnabled)
    {
        registerWithLocalizer(List.of(structureType));
        register0(structureType, isEnabled);
    }

    /**
     * Registers a list of {@link StructureType}s. See {@link #register(StructureType)}.
     *
     * @param structureTypes
     *     The list of {@link StructureType}s to register.
     */
    @Locked.Write
    public void register(List<StructureType> structureTypes)
    {
        registerWithLocalizer(structureTypes);
        structureTypes.forEach(structureType -> register0(structureType, DEFAULT_IS_ENABLED));
    }

    @Override
    @Locked.Read
    public String getDebugInformation()
    {
        final SafeStringBuilder sb = new SafeStringBuilder("Registered structure types:\n");

        sb.append("* Enabled:\n");
        for (final StructureType structureType : enabledStructureTypes)
            sb.append("  - ").append(structureType).append('\n');

        sb.append("* Disabled:\n");
        for (final StructureType structureType : disabledStructureTypes)
            sb.append("  - ").append(structureType).append('\n');

        return sb.toString();
    }
}
