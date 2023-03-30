package nl.pim16aap2.animatedarchitecture.core.util.structureretriever;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IConfig;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureSpecificationManager;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.delayedinput.DelayedStructureSpecificationInputRequest;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a way to retrieve a structure. It may be referenced by its name, its UID, or the object itself.
 */
@Flogger
public sealed abstract class StructureRetriever
{
    /**
     * Gets the structure that is referenced by this {@link StructureRetriever} if exactly 1 structure matches the
     * description.
     * <p>
     * In case the structure is referenced by its name, there may be more than one match (names are not unique). When
     * this happens, no structures are returned.
     *
     * @return The {@link AbstractStructure} if it can be found.
     */
    public abstract CompletableFuture<Optional<AbstractStructure>> getStructure();

    /**
     * Gets the structure that is referenced by this {@link StructureRetriever} and owned by the provided player if
     * exactly 1 structure matches the description.
     * <p>
     * In case the structure is referenced by its name, there may be more than one match (names are not unique). When
     * this happens, no structures are returned.
     *
     * @param player
     *     The {@link IPlayer} that owns the structure.
     * @param permissionLevel
     *     The maximum {@link PermissionLevel} of the player for the structure to be returned.
     * @return The {@link AbstractStructure} if it can be found.
     */
    public abstract CompletableFuture<Optional<AbstractStructure>> getStructure(
        IPlayer player, PermissionLevel permissionLevel);

    /**
     * Gets the structure referenced by this {@link StructureRetriever}.
     * <p>
     * If the {@link ICommandSender} is a player, see {@link #getStructure(IPlayer, PermissionLevel)}, otherwise see
     * {@link #getStructure()}.
     *
     * @param commandSender
     *     The {@link ICommandSender} for whom to retrieve the structures.
     * @param permissionLevel
     *     The maximum {@link PermissionLevel} of the player for the structure to be returned.
     * @return The structure referenced by this {@link StructureRetriever}.
     */
    public CompletableFuture<Optional<AbstractStructure>> getStructure(
        ICommandSender commandSender, PermissionLevel permissionLevel)
    {
        if (commandSender instanceof IPlayer player)
            return getStructure(player, permissionLevel);
        return getStructure();
    }

    /**
     * Attempts to retrieve a structure from its specification (see {@link #getStructure(IPlayer, PermissionLevel)}).
     * <p>
     * If more than 1 match was found, the player will be asked to specify which one they asked for specifically.
     * <p>
     * The amount of time to wait (when required) is determined by {@link IConfig#specificationTimeout()}.
     * <p>
     * See {@link DelayedStructureSpecificationInputRequest}.
     *
     * @param player
     *     The player for whom to get the structure.
     * @param permissionLevel
     *     The maximum {@link PermissionLevel} of the player for the structure to be returned.
     * @return The structure as specified by this {@link StructureRetriever} and with user input in case more than one
     * match was found.
     */
    // TODO: Implement the interactive system.
    @SuppressWarnings("unused")
    public CompletableFuture<Optional<AbstractStructure>> getStructureInteractive(
        IPlayer player, PermissionLevel permissionLevel)
    {
        return getStructure(player, permissionLevel);
    }

    /**
     * Gets all structures referenced by this {@link StructureRetriever}.
     *
     * @return All structures referenced by this {@link StructureRetriever}.
     */
    public CompletableFuture<List<AbstractStructure>> getStructures()
    {
        return optionalToList(getStructure());
    }

    /**
     * Gets all structures referenced by this {@link StructureRetriever} where the provided player is a (co)owner of
     * with any permission level.
     *
     * @param player
     *     The {@link IPlayer} that owns all matching structures.
     * @param permissionLevel
     *     The maximum {@link PermissionLevel} of the player for the structures to be returned.
     * @return All structures referenced by this {@link StructureRetriever}.
     */
    public CompletableFuture<List<AbstractStructure>> getStructures(IPlayer player, PermissionLevel permissionLevel)
    {
        return optionalToList(getStructure(player, permissionLevel));
    }

