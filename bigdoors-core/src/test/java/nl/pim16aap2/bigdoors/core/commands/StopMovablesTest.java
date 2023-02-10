package nl.pim16aap2.bigdoors.core.commands;

import nl.pim16aap2.bigdoors.core.UnitTestUtil;
import nl.pim16aap2.bigdoors.core.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.core.localization.ILocalizer;
import nl.pim16aap2.bigdoors.core.moveblocks.StructureActivityManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

@Timeout(1)
class StopStructuresTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IServer commandSender;

    @Mock
    private StructureActivityManager structureActivityManager;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private StopStructures.IFactory factory;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, true);

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newStopStructures(Mockito.any(ICommandSender.class)))
               .thenAnswer(invoc -> new StopStructures(invoc.getArgument(0, ICommandSender.class),
                                                       localizer, ITextFactory.getSimpleTextFactory(),
                                                       structureActivityManager));
    }

    @Test
    void test()
    {
        Assertions.assertDoesNotThrow(() -> factory.newStopStructures(commandSender).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(structureActivityManager).abortAnimators();
    }
}
