package nl.pim16aap2.animatedarchitecture.core.structures.retriever;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.ExtensionMethod;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IConfig;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import nl.pim16aap2.animatedarchitecture.core.util.StringUtil;
import nl.pim16aap2.animatedarchitecture.core.util.delayedinput.DelayedStructureSpecificationInputRequest;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a way to retrieve structures. It may be referenced by its name, its UID, or the object(s) itself.
 * <p>
 * Once a {@link StructureRetriever} is created, it can be used to retrieve the structure(s) it references using any of
 * the {@code getStructure} methods.
 * <p>
 * For example:
 * <pre>{@code
 * // Get the referenced structure if there is exactly 1 match.
 * final CompletableFuture<Optional<Structure>> result =
 *     structureRetriever.getStructure();
 *
 * // Get the referenced structure if there is exactly 1 match that the player is an admin of.
 * final CompletableFuture<Optional<Structure>> result =
 *     structureRetriever.getStructure(player, PermissionLevel.ADMIN);
 *
 * // Get the referenced structure that the player is the creator of. If there is more than 1 match, the player
 * // will be asked to select one (if supported).
 * final CompletableFuture<Optional<Structure>> result =
 *     structureRetriever.getStructureInteractive(player, PermissionLevel.CREATOR);
 *
 * // Get all structures that match the description.
 * final CompletableFuture<List<Structure>> result =
 *     structureRetriever.getStructures();
 *
 * // Get all structures that match the description and that the player is a user of.
 * final CompletableFuture<List<Structure>> result =
 *     structureRetriever.getStructures(player, PermissionLevel.USER);
 * }</pre>
 */
@Flogger
@ExtensionMethod(CompletableFutureExtensions.class)
public sealed abstract class StructureRetriever
{
    /**
     * Gets the structure that is referenced by this {@link StructureRetriever} if exactly 1 structure matches the
     * description.
     * <p>
     * In case the structure is referenced by its name, there may be more than one match (names are not unique). When
     * this happens, no structures are returned.
     * <p>
     * {@link #getStructureInteractive(IPlayer, PermissionLevel)} can be used to interactively request the user to
     * select a structure if more than 1 match is found.
     *
     * @return The {@link Structure} if it can be found.
     */
    public abstract CompletableFuture<Optional<Structure>> getStructure();

    /**
     * Gets the structure that is referenced by this {@link StructureRetriever} and owned by the provided player if
     * exactly 1 structure matches the description.
     * <p>
     * In case the structure is referenced by its name, there may be more than one match (names are not unique). When
     * this happens, no structures are returned.
     * <p>
     * {@link #getStructureInteractive(IPlayer, PermissionLevel)} can be used to interactively request the user to
     * select a structure if more than 1 match is found.
     *
     * @param player
     *     The {@link IPlayer} that owns the structure.
     * @param permissionLevel
     *     The maximum {@link PermissionLevel} of the player for the structure to be returned.
     * @return The {@link Structure} if it can be found.
     */
    public abstract CompletableFuture<Optional<Structure>> getStructure(
        IPlayer player,
        PermissionLevel permissionLevel
    );

    /**
     * Gets the structure referenced by this {@link StructureRetriever}.
     * <p>
     * If the {@link ICommandSender} is a player, see {@link #getStructure(IPlayer, PermissionLevel)}, otherwise see
     * {@link #getStructure()}.
     * <p>
     * {@link #getStructureInteractive(IPlayer, PermissionLevel)} can be used to interactively request the user to
     * select a structure if more than 1 match is found.
     *
     * @param commandSender
     *     The {@link ICommandSender} for whom to retrieve the structures.
     * @param permissionLevel
     *     The maximum {@link PermissionLevel} of the player for the structure to be returned.
     * @return The structure referenced by this {@link StructureRetriever}.
     */
    public CompletableFuture<Optional<Structure>> getStructure(
        ICommandSender commandSender,
        PermissionLevel permissionLevel)
    {
        if (commandSender instanceof IPlayer player)
            return getStructure(player, permissionLevel);
        return getStructure();
    }

    /**
     * Requests the user to specify which structure they want if more than 1 match was found.
     * <p>
     * If the user does not specify a structure within the timeout, no structure is returned.
     * <p>
     * If the list of input structures contains a single structure, that structure is returned without asking the user.
     *
     * @param structures
     *     The structures to choose from.
     * @param player
     *     The player for whom to get the structure.
     * @param specificationFactory
     *     The factory to use to create the request if needed.
     * @return The structure that the user specified, the structure that was the only match, or {@link Optional#empty()}
     * if no structure was found or the user did not specify a structure within the timeout.
     */
    static CompletableFuture<Optional<Structure>> getStructureInteractive(
        List<Structure> structures,
        IPlayer player,
        DelayedStructureSpecificationInputRequest.Factory specificationFactory)
    {
        if (structures.size() == 1)
            return CompletableFuture.completedFuture(Optional.of(structures.getFirst()));

        if (structures.isEmpty())
            return CompletableFuture.completedFuture(Optional.empty());

        return specificationFactory.get(structures, player);
    }