    /**
     * Gets all structures referenced by this {@link StructureRetriever}.
     * <p>
     * If the {@link ICommandSender} is a player, see {@link #getStructures(IPlayer, PermissionLevel)}, otherwise see
     * {@link #getStructures()}.
     *
     * @param commandSender
     *     The {@link ICommandSender} for whom to retrieve the structures.
     * @param permissionLevel
     *     The maximum {@link PermissionLevel} of the player for the structures to be returned.
     * @return The structures referenced by this {@link StructureRetriever}.
     */
    public CompletableFuture<List<AbstractStructure>> getStructures(
        ICommandSender commandSender, PermissionLevel permissionLevel)
    {
        if (commandSender instanceof IPlayer player)
            return getStructures(player, permissionLevel);
        return getStructures();
    }

    /**
     * Gets a list of (future) structures from an optional one.
     *
     * @param optionalStructure
     *     The (future) optional structure.
     * @return Either an empty list (if the optional was empty) or a singleton list (if the optional was not empty).
     */
    private static CompletableFuture<List<AbstractStructure>> optionalToList(
        CompletableFuture<Optional<AbstractStructure>> optionalStructure)
    {
        return optionalStructure.thenApply(
            structure -> structure.map(Collections::singletonList).orElseGet(Collections::emptyList));
    }

    /**
     * Gets a single (optional) structure from a list of structures if only 1 structure exists in the list.
     *
     * @param list
     *     The list of structures.
     * @return An optional {@link AbstractStructure} if exactly 1 existed in the list, otherwise an empty optional.
     */
    private static Optional<AbstractStructure> listToOptional(List<AbstractStructure> list)
    {
        if (list.size() == 1)
            return Optional.of(list.get(0));
        log.atWarning().log("Tried to get 1 structure but received %d!", list.size());
        return Optional.empty();
    }

    /**
     * Gets a single (optional/future) structure from a list of (future) structures if only 1 structure exists in the
     * list.
     *
     * @param list
     *     The list of (future) structures.
     * @return An optional (future) {@link AbstractStructure} if exactly 1 existed in the list, otherwise an empty
     * optional.
     */
    private static CompletableFuture<Optional<AbstractStructure>> listToOptional(
        CompletableFuture<List<AbstractStructure>> list)
    {
        return list.thenApply(StructureRetriever::listToOptional).exceptionally(Util::exceptionallyOptional);
    }

    /**
     * Filters an optional structure by the provided player and permission level.
     *
     * @param structure
     *     The optional structure. If empty, it will be returned as is.
     * @param player
     *     The player that must own the structure.
     * @param permissionLevel
     *     The permission level. If the structure is not owned by the player with the provided permission level or
     *     lower, it will be filtered out.
     * @return The filtered optional structure.
     */
    private static Optional<AbstractStructure> filter(
        Optional<AbstractStructure> structure, IPlayer player, PermissionLevel permissionLevel)
    {
        return structure.filter(val -> val.isOwner(player, permissionLevel));
    }

    /**
     * Filters a list of structures by the provided player and permission level.
     *
     * @param structures
     *     The structures. If empty, it will be returned as is.
     * @param player
     *     The player that must own the structures.
     * @param permissionLevel
     *     The permission level. If a structures is not owned by the player with the provided permission level or lower,
     *     it will be filtered out.
     * @return The filtered structures.
     */
    private static List<AbstractStructure> filter(
        List<AbstractStructure> structures, IPlayer player, PermissionLevel permissionLevel)
    {
        return structures.stream().filter(structure -> structure.isOwner(player, permissionLevel)).toList();
    }

