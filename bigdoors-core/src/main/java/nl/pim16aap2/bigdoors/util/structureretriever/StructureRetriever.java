package nl.pim16aap2.bigdoors.util.structureretriever;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.StructureSpecificationManager;
import nl.pim16aap2.bigdoors.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.delayedinput.DelayedStructureSpecificationInputRequest;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Represents a way to retrieve a structure. It may be referenced by its name, its UID, or the object itself.
 *
 * @author Pim
 */
public sealed abstract class StructureRetriever
{
    /**
     * Checks if the structure that is being retrieved is available.
     *
     * @return True if the structure is available.
     */
    public boolean isAvailable()
    {
        return false;
    }

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
     *     The {@link IPPlayer} that owns the structure.
     * @return The {@link AbstractStructure} if it can be found.
     */
    public abstract CompletableFuture<Optional<AbstractStructure>> getStructure(IPPlayer player);

    /**
     * Gets the structure referenced by this {@link StructureRetriever}.
     * <p>
     * If the {@link ICommandSender} is a player, see {@link #getStructure(IPPlayer)}, otherwise see
     * {@link #getStructure()}.
     *
     * @param commandSender
     *     The {@link ICommandSender} for whom to retrieve the structures.
     * @return The structure referenced by this {@link StructureRetriever}.
     */
    public CompletableFuture<Optional<AbstractStructure>> getStructure(ICommandSender commandSender)
    {
        if (commandSender instanceof IPPlayer player)
            return getStructure(player);
        return getStructure();
    }

