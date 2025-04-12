package nl.pim16aap2.animatedarchitecture.spigot.util.implementations;

import lombok.Getter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IPlayerFactory;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.commands.CommandDefinition;
import nl.pim16aap2.animatedarchitecture.core.commands.PermissionsStatus;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.localization.PersonalizedLocalizer;
import nl.pim16aap2.animatedarchitecture.core.text.Text;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import nl.pim16aap2.animatedarchitecture.core.util.Limit;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotUtil;
import nl.pim16aap2.animatedarchitecture.spigot.util.text.TextRendererSpigot;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Represents an implementation of {@link IPlayer} for the Spigot platform.
 */
@Flogger
public final class WrappedPlayer implements IPlayer
{
    private final Player spigotPlayer;

    @Getter
    private final ITextFactory textFactory;

    @Getter
    private final PersonalizedLocalizer personalizedLocalizer;

    /**
     * Creates a new {@link WrappedPlayer} object.
     *
     * @param spigotPlayer
     *     The Spigot player to wrap.
     * @param localizer
     *     The localizer to use for localization of messages to send to the player.
     * @param textFactory
     *     The text factory to use for creating text objects.
     * @deprecated Use {@link IPlayerFactory} instead.
     */
    @Deprecated
    public WrappedPlayer(Player spigotPlayer, ILocalizer localizer, ITextFactory textFactory)
    {
        this.spigotPlayer = spigotPlayer;
        this.textFactory = textFactory;
        this.personalizedLocalizer = new PersonalizedLocalizer(localizer, null);
    }

    @Override
    public UUID getUUID()
    {
        return spigotPlayer.getUniqueId();
    }

    @Override
    public boolean hasProtectionBypassPermission()
    {
        return spigotPlayer.isOp() ||
            spigotPlayer.hasPermission(Constants.PERMISSION_PREFIX_ADMIN_BYPASS + "protectionhooks");
    }

    @Override
    public Optional<ILocation> getLocation()
    {
        return Optional.of(SpigotAdapter.wrapLocation(spigotPlayer.getLocation()));
    }

    @Override
    public CompletableFuture<Boolean> hasPermission(String permission)
    {
        return CompletableFuture.completedFuture(isOp() || spigotPlayer.hasPermission(permission));
    }

    @Override
    public CompletableFuture<PermissionsStatus> hasPermission(CommandDefinition command)
    {
        if (isOp())
            return CompletableFuture.completedFuture(new PermissionsStatus(true, true));

        return CompletableFuture.completedFuture(new PermissionsStatus(
            command.getUserPermission().map(spigotPlayer::hasPermission).orElse(false),
            command.getAdminPermission().map(spigotPlayer::hasPermission).orElse(false))
        );
    }

    @Override
    public boolean isOnline()
    {
        return true;
    }

    private OptionalInt getLimit(String permissionSuffix)
    {
        if (spigotPlayer.isOp())
            return OptionalInt.empty();
        return SpigotUtil.getHighestPermissionSuffix(spigotPlayer, permissionSuffix);
    }

    @Override
    public OptionalInt getLimit(Limit limit)
    {
        return getLimit(limit.getUserPermission());
    }

    @Override
    public boolean isOp()
    {
        return spigotPlayer.isOp();
    }

    @Override
    public String getName()
    {
        return spigotPlayer.getName();
    }

    @Override
    public void sendMessage(Text text)
    {
        spigotPlayer.spigot().sendMessage(text.render(new TextRendererSpigot()));
    }

    /**
     * Gets the bukkit player represented by this {@link IPlayer}
     *
     * @return The Bukkit player.
     */
    public Player getBukkitPlayer()
    {
        return spigotPlayer;
    }

    @Override
    public String toString()
    {
        return asString();
    }

    @Override
    public boolean equals(@Nullable Object o)
    {
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (getClass() != o.getClass())
            return false;
        return getUUID().equals(((WrappedPlayer) o).getUUID());
    }

    @Override
    public int hashCode()
    {
        return getUUID().hashCode();
    }

    static @Nullable Locale parseLocale(@Nullable String localeString)
    {
        if (localeString == null)
            return null;

        final String languageTag = localeString.replace("_", "-").toLowerCase(Locale.ROOT);
        final Locale locale = Locale.forLanguageTag(languageTag);

        return locale.getLanguage().isEmpty() ? null : locale;
    }
}
