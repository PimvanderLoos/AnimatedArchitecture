package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.moveblocks.DoorActivityManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;


class StopDoorsTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPServer commandSender;

    @Mock
    private DoorActivityManager doorActivityManager;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private StopDoors.IFactory factory;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);
        UnitTestUtil.redirectSendMessageText(commandSender);

        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, true);

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newStopDoors(Mockito.any(ICommandSender.class)))
               .thenAnswer(invoc -> new StopDoors(invoc.getArgument(0, ICommandSender.class),
                                                  localizer, ITextFactory.getSimpleTextFactory(), doorActivityManager));
    }

    @Test
    @SneakyThrows
    void test()
    {
        Assertions.assertTrue(factory.newStopDoors(commandSender).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(doorActivityManager).stopDoors();
    }
}
