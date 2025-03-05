/**
 * This package contains classes that define, manipulate, and manage structures.
 * <p>
 * <h1 id="sect-retrieval">Structure Instantiation and Retrieval</h1>
 * <p>
 * An important concept to understand is that structures follow the <a
 * href="https://en.wikipedia.org/wiki/Multiton_pattern">multiton pattern</a>. This means that there can be at most one
 * instance of a structure with a given UID. These instances are cached and registered in the
 * {@link nl.pim16aap2.animatedarchitecture.core.structures.StructureRegistry}. While it is possible to create your own
 * instances of structures, these unregistered instances will be of limited use and will likely lead to unexpected
 * behaviour.
 * <p>
 * Instead, you should retrieve instances of structures using a
 * {@link nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever}. The
 * {@link nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory} class can be used to
 * create retrievers that can be used to retrieve structures from the database using a variety of different criteria.
 * <p>
 * <h1 id="sect-thread-safety">Thread-Safety</h1>
 * <p>
 * One thing to keep in mind is that structures are fully thread-safe and can be used from any thread through the use of
 * read/write locks. The downside of this is that it is possible for a thread to block while waiting for a lock to
 * become available. This is especially true for the write lock, which is used when creating or deleting structures.
 * This means that care should be taken when using structures from the main thread. If you are using structures from the
 * main thread, you should consider using asynchronous tasks to avoid blocking the main thread.
 * <p>
 * Care should also be taken to avoid situations where an action is performed that requires a write lock while a read
 * lock is already held. This can lead to deadlocks. The plugin will try to prevent this from happening, but it is still
 * a good idea to keep this in mind, as it can lead to exceptions being thrown when a potential deadlock is detected.
 * <p>
 * <h1 id="sect-modification">Structure Modification</h1>
 * <p>
 * Another thing to keep in mind is that structures are not automatically saved to the database. This means that if you
 * make changes to a structure, those will not be saved to the database until you call the
 * {@link nl.pim16aap2.animatedarchitecture.core.structures.Structure#syncData()} method.
 * <p>
 * When making changes to a structure on behalf of a player, you should use consider using
 * {@link nl.pim16aap2.animatedarchitecture.core.commands} instead. This will ensure that the changes are saved to the
 * database, that the player is notified of any errors that may occur, and that any logging is done correctly.
 * <p>
 * <h1 id="sect-types">Structure Object Types</h1>
 * <p>
 * There are several different types of structure objects that are used to refer to structures. These are:
 * <ul>
 *     <li>{@link nl.pim16aap2.animatedarchitecture.core.structures.IStructureConst}: This is the immutable version of a
 *     structure. It is used to retrieve information about a structure, but cannot be used to modify the structure. All
 *     other types are subtypes of this type.
 * <p>
 *     <b>Never cast objects of this type to its mutable variant</b>. Doing so can lead to all kinds of issues,
 *     including deadlocks.</li>
 *
 *     <li>{@link nl.pim16aap2.animatedarchitecture.core.structures.Structure}: This is the mutable version of a
 *     structure. It is the most commonly used type of structure object. It can be used to modify the structure.
 * <p>
 *     Whenever the documentation refers to a "structure" it is referring to this type unless otherwise specified.
 * <p>
 *     This class refers to the {@link nl.pim16aap2.animatedarchitecture.core.structures.IStructureComponent} interface
 *     for any type-specific behaviors (including, but not limited to, creating new animation components).</li>
 *
 *     <li>{@link nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot}: This is an immutable snapshot
 *     of a structure at a given point in time. Its immutability allows it to be used safely from any thread without
 *     locking or synchronization.</li>
 * </ul>
 * <p>
 * <h1 id="sect-toggle">Toggling a Structure</h1>
 * <p>
 * The preferred method to toggle structures is by using
 * {@link nl.pim16aap2.animatedarchitecture.core.structures.StructureAnimationRequestBuilder}.
 */
@NonNullByDefault
package nl.pim16aap2.animatedarchitecture.core.structures;

import org.eclipse.jdt.annotation.NonNullByDefault;