    /**
     * Attempts to retrieve a structure from its specification (see {@link #getStructure(IPlayer, PermissionLevel)}).
     * <p>
     * If more than 1 match was found, the player will be asked to specify which one they asked for specifically.
     * <p>
     * The amount of time to wait (when required) is determined by {@link IConfig#specificationTimeout()}.
     * <p>
     * See {@link DelayedStructureSpecificationInputRequest}.
     * <p>
     * Not every implementation of {@link StructureRetriever} supports this method, in which case it will return the
     * same as {@link #getStructure(IPlayer, PermissionLevel)} (e.g. when retrieving a structure by its UID).
     *
     * @param player
     *     The player for whom to get the structure.
     * @param permissionLevel
     *     The maximum {@link PermissionLevel} of the player for the structure to be returned.
     * @return The structure as specified by this {@link StructureRetriever} and with user input in case more than one
     * match was found.
     */
    public CompletableFuture<Optional<Structure>> getStructureInteractive(
        IPlayer player,
        PermissionLevel permissionLevel)
    {
        return getStructure(player, permissionLevel);
    }

    /**
     * Gets all structures referenced by this {@link StructureRetriever}.
     *
     * @return All structures referenced by this {@link StructureRetriever}.
     */
    public CompletableFuture<List<Structure>> getStructures()
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
    public CompletableFuture<List<Structure>> getStructures(IPlayer player, PermissionLevel permissionLevel)
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
    public CompletableFuture<List<Structure>> getStructures(
        ICommandSender commandSender,
        PermissionLevel permissionLevel)
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
    private static CompletableFuture<List<Structure>> optionalToList(
        CompletableFuture<Optional<Structure>> optionalStructure)
    {
        return optionalStructure
            .thenApply(structure ->
                structure.map(Collections::singletonList).orElseGet(Collections::emptyList));
    }

    /**
     * Gets a single (optional) structure from a list of structures if only 1 structure exists in the list.
     *
     * @param list
     *     The list of structures.
     * @return An optional {@link Structure} if exactly 1 existed in the list, otherwise an empty optional.
     */
    private static Optional<Structure> listToOptional(List<Structure> list)
    {
        if (list.size() == 1)
            return Optional.of(list.getFirst());

        log.atWarning().log("Tried to get 1 structure but received %d!", list.size());
        return Optional.empty();
    }

