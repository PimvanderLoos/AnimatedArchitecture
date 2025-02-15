package nl.pim16aap2.animatedarchitecture.core.tooluser;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import lombok.Getter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.animation.StructureActivityManager;
import nl.pim16aap2.animatedarchitecture.core.annotations.Initializer;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitectureToolUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IEconomyManager;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.IProtectionHookManager;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.commands.CommandFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.managers.LimitsManager;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAnimationRequestBuilder;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureBuilder;
import nl.pim16aap2.animatedarchitecture.core.text.Text;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

/**
 * This class is responsible for guiding the player through a process using the tool. This process is defined by a
 * {@link Procedure} that is populated with {@link Step}s.
 * <p>
 * The procedure can be used to guide the player through the process of creating a structure, or any other process that
 * uses any kind of input.
 * <p>
 * The main way to interact with this class is the {@link #handleInput(Object)} method. This method takes the provided
 * input and applies it to the current step in the procedure. The expected input type and behavior depend on the
 * currently-active step.
 * <p>
 * The {@link #handleInput(Object)} method is thread-safe and will handle input in a synchronized manner. This means
 * that only one input can be processed at a time. If multiple inputs are provided, they will be processed in the order
 * they were received. The lock is not thread-specific, so subclasses should be careful around threading issues. If a
 * subclass needs to perform multiple operations that require the lock, it should use the {@link #runWithLock(Supplier)}
 * method.
 */
@Flogger
public abstract class ToolUser
{
    @Getter
    private final IPlayer player;

    /**
     * Lock used to ensure that only one input is processed at a time.
     * <p>
     * Should be accessed using {@link #acquireInputLock()} and {@link #releaseInputLock()}.
     */
    private final Semaphore inputLock = new Semaphore(1, true);

    protected final ILocalizer localizer;

    protected final ToolUserManager toolUserManager;

    protected final IProtectionHookManager protectionHookManager;

    protected final IAnimatedArchitectureToolUtil animatedArchitectureToolUtil;

    protected final ITextFactory textFactory;

    protected final Step.Factory.IFactory stepFactory;

    /**
     * The {@link Procedure} that this {@link ToolUser} will go through.
     */
    private volatile Procedure procedure;

    /**
     * Checks if this {@link ToolUser} has been shut down or not.
     */
    @GuardedBy("this")
    private boolean isShutDown = false;

    /**
     * Keeps track of whether this {@link ToolUser} is active or not.
     */
    @GuardedBy("this")
    private boolean active = true;

    /**
     * Keeps track of whether this {@link ToolUser} has been initialized or not.
     */
    private volatile boolean isInitialized = false;

    /**
     * Keeps track of whether the player has the tool or not.
     */
    @GuardedBy("this")
    private boolean playerHasTool = false;

    /**
     * Creates a new {@link ToolUser} for the given player.
     * <p>
     * Do not forget to call {@link #init()} after creating the object. Preferably, this should be done in the
     * constructor.
     *
     * @param context
     *     The context to use.
     * @param player
     *     The player to create the {@link ToolUser} for.
     */
    protected ToolUser(Context context, IPlayer player)
    {
        stepFactory = context.getStepFactory();
        this.player = player;
        localizer = context.getLocalizer();
        toolUserManager = context.getToolUserManager();
        protectionHookManager = context.getProtectionHookManager();
        animatedArchitectureToolUtil = context.getAnimatedArchitectureToolUtil();
        textFactory = context.getTextFactory();
    }

    /**
     * Basic initialization executed that should be called after the constructor.
     * <p>
     * This method will initialize the {@link Procedure} using the steps generated by {@link #generateSteps()} and
     * register this {@link ToolUser} with the {@link ToolUserManager}.
     * <p>
     * If this method is not called, the {@link ToolUser} will be considered uninitialized and any method that requires
     * the procedure (e.g. handling input) will throw an exception.
     * <p>
     * If this method is called more than once, an exception will be thrown.
     *
     * @throws IllegalStateException
     *     If this method is called more than once.
     * @throws RuntimeException
     *     If the procedure could not be instantiated.
     */
    @Initializer
    protected synchronized void init()
    {
        if (isInitialized)
            throw new IllegalStateException("ToolUser has already been initialized!");

        isInitialized = true;

        try
        {
            procedure = new Procedure(generateSteps(), localizer, textFactory);
        }
        catch (InstantiationException | IndexOutOfBoundsException e)
        {
            throw new RuntimeException(
                "Failed to instantiate procedure for ToolUser for player: " + getPlayer().asString(), e);
        }

        if (!active)
            return;

        toolUserManager.registerToolUser(this);
    }

