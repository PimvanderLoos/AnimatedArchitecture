package nl.pim16aap2.bigdoors.storage;

import lombok.AllArgsConstructor;
import lombok.Value;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Keeps track of the different types of SQL statements used for the type-specific data of a {@link DoorType}.
 *
 * @author Pim
 */
public class DoorTypeDataStatementMap
{
    private final @NotNull Map<@NotNull DoorType, @NotNull DoorTypeDataStatementEntry> statementMap = new HashMap<>();

    public DoorTypeDataStatementMap put(final @NotNull DoorType type,
                                        final @NotNull DoorTypeDataStatementEntry doorTypeDataStatementEntry)
    {
        statementMap.put(type, doorTypeDataStatementEntry);
        return this;
    }

    public @NotNull Optional<DoorTypeDataStatement> getStatement(final @NotNull DoorType doorType,
                                                                 final @NotNull SQLStatementType type)
    {
        return Optional.ofNullable(statementMap.get(doorType)).flatMap(entry -> entry.getStatement(type));
    }

    public static class DoorTypeDataStatementEntry
    {
        private final @NotNull Map<@NotNull SQLStatementType, @NotNull DoorTypeDataStatement> statements
            = new HashMap<>(SQLStatementType.values().length);

        public DoorTypeDataStatementEntry(final @NotNull DoorTypeDataStatement insertStatement,
                                          final @NotNull DoorTypeDataStatement updateStatement)
        {
            statements.put(SQLStatementType.INSERT, insertStatement);
            statements.put(SQLStatementType.UPDATE, updateStatement);
        }

        public Optional<DoorTypeDataStatement> getStatement(final @NotNull SQLStatementType type)
        {
            return Optional.ofNullable(statements.get(type));
        }
    }

    @Value
    @AllArgsConstructor
    public static class DoorTypeDataStatement
    {
        /**
         * The actual statement of this {@link DoorTypeDataStatement}.
         */
        @NotNull String statement;

        int argumentCount;

        public DoorTypeDataStatement(final @NotNull Pair<String, Integer> statement)
        {
            this(statement.first, statement.second);
        }
    }

    public enum SQLStatementType
    {
        INSERT,
        UPDATE,
        ;
    }

}
