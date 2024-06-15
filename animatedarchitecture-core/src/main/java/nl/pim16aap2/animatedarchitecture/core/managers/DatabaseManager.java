package nl.pim16aap2.animatedarchitecture.core.managers;

import com.google.common.flogger.StackSize;
import dagger.Lazy;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.longs.LongList;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.PlayerData;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IAnimatedArchitectureEventFactory;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.Restartable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.events.IAnimatedArchitectureEventCaller;
import nl.pim16aap2.animatedarchitecture.core.events.ICancellableAnimatedArchitectureEvent;
import nl.pim16aap2.animatedarchitecture.core.events.IStructureCreatedEvent;
import nl.pim16aap2.animatedarchitecture.core.events.IStructurePrepareCreateEvent;
import nl.pim16aap2.animatedarchitecture.core.events.IStructurePrepareDeleteEvent;
import nl.pim16aap2.animatedarchitecture.core.storage.IStorage;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureModifier;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureOwner;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Manages all database interactions.
 */
@Singleton
@Flogger
public final class DatabaseManager extends Restartable implements IDebuggable
{
    /**
     * The thread pool to use for storage access.
     */
    private volatile ExecutorService threadPool;

    /**
     * The number of threads to use for storage access.
     */
    private static final int THREAD_COUNT = 16;

    private final IStorage db;

    private final StructureDeletionManager structureDeletionManager;
    private final IAnimatedArchitectureEventCaller animatedArchitectureEventCaller;
    private final Lazy<PowerBlockManager> powerBlockManager;
    private final IAnimatedArchitectureEventFactory animatedArchitectureEventFactory;
    private final StructureModifier structureModifier;

    /**
     * Constructs a new {@link DatabaseManager}.
     *
     * @param restartableHolder
     *     The object managing restarts for this object.
     * @param storage
     *     The {@link IStorage} to use for all database calls.
     */
    @Inject
    public DatabaseManager(
        RestartableHolder restartableHolder,
        IStorage storage,
        StructureDeletionManager structureDeletionManager,
        Lazy<PowerBlockManager> powerBlockManager,
        IAnimatedArchitectureEventFactory animatedArchitectureEventFactory,
        IAnimatedArchitectureEventCaller animatedArchitectureEventCaller,
        DebuggableRegistry debuggableRegistry)
    {
        super(restartableHolder);
        db = storage;
        this.structureDeletionManager = structureDeletionManager;
        this.animatedArchitectureEventCaller = animatedArchitectureEventCaller;
        this.powerBlockManager = powerBlockManager;
        this.animatedArchitectureEventFactory = animatedArchitectureEventFactory;
        this.structureModifier = StructureModifier.get(new FriendKey());
        initThreadPool();
        debuggableRegistry.registerDebuggable(this);
    }

    /**
     * Obtains {@link IStorage.DatabaseState} the database is in.
     *
     * @return The {@link IStorage.DatabaseState} the database is in.
     */
    public IStorage.DatabaseState getDatabaseState()
    {
        return db.getDatabaseState();
    }

    @Override
    public void initialize()
    {
        initThreadPool();
    }