    /**
     * Generates the list of {@link Step}s that together will make up the {@link #procedure}.
     *
     * @return The list of {@link Step}s that together will make up the {@link #procedure}.
     *
     * @throws InstantiationException
     *     When a step's factory is incomplete or otherwise invalid.
     */
    protected abstract List<Step> generateSteps()
        throws InstantiationException;

    /**
     * Takes care of the final part of the process. This unregisters this {@link ToolUser} and removes the tool from the
     * player's inventory (if they still have it).
     */
    protected final synchronized void cleanUpProcess()
    {
        assertInitialized();

        if (isShutDown)
            return;
        isShutDown = true;
        removeTool();
        active = false;
        toolUserManager.abortToolUser(this);
    }

    /**
     * Aborts the process this {@link ToolUser} is currently in.
     * <p>
     * By default, this method only calls {@link #cleanUpProcess()}. Subclasses can override this method to add
     * additional behavior.
     */
    public void abort()
    {
        cleanUpProcess();
    }

    /**
     * Adds the AnimatedArchitecture tool from the player's inventory.
     *
     * @param nameKey
     *     The localization key of the name of the tool.
     * @param loreKey
     *     The localization key of the lore of the tool.
     * @param text
     *     The message to send to the player after giving them the tool.
     */
    protected final synchronized void giveTool(String nameKey, String loreKey, @Nullable Text text)
    {
        animatedArchitectureToolUtil.giveToPlayer(
            getPlayer(), localizer.getMessage(nameKey), localizer.getMessage(loreKey));
        playerHasTool = true;

        if (text != null)
            getPlayer().sendMessage(text);
    }

    /**
     * Removes the AnimatedArchitecture tool from the player's inventory.
     */
    protected final synchronized void removeTool()
    {
        animatedArchitectureToolUtil.removeTool(getPlayer());
        playerHasTool = false;
    }

    /**
     * Gets the message for the current step.
     *
     * @return The message of the current step if possible. Otherwise, an empty String is returned.
     */
    @SuppressWarnings("unused")
    public Text getCurrentStepMessage()
    {
        assertInitialized();
        return procedure.getCurrentStepMessage();
    }

    /**
     * Prepares the next step. For example by sending the player some instructions about what they should do.
     * <p>
     * This should be used after proceeding to the next step.
     *
     * @return A {@link CompletableFuture} that will be completed when the current step is prepared.
     * <p>
     * If the step does not wait for user input, the returned future will be the result of
     * {@link #handleInputWithLock(Object)}. Otherwise, it will be completed with true.
     *
     * @throws IllegalStateException
     *     If the lock is not held. See {@link #acquireInputLock()}.
     */
    @CheckReturnValue
    protected CompletableFuture<Boolean> prepareCurrentStep()
    {
        assertInitialized();
        assertLockHeld();

        final var step = procedure.runCurrentStepPreparation();
        if (step == null)
            return CompletableFuture.failedFuture(new NoSuchElementException("Procedure has no active step!"));

        sendMessage(step);

        if (!step.waitForUserInput())
            return handleInputWithLock(null);
        return CompletableFuture.completedFuture(true);
    }

    /**
     * See {@link Procedure#skipToStep(Step)}.
     * <p>
     * After successfully skipping to the target step, the newly-selected step will be prepared. See
     * {@link #prepareCurrentStep()}.
     *
     * @param goalStep
     *     The step to skip to.
     * @return A {@link CompletableFuture} that will be completed an exception if the step could not be found, or the
     * result of {@link #prepareCurrentStep()} otherwise.
     *
     * @throws IllegalStateException
     *     If the lock is not held. See {@link #acquireInputLock()}.
     */
    @SuppressWarnings("unused")
    protected CompletableFuture<Boolean> skipToStep(Step goalStep)
    {
        assertInitialized();
        assertLockHeld();

        if (!procedure.skipToStep(goalStep))
            return CompletableFuture.failedFuture(new NoSuchElementException("Step '" + goalStep + "' not found!"));
        return prepareCurrentStep();
    }

