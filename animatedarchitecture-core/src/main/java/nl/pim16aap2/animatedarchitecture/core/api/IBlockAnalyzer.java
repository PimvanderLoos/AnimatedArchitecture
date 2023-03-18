package nl.pim16aap2.animatedarchitecture.core.api;


/**
 * Analyzes blocks to obtain various information.
 *
 * @param <T>
 *     The type of block/material this class can analyze.
 */
public interface IBlockAnalyzer<T>
{
    /**
     * Check if a block is air or liquid (water, lava).
     *
     * @param block
     *     The block to analyze.
     * @return True if the block is air or liquid.
     */
    boolean isAirOrLiquid(T block);

    /**
     * Check if a block at a given location is air or liquid (water, lava).
     *
     * @param location
     *     The location of the block to analyze.
     * @return True if the block is air or liquid.
     */
    boolean isAirOrLiquid(ILocation location);

    /**
     * Check if a block is on the blacklist of block that is not allowed for animations.
     *
     * @param block
     *     The block to analyze.
     * @return True if the block can be used for animations.
     */
    boolean isAllowed(T block);

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








