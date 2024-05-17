package nl.pim16aap2.animatedarchitecture.spigot.core.hooks;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IProtectionHookManager.HookCheckResult;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.HookPreCheckResult;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.IProtectionHookSpigot;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;


/**
 * Represents a group of {@link HookCheckState}s.
 * <p>
 * This class should be instantiated using {@link HookCheckStateContainer#of(List)}.
 */
@Accessors(fluent = true, chain = true)
@Flogger //
final class HookCheckStateContainer
{
    private final List<HookCheckState> hookCheckStates;

    /**
     * Keeps track of how many hooks bypassed the check.
     */
    private final AtomicInteger bypassedCount = new AtomicInteger(0);

    /**
     * Whether at least one hook denied the check.
     */
    @Getter(value = AccessLevel.PRIVATE)
    @Setter(value = AccessLevel.PRIVATE)
    private volatile boolean isDenied = false;

    @Getter(value = AccessLevel.PRIVATE)
    private volatile @Nullable String denyingHookName = null;

    private HookCheckStateContainer(List<HookCheckState> hookCheckStates)
    {
        this.hookCheckStates = hookCheckStates;
    }

    /**
     * Create a new {@link HookCheckStateContainer} from a list of {@link IProtectionHookSpigot}s.
     *
     * @param hooks
     *     The hooks to create the results from.
     * @return The created {@link HookCheckStateContainer}.
     */
    static HookCheckStateContainer of(List<IProtectionHookSpigot> hooks)
    {
        return new HookCheckStateContainer(hooks.stream().map(HookCheckState::new).toList());
    }

    /**
     * Check if all hooks are bypassed.
     *
     * @return True if all hooks are bypassed.
     */
    public boolean allBypassed()
    {
        return bypassedCount.get() == hookCheckStates.size();
    }

    /**
     * Process the result of a pre-check.
     * <p>
     * If the result is {@link HookPreCheckResult#BYPASS}, the bypassed count is incremented.
     * <p>
     * If the result is {@link HookPreCheckResult#DENY}, the check is denied.
     *
     * @param result
     *     The result to process.
     * @param hookCheckState
     *     The hook check state that produced the result.
     */
    private void processPreCheckResult(
        HookPreCheckResult result,
        @Nullable HookCheckState hookCheckState)
    {
        log.atFinest().log("Processing pre-check result: %s", result);

        if (result == HookPreCheckResult.BYPASS)
            bypassedCount.incrementAndGet();

        if (result == HookPreCheckResult.DENY)
        {
            isDenied(true);
            if (hookCheckState != null)
                denyingHookName = hookCheckState.hookName();
        }
    }

    /**
     * Process the result of a pre-check.
     * <p>
     * If the {@link HookCheckState} that produced the result is available, use
     * {@link #processPreCheckResult(HookPreCheckResult, HookCheckState)} instead.
     *
     * @param result
     *     The result to process.
     */
    private void processPreCheckResult(HookPreCheckResult result)
    {
        processPreCheckResult(result, null);
    }

    /**
     * Run the pre-checks for all hooks synchronously.
     *
     * @param executor
     *     The executor to use for scheduling tasks.
     * @param player
     *     The player to check.
     * @param world
     *     The world to check in.
     */
    private void runPreChecksSync(IExecutor executor, Player player, World world)
    {
        if (allBypassed() || isDenied())
        {
            log.atFiner().log("All hooks are bypassed or at least one hook denied the check. Skipping sync checks.");
            return;
        }

        for (HookCheckState hookCheckState : hookCheckStates)
        {
            if (hookCheckState.isBypassed())
                continue;

            final var result = hookCheckState.preCheck(executor, player, world);
            processPreCheckResult(result, hookCheckState);

            if (result == HookPreCheckResult.DENY)
                break;
        }
    }