    /**
     * Applies an input object to the current step in the {@link #procedure}.
     *
     * @param obj
     *     The object to apply.
     * @return The result of running the step executor on the provided input.
     *
     * @throws IllegalStateException
     *     If the lock is not held. See {@link #acquireInputLock()}.
     */
    @CheckReturnValue
    private CompletableFuture<Boolean> applyInput(@Nullable Object obj)
        throws IllegalStateException
    {
        assertInitialized();
        assertLockHeld();

        try
        {
            return procedure.applyStepExecutor(obj);
        }
        catch (IllegalArgumentException e)
        {
            getPlayer().sendMessage(textFactory, TextType.ERROR, localizer.getMessage("constants.error.generic"));
            return CompletableFuture.failedFuture(new RuntimeException(
                "Provided incompatible input '" + obj + "' for ToolUser '" + toMinimalString() + "'!", e));
        }
        catch (Exception e)
        {
            getPlayer().sendMessage(textFactory, TextType.ERROR, localizer.getMessage("constants.error.generic"));
            abort();
            return CompletableFuture.failedFuture(new RuntimeException(
                "Failed to apply input '" + obj + "' to ToolUser '" + toMinimalString() + "'!", e));
        }
    }

    /**
     * Handles user input for the given step.
     * <p>
     * This method assumes that the input lock is already held. If the caller does not hold the lock, they should use
     * either {@link #handleInput(Object)} or {@link #runWithLock(Supplier)}.
     * <p>
     * This method is intended for use by subclasses or step executors that need to provide input as part of their
     * operation and therefore already hold the lock.
     *
     * @param obj
     *     The input to handle. What actual type is expected depends on the step.
     * @return A completable future that will either complete with true if the step was completed successfully, or false
     * if it was not.
     *
     * @throws IllegalStateException
     *     If the lock is not held.
     */
    @CheckReturnValue
    protected final CompletableFuture<Boolean> handleInputWithLock(@Nullable Object obj)
    {
        assertInitialized();
        assertLockHeld();

        log.atFine().log(
            "Handling input: %s (%s) for step: %s in ToolUser: %s.",
            obj, (obj == null ? "null" : obj.getClass().getSimpleName()),
            procedure.getCurrentStepName(), this);

        if (!isActive())
            return CompletableFuture.failedFuture(new IllegalStateException(
                "Cannot handle input '" + obj + "' for ToolUser '" + this + "' because it is not active!"));

        final boolean isLastStep = !procedure.hasNextStep();

        return applyInput(obj)
            .thenCompose(inputSuccess ->
            {
                if (!inputSuccess)
                    return CompletableFuture.completedFuture(false);

                if (isLastStep)
                {
                    cleanUpProcess();
                    return CompletableFuture.completedFuture(true);
                }

                procedure.handleStepCompletion();

                return prepareCurrentStep().thenApply(ignored -> true);
            })
            .exceptionally(ex ->
            {
                throw new RuntimeException(
                    "An error occurred applying input '" + obj + "' for ToolUser '" + this + "'!", ex);
            });
    }

    /**
     * Handles some kind of input for the given step.
     *
     * @param obj
     *     The input to handle. What actual type is expected depends on the step.
     * @return True if the input was processed successfully.
     */
    public final CompletableFuture<Boolean> handleInput(@Nullable Object obj)
    {
        assertInitialized();

        try
        {
            return runWithLock(() -> handleInputWithLock(obj))
                .exceptionally(ex ->
                {
                    throw new RuntimeException(
                        "An error occurred handling input '" + obj + "' for ToolUser '" + toMinimalString() + "'!", ex);
                });
        }
        catch (Exception e)
        {
            return CompletableFuture.failedFuture(new RuntimeException(
                "Failed to handle input '" + obj + "' for ToolUser '" + toMinimalString() + "'!", e));
        }
    }

    /**
     * Runs the provided supplier while holding the input lock.
     * <p>
     * This method will acquire the input lock, run the supplier, and then release the input lock.
     * <p>
     * If the supplier throws an exception, the lock will still be released.
     *
     * @param supplier
     *     The supplier to run.
     * @param <T>
     *     The type of the result of the supplier.
     * @return The result of the supplier.
     */
    protected final <T> CompletableFuture<T> runWithLock(Supplier<CompletableFuture<T>> supplier)
    {
        assertInitialized();
        acquireInputLock();
        try
        {
            return supplier.get().whenComplete((ignored, ex) -> releaseInputLock());
        }
        catch (Exception e)
        {
            releaseInputLock();
            throw new RuntimeException("Failed to run supplier with lock!", e);
        }
    }

    /**
     * Sends the localized message of the given step to the player that owns this object.
     *
     * @param step
     *     The step to send the message for.
     */
    protected void sendMessage(@Nullable Step step)
    {
        assertInitialized();
        final var message = procedure.getMessage(step);
        if (message.isEmpty())
            log.atWarning().log("Missing translation for step: %s", procedure.getStepName(step));
        else
            getPlayer().sendMessage(message);
    }

