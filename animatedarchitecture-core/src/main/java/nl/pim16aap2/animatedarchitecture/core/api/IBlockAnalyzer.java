package nl.pim16aap2.animatedarchitecture.core.api;


/**
 * Analyzes blocks to obtain various information.
 *
 * @author Pim
 */
public interface IBlockAnalyzer
{
    /**
     * Check if a block is air or liquid (water, lava).
     *
     * @param location
     *     The location of the block.
     * @return True if it is air or liquid.
     */
    boolean isAirOrLiquid(ILocation location);

    /**
     * Check if a block is on the blacklist of types/materials that is not allowed for animations.
     *
     * @param location
     *     The location of the block.
     * @return True if the block can be used for animations.
     */
    boolean isAllowedBlock(ILocation location);

    /**
     * Represents the status of a material.
     *
     * @author Pim
     */
    enum MaterialStatus
    {
        /**
         * The material is blacklisted and cannot in any way be used as an animated block.
         */
        BLACKLISTED,

        /**
         * This material is allowed.
         */
        WHITELISTED,

        /**
         * The material type was not mapped.
         */
        UNMAPPED
    }
}








