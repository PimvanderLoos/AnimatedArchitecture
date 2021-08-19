package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import lombok.val;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DelayedCommandInputManager;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.bigdoors.UnitTestUtil.initPlatform;
import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initCommandSenderPermissions;
import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initDoorRetriever;

class SetOpenDirectionTest
{
    @Mock
    private AbstractDoor door;

    @Mock
    private DoorType doorType;

    private IBigDoorsPlatform platform;

    @Mock
    private DoorRetriever doorRetriever;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @BeforeEach
    void init()
    {
        platform = initPlatform();
        MockitoAnnotations.openMocks(this);

        Mockito.when(door.syncData()).thenReturn(CompletableFuture.completedFuture(true));
        Mockito.when(door.getDoorType()).thenReturn(doorType);

        initCommandSenderPermissions(commandSender, true, true);
        initDoorRetriever(doorRetriever, door);
        Mockito.when(platform.getDelayedCommandInputManager()).thenReturn(new DelayedCommandInputManager());
    }

    @Test
    @SneakyThrows
    void testOpenDirValidity()
    {
        final @NotNull RotateDirection rotateDirection = RotateDirection.CLOCKWISE;

        Mockito.when(doorType.isValidOpenDirection(Mockito.any())).thenReturn(false);
        val command = new SetOpenDirection(commandSender, doorRetriever, rotateDirection);

        Assertions.assertTrue(command.performAction(door).get(1, TimeUnit.SECONDS));
        Mockito.verify(door, Mockito.never()).syncData();
        Mockito.verify(door, Mockito.never()).setOpenDir(rotateDirection);


        Mockito.when(doorType.isValidOpenDirection(rotateDirection)).thenReturn(true);
        Assertions.assertTrue(command.performAction(door).get(1, TimeUnit.SECONDS));
        Mockito.verify(door).setOpenDir(rotateDirection);
        Mockito.verify(door).syncData();
    }

    @Test
    @SneakyThrows
    void testStaticRunners()
    {
        final @NotNull RotateDirection rotateDirection = RotateDirection.CLOCKWISE;
        Mockito.when(doorType.isValidOpenDirection(rotateDirection)).thenReturn(true);

        Assertions.assertTrue(SetOpenDirection.run(commandSender, doorRetriever, rotateDirection)
                                              .get(1, TimeUnit.SECONDS));

        Mockito.verify(door).setOpenDir(rotateDirection);
        Mockito.verify(door).syncData();
    }

    @Test
    @SneakyThrows
    void testDelayedInput()
    {
        final @NotNull RotateDirection rotateDirection = RotateDirection.CLOCKWISE;
        Mockito.when(doorType.isValidOpenDirection(rotateDirection)).thenReturn(true);
        Mockito.when(platform.getLocalizer()).thenReturn(Mockito.mock(ILocalizer.class));

        val first = SetOpenDirection.runDelayed(commandSender, doorRetriever);
        val second = SetOpenDirection.provideDelayedInput(commandSender, rotateDirection);

        Assertions.assertTrue(first.get(1, TimeUnit.SECONDS));
        Assertions.assertEquals(first, second);

        Mockito.verify(door).setOpenDir(rotateDirection);
        Mockito.verify(door).syncData();
    }
}