    /**
     * Sends the localized message of the current step to the player that owns this object.
     */
    protected void sendMessage()
    {
        assertInitialized();
        sendMessage(procedure.getCurrentStep());
    }

    /**
     * Gets the {@link Step} in the {@link #procedure} this {@link ToolUser} is currently at.
     *
     * @return The current {@link Step} in the {@link #procedure}.
     */
    public final Optional<Step> getCurrentStep()
    {
        assertInitialized();
        return Optional.ofNullable(procedure.getCurrentStep());
    }

    /**
     * Checks if a player is allowed to break the block in a given location.
     * <p>
     * If the player is not allowed to break blocks in the location, a message will be sent to them (provided the name
     * of the compat isn't empty).
     *
     * @param loc
     *     The location to check.
     * @return True if the player is allowed to break the block at the given location.
     */
    public CompletableFuture<Boolean> playerHasAccessToLocation(ILocation loc)
    {
        return protectionHookManager
            .canBreakBlock(getPlayer(), loc)
            .thenApply(result ->
            {
                if (result.isAllowed())
                    return true;

                log.atFine().log(
                    "Blocked access to location %s for player %s! Reason: %s",
                    loc, getPlayer(), result.denyingHookName());
                getPlayer().sendMessage(
                    textFactory,
                    TextType.ERROR,
                    localizer.getMessage("tool_user.base.error.no_permission_for_location")
                );
                return false;
            });
    }

    /**
     * Checks if a player is allowed to break all blocks in a given cuboid.
     * <p>
     * If the player is not allowed to break one or more blocks in the cuboid, a message will be sent to them. (provided
     * the name of the compat isn't empty).
     *
     * @param cuboid
     *     The cuboid to check.
     * @param world
     *     The world to check in.
     * @return The cuboid if the player is allowed to break all blocks in it, or an empty Optional otherwise.
     */
    public CompletableFuture<Optional<Cuboid>> playerHasAccessToCuboid(Cuboid cuboid, IWorld world)
    {
        return protectionHookManager
            .canBreakBlocksInCuboid(getPlayer(), cuboid, world)
            .thenApply(result ->
            {
                if (result.isAllowed())
                    return Optional.of(cuboid);

                log.atFine().log(
                    "Blocked access to cuboid %s for player %s in world %s! Reason: %s",
                    cuboid,
                    getPlayer(),
                    world,
                    result.denyingHookName()
                );
                getPlayer().sendMessage(
                    textFactory,
                    TextType.ERROR,
                    localizer.getMessage("tool_user.base.error.no_permission_for_location")
                );
                return Optional.empty();
            });
    }

    /**
     * Keeps track of whether this {@link ToolUser} is active or not.
     *
     * @return true if this tool user is active.
     */
    public final synchronized boolean isActive()
    {
        return active;
    }

    /**
     * Sets the playerHasTool field.
     *
     * @param playerHasTool
     *     The new value.
     * @return The old value.
     */
    protected final synchronized boolean setPlayerHasTool(boolean playerHasTool)
    {
        final boolean old = this.playerHasTool;
        this.playerHasTool = playerHasTool;
        return old;
    }

    /**
     * Accessor for {@link #playerHasTool}.
     *
     * @return The value of {@link #playerHasTool}.
     */
    public final synchronized boolean playerHasTool()
    {
        return playerHasTool;
    }

    /**
     * Accessor for {@link Procedure#getAllSteps()}.
     *
     * @return All the steps in the procedure including any that may have been completed/skipped already.
     */
    protected final List<Step> getAllSteps()
    {
        assertInitialized();
        return procedure.getAllSteps();
    }

    /**
     * Proceeds to the next step in the procedure.
     * <p>
     * This method assumes that the lock is held. See {@link #acquireInputLock()}.
     * <p>
     * This method is a wrapper around {@link Procedure#goToNextStep()}.
     *
     * @throws IllegalStateException
     *     If the lock is not held. See {@link #acquireInputLock()}.
     */
    protected final void goToNextStep()
    {
        assertInitialized();
        assertLockHeld();
        procedure.goToNextStep();
    }

    /**
     * Inserts an existing, named step at the current position.
     * <p>
     * This method assumes that the lock is held. See {@link #acquireInputLock()}.
     * <p>
     * This method is a wrapper around {@link Procedure#insertStep(String)}.
     *
     * @param name
     *     The name of the step to insert.
     * @throws NoSuchElementException
     *     If no step can be found by that name.
     * @throws IllegalStateException
     *     If the lock is not held. See {@link #acquireInputLock()}.
     */
    protected final void insertStep(String name)
    {
        assertInitialized();
        assertLockHeld();
        procedure.insertStep(name);
    }

