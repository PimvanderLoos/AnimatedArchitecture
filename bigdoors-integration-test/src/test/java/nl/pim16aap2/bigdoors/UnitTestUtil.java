package nl.pim16aap2.bigdoors;

import lombok.experimental.UtilityClass;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IRestartableHolder;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DoorRegistry;
import nl.pim16aap2.bigdoors.storage.IStorage;
import nl.pim16aap2.bigdoors.testimplementations.TestConfigLoader;
import nl.pim16aap2.bigdoors.testimplementations.TestMessagingInterface;
import nl.pim16aap2.bigdoors.testimplementations.TestPlatform;
import nl.pim16aap2.bigdoors.util.messages.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

@UtilityClass
public class UnitTestUtil
{
    private boolean isInitialized = false;

    @NotNull
    public final IConfigLoader CONFIG = new TestConfigLoader();
    @NotNull
    public final File TEST_RESOURCE_FOLDER = new File("src/test/resources");
    @NotNull
    public final TestPlatform PLATFORM = new TestPlatform();
    @NotNull
    public String TEST_DIR;

    public final double EPSILON = 1E-6;

    static
    {
        BigDoors.get().setMessagingInterface(new TestMessagingInterface());
        try
        {
            // This will create a fake database manager that can be used for testing.
            Constructor<DatabaseManager> ctor = DatabaseManager.class.getDeclaredConstructor(IRestartableHolder.class);
            ctor.setAccessible(true);
            ctor.newInstance(PLATFORM);
        }
        catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e)
        {
            e.printStackTrace();
            Assertions.fail();
        }

        try
        {
            TEST_DIR = PLATFORM.getDataDirectory().getCanonicalPath() + "/tests";
            System.out.println("test_dir = " + TEST_DIR);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        Assertions.assertNotNull(TEST_DIR);
    }

    public void setDatabaseStorage(final @Nullable IStorage storage)
        throws NoSuchFieldException, IllegalAccessException
    {
        Field field = DatabaseManager.class.getDeclaredField("db");
        field.setAccessible(true);
        field.set(DatabaseManager.get(), storage);
    }

    public ExecutorService getDatabaseManagerThreadPool()
        throws NoSuchFieldException, IllegalAccessException
    {
        Field field = DatabaseManager.class.getDeclaredField("threadPool");
        field.setAccessible(true);
        return (ExecutorService) field.get(DatabaseManager.get());
    }

    public void setupStatic()
        throws NoSuchFieldException, IllegalAccessException
    {
        if (isInitialized)
            return;

        setFakeDoorRegistry();

        BigDoors.get().setBigDoorsPlatform(PLATFORM);
        System.out.println("TEST_RESOURCE_FOLDER = " + TEST_RESOURCE_FOLDER.getAbsolutePath());
        PLATFORM.setMessages(
            new Messages(PLATFORM, new File(TEST_RESOURCE_FOLDER.getAbsolutePath()), "en_US_TEST",
                         BigDoors.get().getPLogger()));
        isInitialized = true;
    }

    /**
     * Stubs the map for the {@link DoorRegistry}. No entries can be added to it or retrieved from it after this method
     * has been called.
     */
    public void setFakeDoorRegistry()
    {
        DoorRegistry.get().init(0, 1, 0, Duration.ofMillis(-1), false);
    }

    /**
     * Checks if an object and an Optional are the same or if they both don't exist/are null.
     *
     * @param obj The object to compare the optional to.
     * @param opt The Optional to compare against the object.
     * @param <T> The type of the Object and Optional.
     * @return The object inside the Optional.
     */
    public <T> T optionalEquals(final @Nullable T obj, final @NotNull Optional<T> opt)
    {
        if (obj == null)
        {
            Assertions.assertTrue(opt.isEmpty());
            return null;
        }
        Assertions.assertTrue(opt.isPresent());
        Assertions.assertEquals(obj, opt.get());
        return opt.get();
    }

    public <T extends Throwable> void assertWrappedThrows(Class<T> expectedType, Executable executable)
    {
        assertWrappedThrows(expectedType, executable, false);
    }

    public <T extends Throwable> void assertWrappedThrows(Class<T> expectedType, Executable executable,
                                                          boolean deepSearch)
    {
        RuntimeException rte = Assertions.assertThrows(RuntimeException.class, executable);
        if (deepSearch)
            while (rte.getCause().getClass() == RuntimeException.class)
                rte = (RuntimeException) rte.getCause();
        Assertions.assertEquals(expectedType, rte.getCause().getClass(), expectedType.toString());
    }
}