    /**
     * Represents a {@link StructureRetriever} that references a structure by its name.
     * <p>
     * Because names are not unique, a single name may reference more than 1 structure (even for a single player).
     */
    @ToString
    @AllArgsConstructor
    @Flogger
    static final class StructureNameRetriever extends StructureRetriever
    {
        @ToString.Exclude
        private final DatabaseManager databaseManager;

        @ToString.Exclude
        private IConfig config;

        @ToString.Exclude
        private StructureSpecificationManager structureSpecificationManager;

        @ToString.Exclude
        private ILocalizer localizer;

        @ToString.Exclude
        private ITextFactory textFactory;

        private final String name;

        @Override
        public CompletableFuture<Optional<AbstractStructure>> getStructure()
        {
            return listToOptional(databaseManager.getStructures(name));
        }

        @Override
        public CompletableFuture<Optional<AbstractStructure>> getStructure(
            IPlayer player, PermissionLevel permissionLevel)
        {
            return listToOptional(databaseManager.getStructures(player, name, permissionLevel));
        }

        @Override
        public CompletableFuture<List<AbstractStructure>> getStructures()
        {
            return databaseManager
                .getStructures(name)
                .exceptionally(Util::exceptionallyList);
        }

        @Override
        public CompletableFuture<List<AbstractStructure>> getStructures(IPlayer player, PermissionLevel permissionLevel)
        {
            return databaseManager
                .getStructures(player, name, permissionLevel)
                .exceptionally(Util::exceptionallyList);
        }

        @Override
        public CompletableFuture<Optional<AbstractStructure>> getStructureInteractive(
            IPlayer player, PermissionLevel permissionLevel)
        {
            return getStructures(player, permissionLevel).thenCompose(
                structuresList ->
                {
                    if (structuresList.size() == 1)
                        return CompletableFuture.completedFuture(Optional.of(structuresList.get(0)));

                    if (structuresList.isEmpty())
                        return CompletableFuture.completedFuture(Optional.empty());

                    final Duration timeOut = Duration.ofSeconds(config.specificationTimeout());
                    return DelayedStructureSpecificationInputRequest
                        .get(timeOut, structuresList, player, localizer, textFactory, structureSpecificationManager);

                }).exceptionally(Util::exceptionallyOptional);
        }
    }

    /**
     * Represents a {@link StructureRetriever} that references a structure by its UID.
     * <p>
     * Because the UID is always unique (by definition), this can never reference more than 1 structure.
     */
    @ToString
    @AllArgsConstructor
    static final class StructureUIDRetriever extends StructureRetriever
    {
        @ToString.Exclude
        private final DatabaseManager databaseManager;

        private final long uid;

        @Override
        public CompletableFuture<Optional<AbstractStructure>> getStructure()
        {
            return databaseManager.getStructure(uid).exceptionally(Util::exceptionallyOptional);
        }

        @Override
        public CompletableFuture<Optional<AbstractStructure>> getStructure(
            IPlayer player, PermissionLevel permissionLevel)
        {
            return databaseManager
                .getStructure(player, uid)
                .thenApply(retrieved -> StructureRetriever.filter(retrieved, player, permissionLevel))
                .exceptionally(Util::exceptionallyOptional);
        }
    }

    /**
     * Represents a {@link StructureRetriever} that references a structure by the object itself.
     */
    @AllArgsConstructor()
    @ToString(doNotUseGetters = true)
    @EqualsAndHashCode(callSuper = false, doNotUseGetters = true)
    static final class StructureObjectRetriever extends StructureRetriever
    {
        private final @Nullable AbstractStructure structure;

        @Override
        public CompletableFuture<Optional<AbstractStructure>> getStructure()
        {
            return CompletableFuture.completedFuture(Optional.ofNullable(structure));
        }

        @Override
        public CompletableFuture<Optional<AbstractStructure>> getStructure(
            IPlayer player, PermissionLevel permissionLevel)
        {
            return CompletableFuture.completedFuture(
                StructureRetriever.filter(Optional.ofNullable(structure), player, permissionLevel));
        }
    }

