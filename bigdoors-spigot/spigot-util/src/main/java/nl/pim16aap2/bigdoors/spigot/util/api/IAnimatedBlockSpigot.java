package nl.pim16aap2.bigdoors.spigot.util.api;

import nl.pim16aap2.bigdoors.core.api.animatedblock.IAnimatedBlock;
import org.bukkit.Material;
import org.bukkit.World;

/**
 * Represents an {@link IAnimatedBlock} with some additional QoL methods for the Spigot platform.
 *
 * @author Pim
 */
public interface IAnimatedBlockSpigot extends IAnimatedBlock
{
    /**
     * Gets the bukkit world this animated block exists in.
     *
     * @return The world this animated block exists in.
     */
    World getBukkitWorld();

    /**
     * Gets the material this block is made of.
     *
     * @return The material this block is made of.
     */
    Material getMaterial();
}
