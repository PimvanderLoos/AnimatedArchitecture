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
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PBlockDataFactorySpigot_V1_15_R1 implements IPBlockDataFactory
{
    @Override
    public Optional<PBlockData> create(IPLocation startLocation, boolean bottom, float radius, float startAngle)
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

        final IPLocation newFBlockLocation = getSpawnLocation(startLocation, bottom);

        final INMSBlock nmsBlock = BigDoors.get().getPlatform().getFallingBlockFactory()
                                           .nmsBlockFactory(startLocation);
        final ICustomCraftFallingBlock fBlock = BigDoors.get().getPlatform().getFallingBlockFactory()
                                                        .fallingBlockFactory(newFBlockLocation, nmsBlock);

        boolean deferPlacement = BlockAnalyzer_V1_15_R1.placeOnSecondPassStatic(vBlock.getType());
        return Optional.of(new PBlockData(fBlock, radius, nmsBlock, startLocation, startAngle, deferPlacement));
    }
}
