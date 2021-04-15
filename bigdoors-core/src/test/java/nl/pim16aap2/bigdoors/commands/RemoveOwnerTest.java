package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.*;

class RemoveOwnerTest
{
    @Mock
    private static DoorRetriever doorRetriever;
    @Mock
    private static AbstractDoorBase door;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer target;

    private RemoveOwner removeOwner;

    @BeforeAll
    static void init()
    {
        initPlatform();
        doorRetriever = Mockito.mock(DoorRetriever.class);
        door = Mockito.mock(AbstractDoorBase.class);
        initDoorRetriever(doorRetriever, door);
    }

    @BeforeEach
    void beforeEach()
    {
        MockitoAnnotations.openMocks(this);
        initCommandSenderPermissions(commandSender, true, true);
        removeOwner = new RemoveOwner(commandSender, doorRetriever, target);
    }

    /**
     * Ensure that even with the bypass permission, certain invalid actions are not allowed. (e.g. removing the original
     * creator).
     */
    @Test
    void testBypassLimitations()
    {
        // The target is not an owner, so this should be false despite the bypass permission.
        Assertions.assertFalse(removeOwner.isAllowed(door, true));

        // Removing the level0 owner is not allowed even with bypass enabled!
        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwner0));
        Assertions.assertFalse(removeOwner.isAllowed(door, true));

        // Removing level>0 owners IS allowed with bypass even if
        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwner1));
        Assertions.assertTrue(removeOwner.isAllowed(door, true));
    }

    @Test
    void testSuccess()
    {
        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner0));
        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwner1));
        Assertions.assertTrue(removeOwner.isAllowed(door, false));

        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwner2));
        Assertions.assertTrue(removeOwner.isAllowed(door, false));
    }

    @Test
    void testPermissionLevels()
    {
        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwner0));
        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner0));
        Assertions.assertFalse(removeOwner.isAllowed(door, false));

        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner1));
        Assertions.assertFalse(removeOwner.isAllowed(door, false));

        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner1));
        Assertions.assertFalse(removeOwner.isAllowed(door, false));

        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwner1));
        Assertions.assertFalse(removeOwner.isAllowed(door, false));

        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwner2));
        Assertions.assertTrue(removeOwner.isAllowed(door, false));

        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwner2));
        Assertions.assertTrue(removeOwner.isAllowed(door, false));

        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner2));
        Assertions.assertFalse(removeOwner.isAllowed(door, true));
    }
}
