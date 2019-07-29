package nl.pim16aap2.bigdoors.spigotutil;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public class PlayerRetriever
{
    @NotNull
    public OfflinePlayer getOfflinePlayer(final @NotNull UUID uuid)
    {
        return Bukkit.getOfflinePlayer(uuid);
    }

    @NotNull
    public Optional<String> nameFromUUID(final @NotNull UUID uuid)
    {
        return SpigotUtil.nameFromUUID(uuid);
    }
}
