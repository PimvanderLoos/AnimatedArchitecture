package nl.pim16aap2.animatedarchitecture.core.structures;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import nl.pim16aap2.animatedarchitecture.core.storage.sqlite.AssignedUIDSqlite;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents an Identifier (ID) that has been assigned to a structure.
 * <p>
 * There are three types of UIDs:
 * <ul>
 *     <li>Registered <b>U</b>IDs: Always {@code >= 1}.
 * <p>
 *     These are UIDs that have been assigned to structures by the storage system.
 * <p>
 *     These are assigned by the database system and should not be assigned manually. Consequently, these are only
 *     available <i>after</i> the structure has been stored in the database.
 *     </li>
 *
 *     <li>Unassigned ID: Always {@code -1}.
 * <p>
 *     This is used for structures that have not been assigned a UID yet.
 * <p>
 *     You can get this value by calling {@link #getUnassignedID()}.
 *     </li>
 *
 *     <li>Unregistered <b>U</b>ID: Always {@code <= -1000}.
 * <p>
 *     Like the unassigned ID, this is used for structures that have not been assigned a UID yet. However, unlike the
 *     unassigned ID, an unregistered ID is guaranteed to be unique.
 * <p>
 *     These can be used for temporary structures that are never stored in the database (e.g. to show a user an
 *     animation preview).
 *     </li>
 * </ul>
 */
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class StructureID
{
    private static final AtomicLong UNREGISTERED_UID_COUNTER = new AtomicLong(-1000L);

    private static final StructureID UNASSIGNED = new StructureID(-1);

    /**
     * The ID that has been assigned to the structure.
     */
    private final long id;

    /**
     * Gets a StructureID used for structures that do not have an ID assigned yet.
     *
     * @return A StructureID with an ID of -1.
     */
    public static StructureID getUnassignedID()
    {
        return UNASSIGNED;
    }

    /**
     * Gets an unregistered ID.
     * <p>
     * This is a unique ID that is not registered in the database.
     *
     * @return A unique unregistered StructureID.
     */
    public static StructureID getUnregisteredID()
    {
        return new StructureID(UNREGISTERED_UID_COUNTER.decrementAndGet());
    }

    /**
     * Creates an AssignedUID object from an AssignedUIDSqlite object.
     *
     * @param assignedUIDSqlite
     *     The AssignedUIDSqlite object to create an AssignedUID object from.
     * @return The created AssignedUID object.
     */
    public static StructureID of(AssignedUIDSqlite assignedUIDSqlite)
    {
        return new StructureID(assignedUIDSqlite.getUid());
    }
}
