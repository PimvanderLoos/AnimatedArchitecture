package nl.pim16aap2.animatedarchitecture.core.api;

import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

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
    CompletableFuture<HookCheckResult> canBreakBlocksInCuboid(IPlayer player, Cuboid cuboid, IWorld world);

    /**
     * @return True if all checks for block-breaking access can be skipped. This may happen when no hooks are enabled.
     */
    boolean canSkipCheck();

    /**
     * Represents the result of a protection hook check.
     *
     * @param isDenied
     *     True if the check was denied.
     * @param denyingHookName
     *     The name of the hook that denied the check, or null if no name is available (e.g. when the check was allowed,
     *     or when multiple hooks denied the check).
     */
    record HookCheckResult(boolean isDenied, @Nullable String denyingHookName)
    {
        /**
         * Generic error result that denies the check.
         */
        public static HookCheckResult ERROR = new HookCheckResult(true, "ERROR");

        /**
         * Check if the check was allowed.
         *
         * @return True if the check was allowed.
         */
        public boolean isAllowed()
        {
            return !isDenied;
        }

        /**
         * Create a new CheckResult that indicates that the check was allowed.
         *
         * @return The new CheckResult.
         */
        public static HookCheckResult allowed()
        {
            return new HookCheckResult(false, null);
        }

        /**
         * Create a new CheckResult that indicates that the check was denied.
         *
         * @param denyingHookName
         *     The name of the hook that denied the check.
         * @return The new CheckResult.
         */
        public static HookCheckResult denied(@Nullable String denyingHookName)
        {
            return new HookCheckResult(true, denyingHookName);
        }

        /**
         * Create a new CheckResult that indicates that the check was denied.
         * <p>
         * This method should be used when the denying hook name is not available.
         * <p>
         * If the denying hook name is available, use {@link #denied(String)} instead.
         *
         * @return The new CheckResult.
         */
        public static HookCheckResult denied()
        {
            return denied(null);
        }
    }
}
