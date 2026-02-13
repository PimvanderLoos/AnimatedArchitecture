package nl.pim16aap2.animatedarchitecture.core.managers;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.config.IConfig;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.types.bigdoor.StructureTypeBigDoor;
import nl.pim16aap2.animatedarchitecture.core.structures.types.clock.StructureTypeClock;
import nl.pim16aap2.animatedarchitecture.core.structures.types.drawbridge.StructureTypeDrawbridge;
import nl.pim16aap2.animatedarchitecture.core.structures.types.flag.StructureTypeFlag;
import nl.pim16aap2.animatedarchitecture.core.structures.types.garagedoor.StructureTypeGarageDoor;
import nl.pim16aap2.animatedarchitecture.core.structures.types.portcullis.StructureTypePortcullis;
import nl.pim16aap2.animatedarchitecture.core.structures.types.revolvingdoor.StructureTypeRevolvingDoor;
import nl.pim16aap2.animatedarchitecture.core.structures.types.slidingdoor.StructureTypeSlidingDoor;
import nl.pim16aap2.animatedarchitecture.core.structures.types.windmill.StructureTypeWindmill;
import nl.pim16aap2.animatedarchitecture.core.util.StringUtil;
import org.jspecify.annotations.Nullable;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * This class manages all {@link StructureType}s. Before a type can be used, it will have to be registered here.
 * <p>
 * Structure types can be enabled and disabled. When a type is disabled, it will not be available for use, but it is
 * still registered and can still be used by other types that may depend on it.
 */
@Singleton
@CustomLog
@ThreadSafe
public final class StructureTypeManager implements IDebuggable
{
    /**
     * List of all available {@link StructureType}s.
     */
    @Getter
    private final List<StructureType> registeredStructureTypes =
        Stream.of(
                StructureTypeBigDoor.get(),
                StructureTypeClock.get(),
                StructureTypeDrawbridge.get(),
                StructureTypeFlag.get(),
                StructureTypeGarageDoor.get(),
                StructureTypePortcullis.get(),
                StructureTypeRevolvingDoor.get(),
                StructureTypeSlidingDoor.get(),
                StructureTypeWindmill.get()
            )
            .sorted(Comparator.comparing(StructureType::getKey))
            .toList();

    /**
     * Map of all registered {@link StructureType}s by their key.
     */
    private final Map<String, StructureType> structureTypeFromKey = registeredStructureTypes.stream()
        .collect(java.util.stream.Collectors.toUnmodifiableMap(
            StructureType::getKey,
            structureType -> structureType
        ));

    /**
     * List of all enabled {@link StructureType}s. This list is volatile to ensure thread-safety when updating the
     * enabled status of types.
     */
    private volatile List<StructureType> enabledStructureTypes = List.copyOf(registeredStructureTypes);

    @Inject
    public StructureTypeManager(DebuggableRegistry debuggableRegistry)
    {
        debuggableRegistry.registerDebuggable(this);
    }

    /**
     * Tries to get a {@link StructureType} from its key as defined by {@link StructureType#getKey()}. This method is
     * case-sensitive.
     *
     * @param key
     *     The key of the type.
     * @return The {@link StructureType} to retrieve, if possible.
     */
    public Optional<StructureType> getFromKey(@Nullable String key)
    {
        if (key == null)
            return Optional.empty();
        return Optional.ofNullable(structureTypeFromKey.get(key));
    }

    /**
     * Updates the enabled status of all registered {@link StructureType}s based on the provided predicate. If a type
     * matches the predicate, it will be enabled; otherwise, it will be disabled.
     * <p>
     * This method should only be called by the configuration manager when the configuration is loaded or reloaded, to
     * ensure that the enabled status of types is always up-to-date with the user-specified configuration.
     * <p>
     * Do not call this method from any other place.
     *
     * @param isEnabledPredicate
     *     A predicate that determines whether a type should be enabled or disabled.
     */
    public void updateEnabledStatusForStructureTypes(Predicate<StructureType> isEnabledPredicate)
    {
        this.enabledStructureTypes = registeredStructureTypes.stream()
            .filter(isEnabledPredicate)
            .toList();
    }

    /**
     * Gets all enabled {@link StructureType}s.
     * <p>
     * Use {@link IConfig#isEnabled(StructureType)} to check whether a specific type is enabled.
     *
     * @return An unmodifiable list of all enabled {@link StructureType}s.
     */
    public List<StructureType> getEnabledStructureTypes()
    {
        return enabledStructureTypes;
    }

    @Override
    public String getDebugInformation()
    {
        return String.format(
            """
                Registered Structure Types: %s
                Enabled Structure Types: %s
                """,
            StringUtil.formatCollection(registeredStructureTypes),
            StringUtil.formatCollection(enabledStructureTypes)
        );
    }
}
