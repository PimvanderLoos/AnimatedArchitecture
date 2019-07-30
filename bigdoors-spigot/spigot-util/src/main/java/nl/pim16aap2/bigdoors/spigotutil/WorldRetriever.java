package nl.pim16aap2.bigdoors.spigotutil;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class WorldRetriever
{
    @Nullable
    public World worldFromString(final @NotNull UUID uuid)
    {
        return Bukkit.getServer().getWorld(uuid);
    }
}
