/**
 * This package contains classes and interfaces related to retrieving structures from the database.
 * <p>
 * <h1 id="sect-factory">Structure Retriever Factory</h1>
 * <p>
 * The primary entry point is the
 * {@link nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory} class, which can be
 * used to create instances of {@link nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever}
 * and {@link nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureFinder}. An instance of the factory
 * can be obtained using
 * {@link nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatform#getStructureRetrieverFactory()}.
 * <p>
 * For more information and example usage, please refer to the documentation of the
 * {@link nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory} class.
 * <p>
 * <h1 id="sect-retrievers">Structure Retrievers</h1>
 * <p>
 * The {@link nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever} class provides a number of
 * utility methods for retrieving structures from the database. The
 * {@link nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory} class contains more
 * information on how to create new structure finders.
 * <p>
 * The creation of a new structure retriever does not immediately query the database. Instead, it will only query the
 * database when one of the retrieval methods is called. This allows the receiver of the retriever to decide what data
 * it needs. For example, if the receiver can handle multiple structures, it can choose to retrieve all structures from
 * the database at once. If the receiver only needs a single structure, it can choose to retrieve only a single one if
 * exactly one structure is found.
 * <p>
 * Additionally, the receiver can specify if the structure should have a specific owner, and, if so, what permission
 * level the owner should have. This allows the receiver to only retrieve structures that the owner has access to.
 * <p>
 * For more information and example usage, please refer to the documentation of the
 * {@link nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever} class.
 * <p>
 * <h1 id="sect-finders">Structure Finders</h1>
 * <p>
 * The {@link nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureFinder} class provides a system to
 * find structures using their name. Specifically, it can be used to progressively search through all structure names to
 * find all structures whose names start with the provided input String.
 * <p>
 * The {@link nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureFinderCache} class is used to cache
 * the structure finders for {@link nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender} instances. This
 * allows requesting a structure finder from the factory for a given command sender and input combination. If an
 * existing entry exists, the input will be applied to it. If no entry exists, a new one is created instead.
 * Alternatively, the caller can request a new structure finder from the factory that bypasses the cache, which will
 * always create a new instance.
 * <p>
 * For more information and example usage, please refer to the documentation of the
 * {@link nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureFinder} class.
 */
package nl.pim16aap2.animatedarchitecture.core.structures.retriever;
