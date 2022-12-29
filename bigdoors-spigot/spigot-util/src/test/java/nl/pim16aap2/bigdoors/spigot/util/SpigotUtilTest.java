package nl.pim16aap2.bigdoors.spigot.util;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

class SpigotUtilTest
{
    @Test
    void durationToTicks()
    {
        Assertions.assertEquals(0, SpigotUtil.durationToTicks(Duration.ofMinutes(-1)));
        Assertions.assertEquals(0, SpigotUtil.durationToTicks(Duration.ofNanos(10)));

        Assertions.assertEquals(1, SpigotUtil.durationToTicks(Duration.ofMillis(1)));
        Assertions.assertEquals(1, SpigotUtil.durationToTicks(Duration.ofMillis(50)));
        Assertions.assertEquals(2, SpigotUtil.durationToTicks(Duration.ofMillis(51)));
    }

    @Test
    void getPermissionSuffixValue()
    {
        Assertions.assertEquals(1, SpigotUtil.getPermissionSuffixValue("bigdoors.test.1", "bigdoors.test."));
        Assertions.assertEquals(10, SpigotUtil.getPermissionSuffixValue("bigdoors.test.10", "bigdoors.test."));
        Assertions.assertEquals(-1, SpigotUtil.getPermissionSuffixValue("bigdoors.not.1", "bigdoors.test."));
        Assertions.assertEquals(-1, SpigotUtil.getPermissionSuffixValue("bigdoors.not.1", "bigdoors.test."));
        Assertions.assertEquals(-1, SpigotUtil.getPermissionSuffixValue("bigdoors.test.", "bigdoors.test."));
        Assertions.assertEquals(-1, SpigotUtil.getPermissionSuffixValue("bigdoors.test.abc", "bigdoors.test."));
    }

    @Test
    void getHighestPermissionSuffix()
    {
        final Player player = initPermissions("bigdoors.test.-1",
                                              "bigdoors.test.10",
                                              "bigdoors.test.5",
                                              "bigdoors.test.abc",
                                              "bigdoors.not_a_test.999");
        Assertions.assertEquals(10, SpigotUtil.getHighestPermissionSuffix(player, "bigdoors.test."));
    }

    private static Player initPermissions(String... nodes)
    {
        final Player player = Mockito.mock(Player.class);
        final PermissionAttachment permissionAttachment = Mockito.mock(PermissionAttachment.class);

        final Set<PermissionAttachmentInfo> effectivePermissions = new HashSet<>(nodes.length);
        for (final String node : nodes)
            effectivePermissions.add(new PermissionAttachmentInfo(player, node, permissionAttachment, true));

        Mockito.when(player.getEffectivePermissions()).thenReturn(effectivePermissions);
        return player;
    }
}