    /**
     * Represents a {@link StructureRetriever} that references a list of structures by the object themselves.
     */
    @ToString
    @AllArgsConstructor()
    @EqualsAndHashCode(callSuper = false, doNotUseGetters = true)
    @Flogger
    static final class StructureListRetriever extends StructureRetriever
    {
        private final List<AbstractStructure> structures;

        @Override
        public CompletableFuture<Optional<AbstractStructure>> getStructure()
        {
            return CompletableFuture.completedFuture(StructureRetriever.listToOptional(structures));
        }

        @Override
        public CompletableFuture<List<AbstractStructure>> getStructures()
        {
            return CompletableFuture.completedFuture(structures);
        }

        private List<AbstractStructure> getStructures0(IPlayer player, PermissionLevel permissionLevel)
        {
            return StructureRetriever.filter(structures, player, permissionLevel);
        }

        @Override
        public CompletableFuture<List<AbstractStructure>> getStructures(IPlayer player, PermissionLevel permissionLevel)
        {
            return CompletableFuture.completedFuture(getStructures0(player, permissionLevel));
        }

        @Override
        public CompletableFuture<Optional<AbstractStructure>> getStructure(
            IPlayer player, PermissionLevel permissionLevel)
        {
            return CompletableFuture.completedFuture(
                StructureRetriever.listToOptional(getStructures0(player, permissionLevel)));
        }
    }

    /**
     * Represents a {@link StructureRetriever} that references a future list of structures by the object themselves.
     */
    @ToString
    @EqualsAndHashCode(callSuper = false, doNotUseGetters = true)
    @Flogger
    static final class FutureStructureListRetriever extends StructureRetriever
    {
        private final CompletableFuture<List<AbstractStructure>> structures;

        FutureStructureListRetriever(CompletableFuture<List<AbstractStructure>> structures)
        {
            this.structures = structures.exceptionally(t -> Util.exceptionally(t, Collections.emptyList()));
        }

        @Override
        public CompletableFuture<Optional<AbstractStructure>> getStructure()
        {
            return structures
                .thenApply(StructureRetriever::listToOptional)
                .exceptionally(Util::exceptionallyOptional);
        }

        @Override
        public CompletableFuture<List<AbstractStructure>> getStructures()
        {
            return structures;
        }

        private CompletableFuture<List<AbstractStructure>> getStructures0(
            IPlayer player, PermissionLevel permissionLevel)
        {
            return structures
                .thenApply(retrieved -> StructureRetriever.filter(retrieved, player, permissionLevel))
                .exceptionally(Util::exceptionallyList);
        }

        @Override
        public CompletableFuture<List<AbstractStructure>> getStructures(IPlayer player, PermissionLevel permissionLevel)
        {
            return getStructures0(player, permissionLevel);
        }

        @Override
        public CompletableFuture<Optional<AbstractStructure>> getStructure(
            IPlayer player, PermissionLevel permissionLevel)
        {
            return getStructures0(player, permissionLevel)
                .thenApply(StructureRetriever::listToOptional)
                .exceptionally(Util::exceptionallyOptional);
        }
    }

    /**
     * Represents a {@link StructureRetriever} that references a future optional structure directly.
     */
    @ToString
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    static final class FutureStructureRetriever extends StructureRetriever
    {
        private final CompletableFuture<Optional<AbstractStructure>> futureStructure;

        @Override
        public CompletableFuture<Optional<AbstractStructure>> getStructure()
        {
            return futureStructure;
        }

        @Override
        public CompletableFuture<Optional<AbstractStructure>> getStructure(
            IPlayer player, PermissionLevel permissionLevel)
        {
            return futureStructure
                .thenApply(retrieved -> StructureRetriever.filter(retrieved, player, permissionLevel))
                .exceptionally(Util::exceptionallyOptional);
        }
    }
}

