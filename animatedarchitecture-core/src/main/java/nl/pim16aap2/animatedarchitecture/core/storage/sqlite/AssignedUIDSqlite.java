package nl.pim16aap2.animatedarchitecture.core.storage.sqlite;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureID;

/**
 * Represents a Unique Identifier (UID) that has been assigned to an object in SQLite.
 * <p>
 * This is an intermediate class that is used to construct the {@link StructureID} object.
 */
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
// TODO: Use modules so we can have sealed/permits across packages.
public final class AssignedUIDSqlite
{
    /**
     * The UID that has been assigned to the object in SQLite.
     */
    private final long uid;

    /**
     * Gets an AssignedUID object with the provided UID.
     *
     * @param uid
     *     The UID assigned to the object.
     * @return An AssignedUID object with the provided UID.
     */
    static StructureID getAssignedUID(long uid)
    {
        // Yes, this is a bit of a hack. We can clean this up once we have modules.
        return StructureID.of(new AssignedUIDSqlite(uid));
    }
}
