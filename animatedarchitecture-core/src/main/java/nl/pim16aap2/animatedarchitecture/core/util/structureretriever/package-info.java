/**
 * This package contains classes and interfaces related to retrieving structures from the database. The primary entry
 * point is the {@link nl.pim16aap2.animatedarchitecture.core.util.structureretriever.StructureRetrieverFactory} class,
 * which can be used to create instances of
 * {@link nl.pim16aap2.animatedarchitecture.core.util.structureretriever.StructureRetriever} and
 * {@link nl.pim16aap2.animatedarchitecture.core.util.structureretriever.StructureFinder}. An instance of the factory
 * can be obtained using
 * {@link nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatform#getStructureRetrieverFactory()}.
 * <p>
 * The {@link nl.pim16aap2.animatedarchitecture.core.util.structureretriever.StructureRetriever} class provides a number
 * of utility methods for retrieving structures from the database. For example, when retrieving a structure with name
 * "MyStructure", you can obtain the retriever using the
 * {@link nl.pim16aap2.animatedarchitecture.core.util.structureretriever.StructureRetrieverFactory#of(String)}. The
 * retriever can then be used to, for example, get only a single structure if there is exactly 1 match, either in the
 * entire database or only when owned by a specific user. Alternatively, you can retrieve all structures that match the
 * input parameters.
 * <p>
 * The {@link nl.pim16aap2.animatedarchitecture.core.util.structureretriever.StructureFinder} class provides a system to
 * find structures using their name. Specifically, it can be used to progressively search through all structure names to
 * find all structures whose names start with the provided input String. The class uses a
 * {@link nl.pim16aap2.animatedarchitecture.core.data.cache.RollingCache} to keep track of provided inputs. This allows
 * both narrowing the search results and rolling back to previous states (e.g. when fixing typos) without causing new
 * database requests. For example, assume we have an input of "My" and some returned results "MyPortcullis" and
 * "MyWindmill". If the next input is then "MyW", the finder will return only "MyWindmill" without querying the database
 * again.
 * <p>
 * The {@link nl.pim16aap2.animatedarchitecture.core.util.structureretriever.StructureFinderCache} class is used to
 * cache the structure finders for {@link nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender} instances.
 * This allows requesting a structure finder from the factory for a given command sender and input combination. If an
 * existing entry exists, the input will be applied to it. If no entry exists, a new one is created instead. The cache
 * is backed by a {@link nl.pim16aap2.animatedarchitecture.core.data.cache.timed.TimedCache}, so the results are only
 * kept in memory for a few minutes. Alternatively, the search methods of the factory class accept a
 * {@link nl.pim16aap2.animatedarchitecture.core.util.structureretriever.StructureRetrieverFactory.StructureFinderMode}
 * parameter that can be used to specify to bypass the cache altogether.
 */
package nl.pim16aap2.animatedarchitecture.core.util.structureretriever;
