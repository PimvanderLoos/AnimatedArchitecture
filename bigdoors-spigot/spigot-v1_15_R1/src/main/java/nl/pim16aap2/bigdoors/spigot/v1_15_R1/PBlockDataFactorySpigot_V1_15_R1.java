package nl.pim16aap2.bigdoors.spigot.v1_15_R1;

import nl.pim16aap2.bigdoors.api.INMSBlock;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.api.IAnimatedBlock;
import nl.pim16aap2.bigdoors.api.factories.IFallingBlockFactory;
import nl.pim16aap2.bigdoors.api.factories.IPBlockDataFactory;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

import javax.inject.Singleton;
import java.util.Optional;

@Singleton
public class PBlockDataFactorySpigot_V1_15_R1 implements IPBlockDataFactory
{
    private final IPLocationFactory locationFactory;
    private final IFallingBlockFactory fallingBlockFactory;

    public PBlockDataFactorySpigot_V1_15_R1(IPLocationFactory locationFactory, IFallingBlockFactory fallingBlockFactory)
    {
        this.locationFactory = locationFactory;
        this.fallingBlockFactory = fallingBlockFactory;
    }

    @Override
    public Optional<PBlockData> create(
        IPLocation startLocation, boolean bottom, float radius, float startAngle)
        throws Exception
    {
        final @Nullable World bukkitWorld = SpigotAdapter.getBukkitWorld(startLocation.getWorld());
        if (bukkitWorld == null)
            return Optional.empty();

        final Block vBlock = bukkitWorld.getBlockAt(startLocation.getBlockX(),
                                                    startLocation.getBlockY(),
                                                    startLocation.getBlockZ());

        if (!BlockAnalyzer_V1_15_R1.isAllowedBlockStatic(vBlock.getType()))
            return Optional.empty();

        final IPLocation newFBlockLocation = getSpawnLocation(locationFactory, startLocation, bottom);

        final INMSBlock nmsBlock = fallingBlockFactory.nmsBlockFactory(startLocation);
        final IAnimatedBlock fBlock = fallingBlockFactory.fallingBlockFactory(newFBlockLocation, nmsBlock);

        final boolean deferPlacement = BlockAnalyzer_V1_15_R1.placeOnSecondPassStatic(vBlock.getType());
        return Optional.of(new PBlockData(fBlock, radius, nmsBlock, startLocation, startAngle, deferPlacement));
    }
}
