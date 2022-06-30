package nl.pim16aap2.bigdoors.spigot.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;

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
}
