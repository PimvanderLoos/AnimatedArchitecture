package nl.pim16aap2.animatedarchitecture.core.structures.retriever;

import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import nl.pim16aap2.animatedarchitecture.core.util.delayedinput.DelayedStructureSpecificationInputRequest;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the factory for {@link StructureRetriever} and {@link StructureFinder} instances.
 * <p>
 * <h1 id="sect-retrievers">Structure Retrievers</h1>
 * <p>
 * This factory contains several methods of creating new structure retrievers using various input types:
 * <pre>{@code
 * // Get the retriever for the structure with UID 1:
 * structureRetrieverFactory.of(1L);
 * structureRetrieverFactory.of("1");
 *
 * // Get the retriever for all structures named "MyDoor":
 * structureRetrieverFactory.of("MyDoor");
 *
 * // Wrap an existing structure in a retriever:
 * structureRetrieverFactory.of(myAbstractStructure);
 * }</pre>
 * <p>
 * <h1 id="sect-finders">Structure Finders</h1>
 * <p>
 * This factory also contains methods for creating new structure finders using the "search" methods.
 * <p>
 * The finders are used to find structures based on partial information.
 * <p>
 * Each finder is associated with an {@link ICommandSender} that is responsible for the finder. This is used to
 * determine which structures the finder is allowed to find. If the command sender is not a player, the finder will be
 * able to find all structures. If it is a player, the finder will only be able to find structures that the player has
 * permission to access. If not specified, only structures that were created by the player will be found.
 * <p>
 * For example, to find all structures whose name starts with "My", you can use:
 * <pre>{@code
 * // Find all structures whose name starts with "My" that were created by the player:
 * structureRetrieverFactory.find(player, "My");
 *
 * // Find all structures whose name starts with "My" that the player either created
 * // or is admin/user of:
 * structureRetrieverFactory.find(player, "My", PermissionLevel.USER);
 * }</pre>
 * <p>
 * The {@link StructureFinderCache} is used to cache the finders. When creating a finder, you can choose whether to use
 * this cache. If the cache is used (which it is unless otherwise specified), the finder will first check if there is a
 * cached result for the given command sender. If there is, the cached result will be used and updated using the
 * provided input. If there is no cached result, a new finder will be created and cached. The cache is backed by a
 * {@link nl.pim16aap2.animatedarchitecture.core.data.cache.timed.TimedCache}, so the results are kept in memory for a
 * few minutes after the last time they were accessed. When specified to not use the cache, the search method will
 * always create a new finder instance.
 * <p>
 * The cache is used to allow calling the search method with progressive input without keeping track of the finder
 * instance yourself. For example, you can call the method with "My" and then with "MyW" to first find all structures
 * whose name starts with "My" and then narrow that down to structures whose name starts with "MyW". When calling the
 * search method within the timeout period of the cache, these two calls to the search method will result in a single
 * database query.
 */
public final class StructureRetrieverFactory
{
    /**
     * The default permission level to use when searching for structures when not specified.
     */
    public static final PermissionLevel DEFAULT_PERMISSION_LEVEL = PermissionLevel.CREATOR;

    /**
     * The default mode to use when searching for structures when not specified.
     */
    public static final StructureFinderMode DEFAULT_MODE = StructureFinderMode.USE_CACHE;

    private final DelayedStructureSpecificationInputRequest.Factory specificationFactory;
    private final DatabaseManager databaseManager;
    private final StructureFinderCache structureFinderCache;

    @Inject
    StructureRetrieverFactory(
        DelayedStructureSpecificationInputRequest.Factory specificationFactory,
        DatabaseManager databaseManager,
        StructureFinderCache structureFinderCache)
    {
        this.specificationFactory = specificationFactory;
        this.databaseManager = databaseManager;
        this.structureFinderCache = structureFinderCache;
    }

    /**
     * Creates a new {@link StructureRetriever} from its ID.
     *
     * @param structureID
     *     The identifier (name or UID) of the structure.
     * @return The new {@link StructureRetriever}.
     */
    public StructureRetriever of(String structureID)
    {
        final OptionalLong structureUID = MathUtil.parseLong(structureID);
        return structureUID.isPresent() ?
            new StructureRetriever.StructureUIDRetriever(databaseManager, structureUID.getAsLong()) :
            new StructureRetriever.StructureNameRetriever(databaseManager, specificationFactory, structureID);
    }

    /**
     * Creates a new {@link StructureRetriever} from its UID.
     *
     * @param structureUID
     *     The UID of the structure.
     * @return The new {@link StructureRetriever}.
     */
    public StructureRetriever of(long structureUID)
    {
        return new StructureRetriever.StructureUIDRetriever(databaseManager, structureUID);
    }

    /**
     * Creates a new {@link StructureRetriever} from the structure object itself.
     *
     * @param structure
     *     The structure object itself.
     * @return The new {@link StructureRetriever}.
     */
    public StructureRetriever of(Structure structure)
    {
        return StructureRetrieverFactory.ofStructure(structure);
    }

    /**
     * Creates a new {@link StructureRetriever} from a structure that is being retrieved.
     *
     * @param structure
     *     The structure that is being retrieved.
     * @return The new {@link StructureRetriever}.
     */
    public StructureRetriever of(
        CompletableFuture<Optional<Structure>> structure)
    {
        return StructureRetrieverFactory.ofStructure(structure);
    }

