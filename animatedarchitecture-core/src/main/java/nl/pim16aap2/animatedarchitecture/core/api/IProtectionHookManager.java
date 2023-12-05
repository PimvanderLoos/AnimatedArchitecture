package nl.pim16aap2.animatedarchitecture.core.api;

import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Class that manages all objects of IProtectionCompat.
 *
 * @author Pim
 */
public interface IProtectionHookManager
{
    /**
     * Check if a player can break a block at a given location.
     *
     * @param player
     *     The {@link IPlayer}.
     * @param loc
     *     The {@link ILocation} to check.
     * @return The name of the IProtectionCompat that objects, if any, or an empty Optional if allowed by all compats.
     */
    CompletableFuture<HookCheckResult> canBreakBlock(IPlayer player, ILocation loc);

    /**
     * Check if a player can break all blocks in a cuboid.
     *
     * @param player
     *     The {@link IPlayer}.
     * @param cuboid
     *     The {@link Cuboid} to check.
     * @param world
     *     The world.
     * @return The name of the IProtectionCompat that objects, if any, or an empty Optional if allowed by all compats.
     */
    CompletableFuture<HookCheckResult> canBreakBlocksBetweenLocs(IPlayer player, Cuboid cuboid, IWorld world);

    /**
     * @return True if all checks for block-breaking access can be skipped. This may happen when no hooks are enabled.
     */
    boolean canSkipCheck();

    /**
     * Represents the result of a protection hook check.
     *
     * @param denyingHookName
     *     The name of the hook that denied the check, or null if no hook denied the check.
     */
    record HookCheckResult(@Nullable String denyingHookName)
    {
        /**
         * Check if the check was denied.
         *
         * @return True if the check was denied.
         */
        public boolean isDenied()
        {
            return denyingHookName != null;
        }

        /**
         * Check if the check was allowed.
         *
         * @return True if the check was allowed.
         */
        public boolean isAllowed()
        {
            return denyingHookName == null;
        }

        /**
         * Create a new CheckResult that indicates that the check was allowed.
         *
         * @return The new CheckResult.
         */
        public static HookCheckResult allowed()
        {
            return new HookCheckResult(null);
        }

        /**
         * Create a new CheckResult that indicates that the check was denied.
         *
         * @param denyingHookName
         *     The name of the hook that denied the check.
         * @return The new CheckResult.
         */
        public static HookCheckResult denied(String denyingHookName)
        {
            return new HookCheckResult(denyingHookName);
        }

        /**
         * Run an action if the check was denied.
         *
         * @param action
         *     The action to run. The name of the hook that denied the check is passed as an argument.
         */
        public void ifDenied(Consumer<String> action)
        {
            if (isDenied())
                action.accept(denyingHookName);
        }
    }
}
