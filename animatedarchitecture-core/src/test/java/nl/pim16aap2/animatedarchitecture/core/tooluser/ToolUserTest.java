package nl.pim16aap2.animatedarchitecture.core.tooluser;

import nl.altindag.log.LogCaptor;
import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.animation.StructureActivityManager;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitectureToolUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IEconomyManager;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.IProtectionHookManager;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.commands.CommandFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.PersonalizedLocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.managers.LimitsManager;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAnimationRequestBuilder;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureBuilder;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.AsyncStepExecutor;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutor;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutorBoolean;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutorVoid;
import nl.pim16aap2.testing.AssistedFactoryMocker;
import nl.pim16aap2.util.reflection.ReflectionBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Timeout(10)
public class ToolUserTest
{
    @Mock
    private IPlayer player;

    private final PersonalizedLocalizer personalizedLocalizer = UnitTestUtil.DUMMY_PERSONALIZED_LOCALIZER;

    @Mock
    private ToolUserManager toolUserManager;

    private Step.Factory.IFactory stepFactory;

    private ToolUser.Context context;

    @BeforeAll
    static void beforeAll()
    {
        // Set the log level to INFO to reduce the spammy output.
        LogCaptor.forClass(ToolUser.class).setLogLevelToInfo();
    }

    @BeforeEach
    void init()
        throws NoSuchMethodException
    {
        when(player.newText()).thenReturn(ITextFactory.getSimpleTextFactory().newText());

        stepFactory = new AssistedFactoryMocker<>(
            Step.Factory.class,
            Step.Factory.IFactory.class,
            CALLS_REAL_METHODS
        ).getFactory();

        context = new ToolUser.Context(
            mock(StructureBuilder.class),
            toolUserManager,
            mock(DatabaseManager.class),
            mock(LimitsManager.class),
            mock(IEconomyManager.class),
            mock(IProtectionHookManager.class),
            mock(IAnimatedArchitectureToolUtil.class),
            mock(StructureAnimationRequestBuilder.class),
            mock(StructureActivityManager.class),
            mock(CommandFactory.class),
            stepFactory
        );
    }

    @Test
    void testAsyncProcedure()
        throws InstantiationException
    {
        final int stepCount = 500;
        final List<Step> steps = new ArrayList<>(stepCount);

        final TestToolUser toolUser = new TestToolUser(context, player);

        // Generate `stepCount` steps with incrementing values.
        // Each step appends its index to the tool user's list of values.
        // We mix sync and async steps to test the async functionality. Every 5th step is sync.
        for (int idx = 0; idx < stepCount; idx++)
        {
            final int value = idx;

            final boolean isAsync = idx % 5 != 0; // Every 5th step is sync.
            final String stepName = "Step_" + value + (isAsync ? " (async)" : " (sync)");

            final StepExecutor stepExecutorSupplier =
                isAsync ?
                    new AsyncStepExecutor<>(Boolean.class, ignored -> toolUser.appendValueAsync(value)) :
                    new StepExecutorBoolean(ignored -> toolUser.appendValue(value));

            steps.add(createStep(personalizedLocalizer, stepFactory, stepName, stepExecutorSupplier));
        }

        setProcedure(toolUser, steps);

        // Execute all steps.
        for (int idx = 0; idx < stepCount; idx++)
            Assertions.assertTrue(toolUser.handleInput(false).join());

        // Verify that all the values in the tool user are present and in the correct order.
        final List<Integer> values = toolUser.getValues();
        Assertions.assertEquals(stepCount, values.size());
        for (int idx = 0; idx < stepCount; idx++)
            Assertions.assertEquals(idx, values.get(idx));
    }

    @Test
    @SuppressWarnings("FutureReturnValueIgnored")
    void testAsyncInputs()
        throws InstantiationException, InterruptedException
    {
        final var toolUser = new TestToolUser(context, player);

        final var steps = List.of(
            createStep(
                personalizedLocalizer,
                stepFactory,
                "step_1",
                new AsyncStepExecutor<>(
                    Integer.class, ignored ->
                {
                    sleep(500);
                    return toolUser.appendValueAsync(1);
                })),
            createStep(
                personalizedLocalizer,
                stepFactory,
                "step_2",
                new AsyncStepExecutor<>(Integer.class, ignored -> toolUser.appendValueAsync(2))),
            createStep(
                personalizedLocalizer,
                stepFactory,
                "step_3",
                new AsyncStepExecutor<>(Integer.class, ignored -> toolUser.appendValueAsync(3)))
        );

        setProcedure(toolUser, steps);

        final var countDownLatch = new CountDownLatch(3);

        for (int idx = 0; idx < 3; idx++)
            toolUser.handleInput(idx).thenAccept(ignored -> countDownLatch.countDown());

        countDownLatch.await();
        Assertions.assertEquals(List.of(1, 2, 3), toolUser.getValues());
    }

    private static void sleep(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static Step createStep(
        PersonalizedLocalizer localizer,
        Step.Factory.IFactory stepFactory,
        String name,
        StepExecutor stepExecutor)
        throws InstantiationException
    {
        return stepFactory
            .stepName(localizer, name)
            .stepExecutor(stepExecutor)
            .textSupplier(text -> text.append(name))
            .construct();
    }

    private void setProcedure(ToolUser toolUser, List<Step> steps)
    {
        final var newProcedure = new Procedure(steps, player);

        ReflectionBuilder
            .findField()
            .inClass(ToolUser.class)
            .withName("procedure")
            .setAccessible()
            .set(toolUser, newProcedure);
    }

    private static class TestToolUser extends ToolUser
    {
        private final List<Integer> values = Collections.synchronizedList(new ArrayList<>());

        public TestToolUser(Context context, IPlayer player)
        {
            super(context, player);
            init();
        }

        @Override
        protected List<Step> generateSteps()
            throws InstantiationException
        {
            // Add a dummy step to prevent the procedure from being empty.
            return List.of(createStep(localizer, stepFactory, "Step_default", new StepExecutorVoid(() -> false)));
        }

        public synchronized CompletableFuture<Boolean> appendValueAsync(Integer value)
        {
            return CompletableFuture.supplyAsync(() -> appendValue(value), Executors.newVirtualThreadPerTaskExecutor());
        }

        public synchronized boolean appendValue(Integer value)
        {
            values.add(value);
            return true;
        }

        public synchronized List<Integer> getValues()
        {
            return new ArrayList<>(values);
        }
    }
}