    /**
     * Ensures that the input lock is held.
     *
     * @throws IllegalStateException
     *     If the lock is not held.
     */
    protected final void assertLockHeld()
    {
        if (inputLock.availablePermits() == 1)
            throw new IllegalStateException("Failed to assert that the input lock is held!");
    }

    /**
     * Acquires the input lock.
     * <p>
     * If the lock is not currently held, the thread will wait until it is available.
     * <p>
     * Once the lock is acquired, it will be held until {@link #releaseInputLock()} is called.
     * <p>
     * The lock has no notion of ownership, so it is not reentrant and, once acquired, it can be released by any
     * thread.
     */
    private void acquireInputLock()
    {
        try
        {
            inputLock.acquire();
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(
                "Thread was interrupted waiting for ToolUser input semaphore to become available!", e);
        }
    }

    /**
     * Releases the input lock.
     * <p>
     * It does not matter which thread acquired the lock in the first place, any thread can release it.
     *
     * @throws IllegalStateException
     *     if the lock is not currently held.
     */
    private void releaseInputLock()
    {
        if (inputLock.availablePermits() != 0)
            throw new IllegalStateException("Failed to release the input lock!");
        inputLock.release();
    }

    /**
     * Checks if the {@link ToolUser} has been initialized.
     * <p>
     * Subclasses have to explicitly initialize the {@link ToolUser} by calling {@link #init()}. This method can be used
     * to catch cases where the {@link ToolUser} has not been initialized yet.
     *
     * @throws IllegalStateException
     *     if the {@link ToolUser} has not been initialized yet.
     */
    private void assertInitialized()
    {
        if (!isInitialized)
            throw new IllegalStateException("ToolUser has not been initialized yet!");
    }

    /**
     * Returns a minimal string representation of this object.
     * <p>
     * This method is intended for logging purposes, as the full string representation may be too verbose.
     *
     * @return A minimal string representation of this object.
     */
    public synchronized String toMinimalString()
    {
        return "ToolUser [" + getClass().getSimpleName() + "] {" +
            "player=" + player +
            ", currentStep=" + procedure.getCurrentStep() +
            '}';
    }

    @Override
    public synchronized String toString()
    {
        return "ToolUser(player=" + this.player +
            ", isShutDown=" + this.isShutDown +
            ", active=" + this.active +
            ", playerHasTool=" + this.playerHasTool +
            ", inputLock=" + inputLock +
            ", procedure=" + this.procedure +
            ")";
    }

    @Getter
    public static final class Context
    {
        private final StructureBuilder structureBuilder;
        private final ILocalizer localizer;
        private final ITextFactory textFactory;
        private final ToolUserManager toolUserManager;
        private final DatabaseManager databaseManager;
        private final LimitsManager limitsManager;
        private final IEconomyManager economyManager;
        private final IProtectionHookManager protectionHookManager;
        private final IAnimatedArchitectureToolUtil animatedArchitectureToolUtil;
        private final CommandFactory commandFactory;
        private final @Nullable StructureAnimationRequestBuilder structureAnimationRequestBuilder;
        private final StructureActivityManager structureActivityManager;
        private final Step.Factory.IFactory stepFactory;

        @Inject
        public Context(
            StructureBuilder structureBuilder,
            ILocalizer localizer,
            ITextFactory textFactory,
            ToolUserManager toolUserManager,
            DatabaseManager databaseManager,
            LimitsManager limitsManager,
            IEconomyManager economyManager,
            IProtectionHookManager protectionHookManager,
            IAnimatedArchitectureToolUtil animatedArchitectureToolUtil,
            @Nullable StructureAnimationRequestBuilder structureAnimationRequestBuilder,
            StructureActivityManager structureActivityManager,
            CommandFactory commandFactory,
            Step.Factory.IFactory stepFactory)
        {
            this.structureBuilder = structureBuilder;
            this.localizer = localizer;
            this.toolUserManager = toolUserManager;
            this.databaseManager = databaseManager;
            this.limitsManager = limitsManager;
            this.economyManager = economyManager;
            this.protectionHookManager = protectionHookManager;
            this.animatedArchitectureToolUtil = animatedArchitectureToolUtil;
            this.structureAnimationRequestBuilder = structureAnimationRequestBuilder;
            this.structureActivityManager = structureActivityManager;
            this.commandFactory = commandFactory;
            this.textFactory = textFactory;
            this.stepFactory = stepFactory;
        }
    }
}