    /**
     * Attempts to retrieve a structure from its specification (see {@link #getStructure(IPPlayer)}).
     * <p>
     * If more than 1 match was found, the player will be asked to specify which one they asked for specifically.
     * <p>
     * The amount of time to wait (when required) is determined by {@link IConfigLoader#specificationTimeout()}.
     * <p>
     * See {@link DelayedStructureSpecificationInputRequest}.
     *
     * @param player
     *     The player for whom to get the structure.
     * @return The structure as specified by this {@link StructureRetriever} and with user input in case more than one
     * match was found.
     */
    // TODO: Implement the interactive system.
    public CompletableFuture<Optional<AbstractStructure>> getStructureInteractive(IPPlayer player)
    {
        return getStructure(player);
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
     *     The {@link IPPlayer} that owns all matching structures.
     * @return All structures referenced by this {@link StructureRetriever}.
     */
    public CompletableFuture<List<AbstractStructure>> getStructures(IPPlayer player)
    {
        return optionalToList(getStructure(player));
    }

    /**
     * Gets all structures referenced by this {@link StructureRetriever}.
     * <p>
     * If the {@link ICommandSender} is a player, see {@link #getStructures(IPPlayer)}, otherwise see
     * {@link #getStructures()}.
     *
     * @param commandSender
     *     The {@link ICommandSender} for whom to retrieve the structures.
     * @return The structures referenced by this {@link StructureRetriever}.
     */
    public CompletableFuture<List<AbstractStructure>> getStructures(ICommandSender commandSender)
    {
        if (commandSender instanceof IPPlayer player)
            return getStructures(player);
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
        return optionalStructure.thenApply(structure -> structure.map(Collections::singletonList)
                                                                 .orElseGet(Collections::emptyList));
    }

    /**
     * Represents a {@link StructureRetriever} that references a structure by its name.
     * <p>
     * Because names are not unique, a single name may reference more than 1 structure (even for a single player).
     *
     * @author Pim
     */
    @ToString
    @AllArgsConstructor
    @Flogger
    static final class StructureNameRetriever extends StructureRetriever
    {
        @ToString.Exclude
        private final DatabaseManager databaseManager;

        @ToString.Exclude
        private IConfigLoader config;

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
        public CompletableFuture<Optional<AbstractStructure>> getStructure(IPPlayer player)
        {
            return listToOptional(databaseManager.getStructures(player, name));
        }

        @Override
        public CompletableFuture<List<AbstractStructure>> getStructures()
        {
            return databaseManager.getStructures(name)
                                  .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
        }

        @Override
        public CompletableFuture<List<AbstractStructure>> getStructures(IPPlayer player)
        {
            return databaseManager.getStructures(player, name)
                                  .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
        }

        @Override
        public CompletableFuture<Optional<AbstractStructure>> getStructureInteractive(IPPlayer player)
        {
            return getStructures(player).thenCompose(
                structuresList ->
                {
                    if (structuresList.size() == 1)
                        return CompletableFuture.completedFuture(Optional.of(structuresList.get(0)));

                    if (structuresList.isEmpty())
                        return CompletableFuture.completedFuture(Optional.empty());

                    final Duration timeOut = Duration.ofSeconds(config.specificationTimeout());
                    return DelayedStructureSpecificationInputRequest.get(timeOut, structuresList, player, localizer,
                                                                         textFactory, structureSpecificationManager);

                }).exceptionally(Util::exceptionallyOptional);
        }

        /**
         * Gets a single (optional/future) structure from a list of (future) structures if only 1 structure exists in
         * the list.
         *
         * @param list
         *     The list of (future) structures.
         * @return An optional (future) {@link AbstractStructure} if exactly 1 existed in the list, otherwise an empty
         * optional.
         */
        private CompletableFuture<Optional<AbstractStructure>> listToOptional(
            CompletableFuture<List<AbstractStructure>> list)
        {
            return list.<Optional<AbstractStructure>>thenApply(
                structuresList ->
                {
                    if (structuresList.size() == 1)
                        return Optional.of(structuresList.get(0));
                    log.atWarning().log("Tried to get 1 structure but received %d!", structuresList.size());
                    return Optional.empty();
                }).exceptionally(Util::exceptionallyOptional);
        }
    }

    /**
     * Represents a {@link StructureRetriever} that references a structure by its UID.
     * <p>
     * Because the UID is always unique (by definition), this can never reference more than 1 structure.
     *
     * @author Pim
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
        public CompletableFuture<Optional<AbstractStructure>> getStructure(IPPlayer player)
        {
            return databaseManager.getStructure(player, uid).exceptionally(Util::exceptionallyOptional);
        }
    }

    /**
     * Represents a {@link StructureRetriever} that references a structure by the object itself.
     *
     * @author Pim
     */
    @AllArgsConstructor()
    @ToString(doNotUseGetters = true)
    @EqualsAndHashCode(callSuper = false, doNotUseGetters = true)
    static final class StructureObjectRetriever extends StructureRetriever
    {
        private final @Nullable AbstractStructure structure;

        @Override
        public boolean isAvailable()
        {
            return structure != null;
        }

        @Override
        public CompletableFuture<Optional<AbstractStructure>> getStructure()
        {
            return CompletableFuture.completedFuture(Optional.ofNullable(structure));
        }

        @Override
        public CompletableFuture<Optional<AbstractStructure>> getStructure(IPPlayer player)
        {
            return structure != null && structure.isOwner(player) ?
                   getStructure() : CompletableFuture.completedFuture(Optional.empty());
        }
    }

    /**
     * Represents a {@link StructureRetriever} that references a list of structures by the object themselves.
     *
     * @author Pim
     */
    @ToString
    @AllArgsConstructor()
    @EqualsAndHashCode(callSuper = false, doNotUseGetters = true)
    @Flogger
    static final class StructureListRetriever extends StructureRetriever
    {
        private final List<AbstractStructure> structures;

        @Override
        public boolean isAvailable()
        {
            return true;
        }

        @Override
        public CompletableFuture<Optional<AbstractStructure>> getStructure()
        {
            if (structures.size() == 1)
                return CompletableFuture.completedFuture(Optional.of(structures.get(0)));

            log.atWarning().log("Tried to get 1 structure but received %d!", structures.size());
            return CompletableFuture.completedFuture(Optional.empty());
        }

        @Override
        public CompletableFuture<List<AbstractStructure>> getStructures()
        {
            return CompletableFuture.completedFuture(structures);
        }

        private List<AbstractStructure> getStructures0(IPPlayer player)
        {
            final UUID playerUUID = player.getUUID();
            return structures.stream().filter(structure -> structure.isOwner(playerUUID)).collect(Collectors.toList());
        }

        @Override
        public CompletableFuture<List<AbstractStructure>> getStructures(IPPlayer player)
        {
            return CompletableFuture.completedFuture(getStructures0(player));
        }

        @Override
        public CompletableFuture<Optional<AbstractStructure>> getStructure(IPPlayer player)
        {
            final List<AbstractStructure> ret = getStructures0(player);

            if (ret.size() == 1)
                return CompletableFuture.completedFuture(Optional.of(ret.get(0)));

            log.atWarning().log("Tried to get 1 structure but received %d!", ret.size());
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    /**
     * Represents a {@link StructureRetriever} that references a future list of structures by the object themselves.
     *
     * @author Pim
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
        public boolean isAvailable()
        {
            return structures.isDone();
        }

        @Override
        public CompletableFuture<Optional<AbstractStructure>> getStructure()
        {
            return structures.thenApply(
                lst ->
                {
                    if (lst.size() == 1)
                        return Optional.of(lst.get(0));
                    log.atWarning().log("Tried to get 1 structure but received %d!", lst.size());
                    return Optional.empty();
                });
        }

        @Override
        public CompletableFuture<List<AbstractStructure>> getStructures()
        {
            return structures;
        }

        private CompletableFuture<List<AbstractStructure>> getStructures0(IPPlayer player)
        {
            final UUID playerUUID = player.getUUID();
            return structures.thenApply(
                retrieved -> retrieved.stream().filter(structure -> structure.isOwner(playerUUID)).toList());
        }

        @Override
        public CompletableFuture<List<AbstractStructure>> getStructures(IPPlayer player)
        {
            return getStructures0(player);
        }

        @Override
        public CompletableFuture<Optional<AbstractStructure>> getStructure(IPPlayer player)
        {
            return getStructures0(player).thenApply(
                lst ->
                {
                    if (lst.size() == 1)
                        return Optional.of(lst.get(0));
                    log.atWarning().log("Tried to get 1 structure but received %d!", lst.size());
                    return Optional.empty();
                });
        }
    }

    /**
     * Represents a {@link StructureRetriever} that references a future optional structure directly.
     *
     * @author Pim
     */
    @ToString
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    static final class FutureStructureRetriever extends StructureRetriever
    {
        private final CompletableFuture<Optional<AbstractStructure>> futureStructure;

        @Override
        public boolean isAvailable()
        {
            return futureStructure.isDone() && !futureStructure.isCancelled() &&
                !futureStructure.isCompletedExceptionally();
        }

        @Override
        public CompletableFuture<Optional<AbstractStructure>> getStructure()
        {
            return futureStructure;
        }

        @Override
        public CompletableFuture<Optional<AbstractStructure>> getStructure(IPPlayer player)
        {
            return futureStructure.thenApply(
                structureOpt ->
                {
                    final boolean playerIsPresent =
                        structureOpt.map(structure -> structure.isOwner(player)).orElse(false);
                    return playerIsPresent ? structureOpt : Optional.empty();
                });
        }
    }
}

