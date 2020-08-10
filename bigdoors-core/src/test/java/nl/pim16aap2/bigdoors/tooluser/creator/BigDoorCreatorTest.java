package nl.pim16aap2.bigdoors.tooluser.creator;

import junit.framework.Assert;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.BigDoor;
import nl.pim16aap2.bigdoors.storage.IStorage;
import nl.pim16aap2.bigdoors.testimplementations.TestEconomyManager;
import nl.pim16aap2.bigdoors.testimplementations.TestPPlayer;
import nl.pim16aap2.bigdoors.tooluser.step.Step;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(MockitoExtension.class)
class BigDoorCreatorTest
{
    private static final IPPlayer PLAYER =
        new TestPPlayer(UUID.fromString("f373bb8d-dd2d-496e-a9c5-f9a0c45b2db5"), "user");
    private static ExecutorService threadPool;

    static
    {
        UnitTestUtil.setupStatic();
    }

    @Mock
    private IStorage fakeStorage;

    @BeforeEach
    public void initMocks()
    {
        MockitoAnnotations.initMocks(this);
    }

    @BeforeEach
    public void setupFakeDatabaseManager()
    {
        try
        {
            UnitTestUtil.setDatabaseStorage(fakeStorage);
            threadPool = UnitTestUtil.getDatabaseManagerThreadPool();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Assert.fail();
        }
        Assert.assertNotNull(threadPool);
    }

    // Set up basic stuff.
    @BeforeAll
    public static void basicSetup()
    {
        PLogger.get().setConsoleLogging(true);
        PLogger.get().setOnlyLogExceptions(true);
    }

    private static final @NotNull Vector3Di min = new Vector3Di(10, 15, 20);
    private static final @NotNull Vector3Di max = new Vector3Di(20, 25, 30);
    private static final @NotNull Vector3Di engine = new Vector3Di(15, 15, 25);
    private static final @NotNull Vector3Di powerblock = new Vector3Di(40, 40, 40);
    private static final @NotNull String doorName = "testDoor";
    private static final @NotNull IPWorld world =
        UnitTestUtil.PLATFORM.getPWorldFactory().create(UUID.fromString("f373bb8d-dd2d-496e-a9c5-f9a0c45b2db8"));
    private static final @NotNull IPWorld world2 =
        UnitTestUtil.PLATFORM.getPWorldFactory().create(UUID.fromString("9ba0de97-01ef-4b4f-b12c-025ff84a6931"));
    private static final @NotNull RotateDirection openDirection = RotateDirection.COUNTERCLOCKWISE;

    private static final @NotNull DoorOwner doorOwner = new DoorOwner(-1, 0, PLAYER);
    private static final @NotNull AbstractDoorBase.DoorData doorData
        = new AbstractDoorBase.DoorData(-1, doorName, min, max, engine, powerblock, world, false, openDirection,
                                        doorOwner, false);
    private static final @NotNull BigDoor actualDoor = new BigDoor(doorData);

    /**
     * Sets up hijacking of any calls to the fake database ({@link #fakeStorage}) to insert a new door.
     *
     * @return The reference to the {@link AbstractDoorBase} that was supposed to be inserted into the database.
     */
    @NotNull
    private AtomicReference<AbstractDoorBase> setupInsertHijack()
    {
        // Door insertion is handled by a thread pool (take a look at the DatabaseManager).
        // When capturing the method call to insert the door, the result will be stored on this AtomicReference,
        // So that when it is compared to the actual door later on, it'll be possible to fail the test if it doesn't
        // match (which isn't possible from a secondary thread).
        final @NotNull AtomicReference<AbstractDoorBase> resultDoorRef = new AtomicReference<>();

        // Capture the call to the fake database that is supposed to insert the door. Then chcck if the door being
        // inserted is the door we will try to create.
        Mockito.when(fakeStorage.insert(Mockito.any())).thenAnswer((Answer<Boolean>) invocation ->
        {
            Object[] args = invocation.getArguments();
            // Yeah, this may throw all kinds of exceptions and whatnot. However, because of it being run on a separate
            // thread, it doesn't matter. It'll either set the variable, or fail to do so.
            // In case of an SQL exception, it'll end up in the log anyway.
            resultDoorRef.set(((BigDoor) args[0]));

            return Boolean.TRUE;
        });

        return resultDoorRef;
    }

