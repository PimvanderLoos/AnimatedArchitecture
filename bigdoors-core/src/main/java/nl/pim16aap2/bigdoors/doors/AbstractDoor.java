package nl.pim16aap2.bigdoors.doors;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEventTogglePrepare;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEventToggleStart;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.managers.DoorRegistry;
import nl.pim16aap2.bigdoors.moveblocks.AutoCloseScheduler;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.DoorToggleResult;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Represents the abstract base all door types should extend.
 *
 * @author Pim
 */
@EqualsAndHashCode
public abstract class AbstractDoor implements IDoor
{
    @SuppressWarnings("NullableProblems") // IntelliJ Struggles with <?> and nullability... :(
    @EqualsAndHashCode.Exclude
    private final DoorSerializer<?> serializer;
    private final DoorRegistry doorRegistry;
    private final AutoCloseScheduler autoCloseScheduler;

    @Getter
    protected final DoorBase doorBase;
    protected final IPLogger logger;
    protected final ILocalizer localizer;
    protected final DoorOpeningHelper doorOpeningHelper;


    protected AbstractDoor(DoorBase doorBase, IPLogger logger, ILocalizer localizer,
                           DoorRegistry doorRegistry, AutoCloseScheduler autoCloseScheduler,
                           DoorOpeningHelper doorOpeningHelper)
    {
        serializer = getDoorType().getDoorSerializer(logger);
        this.doorBase = doorBase;
        this.logger = logger;
        this.localizer = localizer;
        this.doorRegistry = doorRegistry;
        this.autoCloseScheduler = autoCloseScheduler;
        this.doorOpeningHelper = doorOpeningHelper;

        logger.logMessage(Level.FINEST, "Instantiating door: " + doorBase.getDoorUID());
        if (doorBase.getDoorUID() > 0 &&
            !doorRegistry.registerDoor(new Registrable()))
        {
            final IllegalStateException exception = new IllegalStateException(
                "Tried to create new door \"" + doorBase.getDoorUID() + "\" while it is already registered!");
            logger.logThrowableSilently(exception);
            throw exception;
        }
    }

    protected AbstractDoor(DoorBase doorBase)
    {
        this(doorBase, doorBase.getLogger(), doorBase.getLocalizer(), doorBase.getDoorRegistry(),
             doorBase.getAutoCloseScheduler(), doorBase.getDoorOpeningHelper());
    }

    /**
     * Gets the {@link DoorType} of doorBase door.
     *
     * @return The {@link DoorType} of doorBase door.
     */
    public abstract DoorType getDoorType();

    /**
     * Checks if this door can be opened instantly (i.e. skip the animation).
     * <p>
     * This describes whether skipping the animation actually does anything. When a door can skip its animation, it
     * means that the actual toggle has an effect on the world and as such toggling it without an animation does
     * something.
     * <p>
     * Doors that do not have any effect other than their animation because all their blocks start and stop at exactly
     * the same place (i.e. flags) cannot skip their animation because skipping the animation would mean removing the
     * blocks of the door from their locations and then placing them back at exactly the same position. This would be a
     * waste of performance for both the server and the client and as such should be prevented.
     *
     * @return True, if the door can skip its animation.
     */
    public boolean canSkipAnimation()
    {
        return false;
    }

    /**
     * Finds the new minimum and maximum coordinates (represented by a {@link Cuboid}) of this door that would be the
     * result of toggling it.
     *
     * @return The {@link Cuboid} that would represent the door if it was toggled right now.
     */
    public abstract Optional<Cuboid> getPotentialNewCoordinates();

    /**
     * Gets the direction the door would go given its current state.
     *
     * @return The direction the door would go if it were to be toggled.
     */
    public abstract RotateDirection getCurrentToggleDir();

    /**
     * Cycle the {@link RotateDirection} direction this {@link IDoor} will open in. By default, it'll set and return the
     * opposite direction of the current direction.
     * <p>
     * Note that this does not actually change the open direction; it merely tells you which direction comes next!
     *
     * @return The new {@link RotateDirection} direction this {@link IDoor} will open in.
     */
    @SuppressWarnings("unused")
    public RotateDirection cycleOpenDirection()
    {
        final List<RotateDirection> validOpenDirections = getDoorType().getValidOpenDirections();
        final int currentIdx = Math.max(0, validOpenDirections.indexOf(getOpenDir()));
        return validOpenDirections.get((currentIdx + 1) % validOpenDirections.size());
    }