    /**
     * Gets a single (optional/future) structure from a list of (future) structures if only 1 structure exists in the
     * list.
     *
     * @param list
     *     The list of (future) structures.
     * @return An optional (future) {@link Structure} if exactly 1 existed in the list, otherwise an empty optional.
     */
    private static CompletableFuture<Optional<Structure>> listToOptional(CompletableFuture<List<Structure>> list)
    {
        return list.thenApply(StructureRetriever::listToOptional);
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
    private static Optional<Structure> filter(
        Optional<Structure> structure,
        IPlayer player,
        PermissionLevel permissionLevel)
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
    private static List<Structure> filter(
        List<Structure> structures,
        IPlayer player,
        PermissionLevel permissionLevel)
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
        private final DelayedStructureSpecificationInputRequest.Factory specificationFactory;

        private final String name;

        @Override
        public CompletableFuture<Optional<Structure>> getStructure()
        {
            return listToOptional(databaseManager.getStructures(name));
        }

        @Override
        public CompletableFuture<Optional<Structure>> getStructure(
            IPlayer player,
            PermissionLevel permissionLevel)
        {
            return listToOptional(databaseManager.getStructures(player, name, permissionLevel));
        }

        @Override
        public CompletableFuture<List<Structure>> getStructures()
        {
            return databaseManager
                .getStructures(name)
                .withExceptionContext("Get structures by name '%s'", name);
        }

        @Override
        public CompletableFuture<List<Structure>> getStructures(IPlayer player, PermissionLevel permissionLevel)
        {
            return databaseManager
                .getStructures(player, name, permissionLevel)
                .withExceptionContext(
                    "Get structures by name '%s' for player %s with permission level %s",
                    name,
                    player,
                    permissionLevel
                );
        }

        @Override
        public CompletableFuture<Optional<Structure>> getStructureInteractive(
            IPlayer player,
            PermissionLevel permissionLevel)
        {
            return getStructures(player, permissionLevel)
                .thenCompose(structures -> getStructureInteractive(structures, player, specificationFactory))
                .withExceptionContext(
                    "Interactively get structure by name '%s' for player %s with permission level %s",
                    name,
                    player,
                    permissionLevel
                );
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
        public CompletableFuture<Optional<Structure>> getStructure()
        {
            return databaseManager
                .getStructure(uid)
                .withExceptionContext("Get structure by UID %d", uid);
        }

        @Override
        public CompletableFuture<Optional<Structure>> getStructure(
            IPlayer player,
            PermissionLevel permissionLevel)
        {
            return databaseManager
                .getStructure(player, uid)
                .thenApply(retrieved -> StructureRetriever.filter(retrieved, player, permissionLevel))
                .withExceptionContext(
                    "Get structure by UID %d for player %s with permission level %s",
                    uid,
                    player,
                    permissionLevel
                );
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
        private final @Nullable Structure structure;

        @Override
        public CompletableFuture<Optional<Structure>> getStructure()
        {
            return CompletableFuture.completedFuture(Optional.ofNullable(structure));
        }

        @Override
        public CompletableFuture<Optional<Structure>> getStructure(
            IPlayer player,
            PermissionLevel permissionLevel)
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
        @ToString.Exclude
        private final DelayedStructureSpecificationInputRequest.Factory specificationFactory;

        private final List<Structure> structures;

        @Override
        public CompletableFuture<Optional<Structure>> getStructure()
        {
            return CompletableFuture.completedFuture(StructureRetriever.listToOptional(structures));
        }

        @Override
        public CompletableFuture<List<Structure>> getStructures()
        {
            return CompletableFuture.completedFuture(structures);
        }

        private List<Structure> getStructures0(IPlayer player, PermissionLevel permissionLevel)
        {
            return StructureRetriever.filter(structures, player, permissionLevel);
        }

        @Override
        public CompletableFuture<List<Structure>> getStructures(IPlayer player, PermissionLevel permissionLevel)
        {
            return CompletableFuture.completedFuture(getStructures0(player, permissionLevel));
        }

        @Override
        public CompletableFuture<Optional<Structure>> getStructure(
            IPlayer player,
            PermissionLevel permissionLevel)
        {
            return CompletableFuture.completedFuture(
                StructureRetriever.listToOptional(getStructures0(player, permissionLevel)));
        }

        @Override
        public CompletableFuture<Optional<Structure>> getStructureInteractive(
            IPlayer player,
            PermissionLevel permissionLevel)
        {
            return getStructures(player, permissionLevel)
                .thenCompose(structures ->
                    getStructureInteractive(structures, player, specificationFactory))
                .withExceptionContext(
                    "Interactively get structure for player %s with permission level %s from structures: %s",
                    player,
                    permissionLevel,
                    StringUtil.formatCollection(structures, Structure::getBasicInfo)
                );
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
        @ToString.Exclude
        private final DelayedStructureSpecificationInputRequest.Factory specificationFactory;

        private final CompletableFuture<List<Structure>> structures;

        FutureStructureListRetriever(
            DelayedStructureSpecificationInputRequest.Factory specificationFactory,
            CompletableFuture<List<Structure>> structures)
        {
            this.specificationFactory = specificationFactory;
            this.structures = structures.withExceptionContext(
                "Get structures from future structures %s",
                structures
            );
        }

        @Override
        public CompletableFuture<Optional<Structure>> getStructure()
        {
            return structures
                .thenApply(StructureRetriever::listToOptional);
        }

        @Override
        public CompletableFuture<List<Structure>> getStructures()
        {
            return structures;
        }

        private CompletableFuture<List<Structure>> getStructures0(
            IPlayer player,
            PermissionLevel permissionLevel)
        {
            return structures
                .thenApply(retrieved -> StructureRetriever.filter(retrieved, player, permissionLevel))
                .withExceptionContext(
                    "Get structures for player %s with permission level %s from future structures %s",
                    player,
                    permissionLevel,
                    structures
                );
        }

        @Override
        public CompletableFuture<List<Structure>> getStructures(IPlayer player, PermissionLevel permissionLevel)
        {
            return getStructures0(player, permissionLevel);
        }

        @Override
        public CompletableFuture<Optional<Structure>> getStructure(
            IPlayer player,
            PermissionLevel permissionLevel)
        {
            return getStructures0(player, permissionLevel)
                .thenApply(StructureRetriever::listToOptional);
        }

        @Override
        public CompletableFuture<Optional<Structure>> getStructureInteractive(
            IPlayer player,
            PermissionLevel permissionLevel)
        {
            return getStructures(player, permissionLevel)
                .thenCompose(structures ->
                    getStructureInteractive(structures, player, specificationFactory))
                .withExceptionContext(
                    "Interactively get structure for player %s with permission level %s from future structures %s",
                    player,
                    permissionLevel,
                    structures
                );
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
        private final CompletableFuture<Optional<Structure>> futureStructure;

        @Override
        public CompletableFuture<Optional<Structure>> getStructure()
        {
            return futureStructure.withExceptionContext("Get structure from future structure %s", futureStructure);
        }

        @Override
        public CompletableFuture<Optional<Structure>> getStructure(
            IPlayer player,
            PermissionLevel permissionLevel)
        {
            return futureStructure
                .thenApply(retrieved -> StructureRetriever.filter(retrieved, player, permissionLevel))
                .withExceptionContext(
                    "Get structure for player %s with permission level %s from future structure %s",
                    player,
                    permissionLevel,
                    futureStructure
                );
        }
    }
}

