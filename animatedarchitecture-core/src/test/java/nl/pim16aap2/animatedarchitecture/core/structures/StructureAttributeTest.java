package nl.pim16aap2.animatedarchitecture.core.structures;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StructureAttributeTest
{
    @Test
    void testPermissionCheck()
    {
        final StructureAttribute attribute = Mockito.mock(StructureAttribute.class, InvocationOnMock::callRealMethod);
        Mockito.when(attribute.getPermissionLevel()).thenReturn(PermissionLevel.ADMIN);

        Assertions.assertFalse(attribute.canAccessWith(PermissionLevel.NO_PERMISSION));
        Assertions.assertFalse(attribute.canAccessWith(PermissionLevel.USER));
        Assertions.assertTrue(attribute.canAccessWith(PermissionLevel.ADMIN));
        Assertions.assertTrue(attribute.canAccessWith(PermissionLevel.CREATOR));
    }
}
