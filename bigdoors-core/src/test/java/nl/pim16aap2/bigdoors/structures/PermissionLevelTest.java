package nl.pim16aap2.bigdoors.structures;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PermissionLevelTest
{
    @Test
    void testFromId()
    {
        Assertions.assertEquals(PermissionLevel.CREATOR, PermissionLevel.fromValue(0));
        Assertions.assertEquals(PermissionLevel.ADMIN, PermissionLevel.fromValue(100));
        Assertions.assertEquals(PermissionLevel.USER, PermissionLevel.fromValue(200));
        Assertions.assertEquals(PermissionLevel.NO_PERMISSION, PermissionLevel.fromValue(999));
        Assertions.assertNull(PermissionLevel.fromValue(-1));
        Assertions.assertNull(PermissionLevel.fromValue(1));
        Assertions.assertNull(PermissionLevel.fromValue(99));
        Assertions.assertNull(PermissionLevel.fromValue(400));
    }

    @Test
    void testIsLowerEqualsThan()
    {
        Assertions.assertTrue(PermissionLevel.CREATOR.isLowerThanOrEquals(PermissionLevel.CREATOR));
        Assertions.assertTrue(PermissionLevel.CREATOR.isLowerThanOrEquals(PermissionLevel.ADMIN));
        Assertions.assertTrue(PermissionLevel.ADMIN.isLowerThanOrEquals(PermissionLevel.USER));
        Assertions.assertTrue(PermissionLevel.USER.isLowerThanOrEquals(PermissionLevel.NO_PERMISSION));

        Assertions.assertTrue(PermissionLevel.NO_PERMISSION.isLowerThanOrEquals(PermissionLevel.NO_PERMISSION));
        Assertions.assertFalse(PermissionLevel.NO_PERMISSION.isLowerThanOrEquals(PermissionLevel.USER));
        Assertions.assertFalse(PermissionLevel.USER.isLowerThanOrEquals(PermissionLevel.ADMIN));
        Assertions.assertFalse(PermissionLevel.ADMIN.isLowerThanOrEquals(PermissionLevel.CREATOR));
    }

    @Test
    void testIsLowerThan()
    {
        Assertions.assertFalse(PermissionLevel.CREATOR.isLowerThan(PermissionLevel.CREATOR));
        Assertions.assertTrue(PermissionLevel.CREATOR.isLowerThan(PermissionLevel.ADMIN));
        Assertions.assertTrue(PermissionLevel.ADMIN.isLowerThan(PermissionLevel.USER));
        Assertions.assertTrue(PermissionLevel.USER.isLowerThan(PermissionLevel.NO_PERMISSION));

        Assertions.assertFalse(PermissionLevel.NO_PERMISSION.isLowerThan(PermissionLevel.NO_PERMISSION));
        Assertions.assertFalse(PermissionLevel.NO_PERMISSION.isLowerThan(PermissionLevel.USER));
        Assertions.assertFalse(PermissionLevel.USER.isLowerThan(PermissionLevel.ADMIN));
        Assertions.assertFalse(PermissionLevel.ADMIN.isLowerThan(PermissionLevel.CREATOR));
    }
}
