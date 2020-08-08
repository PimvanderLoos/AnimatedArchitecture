package nl.pim16aap2.bigdoors.tooluser.creator;

import junit.framework.Assert;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.BigDoor;
import nl.pim16aap2.bigdoors.testimplementations.TestPPlayer;
import nl.pim16aap2.bigdoors.tooluser.step.Step;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class BigDoorCreatorTest
{
    private static final IPPlayer PLAYER =
        new TestPPlayer(UUID.fromString("f373bb8d-dd2d-496e-a9c5-f9a0c45b2db5"), "user");

    // Set up basic stuff.
    @BeforeAll
    public static void basicSetup()
    {
        UnitTestUtil.setupStatic();
        PLogger.get().setConsoleLogging(true);
        PLogger.get().setOnlyLogExceptions(true);
    }

    @Test
    public void testCreation()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        final @NotNull BigDoorCreator bdc = new BigDoorCreator(PLAYER);
        final @NotNull Vector3Di min = new Vector3Di(10, 15, 20);
        final @NotNull Vector3Di max = new Vector3Di(20, 25, 30);
        final @NotNull Vector3Di engine = new Vector3Di(15, 15, 25);
        final @NotNull Vector3Di powerblock = new Vector3Di(40, 40, 40);
        final @NotNull String doorName = "testDoor";
        final @NotNull IPWorld world =
            UnitTestUtil.PLATFORM.getPWorldFactory().create(UUID.fromString("f373bb8d-dd2d-496e-a9c5-f9a0c45b2db8"));
        final @NotNull IPWorld world2 =
            UnitTestUtil.PLATFORM.getPWorldFactory().create(UUID.fromString("9ba0de97-01ef-4b4f-b12c-025ff84a6931"));
        final @NotNull RotateDirection openDirection = RotateDirection.COUNTERCLOCKWISE;

        final @NotNull DoorOwner doorOwner = new DoorOwner(-1, 0, PLAYER);
        final @NotNull AbstractDoorBase.DoorData doorData
            = new AbstractDoorBase.DoorData(-1, doorName, min, max, engine, powerblock, world, false, openDirection,
                                            doorOwner, false);

        final @NotNull BigDoor bigDoor = new BigDoor(doorData);


        final @NotNull Optional<Step> step = bdc.getCurrentStep();
        Assert.assertTrue(step.isPresent());
        Assert.assertEquals("CREATOR_BIGDOOR_INIT", bdc.getStepMessage(step.get()));

        Assert.assertTrue(bdc.handleInput(doorName));
        Assert.assertFalse(bdc.handleConfirm());
        Assert.assertTrue(bdc.handleInput(min.toLocation(world)));
        Assert.assertTrue(bdc.handleInput(max.toLocation(world)));
        Assert.assertTrue(bdc.handleInput(engine.toLocation(world)));
        Assert.assertFalse(bdc.handleInput(engine.toLocation(world)));
        Assert.assertFalse(bdc.handleInput(powerblock.toLocation(world2)));
        Assert.assertTrue(bdc.handleInput(powerblock.toLocation(world)));
        Assert.assertFalse(bdc.handleInput("3"));
        Assert.assertFalse(bdc.handleInput(RotateDirection.NONE.name()));
        Assert.assertTrue(bdc.handleInput(openDirection.name()));

        Method method = BigDoorCreator.class.getDeclaredMethod("constructDoor");
        method.setAccessible(true);
        final @Nullable AbstractDoorBase doorResult = (AbstractDoorBase) method.invoke(bdc);

        Assert.assertNotNull(doorResult);
        Assert.assertEquals(bigDoor, doorResult);
    }
}
