package nl.pim16aap2.animatedarchitecture.lightkeeper.e2e;

import nl.pim16aap2.lightkeeper.framework.CommandResult;
import nl.pim16aap2.lightkeeper.framework.CommandSource;
import nl.pim16aap2.lightkeeper.framework.ILightkeeperFramework;
import nl.pim16aap2.lightkeeper.framework.MenuHandle;
import nl.pim16aap2.lightkeeper.framework.MenuItemSnapshot;
import nl.pim16aap2.lightkeeper.framework.PlayerHandle;
import nl.pim16aap2.lightkeeper.framework.Vector3Di;
import nl.pim16aap2.lightkeeper.framework.WorldHandle;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

final class AnimatedArchitectureE2eSupport
{
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("(?i)\u00a7[0-9A-FK-ORX]");
    private static final Duration DEFAULT_WAIT_TIMEOUT = Duration.ofSeconds(20);
    private static final String[] ANIMATED_ARCHITECTURE_USER_PERMISSIONS = {
        "animatedarchitecture.user.base",
        "animatedarchitecture.user.create.portcullis",
        "animatedarchitecture.user.info",
        "animatedarchitecture.user.liststructures",
        "animatedarchitecture.user.toggle"
    };

    private AnimatedArchitectureE2eSupport()
    {
    }

    static PlayerHandle createAnimatedArchitecturePlayer(
        ILightkeeperFramework framework,
        WorldHandle world,
        String name,
        Vector3Di location)
    {
        final PlayerHandle player = framework.buildPlayer()
            .withName(name)
            .atLocation(world, location.x(), location.y(), location.z())
            .build();
        grantPermissions(framework, name, ANIMATED_ARCHITECTURE_USER_PERMISSIONS);
        player.andWaitTicks(10);
        return player;
    }

    static String uniqueName(String prefix)
    {
        final String serverType = System.getProperty("animatedarchitecture.e2e.serverType", "server");
        final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return prefix + "_" + serverType + "_" + suffix;
    }

    static void placePortcullisFixture(WorldHandle world, Vector3Di lowerBlock)
    {
        world.setBlockAt(lowerBlock, "minecraft:stone");
        world.setBlockAt(offset(lowerBlock, 0, 1, 0), "minecraft:stone");
        world.setBlockAt(offset(lowerBlock, 0, -1, 0), "minecraft:gold_block");
    }

    static void createClosedUpwardPortcullis(
        PlayerHandle player,
        String structureName,
        Vector3Di lowerBlock,
        int blocksToMove)
    {
        player.executeCommand("aa newstructure portcullis " + structureName)
            .andWaitTicks(5);
        completeActivePortcullisCreator(player, lowerBlock, blocksToMove);
    }

    static void completeActivePortcullisCreator(
        PlayerHandle player,
        Vector3Di lowerBlock,
        int blocksToMove)
    {
        player
            .leftClickBlock(lowerBlock)
            .andWaitTicks(5)
            .leftClickBlock(offset(lowerBlock, 0, 1, 0))
            .andWaitTicks(5)
            .leftClickBlock(offset(lowerBlock, 0, -1, 0))
            .andWaitTicks(5)
            .executeCommand("aa setopenstatus Closed")
            .andWaitTicks(5)
            .executeCommand("aa setopendirection Up")
            .andWaitTicks(5)
            .executeCommand("aa setblockstomove " + blocksToMove)
            .andWaitTicks(5)
            .executeCommand("aa confirm")
            .andWaitTicks(20);
    }

    static void setCreatorName(PlayerHandle player, String structureName)
    {
        player.executeCommand("aa setname " + structureName)
            .andWaitTicks(5);
    }

    static void toggleAndWaitForOpen(
        ILightkeeperFramework framework,
        PlayerHandle player,
        WorldHandle world,
        String structureName,
        Vector3Di lowerBlock,
        int blocksToMove)
    {
        player.executeCommand("aa toggle " + structureName)
            .andWaitTicks(5);
        waitForBlock(framework, world, offset(lowerBlock, 0, blocksToMove, 0), "minecraft:stone");
        waitForBlock(framework, world, offset(lowerBlock, 0, blocksToMove + 1, 0), "minecraft:stone");
        waitForBlock(framework, world, lowerBlock, "minecraft:air");
        waitForBlock(framework, world, offset(lowerBlock, 0, 1, 0), "minecraft:air");
    }

    static Vector3Di offset(Vector3Di vector, int x, int y, int z)
    {
        return new Vector3Di(vector.x() + x, vector.y() + y, vector.z() + z);
    }

    static void waitForBlock(
        ILightkeeperFramework framework,
        WorldHandle world,
        Vector3Di position,
        String expectedMaterial)
    {
        framework.waitUntil(
            () -> normalizeMaterial(world.blockTypeAt(position)).equals(normalizeMaterial(expectedMaterial)),
            DEFAULT_WAIT_TIMEOUT
        );
    }

    static MenuHandle clickMenuItemContaining(MenuHandle menu, String expectedDisplayName)
    {
        final int slot = menu.snapshot()
            .items()
            .stream()
            .filter(item -> normalizedDisplayName(item).contains(expectedDisplayName))
            .mapToInt(MenuItemSnapshot::slot)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Could not find menu item containing '%s' in menu '%s'."
                    .formatted(expectedDisplayName, menu.snapshot().title())
            ));
        return menu.clickAtIndex(slot);
    }

    static void assertServerHealthy(ILightkeeperFramework framework)
    {
        nl.pim16aap2.lightkeeper.framework.assertions.LightkeeperAssertions.assertThat(framework)
            .hasNoServerErrors();
    }

    private static void grantPermissions(ILightkeeperFramework framework, String playerName, String... permissions)
    {
        for (final String permission : permissions)
            executeConsole(framework, "lp user " + playerName + " permission set " + permission + " true");
    }

    private static void executeConsole(ILightkeeperFramework framework, String command)
    {
        final CommandResult result = framework.executeCommand(CommandSource.CONSOLE, command);
        assertThat(result.success())
            .as(result.message())
            .isTrue();
    }

    private static String normalizedDisplayName(MenuItemSnapshot item)
    {
        return COLOR_CODE_PATTERN
            .matcher(Objects.requireNonNullElse(item.displayName(), ""))
            .replaceAll("");
    }

    private static String normalizeMaterial(String material)
    {
        final String normalized = material.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("minecraft:") ? normalized : "minecraft:" + normalized;
    }
}
