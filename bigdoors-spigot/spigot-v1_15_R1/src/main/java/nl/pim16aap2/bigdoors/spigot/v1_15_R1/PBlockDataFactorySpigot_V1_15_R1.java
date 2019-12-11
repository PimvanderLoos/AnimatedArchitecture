package nl.pim16aap2.bigdoors.spigot.v1_15_R1;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.ICustomCraftFallingBlock;
import nl.pim16aap2.bigdoors.api.INMSBlock;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.api.factories.IPBlockDataFactory;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class PBlockDataFactorySpigot_V1_15_R1 implements IPBlockDataFactory
{
    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public Optional<PBlockData> create(final @NotNull IPLocation startLocation, final boolean bottom,
                                       final float radius, final float startAngle)
    {
        final World bukkitWorld = SpigotAdapter.getBukkitWorld(startLocation.getWorld());
        if (bukkitWorld == null)
            return Optional.empty();

        final Block vBlock = bukkitWorld.getBlockAt(startLocation.getBlockX(),
                                                    startLocation.getBlockY(),
                                                    startLocation.getBlockZ());

        if (!BlockAnalyzer_V1_15_R1.isAllowedBlockStatic(vBlock.getType()))
            return Optional.empty();

        final IPLocation newFBlockLocation = BigDoors.get().getPlatform().getPLocationFactory()
                                                     .create(startLocation.getWorld(),
                                                             startLocation.getBlockX() + 0.5,
                                                             startLocation.getBlockY() - 0.020,
                                                             startLocation.getBlockZ() + 0.5);

        // Move the lowest blocks up a little, so the client won't predict they're
        // touching through the ground, which would make them slower than the rest.
        if (bottom)
            newFBlockLocation.setY(newFBlockLocation.getY() + .010001);

        final INMSBlock nmsBlock = BigDoors.get().getPlatform().getFallingBlockFactory()
                                           .nmsBlockFactory(startLocation);
        final ICustomCraftFallingBlock fBlock = BigDoors.get().getPlatform().getFallingBlockFactory()
                                                        .fallingBlockFactory(newFBlockLocation, nmsBlock);

        boolean deferPlacement = BlockAnalyzer_V1_15_R1.placeOnSecondPassStatic(vBlock.getType());
        return Optional.of(new PBlockData(fBlock, radius, nmsBlock, startLocation, startAngle, deferPlacement));
    }
}
