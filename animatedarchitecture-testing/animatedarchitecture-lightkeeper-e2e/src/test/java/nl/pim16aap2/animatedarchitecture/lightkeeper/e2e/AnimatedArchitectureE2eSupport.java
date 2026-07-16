package nl.pim16aap2.animatedarchitecture.lightkeeper.e2e;

import nl.pim16aap2.lightkeeper.framework.ILightkeeperFramework;
import nl.pim16aap2.lightkeeper.framework.MenuHandle;
import nl.pim16aap2.lightkeeper.framework.MenuItemSnapshot;
import nl.pim16aap2.lightkeeper.framework.PlayerHandle;
import nl.pim16aap2.lightkeeper.framework.Vector3Di;
import nl.pim16aap2.lightkeeper.framework.WorldHandle;

import java.time.Duration;
import java.util.List;
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
        final PlayerHandle player = framework.bots().builder()
            .withName(name)
            .atLocation(world, location.x(), location.y(), location.z())
            .withPermissions(ANIMATED_ARCHITECTURE_USER_PERMISSIONS)
            .build();
        player.andWaitTicks(10);
        return player;
    }

    static String uniqueName(String prefix)
    {
        final String serverType = System.getProperty("animatedarchitecture.e2e.serverType", "server");
        final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return prefix + "_" + serverType + "_" + suffix;
    }

    static String uniquePlayerName(String prefix)
    {
        final String serverType = System.getProperty("animatedarchitecture.e2e.serverType", "server");
        final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return prefix + "_" + serverType.charAt(0) + "_" + suffix;
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
        player.leftClickBlock(lowerBlock);
        player.andWaitTicks(5);
        player.leftClickBlock(offset(lowerBlock, 0, 1, 0));
        player.andWaitTicks(5);
        player.leftClickBlock(offset(lowerBlock, 0, -1, 0));
        player.andWaitTicks(5);
        player
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
        final String expected = normalizeMaterial(expectedMaterial);
        try
        {
            framework.waitUntil(
                () -> normalizeMaterial(world.blockTypeAt(position)).equals(expected),
                DEFAULT_WAIT_TIMEOUT
            );
        }
        catch (IllegalStateException exception)
        {
            final String actual = normalizeMaterial(world.blockTypeAt(position));
            final String serverType = System.getProperty("animatedarchitecture.e2e.serverType", "unknown");
            throw new IllegalStateException(
                "Timed out waiting for block at %s in world '%s' on %s. Expected '%s', found '%s'."
                    .formatted(position, world.name(), serverType, expected, actual),
                exception
            );
        }
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
        final List<String> unexpectedErrorLines = framework.server().output()
            .stream()
            .filter(AnimatedArchitectureE2eSupport::isUnexpectedServerErrorLine)
            .toList();
        assertThat(unexpectedErrorLines)
            .as("server output should not contain unexpected error lines")
            .isEmpty();
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

    private static boolean isUnexpectedServerErrorLine(String line)
    {
        if (line.contains("Encountered error creating block state from block data")
            && line.contains("minecraft:moving_piston"))
        {
            return false;
        }

        final String normalized = line.toLowerCase(Locale.ROOT);
        return normalized.contains("severe")
            || normalized.contains("[error]")
            || normalized.contains("exception")
            || normalized.contains("caused by:");
    }
}