    /**
     * Gets the {@link StructureFinder} to find structures from partial string matches.
     * <p>
     * If a {@link StructureFinder} already exists for this
     *
     * @param commandSender
     *     The command sender (e.g. player) that is responsible for searching for the structure.
     * @param input
     *     The input to use as search query.
     * @param mode
     *     The mode to use for obtaining a {@link StructureFinder} instance. Defaults to
     *     {@link StructureFinderMode#USE_CACHE}.
     * @param maxPermission
     *     The maximum permission (inclusive) of the structure owner of the structures to find. Does not apply if the
     *     command sender is not a player. Defaults to {@link PermissionLevel#CREATOR}.
     * @param properties
     *     The properties that the structures must have. When specified, only structures that have all of these
     *     properties will be found. When not specified, the properties of the structures are not checked.
     * @return The {@link StructureFinder} instance.
     */
    public StructureFinder search(
        ICommandSender commandSender,
        String input,
        StructureFinderMode mode,
        PermissionLevel maxPermission,
        Collection<Property<?>> properties)
    {
        return mode == StructureFinderMode.USE_CACHE ?
            structureFinderCache.getStructureFinder(commandSender, input, maxPermission, properties) :
            new StructureFinder(this, databaseManager, commandSender, input, maxPermission, properties);
    }

    /**
     * See {@link #search(ICommandSender, String, StructureFinderMode, PermissionLevel, Collection)}.
     */
    public StructureFinder search(
        ICommandSender commandSender,
        String input,
        StructureFinderMode mode,
        PermissionLevel maxPermission,
        Property<?>... properties)
    {
        return search(commandSender, input, mode, maxPermission, Set.of(properties));
    }

    /**
     * See {@link #search(ICommandSender, String, StructureFinderMode, PermissionLevel, Collection)}.
     * <p>
     * Uses {@link #DEFAULT_MODE} as the default caching mode.
     */
    public StructureFinder search(
        ICommandSender commandSender,
        String input,
        PermissionLevel maxPermission,
        Collection<Property<?>> properties)
    {
        return search(commandSender, input, StructureFinderMode.USE_CACHE, maxPermission, properties);
    }

    /**
     * See {@link #search(ICommandSender, String, StructureFinderMode, PermissionLevel, Collection)}.
     * <p>
     * Uses {@link #DEFAULT_PERMISSION_LEVEL} as the default permission level.
     * <p>
     * Uses {@link #DEFAULT_MODE} as the default caching mode.
     */
    public StructureFinder search(
        ICommandSender commandSender,
        String input,
        Collection<Property<?>> properties)
    {
        return search(commandSender, input, DEFAULT_MODE, DEFAULT_PERMISSION_LEVEL, properties);
    }

    /**
     * See {@link #search(ICommandSender, String, StructureFinderMode, PermissionLevel, Collection)}.
     * <p>
     * Uses {@link PermissionLevel#CREATOR} as the default permission level.
     */
    public StructureFinder search(
        ICommandSender commandSender,
        String input,
        StructureFinderMode mode,
        Collection<Property<?>> properties)
    {
        return search(commandSender, input, mode, DEFAULT_PERMISSION_LEVEL, properties);
    }

    /**
     * Creates a new {@link StructureRetriever} from the structure object itself.
     *
     * @param structure
     *     The structure object itself.
     * @return The new {@link StructureRetriever}.
     */
    public static StructureRetriever ofStructure(@Nullable Structure structure)
    {
        return new StructureRetriever.StructureObjectRetriever(structure);
    }

    /**
     * Creates a new {@link StructureRetriever} from a structure that is still being retrieved.
     *
     * @param structure
     *     The future structure.
     * @return The new {@link StructureRetriever}.
     */
    public static StructureRetriever ofStructure(CompletableFuture<Optional<Structure>> structure)
    {
        return new StructureRetriever.FutureStructureRetriever(structure);
    }

    /**
     * Creates a new {@link StructureRetriever} from a list of structures.
     *
     * @param structures
     *     The structures.
     * @return The new {@link StructureRetriever}.
     */
    @SuppressWarnings("unused")
    public StructureRetriever ofStructures(List<Structure> structures)
    {
        return new StructureRetriever.StructureListRetriever(specificationFactory, structures);
    }

    /**
     * Creates a new {@link StructureRetriever} from a list of structures.
     *
     * @param structures
     *     The structures.
     * @return The new {@link StructureRetriever}.
     */
    public StructureRetriever ofStructures(CompletableFuture<List<Structure>> structures)
    {
        return new StructureRetriever.FutureStructureListRetriever(specificationFactory, structures);
    }

    /**
     * Represents different ways a structure finder can be instantiated.
     */
    public enum StructureFinderMode
    {
        /**
         * Re-use a cached structure finder if possible. If no cached version is available, a new one will be
         * instantiated.
         */
        USE_CACHE,

        /**
         * Create a new instance of the finder regardless of whether a cached mapping exists.
         * <p>
         * The new instance is not added to the cache.
         */
        NEW_INSTANCE
    }
}