    /**
     * Gets the {@link Supplier} for the {@link BlockMover} for doorBase type.
     * <p>
     * doorBase method MUST BE CALLED FROM THE MAIN THREAD! (Because of MC, spawning entities needs to happen
     * synchronously)
     *
     * @param cause
     *     What caused doorBase action.
     * @param time
     *     The amount of time doorBase {@link DoorBase} will try to use to move. The maximum speed is limited, so at a
     *     certain point lower values will not increase door speed.
     * @param skipAnimation
     *     If the {@link DoorBase} should be opened instantly (i.e. skip animation) or not.
     * @param newCuboid
     *     The {@link Cuboid} representing the area the door will take up after the toggle.
     * @param responsible
     *     The {@link IPPlayer} responsible for the door action.
     * @param actionType
     *     The type of action that will be performed by the BlockMover.
     * @param context
     *     The {@link BlockMover.Context} to run the block mover in.
     * @return The {@link BlockMover} for doorBase class.
     */
    protected abstract BlockMover constructBlockMover(BlockMover.Context context, DoorActionCause cause, double time,
                                                      boolean skipAnimation, Cuboid newCuboid, IPPlayer responsible,
                                                      DoorActionType actionType)
        throws Exception;

    /**
     * Attempts to toggle a door. Think twice before using doorBase method. Instead, please look at {@link
     * DoorToggleRequestFactory}.
     *
     * @param cause
     *     What caused doorBase action.
     * @param messageReceiver
     *     Who will receive any messages that have to be sent.
     * @param responsible
     *     Who is responsible for doorBase door. Either the player who directly toggled it (via a command or the GUI),
     *     or the prime owner when doorBase data is not available.
     * @param time
     *     The amount of time doorBase {@link DoorBase} will try to use to move. The maximum speed is limited, so at a
     *     certain point lower values will not increase door speed.
     * @param skipAnimation
     *     If the {@link DoorBase} should be opened instantly (i.e. skip animation) or not.
     * @param actionType
     *     The type of action.
     * @return The result of the attempt.
     */
    // TODO: Simplify this method.
    @SuppressWarnings({"unused", "squid:S1172"}) // messageReceiver isn't used yet, but it will be.
    final synchronized DoorToggleResult toggle(DoorActionCause cause, IMessageable messageReceiver,
                                               IPPlayer responsible, double time, boolean skipAnimation,
                                               DoorActionType actionType)
    {
        if (!doorOpeningHelper.isMainThread())
        {
            logger.logThrowable(
                new IllegalStateException("Doors must be toggled on the main thread!"));
            return DoorToggleResult.ERROR;
        }

        if (getOpenDir() == RotateDirection.NONE)
        {
            logger.logThrowable(new IllegalStateException("OpenDir cannot be NONE!"));
            return DoorToggleResult.ERROR;
        }

        if (!doorRegistry.isRegistered(this))
            return doorOpeningHelper.abort(this, DoorToggleResult.INSTANCE_UNREGISTERED, cause, responsible,
                                           messageReceiver);

        if (skipAnimation && !canSkipAnimation())
            return doorOpeningHelper.abort(this, DoorToggleResult.ERROR, cause, responsible, messageReceiver);

        final DoorToggleResult isOpenable = doorOpeningHelper.canBeToggled(this, actionType);
        if (isOpenable != DoorToggleResult.SUCCESS)
            return doorOpeningHelper.abort(this, isOpenable, cause, responsible, messageReceiver);

        if (doorBase.exceedSizeLimit(responsible))
            return doorOpeningHelper.abort(this, DoorToggleResult.TOO_BIG, cause, responsible, messageReceiver);

        final Optional<Cuboid> newCuboid = getPotentialNewCoordinates();

        if (newCuboid.isEmpty())
            return doorOpeningHelper.abort(this, DoorToggleResult.ERROR, cause, responsible, messageReceiver);

        final IDoorEventTogglePrepare prepareEvent =
            doorOpeningHelper.callTogglePrepareEvent(this, cause, actionType, responsible,
                                                     time, skipAnimation, newCuboid.get());

        if (prepareEvent.isCancelled())
            return doorOpeningHelper.abort(this, DoorToggleResult.CANCELLED, cause, responsible, messageReceiver);

        final @Nullable IPPlayer responsiblePlayer = cause.equals(DoorActionCause.PLAYER) ? responsible : null;
        if (!doorOpeningHelper.isLocationEmpty(newCuboid.get(), getCuboid(), responsiblePlayer, getWorld()))
            return doorOpeningHelper.abort(this, DoorToggleResult.OBSTRUCTED, cause, responsible, messageReceiver);

        if (!doorOpeningHelper.canBreakBlocksBetweenLocs(this, newCuboid.get(), responsible))
            return doorOpeningHelper.abort(this, DoorToggleResult.NO_PERMISSION, cause, responsible, messageReceiver);

        final CompletableFuture<Boolean> scheduled =
            doorOpeningHelper.registerBlockMover(doorBase, this, cause, time, skipAnimation,
                                                 newCuboid.get(), responsible, actionType);

        if (!scheduled.join())
            return DoorToggleResult.ERROR;

        final IDoorEventToggleStart toggleStartEvent =
            doorOpeningHelper.callToggleStartEvent(this, cause, actionType, responsible,
                                                   time, skipAnimation, newCuboid.get());

        return DoorToggleResult.SUCCESS;
    }

