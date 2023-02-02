package nl.pim16aap2.bigdoors.util.structureretriever;

import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.StructureSpecificationManager;
import nl.pim16aap2.bigdoors.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.structures.PermissionLevel;
import nl.pim16aap2.bigdoors.util.Util;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the factory for {@link nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever}s.
 *
 * @author Pim
 */
public final class StructureRetrieverFactory
{
    private final DatabaseManager databaseManager;
    private final IConfigLoader config;
    private final StructureSpecificationManager structureSpecificationManager;
    private final nl.pim16aap2.bigdoors.util.structureretriever.StructureFinderCache structureFinderCache;
    private final ILocalizer localizer;
    private final ITextFactory textFactory;

    @Inject
    public StructureRetrieverFactory(
        DatabaseManager databaseManager, IConfigLoader config,
        StructureSpecificationManager structureSpecificationManager,
        nl.pim16aap2.bigdoors.util.structureretriever.StructureFinderCache structureFinderCache, ILocalizer localizer,
        ITextFactory textFactory)
    {
        this.databaseManager = databaseManager;
        this.config = config;
        this.structureSpecificationManager = structureSpecificationManager;
        this.structureFinderCache = structureFinderCache;
        this.localizer = localizer;
        this.textFactory = textFactory;
    }

    /**
     * Creates a new {@link nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever} from its ID.
     *
     * @param structureID
     *     The identifier (name or UID) of the structure.
     * @return The new {@link nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever}.
     */
    public nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever of(String structureID)
    {
        final OptionalLong structureUID = Util.parseLong(structureID);
        return structureUID.isPresent() ?
               new nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever.StructureUIDRetriever(
                   databaseManager, structureUID.getAsLong()) :
               new nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever.StructureNameRetriever(
                   databaseManager, config, structureSpecificationManager,
                   localizer, textFactory, structureID);
    }

    /**
     * Creates a new {@link nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever} from its UID.
     *
     * @param structureUID
     *     The UID of the structure.
     * @return The new {@link nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever}.
     */
    public nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever of(long structureUID)
    {
        return new nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever.StructureUIDRetriever(
            databaseManager, structureUID);
    }

    /**
     * Creates a new {@link nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever} from the structure object
     * itself.
     *
     * @param structure
     *     The structure object itself.
     * @return The new {@link nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever}.
     */
    public nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever of(AbstractStructure structure)
    {
        return StructureRetrieverFactory.ofStructure(structure);
    }

    /**
     * Creates a new {@link nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever} from a structure that is
     * being retrieved.
     *
     * @param structure
     *     The structure that is being retrieved.
     * @return The new {@link nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever}.
     */
    public nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever of(
        CompletableFuture<Optional<AbstractStructure>> structure)
    {
        return StructureRetrieverFactory.ofStructure(structure);
    }

    /**
     * Gets the {@link nl.pim16aap2.bigdoors.util.structureretriever.StructureFinder} to find structures from partial
     * string matches.
     * <p>
     * If a {@link nl.pim16aap2.bigdoors.util.structureretriever.StructureFinder} already exists for this
     *
     * @param commandSender
     *     The command sender (e.g. player) that is responsible for searching for the structure.
     * @param input
     *     The input to use as search query.
     * @param mode
     *     The mode to use for obtaining a {@link nl.pim16aap2.bigdoors.util.structureretriever.StructureFinder}
     *     instance. Defaults to {@link StructureFinderMode#USE_CACHE}.
     * @param maxPermission
     *     The maximum permission (inclusive) of the structure owner of the structures to find. Does not apply if the
     *     command sender is not a player. Defaults to {@link PermissionLevel#CREATOR}.
     * @return The {@link nl.pim16aap2.bigdoors.util.structureretriever.StructureFinder} instance.
     */
    public nl.pim16aap2.bigdoors.util.structureretriever.StructureFinder search(
        ICommandSender commandSender, String input, StructureFinderMode mode, PermissionLevel maxPermission)
    {
        return mode == StructureFinderMode.USE_CACHE ?
               structureFinderCache.getStructureFinder(commandSender, input, maxPermission) :
               new nl.pim16aap2.bigdoors.util.structureretriever.StructureFinder(this, databaseManager, commandSender,
                                                                                 input, maxPermission);
    }

    /**
     * See {@link #search(ICommandSender, String, StructureFinderMode)}.
     */
    public nl.pim16aap2.bigdoors.util.structureretriever.StructureFinder search(
        ICommandSender commandSender, String input, PermissionLevel maxPermission)
    {
        return search(commandSender, input, StructureFinderMode.USE_CACHE, maxPermission);
    }

    /**
     * See {@link #search(ICommandSender, String, StructureFinderMode)}.
     */
    public nl.pim16aap2.bigdoors.util.structureretriever.StructureFinder search(
        ICommandSender commandSender, String input)
    {
        return search(commandSender, input, StructureFinderMode.USE_CACHE);
    }

    /**
     * See {@link #search(ICommandSender, String, StructureFinderMode, PermissionLevel)}.
     */
    public nl.pim16aap2.bigdoors.util.structureretriever.StructureFinder search(
        ICommandSender commandSender, String input, StructureFinderMode mode)
    {
        return search(commandSender, input, mode, PermissionLevel.CREATOR);
    }

    /**
     * Creates a new {@link nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever} from the structure object
     * itself.
     *
     * @param structure
     *     The structure object itself.
     * @return The new {@link nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever}.
     */
    public static nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever ofStructure(
        @Nullable AbstractStructure structure)
    {
        return new nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever.StructureObjectRetriever(structure);
    }

    /**
     * Creates a new {@link nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever} from a structure that is
     * still being retrieved.
     *
     * @param structure
     *     The future structure.
     * @return The new {@link nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever}.
     */
    public static nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever ofStructure(
        CompletableFuture<Optional<AbstractStructure>> structure)
    {
        return new nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever.FutureStructureRetriever(structure);
    }

    /**
     * Creates a new {@link nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever} from a list of
     * structures.
     *
     * @param structures
     *     The structures.
     * @return The new {@link nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever}.
     */
    public static nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever ofStructures(
        List<AbstractStructure> structures)
    {
        return new nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever.StructureListRetriever(structures);
    }

    /**
     * Creates a new {@link nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever} from a list of
     * structures.
     *
     * @param structures
     *     The structures.
     * @return The new {@link nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever}.
     */
    public static nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever ofStructures(
        CompletableFuture<List<AbstractStructure>> structures)
    {
        return new nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever.FutureStructureListRetriever(
            structures);
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