    /**
     * Run the pre-checks for all hooks asynchronously.
     *
     * @param executor
     *     The executor to use for scheduling tasks.
     * @param player
     *     The player to check.
     * @param world
     *     The world to check in.
     * @return A {@link CompletableFuture} that completes when all checks are done.
     */
    private CompletableFuture<Void> runPreChecksAsync(IExecutor executor, Player player, World world)
    {
        if (allBypassed() || isDenied())
        {
            log.atFiner().log(
                "All hooks are bypassed or at least one hook denied the check. Skipping async checks.");
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.allOf(
            hookCheckStates.stream()
                .map(hookCheckState -> hookCheckState
                    .preCheckAsync(executor, player, world)
                    .exceptionally(e ->
                    {
                        log.atSevere().withCause(e).log(
                            "An exception occurred while running async pre-check for hook '%s'.",
                            hookCheckState.hookName()
                        );
                        this.denyingHookName = hookCheckState.hookErrorName();
                        return HookPreCheckResult.DENY;
                    })
                    .thenAccept(this::processPreCheckResult))
                .toArray(CompletableFuture[]::new));
    }

    /**
     * Run the pre-checks for all hooks.
     *
     * @param executor
     *     The executor to use for scheduling tasks.
     * @param player
     *     The player to check.
     * @param world
     *     The world to check in.
     * @return A {@link CompletableFuture} that completes when all checks are done.
     */
    private CompletableFuture<Void> runPreChecks(IExecutor executor, Player player, World world)
    {
        final CompletableFuture<Void> results;
        if (executor.isMainThread())
        {
            runPreChecksSync(executor, player, world);
            results = CompletableFuture.runAsync(() -> runPreChecksSync(executor, player, world));
        }
        else
        {
            results =
                runPreChecksAsync(executor, player, world)
                    .thenRun(() ->
                        executor.runOnMainThread(() ->
                            runPreChecksSync(executor, player, world)));
        }
        return results.thenRun(() ->
            log.atFinest().log("Result of pre-checks for player %s in world '%s': %s", player, world.getName(), this));
    }

    /**
     * Run the main check for all hooks.
     * <p>
     * Each check is run sequentially. If a check denies the action, the remaining checks are skipped.
     * <p>
     * If {@link #isDenied()} is true, the checks are skipped and the result is a denied state.
     * <p>
     * If {@link #allBypassed()} is true, the checks are skipped and the result is an allowed state.
     *
     * @param executor
     *     The executor to use for scheduling tasks.
     * @param function
     *     The function to run for each hook. This is intended to be either
     *     {@link IProtectionHookSpigot#canBreakBlock(Player, Location)} or
     *     {@link IProtectionHookSpigot#canBreakBlocksInCuboid(Player, World, Cuboid)}.
     * @return The result of the checks. This is a {@link CompletableFuture} that completes when all checks are done.
     *
     * @throws IllegalStateException
     *     If the method is called on the wrong thread.
     */
    private CompletableFuture<HookCheckResult> runMainChecks(
        IExecutor executor,
        Function<IProtectionHookSpigot, CompletableFuture<Boolean>> function)
    {
        executor.assertMainThread();

        if (isDenied())
        {
            log.atFine().log("At least one hook denied the check. Skipping main checks.");
            return CompletableFuture.completedFuture(HookCheckResult.denied(denyingHookName()));
        }
        else if (allBypassed())
        {
            log.atFine().log("All hooks are bypassed. Skipping main checks.");
            return CompletableFuture.completedFuture(HookCheckResult.allowed());
        }

        var result = CompletableFuture.completedFuture(HookCheckResult.allowed());

        for (HookCheckState hookCheckState : hookCheckStates)
        {
            if (hookCheckState.isBypassed())
            {
                log.atFinest().log("Hook '%s' is bypassed. Skipping check...", hookCheckState.hook().getName());
                continue;
            }

            // Compose the result with the next check.
            // This way, we run the checks sequentially and stop when a check denies the action.
            result = result.thenCompose(previousResult ->
            {
                log.atFinest().log("Checking hook %s", hookCheckState.hook().getName());

                // Propagate the previous result if it was denied. We only need a single hook to deny the check.
                if (previousResult.isDenied())
                {
                    log.atFiner().log(
                        "Not checking hook %s because it was already denied by hook %s",
                        hookCheckState.hook().getName(), previousResult.denyingHookName()
                    );
                    return CompletableFuture.completedFuture(previousResult);
                }

                return hookCheckState.check(function);
            });
        }
        return result
            .exceptionally(e ->
            {
                log.atSevere().withCause(e).log("An exception occurred while running main checks.");
                return HookCheckResult.ERROR;
            });
    }

    /**
     * Implementation of {@link #runAllChecks(IExecutor, Player, World, Function)}.
     */
    private CompletableFuture<HookCheckResult> runAllChecks0(
        IExecutor executor,
        Player player,
        World world,
        Function<IProtectionHookSpigot, CompletableFuture<Boolean>> function)
    {
        return runPreChecks(executor, player, world)
            .thenCompose(container ->
                executor.composeOnMainThread(() ->
                    runMainChecks(executor, function)))
            .exceptionally(e ->
            {
                log.atSevere().withCause(e).log("An exception occurred while running all checks.");
                return HookCheckResult.denied("Unknown Error");
            })
            .thenApply(result ->
            {
                if (result.isDenied())
                    isDenied(true);
                log.atFinest().log(
                    "Result of all checks for player %s in world '%s': %s",
                    player, world.getName(), this
                );
                return result;
            });
    }

    /**
     * Run all checks for all hooks.
     * <p>
     * This method runs the pre-checks first and then runs the checks for all hooks.
     * <p>
     * If the pre-checks result in a bypass or a denial, the full checks are skipped.
     *
     * @param executor
     *     The executor to use for scheduling tasks.
     * @param player
     *     The player to check.
     * @param world
     *     The world to check in.
     * @param function
     *     The function to run for each hook. This is intended to be either
     *     {@link IProtectionHookSpigot#canBreakBlock(Player, Location)} or
     *     {@link IProtectionHookSpigot#canBreakBlocksInCuboid(Player, World, Cuboid)}.
     * @return The result of the checks. This is a {@link CompletableFuture} that completes when all checks are done.
     */
    public CompletableFuture<HookCheckResult> runAllChecks(
        IExecutor executor,
        Player player,
        World world,
        Function<IProtectionHookSpigot, CompletableFuture<Boolean>> function)
    {
        try
        {
            return runAllChecks0(executor, player, world, function);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("An exception occurred while running all checks.");
            return CompletableFuture.completedFuture(HookCheckResult.ERROR);
        }
    }

    /**
     * Returns a string representation of the object.
     * <p>
     * This method uses no synchronization, so the result may not reflect the current state of the object.
     *
     * @return
     */
    @Override
    public String toString()
    {
        return "HookCheckResults{" +
            "isDenied=" + isDenied +
            ", bypassedCount=" + bypassedCount +
            ", hookCheckStates=" + hookCheckStates +
            '}';
    }

    /**
     * Represents the state of a hook check.
     */
    @Flogger
    @Accessors(fluent = true) //
    private static final class HookCheckState
    {
        @Getter
        private final IProtectionHookSpigot hook;
        private volatile HookPreCheckResult result = HookPreCheckResult.ALLOW;

        private HookCheckState(IProtectionHookSpigot hook)
        {
            this.hook = hook;
        }

        private CompletableFuture<HookCheckResult> check(
            Function<IProtectionHookSpigot, CompletableFuture<Boolean>> predicate)
        {
            if (result != HookPreCheckResult.ALLOW)
            {
                log.atFiner().log(
                    "Hook '%s' is already in the state '%s'. Skipping check...",
                    hookName(), result
                );

                return CompletableFuture.completedFuture(HookCheckResult.allowed());
            }

            return predicate.apply(hook)
                .thenApply(allowed ->
                {
                    log.atFinest().log("Hook '%s' main check result: %s", hookName(), allowed);
                    if (!allowed)
                    {
                        this.result = HookPreCheckResult.DENY;
                        log.atFine().log("Hook '%s' denied the check.", hookName());
                        return HookCheckResult.denied(hookName());
                    }
                    return HookCheckResult.allowed();
                })
                .exceptionally(e ->
                {
                    log.atSevere().withCause(e).log(
                        "An exception occurred while running check for hook '%s'.",
                        hookName()
                    );
                    this.result = HookPreCheckResult.DENY;
                    return HookCheckResult.denied(hookErrorName());
                });
        }

        /**
         * Get the name of the hook.
         *
         * @return The name of the hook.
         */
        String hookName()
        {
            return hook.getName();
        }

        /**
         * Get the name of the hook with an error prefix.
         *
         * @return The name of the hook with an error prefix.
         */
        String hookErrorName()
        {
            return "ERROR: " + hookName();
        }

        /**
         * Process the result of a pre-check and updates the state accordingly.
         *
         * @param result
         *     The result to process.
         * @return The result.
         */
        private HookPreCheckResult processPreCheckResult(HookPreCheckResult result)
        {
            if (result == HookPreCheckResult.BYPASS)
            {
                log.atFiner().log("Hook '%s' bypassed the check.", hookName());
                this.result = HookPreCheckResult.BYPASS;
            }
            else if (result == HookPreCheckResult.DENY)
            {
                log.atFine().log("Hook '%s' denied the check.", hookName());
                this.result = HookPreCheckResult.DENY;
            }
            else
                log.atFiner().log("Hook '%s' pre-check result: %s", hookName(), result);
            return result;
        }

        /**
         * Check if the check is bypassed.
         *
         * @return True if the check is bypassed.
         */
        private boolean isBypassed()
        {
            return result == HookPreCheckResult.BYPASS;
        }

        /**
         * Run the pre-check for the hook and updates the state accordingly.
         * <p>
         * If the current state is anything other than {@link HookPreCheckResult#ALLOW}, the check is skipped and
         * {@link HookPreCheckResult#ALLOW} is returned because the check is skipped and
         * {@link HookPreCheckResult#ALLOW} is the most neutral result.
         * <p>
         * See {@link IProtectionHookSpigot#preCheck(Player, World)}.
         *
         * @param player
         *     The player to check.
         * @param world
         *     The world to check in.
         * @return The result of the pre-check. If the state is bypassed, {@link HookPreCheckResult#ALLOW} is returned.
         *
         * @throws IllegalStateException
         *     If the method is called on the wrong thread.
         */
        private HookPreCheckResult preCheck(IExecutor executor, Player player, World world)
        {
            executor.assertMainThread();

            if (this.result != HookPreCheckResult.ALLOW)
            {
                log.atFiner().log(
                    "Hook '%s' is already in the state '%s'. Skipping sync pre-check...",
                    hookName(), this.result);

                return HookPreCheckResult.ALLOW;
            }

            HookPreCheckResult result;
            try
            {
                result = hook.preCheck(player, world);
            }
            catch (Exception e)
            {
                log.atSevere().withCause(e)
                    .log("An exception occurred while running pre-check for hook '%s'.", hookName());
                result = HookPreCheckResult.DENY;
            }

            return processPreCheckResult(result);
        }

        /**
         * Run the pre-check for the hook asynchronously and updates the state accordingly.
         * <p>
         * If the current state is anything other than {@link HookPreCheckResult#ALLOW}, the check is skipped and the
         * future completes with {@link HookPreCheckResult#ALLOW} because the check is skipped and
         * {@link HookPreCheckResult#ALLOW} is the most neutral result.
         * <p>
         * See {@link IProtectionHookSpigot#preCheckAsync(Player, World)}.
         *
         * @param executor
         *     The executor to use for scheduling tasks.
         * @param player
         *     The player to check.
         * @param world
         *     The world to check in.
         * @return A {@link CompletableFuture} that completes when the check is done. If the state is bypassed, the
         * future * completes with {@link HookPreCheckResult#ALLOW}.
         */
        private CompletableFuture<HookPreCheckResult> preCheckAsync(IExecutor executor, Player player, World world)
        {
            executor.assertNotMainThread();

            if (this.result != HookPreCheckResult.ALLOW)
            {
                log.atFiner().log(
                    "Hook '%s' is already in the state '%s'. Skipping async pre-check...",
                    hookName(), this.result);

                return CompletableFuture.completedFuture(HookPreCheckResult.ALLOW);
            }

            return hook
                .preCheckAsync(player, world)
                .exceptionally(e ->
                {
                    log.atSevere().withCause(e).log(
                        "An exception occurred while running async pre-check for hook '%s'.", hookName());
                    return HookPreCheckResult.DENY;
                })
                .thenApply(this::processPreCheckResult);
        }

        @Override
        public String toString()
        {
            return "HookCheckState{" +
                "hook='" + hookName() +
                "', result: " + result +
                '}';
        }
    }
}
