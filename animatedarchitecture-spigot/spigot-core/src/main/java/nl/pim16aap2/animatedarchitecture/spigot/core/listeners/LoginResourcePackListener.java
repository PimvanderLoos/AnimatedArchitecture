package nl.pim16aap2.animatedarchitecture.spigot.core.listeners;

import com.google.common.io.BaseEncoding;
import lombok.Getter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.spigot.core.config.ConfigSpigot;
import nl.pim16aap2.util.reflection.ReflectionBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.semver4j.Semver;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Represents a listener that keeps track of {@link Player}s logging in to send them the resource pack.
 */
@Singleton
@Flogger
public class LoginResourcePackListener extends AbstractListener
{
    private static final UUID ANIMATED_ARCHITECTURE_RESOURCE_PACK_ID =
        UUID.fromString("f8039d0d-666f-4923-b31d-d8928de8481f");

    private static final @Nullable MethodHandle METHOD_ADD_RESOURCE_PACK = getAddResourcePackMethodHandle();

    private final ResourcePackDetails resourcePackDetails;
    private final ILocalizer localizer;
    private final BiConsumer<Player, ResourcePackDetails> resourcePackSender;

    @Inject
    LoginResourcePackListener(
        @Named("serverVersion") Semver serverVersion,
        ILocalizer localizer,
        RestartableHolder holder,
        ConfigSpigot config,
        JavaPlugin plugin)
    {
        super(holder, plugin, config::isResourcePackEnabled);

        this.resourcePackDetails = ResourcePackDetails.getForVersion(serverVersion);
        this.localizer = localizer;
        this.resourcePackSender = METHOD_ADD_RESOURCE_PACK == null ?
            this::setResourcePack :
            (player, resourcePackDetails) -> addResourcePack(METHOD_ADD_RESOURCE_PACK, player, resourcePackDetails);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        try
        {
            resourcePackSender.accept(event.getPlayer(), this.resourcePackDetails);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to send resource pack to player %s", event.getPlayer().getName());
        }
    }

    private void addResourcePack(
        MethodHandle addResourcePack,
        Player player,
        ResourcePackDetails resourcePackDetails)
    {
        try
        {
            addResourcePack.invokeExact(
                player,
                ANIMATED_ARCHITECTURE_RESOURCE_PACK_ID,
                resourcePackDetails.getUrl(),
                resourcePackDetails.getHash(),
                localizer.getMessage("core.resource_pack.message"),
                false
            );
        }
        catch (Throwable throwable)
        {
            log.atSevere().withCause(throwable).log("Failed to add resource pack to player!");
        }
    }

    private void setResourcePack(Player player, ResourcePackDetails resourcePackDetails)
    {
        player.setResourcePack(resourcePackDetails.getUrl(), resourcePackDetails.getHash());
    }

    @Override
    public void initialize()
    {
        super.initialize();
    }

    private static @Nullable MethodHandle getAddResourcePackMethodHandle()
    {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        final @Nullable Method method = ReflectionBuilder
            .findMethod()
            .inClass(Player.class)
            .withName("addResourcePack")
            .withParameters(
                UUID.class,
                String.class,
                byte[].class,
                String.class,
                boolean.class)
            .getNullable();

        if (method == null)
            return null;

        try
        {
            return lookup.unreflect(method);
        }
        catch (IllegalAccessException e)
        {
            log.atSevere().withCause(e).log("Failed to access addResourcePack method");
        }
        return null;
    }

    /**
     * Represents the details of the resource pack.
     * <p>
     * Each resource pack has a URL and a minimum and maximum version for which it is suitable.
     * <p>
     * The URL is the URL to the resource pack file.
     * <p>
     * The minimum and maximum version are the taken from the <a
     * href="https://minecraft.fandom.com/wiki/Pack_format">wiki page</a>.
     */
    enum ResourcePackDetails
    {
        FORMAT_15(
            "https://www.dropbox.com/scl/fi/htm9qmsg9ovkzsnnqfkrp/AnimatedArchitectureResourcePack-Format15.zip?" +
                "rlkey=vynzgqlxrxq5kj0s1x0vkksz2&st=lxa7d8ra&dl=1",
            "4694dfab385719f08d74f6a15c720a065f4347d5",
            Semver.of(1, 20, 0),
            Semver.of(1, 20, 1)
        ),

        FORMAT_18(
            "https://www.dropbox.com/scl/fi/jqcas7ctevgtwemy5igfy/AnimatedArchitectureResourcePack-Format18.zip?" +
                "rlkey=wfvtyfk1x7x7h04idy5xlo7b2&st=ass65deo&dl=1",
            "1edf3c8147d5df17a17bc074c82f5bc10a978a67",
            Semver.of(1, 20, 2),
            Semver.of(1, 20, 2)
        ),