    /**
     * Handles a change in redstone current for this door's powerblock.
     *
     * @param newCurrent
     *     The new current of the powerblock.
     */
    @SuppressWarnings("unused")
    public final void onRedstoneChange(int newCurrent)
    {
        doorBase.onRedstoneChange(this, newCurrent);
    }

    /**
     * Synchronizes all data of this door with the database.
     *
     * @return True if the synchronization was successful.
     */
    public final synchronized CompletableFuture<Boolean> syncData()
    {
        try
        {
            return doorBase.syncData(serializer.serialize(this));
        }
        catch (Exception e)
        {
            logger.logThrowable(e, "Failed to sync data for door: " + getBasicInfo());
        }
        return CompletableFuture.completedFuture(false);
    }

    public synchronized String getBasicInfo()
    {
        return getDoorUID() + " (" + getPrimeOwner() + ") - " + getDoorType().getSimpleName() + ": " + getName();
    }

    @Override
    public synchronized String toString()
    {
        String ret = doorBase + "\n"
            + "Type-specific data:\n"
            + "type: " + getDoorType() + "\n";

        ret += serializer.toString(this);

        return ret;
    }

    @Override
    public Cuboid getCuboid()
    {
        return doorBase.getCuboid();
    }

    @Override
    public boolean isOpenable()
    {
        return doorBase.isOpenable();
    }

    @Override
    public boolean isCloseable()
    {
        return doorBase.isCloseable();
    }

    @Override
    public List<DoorOwner> getDoorOwners()
    {
        return doorBase.getDoorOwners();
    }

    @Override
    public Optional<DoorOwner> getDoorOwner(UUID uuid)
    {
        return doorBase.getDoorOwner(uuid);
    }

    @Override
    public boolean isDoorOwner(UUID player)
    {
        return doorBase.isDoorOwner(player);
    }

    @Override
    public void setCoordinates(Cuboid newCuboid)
    {
        doorBase.setCoordinates(newCuboid);
    }

    @Override
    public void setCoordinates(Vector3Di posA, Vector3Di posB)
    {
        doorBase.setCoordinates(posA, posB);
    }

    @Override
    public Vector3Di getMinimum()
    {
        return doorBase.getMinimum();
    }

    @Override
    public Vector3Di getMaximum()
    {
        return doorBase.getMaximum();
    }

    @Override
    public Vector3Di getDimensions()
    {
        return doorBase.getDimensions();
    }

    @Override
    public void setEngine(Vector3Di pos)
    {
        doorBase.setEngine(pos);
    }

    @Override
    public void setPowerBlockPosition(Vector3Di pos)
    {
        doorBase.setPowerBlockPosition(pos);
    }

    @Override
    public int getBlockCount()
    {
        return doorBase.getBlockCount();
    }

    @Override
    public long getSimplePowerBlockChunkHash()
    {
        return doorBase.getSimplePowerBlockChunkHash();
    }

    @Override
    public void setName(String name)
    {
        doorBase.setName(name);
    }

    @Override
    public void setOpen(boolean open)
    {
        doorBase.setOpen(open);
    }

    @Override
    public void setOpenDir(RotateDirection openDir)
    {
        doorBase.setOpenDir(openDir);
    }

    @Override
    public void setLocked(boolean locked)
    {
        doorBase.setLocked(locked);
    }

    @Override
    public long getDoorUID()
    {
        return doorBase.getDoorUID();
    }

    @Override
    public IPWorld getWorld()
    {
        return doorBase.getWorld();
    }

    @Override
    public Vector3Di getEngine()
    {
        return doorBase.getEngine();
    }

    @Override
    public Vector3Di getPowerBlock()
    {
        return doorBase.getPowerBlock();
    }

    @Override
    public String getName()
    {
        return doorBase.getName();
    }

    @Override
    public boolean isOpen()
    {
        return doorBase.isOpen();
    }

    @Override
    public RotateDirection getOpenDir()
    {
        return doorBase.getOpenDir();
    }

    @Override
    public boolean isLocked()
    {
        return doorBase.isLocked();
    }

    @Override
    public DoorOwner getPrimeOwner()
    {
        return doorBase.getPrimeOwner();
    }

    public class Registrable
    {
        private Registrable()
        {
        }

        /**
         * Gets the {@link DoorBase} that is associated with this {@link Registrable}.
         *
         * @return The {@link DoorBase} that is associated with this {@link Registrable}.
         */
        public AbstractDoor getAbstractDoorBase()
        {
            return AbstractDoor.this;
        }
    }
}
