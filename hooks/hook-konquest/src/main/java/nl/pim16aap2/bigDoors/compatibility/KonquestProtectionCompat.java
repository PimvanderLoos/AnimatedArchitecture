package nl.pim16aap2.bigDoors.compatibility;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.api.model.KonquestTerritoryType;
import konquest.api.model.KonquestUpgrade;
import konquest.model.KonCamp;
import konquest.model.KonCapital;
import konquest.model.KonDirective;
import konquest.model.KonMonumentTemplate;
import konquest.model.KonPlayer;
import konquest.model.KonTerritory;
import konquest.model.KonTown;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class KonquestProtectionCompat implements IProtectionCompat
{
    @SuppressWarnings("SpellCheckingInspection")
    private static final String NAME = "Konquest";

    private final @Nullable Konquest konquest;

    public KonquestProtectionCompat(@SuppressWarnings("unused") HookContext hookContext)
    {
        final @Nullable KonquestPlugin konquestPlugin = (KonquestPlugin) Bukkit.getPluginManager().getPlugin(NAME);
        konquest = konquestPlugin == null ? null : konquestPlugin.getKonquestInstance();
    }

    @Override
    public boolean canBreakBlock(Player player, Location loc)
    {
        if (konquest == null)
            return true;

        final World world = Objects.requireNonNull(loc.getWorld());
        if (konquest.isWorldIgnored(world))
            return true;

        final KonPlayer konPlayer = konquest.getPlayerManager().getPlayer(player);

        return konquest.getKingdomManager().isChunkClaimed(loc) ?
               checkTerritory(konquest, konPlayer, loc, konquest.getKingdomManager().getChunkTerritory(loc)) :
               wildAllowed(konquest, konPlayer);
    }

    public boolean wildAllowed(Konquest konquest, KonPlayer player)
    {
        final boolean wildConfigured =
            konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.wild_build", true);
        return player.isAdminBypassActive() || wildConfigured;
    }

    private boolean checkTerritory(Konquest konquest, KonPlayer player, Location loc, KonTerritory territory)
    {
        if (player.isAdminBypassActive())
            return true;

        if (territory instanceof KonCapital)
        {
            if (!konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.capital_build", false))
                return false;

            final @Nullable KonMonumentTemplate template = territory.getKingdom().getMonumentTemplate();
            if (template != null && template.isLocInside(loc))
                return false;

            if (!player.getKingdom().equals(territory.getKingdom()))
                return false;
        }

        if (territory.getTerritoryType().equals(KonquestTerritoryType.TOWN) && territory instanceof KonTown)
        {
            final KonTown town = (KonTown) territory;
            if (town.isLocInsideCenterChunk(loc))
                return false;

            if (!player.getKingdom().equals(town.getKingdom()))
            {
                if (territory.getKingdom().isPeaceful() ||
                    player.getKingdom().isPeaceful() ||
                    town.getKingdom().isOfflineProtected())
                    return false;

                int upgradeLevel = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonquestUpgrade.WATCH);
                if (upgradeLevel > 0 && town.getNumResidentsOnline() < upgradeLevel ||
                    konquest.getGuildManager().isArmistice(player, town) ||
                    town.isShielded() ||
                    town.isArmored())
                    return false;
            }
            else
            {
                if (!town.isOpen() && !town.isPlayerResident(player.getOfflineBukkitPlayer()))
                    return false;

                if (town.isOpen() || !town.isOpen() && town.isPlayerResident(player.getOfflineBukkitPlayer()))
                {
                    if (konquest.getPlotManager().isPlayerPlotProtectBuild(town, loc, player.getBukkitPlayer()))
                        return false;

                    if (town.isPlotOnly() && !town.isPlayerKnight(player.getOfflineBukkitPlayer()) && !town.hasPlot(loc))
                        return false;
                }
                konquest.getDirectiveManager().updateDirectiveProgress(player, KonDirective.BUILD_TOWN);
            }
        }

        if (territory.getTerritoryType().equals(KonquestTerritoryType.CAMP) && territory instanceof KonCamp)
        {
            final KonCamp camp = (KonCamp) territory;

            final boolean isMemberAllowedEdit =
                konquest.getConfigManager().getConfig("core").getBoolean("core.camps.clan_allow_edit_offline", false);

            if (camp.isProtected() && !isMemberAllowedEdit)
                return false;
        }

        return !territory.getTerritoryType().equals(KonquestTerritoryType.RUIN);
    }

    @Override
    public boolean canBreakBlocksBetweenLocs(Player player, Location loc1, Location loc2)
    {
        if (konquest == null)
            return true;

        final World world = Objects.requireNonNull(loc1.getWorld());

        if (konquest.isWorldIgnored(world))
            return true;

        final KonPlayer konPlayer = konquest.getPlayerManager().getPlayer(player);

        final int x1 = Math.min(loc1.getBlockX(), loc2.getBlockX()) >> 4;
        final int y1 = Math.min(loc1.getBlockY(), loc2.getBlockY());
        final int z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ()) >> 4;
        final int x2 = Math.max(loc1.getBlockX(), loc2.getBlockX()) >> 4;
        final int y2 = Math.max(loc1.getBlockY(), loc2.getBlockY());
        final int z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ()) >> 4;

        final Location loc = new Location(world, 0, (y2 + y1) / 2f, 0);

        boolean wildChecked = false;

        for (int chunkX = x1; chunkX <= x2; ++chunkX)
        {
            loc.setX(chunkX << 4);
            for (int chunkZ = z1; chunkZ <= z2; ++chunkZ)
            {
                loc.setZ(chunkZ << 4);
                final boolean claimed = konquest.getKingdomManager().isChunkClaimed(loc);
                if (claimed)
                {
                    final KonTerritory territory = konquest.getKingdomManager().getChunkTerritory(loc);
                    if (!checkTerritory(konquest, konPlayer, loc, territory))
                        return false;
                }
                else if (!wildChecked)
                {
                    if (!wildAllowed(konquest, konPlayer))
                        return false;
                    wildChecked = true;
                }
            }
        }
        return true;
    }

    @Override
    public boolean success()
    {
        return konquest != null;
    }

    @Override
    public String getName()
    {
        return NAME;
    }
}
