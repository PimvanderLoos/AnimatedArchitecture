package nl.pim16aap2.animatedarchitecture.core.util.structureretriever;

import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.delayedinput.DelayedStructureSpecificationInputRequest;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the factory for {@link StructureRetriever}s.
 *
 * @author Pim
 */
public final class StructureRetrieverFactory
{
    private final DelayedStructureSpecificationInputRequest.Factory specificationFactory;
    private final DatabaseManager databaseManager;
    private final StructureFinderCache structureFinderCache;

    @Inject StructureRetrieverFactory(
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
        final OptionalLong structureUID = Util.parseLong(structureID);
        return structureUID.isPresent() ?
               new StructureRetriever.StructureUIDRetriever(
                   databaseManager, structureUID.getAsLong()) :
               new StructureRetriever.StructureNameRetriever(
                   databaseManager, specificationFactory, structureID);
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
        return new StructureRetriever.StructureUIDRetriever(
            databaseManager, structureUID);
    }

    /**
     * Creates a new {@link StructureRetriever} from the structure object itself.
     *
     * @param structure
     *     The structure object itself.
     * @return The new {@link StructureRetriever}.
     */
    public StructureRetriever of(AbstractStructure structure)
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
        CompletableFuture<Optional<AbstractStructure>> structure)
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
     * @return The {@link StructureFinder} instance.
     */
    public StructureFinder search(
        ICommandSender commandSender, String input, StructureFinderMode mode, PermissionLevel maxPermission)
    {
        return mode == StructureFinderMode.USE_CACHE ?
               structureFinderCache.getStructureFinder(commandSender, input, maxPermission) :
               new StructureFinder(this, databaseManager, commandSender, input, maxPermission);
    }

    /**
     * See {@link #search(ICommandSender, String, StructureFinderMode)}.
     */
    public StructureFinder search(
        ICommandSender commandSender, String input, PermissionLevel maxPermission)
    {
        return search(commandSender, input, StructureFinderMode.USE_CACHE, maxPermission);
    }

    /**
     * See {@link #search(ICommandSender, String, StructureFinderMode)}.
     */
    public StructureFinder search(
        ICommandSender commandSender, String input)
    {
        return search(commandSender, input, StructureFinderMode.USE_CACHE);
    }

    /**
     * See {@link #search(ICommandSender, String, StructureFinderMode, PermissionLevel)}.
     */
    public StructureFinder search(
        ICommandSender commandSender, String input, StructureFinderMode mode)
    {
        return search(commandSender, input, mode, PermissionLevel.CREATOR);
    }

    /**
     * Creates a new {@link StructureRetriever} from the structure object itself.
     *
     * @param structure
     *     The structure object itself.
     * @return The new {@link StructureRetriever}.
     */
    public static StructureRetriever ofStructure(@Nullable AbstractStructure structure)
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
    public static StructureRetriever ofStructure(CompletableFuture<Optional<AbstractStructure>> structure)
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
    public StructureRetriever ofStructures(List<AbstractStructure> structures)
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
    public StructureRetriever ofStructures(
        CompletableFuture<List<AbstractStructure>> structures)
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
