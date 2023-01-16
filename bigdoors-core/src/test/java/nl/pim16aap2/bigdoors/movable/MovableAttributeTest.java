package nl.pim16aap2.bigdoors.movable;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

class MovableAttributeTest
{
    @Test
    void testPermissionCheck()
    {
        final MovableAttribute attribute = Mockito.mock(MovableAttribute.class, InvocationOnMock::callRealMethod);
        Mockito.when(attribute.getPermissionLevel()).thenReturn(PermissionLevel.ADMIN);

        Assertions.assertFalse(attribute.canAccessWith(PermissionLevel.NO_PERMISSION));
        Assertions.assertFalse(attribute.canAccessWith(PermissionLevel.USER));
        Assertions.assertTrue(attribute.canAccessWith(PermissionLevel.ADMIN));
        Assertions.assertTrue(attribute.canAccessWith(PermissionLevel.CREATOR));
    }
}