    @Override
    public void shutDown()
    {
        threadPool.shutdown();
        try
        {
            if (!threadPool.awaitTermination(30, TimeUnit.SECONDS))
                log.atSevere().log(
                    "Timed out waiting to terminate DatabaseManager ExecutorService!" +
                        " The database may be out of sync with the world!"
                );
        }
        catch (InterruptedException exception)
        {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                "Thread got interrupted waiting for DatabaseManager ExecutorService to terminate!" +
                    " The database may be out of sync with the world!",
                exception
            );
        }
    }

    private void initThreadPool()
    {
        this.threadPool = Executors.newFixedThreadPool(THREAD_COUNT);
    }

    /**
     * Inserts a {@link AbstractStructure} into the database and assumes that the structure was NOT created by an
     * {@link IPlayer}. See {@link #addStructure(AbstractStructure, IPlayer)}.
     *
     * @param structure
     *     The new {@link AbstractStructure}.
     * @return The future result of the operation. If the operation was successful this will be true.
     */
    public CompletableFuture<StructureInsertResult> addStructure(AbstractStructure structure)
    {
        return addStructure(structure, null);
    }

    /**
     * Inserts a {@link AbstractStructure} into the database.
     *
     * @param structure
     *     The new {@link AbstractStructure}.
     * @param responsible
     *     The {@link IPlayer} responsible for creating the structure. This is used for the
     *     {@link IStructurePrepareCreateEvent} and the {@link IStructureCreatedEvent}. This may be null.
     * @return The future result of the operation.
     */
    public CompletableFuture<StructureInsertResult> addStructure(
        AbstractStructure structure,
        @Nullable IPlayer responsible)
    {
        final var ret = callCancellableEvent(fact -> fact
            .createPrepareStructureCreateEvent(structure, responsible))
            .thenApplyAsync(event ->
            {
                if (event.isCancelled())
                    return new StructureInsertResult(Optional.empty(), true);

                final Optional<AbstractStructure> result = db.insert(structure);
                result.ifPresentOrElse(
                    newStructure ->
                    {
                        powerBlockManager.get().onStructureAddOrRemove(
                            newStructure.getWorld().worldName(),
                            new Vector3Di(
                                newStructure.getPowerBlock().x(),
                                newStructure.getPowerBlock().y(),
                                newStructure.getPowerBlock().z())
                        );
                        newStructure.verifyRedstoneState();
                    },
                    () -> log.atSevere().withStackTrace(StackSize.FULL).log("Failed to process event: %s", event)
                );

                return new StructureInsertResult(result, false);
            }, threadPool)
            .exceptionally(ex -> Util.exceptionally(ex, new StructureInsertResult(Optional.empty(), false)));

        ret.thenAccept(result -> result.structure.ifPresent(unused -> callStructureCreatedEvent(result, responsible)))
            .exceptionally(Util::exceptionally);

        return ret;
    }

    /**
     * Calls the {@link IStructureCreatedEvent}.
     *
     * @param result
     *     The result of trying to add a structure to the database.
     * @param responsible
     *     The {@link IPlayer} responsible for creating it, if an {@link IPlayer} was responsible for it. If not, this
     *     is null.
     */
    private void callStructureCreatedEvent(StructureInsertResult result, @Nullable IPlayer responsible)
    {
        CompletableFuture
            .runAsync(() ->
            {
                if (result.cancelled() || result.structure().isEmpty())
                    return;

                final IStructureCreatedEvent structureCreatedEvent =
                    animatedArchitectureEventFactory.createStructureCreatedEvent(result.structure().get(), responsible);

                animatedArchitectureEventCaller.callAnimatedArchitectureEvent(structureCreatedEvent);
            })
            .exceptionally(Util::exceptionally);
    }

    /**
     * Removes a {@link AbstractStructure} from the database and assumes that the structure was NOT deleted by an
     * {@link IPlayer}. See {@link #deleteStructure(AbstractStructure, IPlayer)}.
     *
     * @param structure
     *     The structure that will be deleted.
     * @return The future result of the operation.
     */
    @SuppressWarnings("unused")
    public CompletableFuture<ActionResult> deleteStructure(AbstractStructure structure)
    {
        return deleteStructure(structure, null);
    }

    /**
     * Removes a {@link AbstractStructure} from the database.
     *
     * @param structure
     *     The structure that will be deleted.
     * @param responsible
     *     The {@link IPlayer} responsible for creating the structure. This is used for the
     *     {@link IStructurePrepareDeleteEvent}. This may be null.
     * @return The future result of the operation.
     */
    public CompletableFuture<ActionResult> deleteStructure(AbstractStructure structure, @Nullable IPlayer responsible)
    {
        return callCancellableEvent(fact -> fact
            .createPrepareDeleteStructureEvent(structure, responsible))
            .thenApplyAsync(event ->
            {
                if (event.isCancelled())
                    return ActionResult.CANCELLED;

                final StructureSnapshot snapshot = structure.getSnapshot();
                final boolean result = db.removeStructure(snapshot.getUid());
                if (!result)
                {
                    log.atSevere().withStackTrace(StackSize.FULL).log("Failed to process event: %s", event);
                    return ActionResult.FAIL;
                }

                structureDeletionManager.onStructureDeletion(snapshot);

                return ActionResult.SUCCESS;
            }, threadPool)
            .exceptionally(ex -> Util.exceptionally(ex, ActionResult.FAIL));
    }

    /**
     * Gets a list of structure UIDs that have their rotation point in a given chunk.
     *
     * @param chunkX
     *     The x-coordinate of the chunk (in chunk space).
     * @param chunkZ
     *     The z-coordinate of the chunk (in chunk space).
     * @return A list of structure UIDs that have their rotation point in a given chunk.
     */
    public CompletableFuture<List<AbstractStructure>> getStructuresInChunk(int chunkX, int chunkZ)
    {
        final long chunkId = Util.getChunkId(chunkX, chunkZ);
        return CompletableFuture
            .supplyAsync(() -> db.getStructuresInChunk(chunkId), threadPool)
            .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
    }

    /**
     * Obtains all structures of a given type.
     *
     * @param typeName
     *     The name of the type. See {@link StructureType#getFullName()}.
     * @return All structures of the given type.
     */
    public CompletableFuture<List<AbstractStructure>> getStructuresOfType(String typeName)
    {
        return CompletableFuture
            .supplyAsync(() -> db.getStructuresOfType(typeName), threadPool)
            .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
    }

    /**
     * Obtains all structures of a specific version of a given type.
     *
     * @param typeName
     *     The name of the type. See {@link StructureType#getFullName()}.
     * @param version
     *     The version of the type.
     * @return All structures of the given type and version.
     */
    public CompletableFuture<List<AbstractStructure>> getStructuresOfType(String typeName, int version)
    {
        return CompletableFuture
            .supplyAsync(() -> db.getStructuresOfType(typeName, version), threadPool)
            .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
    }

    /**
     * Gets all {@link AbstractStructure} owned by a player. Only searches for {@link AbstractStructure} with a given
     * name if one was provided.
     *
     * @param playerUUID
     *     The {@link UUID} of the payer.
     * @param structureID
     *     The name or the UID of the {@link AbstractStructure} to search for. Can be null.
     * @return All {@link AbstractStructure} owned by a player with a specific name.
     */
    public CompletableFuture<List<AbstractStructure>> getStructures(UUID playerUUID, String structureID)
    {
        // Check if the name is actually the UID of the structure.
        final OptionalLong structureUID = Util.parseLong(structureID);
        if (structureUID.isPresent())
            return CompletableFuture
                .supplyAsync(() -> db.getStructure(playerUUID, structureUID.getAsLong())
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList()), threadPool)
                .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));

        return CompletableFuture
            .supplyAsync(() -> db.getStructures(playerUUID, structureID), threadPool)
            .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
    }

    /**
     * See {@link #getStructures(UUID, String)}.
     */
    public CompletableFuture<List<AbstractStructure>> getStructures(IPlayer player, String name)
    {
        return getStructures(player.getUUID(), name);
    }

    /**
     * Gets all {@link AbstractStructure} owned by a player.
     *
     * @param playerUUID
     *     The {@link UUID} of the player.
     * @return All {@link AbstractStructure} owned by a player.
     */
    public CompletableFuture<List<AbstractStructure>> getStructures(UUID playerUUID)
    {
        return CompletableFuture
            .supplyAsync(() -> db.getStructures(playerUUID), threadPool)
            .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
    }

    /**
     * See {@link #getStructures(UUID)}.
     */
    public CompletableFuture<List<AbstractStructure>> getStructures(IPlayer player)
    {
        return getStructures(player.getUUID());
    }

    /**
     * Gets all {@link AbstractStructure} owned by a player with a specific name.
     *
     * @param player
     *     The player whose structures to retrieve.
     * @param name
     *     The name of the {@link AbstractStructure} to search for.
     * @param maxPermission
     *     The maximum level of ownership (inclusive) this player has over the {@link AbstractStructure}s.
     * @return All {@link AbstractStructure} owned by a player with a specific name.
     */
    public CompletableFuture<List<AbstractStructure>> getStructures(
        IPlayer player,
        String name,
        PermissionLevel maxPermission)
    {
        return CompletableFuture
            .supplyAsync(() -> db.getStructures(player.getUUID(), name, maxPermission), threadPool)
            .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
    }

    /**
     * Gets all {@link AbstractStructure}s with a specific name, regardless over ownership.
     *
     * @param name
     *     The name of the {@link AbstractStructure}s.
     * @return All {@link AbstractStructure}s with a specific name.
     */
    public CompletableFuture<List<AbstractStructure>> getStructures(String name)
    {
        return CompletableFuture
            .supplyAsync(() -> db.getStructures(name), threadPool)
            .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
    }

    /**
     * Updates the name of a player in the database, to make sure the player's name and UUID don't go out of sync.
     *
     * @param player
     *     The Player.
     * @return The future result of the operation. If the operation was successful this will be true.
     */
    @SuppressWarnings({"unused", "UnusedReturnValue"})
    public CompletableFuture<Boolean> updatePlayer(IPlayer player)
    {
        return CompletableFuture
            .supplyAsync(() -> db.updatePlayerData(player.getPlayerData()), threadPool)
            .exceptionally(ex -> Util.exceptionally(ex, Boolean.FALSE));
    }

    /**
     * Tries to find the {@link PlayerData} for a player with the given {@link UUID}.
     *
     * @param uuid
     *     The {@link UUID} of a player.
     * @return The {@link PlayerData} that represents the player.
     */
    public CompletableFuture<Optional<PlayerData>> getPlayerData(UUID uuid)
    {
        return CompletableFuture
            .supplyAsync(() -> db.getPlayerData(uuid), threadPool)
            .exceptionally(Util::exceptionallyOptional);
    }

    /**
     * Tries to get all the players with a given name. Because names are not unique, this may result in any number of
     * matches.
     * <p>
     * If you know the player's UUID, it is recommended to use {@link #getPlayerData(UUID)} instead.
     *
     * @param playerName
     *     The name of the player(s).
     * @return All the players with the given name.
     */
    @SuppressWarnings("unused")
    public CompletableFuture<List<PlayerData>> getPlayerData(String playerName)
    {
        return CompletableFuture
            .supplyAsync(() -> db.getPlayerData(playerName), threadPool)
            .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
    }

    /**
     * Gets the {@link AbstractStructure} with a specific UID.
     *
     * @param structureUID
     *     The UID of the {@link AbstractStructure}.
     * @return The {@link AbstractStructure} if it exists.
     */
    public CompletableFuture<Optional<AbstractStructure>> getStructure(long structureUID)
    {
        return CompletableFuture
            .supplyAsync(() -> db.getStructure(structureUID), threadPool)
            .exceptionally(Util::exceptionallyOptional);
    }

    /**
     * Gets the {@link AbstractStructure} with the given UID owned by the player. If the given player does not own the
     * provided structure, no structure will be returned.
     *
     * @param player
     *     The {@link IPlayer}.
     * @param structureUID
     *     The UID of the {@link AbstractStructure}.
     * @return The {@link AbstractStructure} with the given UID if it exists and the provided player owns it.
     */
    public CompletableFuture<Optional<AbstractStructure>> getStructure(IPlayer player, long structureUID)
    {
        return getStructure(player.getUUID(), structureUID);
    }

    /**
     * Gets the {@link AbstractStructure} with the given UID owned by the player. If the given player does not own the *
     * provided structure, no structure will be returned.
     *
     * @param uuid
     *     The {@link UUID} of the player.
     * @param structureUID
     *     The UID of the {@link AbstractStructure}.
     * @return The {@link AbstractStructure} with the given UID if it exists and the provided player owns it.
     */
    public CompletableFuture<Optional<AbstractStructure>> getStructure(UUID uuid, long structureUID)
    {
        return CompletableFuture
            .supplyAsync(() -> db.getStructure(uuid, structureUID), threadPool)
            .exceptionally(Util::exceptionallyOptional);
    }

    /**
     * Gets the number of {@link AbstractStructure}s owned by a player.
     *
     * @param playerUUID
     *     The {@link UUID} of the player.
     * @return The number of {@link AbstractStructure}s this player owns.
     */
    @SuppressWarnings("unused")
    public CompletableFuture<Integer> countStructuresOwnedByPlayer(UUID playerUUID)
    {
        return CompletableFuture
            .supplyAsync(() -> db.getStructureCountForPlayer(playerUUID), threadPool)
            .exceptionally(ex -> Util.exceptionally(ex, -1));
    }

    /**
     * Counts the number of {@link AbstractStructure}s with a specific name owned by a player.
     *
     * @param playerUUID
     *     The {@link UUID} of the player.
     * @param structureName
     *     The name of the structure.
     * @return The number of {@link AbstractStructure}s with a specific name owned by a player.
     */
    @SuppressWarnings("unused")
    public CompletableFuture<Integer> countStructuresOwnedByPlayer(UUID playerUUID, String structureName)
    {
        return CompletableFuture
            .supplyAsync(() -> db.getStructureCountForPlayer(playerUUID, structureName), threadPool)
            .exceptionally(ex -> Util.exceptionally(ex, -1));
    }

    /**
     * The number of {@link AbstractStructure}s in the database with a specific name.
     *
     * @param structureName
     *     The name of the {@link AbstractStructure}.
     * @return The number of {@link AbstractStructure}s with a specific name.
     */
    @SuppressWarnings("unused")
    public CompletableFuture<Integer> countStructuresByName(String structureName)
    {
        return CompletableFuture
            .supplyAsync(() -> db.getStructureCountByName(structureName), threadPool)
            .exceptionally(ex -> Util.exceptionally(ex, -1));
    }

    /**
     * Adds a player as owner to a {@link AbstractStructure} at a given level of ownership and assumes that the
     * structure was NOT deleted by an {@link IPlayer}. See
     * {@link #addOwner(AbstractStructure, IPlayer, PermissionLevel, IPlayer)}.
     *
     * @param structure
     *     The {@link AbstractStructure}.
     * @param player
     *     The {@link IPlayer}.
     * @param permission
     *     The level of ownership.
     * @return The future result of the operation.
     */
    public CompletableFuture<ActionResult> addOwner(
        AbstractStructure structure,
        IPlayer player,
        PermissionLevel permission)
    {
        return addOwner(structure, player, permission, null);
    }

    /**
     * Adds a player as owner to a {@link AbstractStructure} at a given level of ownership.
     *
     * @param structure
     *     The {@link AbstractStructure}.
     * @param player
     *     The {@link IPlayer}.
     * @param permission
     *     The level of ownership.
     * @return The future result of the operation.
     */
    public CompletableFuture<ActionResult> addOwner(
        AbstractStructure structure,
        IPlayer player,
        PermissionLevel permission,
        @Nullable IPlayer responsible)
    {
        if (permission.getValue() < 1 || permission == PermissionLevel.NO_PERMISSION)
            return CompletableFuture.completedFuture(ActionResult.FAIL);

        final var newOwner = new StructureOwner(structure.getUid(), permission, player.getPlayerData());

        return callCancellableEvent(fact -> fact
            .createStructurePrepareAddOwnerEvent(structure, newOwner, responsible))
            .thenApplyAsync(event ->
            {
                if (event.isCancelled())
                    return ActionResult.CANCELLED;

                if (!structureModifier.addOwner(structure, newOwner))
                {
                    log.atSevere().log("Failed to add owner %s to structure %s!", newOwner, structure);
                    return ActionResult.FAIL;
                }

                final boolean result = db.addOwner(structure.getUid(), newOwner.playerData(), permission);
                if (!result)
                {
                    log.atSevere().withStackTrace(StackSize.FULL).log("Failed to process event: %s", event);
                    structureModifier.removeOwner(structure, newOwner.playerData().getUUID());
                    return ActionResult.FAIL;
                }

                return ActionResult.SUCCESS;
            }, threadPool)
            .exceptionally(ex -> Util.exceptionally(ex, ActionResult.FAIL));
    }

    /**
     * Calls an {@link ICancellableAnimatedArchitectureEvent} and checks if it was cancelled or not.
     *
     * @param factoryMethod
     *     The method to use to construct the event.
     * @return True if the create event was cancelled, otherwise false.
     */
    private <T extends ICancellableAnimatedArchitectureEvent> CompletableFuture<T> callCancellableEvent(
        Function<IAnimatedArchitectureEventFactory, T> factoryMethod)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            final T event = factoryMethod.apply(animatedArchitectureEventFactory);
            log.atFinest().log("Calling event: %s", event);
            animatedArchitectureEventCaller.callAnimatedArchitectureEvent(event);
            log.atFinest().log("Processed event: %s", event);
            return event;
        });
    }

    /**
     * Remove a {@link IPlayer} as owner of a {@link AbstractStructure}.
     *
     * @param structure
     *     The {@link AbstractStructure}.
     * @param player
     *     The {@link IPlayer}.
     * @return True if owner removal was successful.
     */
    public CompletableFuture<ActionResult> removeOwner(AbstractStructure structure, IPlayer player)
    {
        return removeOwner(structure, player, null);
    }

    /**
     * Remove a {@link IPlayer} as owner of a {@link AbstractStructure}.
     *
     * @param structure
     *     The {@link AbstractStructure}.
     * @param player
     *     The {@link IPlayer}.
     * @param responsible
     *     The {@link IPlayer} responsible for creating the structure. This is used for the
     *     {@link IStructurePrepareDeleteEvent}. This may be null.
     * @return The future result of the operation.
     */
    public CompletableFuture<ActionResult> removeOwner(
        AbstractStructure structure,
        IPlayer player,
        @Nullable IPlayer responsible)
    {
        return removeOwner(structure, player.getUUID(), responsible);
    }

    /**
     * Remove a {@link IPlayer} as owner of a {@link AbstractStructure} and assumes that the structure was NOT deleted
     * by an {@link IPlayer}. See {@link #removeOwner(AbstractStructure, UUID, IPlayer)}.
     *
     * @param structure
     *     The {@link AbstractStructure}.
     * @param playerUUID
     *     The {@link UUID} of the {@link IPlayer}.
     * @return The future result of the operation.
     */
    public CompletableFuture<ActionResult> removeOwner(AbstractStructure structure, UUID playerUUID)
    {
        return removeOwner(structure, playerUUID, null);
    }

    /**
     * Remove a {@link IPlayer} as owner of a {@link AbstractStructure}.
     *
     * @param structure
     *     The {@link AbstractStructure}.
     * @param playerUUID
     *     The {@link UUID} of the {@link IPlayer}.
     * @param responsible
     *     The {@link IPlayer} responsible for creating the structure. This is used for the
     *     {@link IStructurePrepareDeleteEvent}. This may be null.
     * @return The future result of the operation.
     */
    public CompletableFuture<ActionResult> removeOwner(
        AbstractStructure structure,
        UUID playerUUID,
        @Nullable IPlayer responsible)
    {
        final Optional<StructureOwner> structureOwner = structure.getOwner(playerUUID);
        if (structureOwner.isEmpty())
        {
            log.atFine().log(
                "Trying to remove player: %s from structure: %d, but the player is not an owner!",
                playerUUID,
                structure.getUid()
            );
            return CompletableFuture.completedFuture(ActionResult.FAIL);
        }
        if (structureOwner.get().permission() == PermissionLevel.CREATOR)
        {
            log.atFine().log(
                "Trying to remove player: %s from structure: %d, but the player is the prime owner! " +
                    "This is not allowed!",
                playerUUID,
                structure.getUid()
            );
            return CompletableFuture.completedFuture(ActionResult.FAIL);
        }

        return callCancellableEvent(fact -> fact
            .createStructurePrepareRemoveOwnerEvent(structure, structureOwner.get(), responsible))
            .thenApplyAsync(event ->
            {
                if (event.isCancelled())
                    return ActionResult.CANCELLED;

                final @Nullable StructureOwner oldOwner = structureModifier.removeOwner(structure, playerUUID);
                if (oldOwner == null)
                {
                    log.atSevere().log("Failed to remove owner %s from structure %s!", structureOwner.get(), structure);
                    return ActionResult.FAIL;
                }

                final boolean result = db.removeOwner(structure.getUid(), playerUUID);
                if (!result)
                {
                    log.atSevere().withStackTrace(StackSize.FULL).log("Failed to process event: %s", event);
                    structureModifier.addOwner(structure, oldOwner);
                    return ActionResult.FAIL;
                }

                return ActionResult.SUCCESS;
            }, threadPool)
            .exceptionally(ex -> Util.exceptionally(ex, ActionResult.FAIL));
    }

    /**
     * Updates the all data of an {@link AbstractStructure}. This includes both the base data and the type-specific
     * data.
     *
     * @param snapshot
     *     The {@link AbstractStructure} that describes the base data of structure.
     * @param typeData
     *     The type-specific data of this structure represented as a json String.
     * @return The result of the operation.
     */
    public CompletableFuture<DatabaseManager.ActionResult> syncStructureData(
        StructureSnapshot snapshot,
        String typeData)
    {
        return CompletableFuture
            .supplyAsync(
                () -> db.syncStructureData(snapshot, typeData) ? ActionResult.SUCCESS : ActionResult.FAIL,
                threadPool)
            .exceptionally(ex -> Util.exceptionally(ex, ActionResult.FAIL));
    }

    /**
     * Retrieves all {@link StructureIdentifier}s that start with the provided input.
     * <p>
     * For example, this method can retrieve the identifiers "1", "10", "11", "100", etc from an input of "1" or
     * "MyDoor", "MyPortcullis", "MyOtherStructure", etc. from an input of "My".
     *
     * @param input
     *     The partial identifier to look for.
     * @param player
     *     The player that should own the structures. May be null to disregard ownership.
     * @return All {@link StructureIdentifier}s that start with the provided input.
     */
    public CompletableFuture<List<StructureIdentifier>> getIdentifiersFromPartial(
        String input,
        @Nullable IPlayer player,
        PermissionLevel maxPermission)
    {
        return CompletableFuture
            .supplyAsync(() -> db.getPartialIdentifiers(input, player, maxPermission), threadPool)
            .exceptionally(t -> Util.exceptionally(t, Collections.emptyList()));
    }

    /**
     * Checks if a world contains any structures.
     *
     * @param worldName
     *     The name of the world.
     * @return True if at least 1 structure exists in the world.
     */
    CompletableFuture<Boolean> isAnimatedArchitectureWorld(String worldName)
    {
        return CompletableFuture
            .supplyAsync(() -> db.isAnimatedArchitectureWorld(worldName), threadPool)
            .exceptionally(ex -> Util.exceptionally(ex, Boolean.FALSE));
    }

    /**
     * Gets a map of location hashes and their connected powerblocks for all structures in a chunk.
     * <p>
     * The key is the hashed location in chunk space, the value is the list of UIDs of the structures whose powerblocks
     * occupies that location.
     *
     * @param chunkId
     *     The id of the chunk the structures are in.
     * @return A map of location hashes and their connected powerblocks for all structures in a chunk.
     */
    CompletableFuture<Int2ObjectMap<LongList>> getPowerBlockData(long chunkId)
    {
        return CompletableFuture
            .supplyAsync(() -> db.getPowerBlockData(chunkId), threadPool)
            .exceptionally(ex -> Util.exceptionally(ex, Int2ObjectMaps.emptyMap()));
    }

    @Override
    public String getDebugInformation()
    {
        return "Database status: " + threadPool;
    }

    /**
     * Represents the result of an action requested from the database. E.g. deleting a structure.
     */
    public enum ActionResult
    {
        /**
         * The request was cancelled. E.g. by an {@link ICancellableAnimatedArchitectureEvent} event.
         */
        CANCELLED,

        /**
         * Success! Everything went as expected.
         */
        SUCCESS,

        /**
         * Something went wrong. Check the logs?
         */
        FAIL
    }

    public static final class FriendKey
    {
        private FriendKey()
        {
        }
    }

    /**
     * Contains the result of an attempt to insert a structure into the database.
     *
     * @param structure
     *     The structure as it was inserted into the database. This will be empty when inserted failed.
     * @param cancelled
     *     Whether the insertion was cancelled. An insertion may be cancelled if some listener cancels the
     *     {@link IStructurePrepareCreateEvent} event.
     */
    public record StructureInsertResult(Optional<AbstractStructure> structure, boolean cancelled)
    {
    }

    /**
     * Represents the identifier of a structure. This is a combination of the UID and the name of the structure.
     *
     * @param uid
     *     The UID of the structure.
     * @param name
     *     The name of the structure.
     */
    public record StructureIdentifier(long uid, String name)
    {
    }
}
