package nl.pim16aap2.animatedarchitecture.spigot.core.animation;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.Color;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IHighlightedBlock;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

@Flogger
public final class HighlightedBlockDisplay implements IHighlightedBlock
{
    private final IExecutor executor;
    private final RotatedPosition startPosition;
    private final World bukkitWorld;
    private final BlockData blockData;

    private @Nullable BlockDisplay entity;

    public HighlightedBlockDisplay(IExecutor executor, RotatedPosition startPosition, IWorld world, Color color)
    {
        this.executor = executor;
        this.startPosition = startPosition;
        this.bukkitWorld = Util.requireNonNull(SpigotAdapter.getBukkitWorld(world), "Bukkit World");
        this.blockData = newBlockData(color);
    }

    private static BlockData newBlockData(Color color)
    {
        final Material mat = switch (color)
            {
                case DARK_RED -> Material.RED_STAINED_GLASS;
                case RED -> Material.PINK_STAINED_GLASS;
                case GOLD -> Material.ORANGE_STAINED_GLASS;
                case YELLOW -> Material.YELLOW_STAINED_GLASS;
                case DARK_GREEN -> Material.GREEN_STAINED_GLASS;
                case GREEN -> Material.LIME_STAINED_GLASS;
                case AQUA -> Material.LIGHT_BLUE_STAINED_GLASS;
                case DARK_AQUA -> Material.CYAN_STAINED_GLASS;
                case DARK_BLUE, BLUE -> Material.BLUE_STAINED_GLASS;
                case DARK_PURPLE -> Material.PURPLE_STAINED_GLASS;
                case LIGHT_PURPLE -> Material.MAGENTA_STAINED_GLASS;
                case WHITE -> Material.WHITE_STAINED_GLASS;
                case GRAY -> Material.LIGHT_GRAY_STAINED_GLASS_PANE;
                case DARK_GRAY -> Material.GRAY_STAINED_GLASS_PANE;
                case BLACK -> Material.BLACK_STAINED_GLASS;
            };
        return mat.createBlockData();
    }

    public synchronized void spawn()
    {
        executor.assertMainThread("Highlighted blocks must be spawned on the main thread!");
        if (this.entity != null)
            kill();
        this.entity = BlockDisplayHelper.spawn(executor, bukkitWorld, startPosition, blockData);
        this.entity.setViewRange(1F);
        this.entity.setGlowing(true);
        this.entity.setBrightness(new Display.Brightness(15, 15));
        //noinspection deprecation
        this.entity.setVisibleByDefault(false);
    }

    @Override
    public synchronized void kill()
    {
        executor.assertMainThread("Highlighted blocks must be killed on the main thread!");
        if (this.entity == null)
            return;
        this.entity.remove();
        this.entity = null;
    }

    @Override
    public synchronized void moveToTarget(RotatedPosition target)
    {
        BlockDisplayHelper.moveToTarget(entity, startPosition, target);
    }

    public synchronized @Nullable Entity getEntity()
    {
        return entity;
    }
}
