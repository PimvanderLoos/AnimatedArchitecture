package nl.pim16aap2.animatedarchitecture.spigot;

import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotUtil;
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
        Assertions.assertEquals(1, SpigotUtil.getPermissionSuffixValue("animatedarchitecture.test.1",
                                                                       "animatedarchitecture.test."));
        Assertions.assertEquals(10, SpigotUtil.getPermissionSuffixValue("animatedarchitecture.test.10",
                                                                        "animatedarchitecture.test."));
        Assertions.assertEquals(-1, SpigotUtil.getPermissionSuffixValue("animatedarchitecture.not.1",
                                                                        "animatedarchitecture.test."));
        Assertions.assertEquals(-1, SpigotUtil.getPermissionSuffixValue("animatedarchitecture.not.1",
                                                                        "animatedarchitecture.test."));
        Assertions.assertEquals(-1, SpigotUtil.getPermissionSuffixValue("animatedarchitecture.test.",
                                                                        "animatedarchitecture.test."));
        Assertions.assertEquals(-1, SpigotUtil.getPermissionSuffixValue("animatedarchitecture.test.abc",
                                                                        "animatedarchitecture.test."));
    }

    @Test
    void getHighestPermissionSuffix()
    {
        final Player player = initPermissions("animatedarchitecture.test.-1",
                                              "animatedarchitecture.test.10",
                                              "animatedarchitecture.test.5",
                                              "animatedarchitecture.test.abc",
                                              "animatedarchitecture.not_a_test.999");
        Assertions.assertEquals(10, SpigotUtil.getHighestPermissionSuffix(player, "animatedarchitecture.test."));
    }

    private static Player initPermissions(String... nodes)
    {
        final Player player = Mockito.mock(Player.class);
        final PermissionAttachment permissionAttachment = Mockito.mock(PermissionAttachment.class);

        final Set<PermissionAttachmentInfo> effectivePermissions = new HashSet<>(MathUtil.ceil(1.25 * nodes.length));
        for (final String node : nodes)
            effectivePermissions.add(new PermissionAttachmentInfo(player, node, permissionAttachment, true));

        Mockito.when(player.getEffectivePermissions()).thenReturn(effectivePermissions);
        return player;
    }
}