    @Test
    public void testFreeCreation()
        throws InterruptedException
    {
        ((TestEconomyManager) UnitTestUtil.PLATFORM.getEconomyManager()).isEconomyEnabled = false;
        final @NotNull AtomicReference<AbstractDoorBase> resultDoorRef = setupInsertHijack();
        final @NotNull BigDoorCreator bdc = new BigDoorCreator(PLAYER);

        final @NotNull Optional<Step> step = bdc.getCurrentStep();
        Assert.assertTrue(step.isPresent());
        Assert.assertEquals("CREATOR_GENERAL_GIVENAME", bdc.getStepMessage(step.get()));

        Assert.assertTrue(bdc.handleInput(doorName));
        Assert.assertFalse(bdc.handleInput(true));
        Assert.assertFalse(bdc.handleInput(false));
        Assert.assertTrue(bdc.handleInput(min.toLocation(world)));
        Assert.assertTrue(bdc.handleInput(max.toLocation(world)));
        Assert.assertTrue(bdc.handleInput(engine.toLocation(world)));
        Assert.assertFalse(bdc.handleInput(engine.toLocation(world)));
        Assert.assertFalse(bdc.handleInput(powerblock.toLocation(world2)));
        Assert.assertTrue(bdc.handleInput(powerblock.toLocation(world)));
        Assert.assertFalse(bdc.handleInput("3"));
        Assert.assertFalse(bdc.handleInput(RotateDirection.NONE.name()));

        // Causes the actual insertion.
        Assert.assertTrue(bdc.handleInput(openDirection.name()));

        // Wait for the thread pool to finish inserting the door etc,
        threadPool.awaitTermination(100L, TimeUnit.MILLISECONDS);
        AbstractDoorBase resultDoor = resultDoorRef.get();
        Assert.assertNotNull(resultDoor);
        Assert.assertEquals(actualDoor, resultDoor);
    }

    @Test
    public void testPriceCreation()
        throws InterruptedException
    {
        ((TestEconomyManager) UnitTestUtil.PLATFORM.getEconomyManager()).price = OptionalDouble.of(10.746D);
        ((TestEconomyManager) UnitTestUtil.PLATFORM.getEconomyManager()).isEconomyEnabled = true;
        ((TestEconomyManager) UnitTestUtil.PLATFORM.getEconomyManager()).buyDoor = true;
        final @NotNull AtomicReference<AbstractDoorBase> resultDoorRef = setupInsertHijack();
        final @NotNull BigDoorCreator bdc = new BigDoorCreator(PLAYER);

        @NotNull Optional<Step> step = bdc.getCurrentStep();
        Assert.assertTrue(step.isPresent());
        Assert.assertEquals("CREATOR_GENERAL_GIVENAME", bdc.getStepMessage(step.get()));

        Assert.assertTrue(bdc.handleInput(doorName));
        Assert.assertFalse(bdc.handleInput(true));
        Assert.assertFalse(bdc.handleInput(false));
        Assert.assertTrue(bdc.handleInput(min.toLocation(world)));
        Assert.assertTrue(bdc.handleInput(max.toLocation(world)));
        Assert.assertTrue(bdc.handleInput(engine.toLocation(world)));
        Assert.assertFalse(bdc.handleInput(engine.toLocation(world)));
        Assert.assertFalse(bdc.handleInput(powerblock.toLocation(world2)));
        Assert.assertTrue(bdc.handleInput(powerblock.toLocation(world)));
        Assert.assertFalse(bdc.handleInput("3"));
        Assert.assertFalse(bdc.handleInput(RotateDirection.NONE.name()));
        Assert.assertTrue(bdc.handleInput(openDirection.name()));

        step = bdc.getCurrentStep();
        Assert.assertTrue(step.isPresent());
        Assert.assertEquals("CREATOR_GENERAL_CONFIRMPRICE10.75", bdc.getStepMessage(step.get()));

        // Causes the actual insertion.
        Assert.assertTrue(bdc.handleInput(true));


        // Wait for the thread pool to finish inserting the door etc,
        threadPool.awaitTermination(100L, TimeUnit.MILLISECONDS);
        AbstractDoorBase resultDoor = resultDoorRef.get();
        Assert.assertNotNull(resultDoor);
        Assert.assertEquals(actualDoor, resultDoor);
    }
}
