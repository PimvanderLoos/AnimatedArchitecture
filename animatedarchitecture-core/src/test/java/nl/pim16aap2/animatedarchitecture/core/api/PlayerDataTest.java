package nl.pim16aap2.animatedarchitecture.core.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class PlayerDataTest
{
    @Test
    public void test()
    {
        final UUID uuid = UUID.randomUUID();
        final String name = "name";

        final var limits = new LimitContainer(20, 30, 40, 50);


        final PlayerData playerData1 = new PlayerData(uuid, name, limits, false, false);
        final PlayerData playerData2 = new PlayerData(uuid, name, limits, true, false);
        final PlayerData playerData3 = new PlayerData(uuid, name, limits, false, true);
        final PlayerData playerData4 = new PlayerData(uuid, name, limits, true, true);

        Assertions.assertEquals(0, playerData1.getPermissionsFlag());
        Assertions.assertEquals(1, playerData2.getPermissionsFlag());
        Assertions.assertEquals(2, playerData3.getPermissionsFlag());
        Assertions.assertEquals(3, playerData4.getPermissionsFlag());

        Assertions.assertEquals(playerData1, new PlayerData(uuid, name, limits, 0));
        Assertions.assertEquals(playerData2, new PlayerData(uuid, name, limits, 1));
        Assertions.assertEquals(playerData3, new PlayerData(uuid, name, limits, 2));
        Assertions.assertEquals(playerData4, new PlayerData(uuid, name, limits, 3));
    }
}