        FORMAT_22(
            "https://www.dropbox.com/scl/fi/ldpyekzkiuzg3pgwji1u5/AnimatedArchitectureResourcePack-Format22.zip?" +
                "rlkey=3l4dzu7kqjecn7p21i9pd9v8g&st=xmhz5un0&dl=1",
            "891223422d266bf1a66c5190c5b506c75b46e7c3",
            Semver.of(1, 20, 3),
            Semver.of(1, 20, 4)
        ),

        FORMAT_32(
            "https://www.dropbox.com/scl/fi/xhk4p6uypesrucxhmqvxu/AnimatedArchitectureResourcePack-Format32.zip?" +
                "rlkey=29clxqbyhpn7hxq5r1b1prvgc&st=ghksx4ly&dl=1",
            "7b91f73c3cd69a1a7bb380c291d4cbb38871253e",
            Semver.of(1, 20, 5),
            Semver.of(1, 20, 6)
        ),

        FORMAT_34(
            "https://www.dropbox.com/scl/fi/tz3ic040ehv45xr6kemqy/AnimatedArchitectureResourcePack-Format34.zip?" +
                "rlkey=7k9jkblzxj3xb5my7dugfyj7a&st=s9sv8vl0&dl=1",
            "c49b27c4a7a5abd923048397db1c8024eaadb40a",
            Semver.of(1, 21, 0),
            Semver.of(1, 21, 3)
        ),

        FORMAT_46(
            "https://www.dropbox.com/scl/fi/vr0cxjomuukfj88c6k66k/AnimatedArchitectureResourcePack-Format46.zip?" +
                "rlkey=0ttpgjt13mpgi2cwxqc5etvxh&st=hlesau5y&dl=1",
            "489ac0b5b00a33ba37483232c18ad13f1c4b1967",
            Semver.of(1, 21, 4),
            Semver.of(1, 21, 4)
        ),

        FORMAT_51(
            "https://www.dropbox.com/scl/fi/sfrr6z8v4eqmw820yfhl9/AnimatedArchitectureResourcePack-Format51.zip?" +
                "rlkey=gnimfw7oynixyygxpf62i5fux&st=waxn7itz&dl=1",
            "2d3e655de27896f8c5c14661cb5eb8c13b678d07",
            Semver.of(1, 21, 5),
            Semver.of(1, 21, 5)
        ),

        ;

        public static final List<ResourcePackDetails> VALUES = List.of(values());
        public static final ResourcePackDetails LATEST = VALUES.getLast();

        private final String url;

        private final byte[] hash;

        @Getter
        private final Semver minVersion;

        @Getter
        private final Semver maxVersion;

        /**
         * @param url
         *     The URL to the resource pack.
         * @param hash
         *     The SHA1 hash of the resource pack.
         * @param minVersion
         *     The minimum version for which the resource pack is suitable (inclusive).
         * @param maxVersion
         *     The maximum version for which the resource pack is suitable (inclusive).
         */
        ResourcePackDetails(String url, String hash, Semver minVersion, Semver maxVersion)
        {
            this.url = url;
            this.hash = decodeHash(hash);

            this.minVersion = minVersion;
            this.maxVersion = maxVersion;
        }

        /**
         * Decodes the hash from a hexadecimal string to a byte array.
         *
         * @param hash
         *     The hash to decode.
         * @return The decoded hash.
         */
        static byte[] decodeHash(String hash)
        {
            if (hash.length() != 40)
                throw new IllegalArgumentException(
                    "The hash must be 40 characters long! Got: " + hash.length() + " characters.");

            return BaseEncoding.base16().decode(hash.toUpperCase(Locale.ROOT));
        }

        /**
         * Finds the resource pack data most suitable for the given version.
         * <p>
         * If no suitable resource pack data is found, the latest resource pack data is returned.
         *
         * @param version
         *     The version for which to find the most suitable resource pack data.
         * @return The resource pack data most suitable for the given version.
         */
        public static ResourcePackDetails getForVersion(Semver version)
        {
            // e.g. 1.20.0-pre1 -> 1.20.0; We don't support non-release versions.
            final Semver testVersion = version.withClearedPreReleaseAndBuild();

            if (testVersion.isGreaterThan(LATEST.minVersion))
                return LATEST;

            final var lowestVersion = VALUES.getFirst().minVersion;
            if (testVersion.isLowerThan(lowestVersion))
                throw new IllegalArgumentException(
                    "Version '" + testVersion + "' is lower than the lowest supported version '" + lowestVersion + "'");

            for (final var entry : VALUES)
                if (testVersion.isGreaterThanOrEqualTo(entry.minVersion) &&
                    testVersion.isLowerThanOrEqualTo(entry.maxVersion))
                    return entry;

            return LATEST;
        }

        /**
         * Gets the URL to the resource pack.
         *
         * @return The URL to the resource pack.
         */
        String getUrl()
        {
            return url;
        }

        /**
         * Gets the hash of the resource pack.
         * <p>
         * If no hash is set, an empty byte array is returned.
         * <p>
         * If provided, the hash is an SHA-1 hash of the resource pack (20 bytes).
         *
         * @return The hash of the resource pack.
         */
        byte[] getHash()
        {
            return hash;
        }
    }
}
