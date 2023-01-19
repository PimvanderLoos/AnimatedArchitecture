package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetrieverFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Timeout(1)
class ListMovablesTest
{
    private List<AbstractMovable> movables;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer playerCommandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPServer serverCommandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private ListMovables.IFactory factory;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        final int size = 3;
        movables = new ArrayList<>(size);
        for (int idx = 0; idx < size; ++idx)
            movables.add(Mockito.mock(AbstractMovable.class));

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newListMovables(Mockito.any(ICommandSender.class),
                                             Mockito.any(MovableRetriever.class)))
               .thenAnswer(invoc -> new ListMovables(invoc.getArgument(0, ICommandSender.class), localizer,
                                                     ITextFactory.getSimpleTextFactory(),
                                                     invoc.getArgument(1, MovableRetriever.class)));
    }

    @Test
    void testBypass()
    {
        MovableRetriever retriever = MovableRetrieverFactory.ofMovables(movables);

        // No movables will be found, because the command sender is not an owner of any them.
        CommandTestingUtil.initCommandSenderPermissions(playerCommandSender, true, false);
        Assertions.assertDoesNotThrow(
            () -> factory.newListMovables(playerCommandSender, retriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(playerCommandSender)
               .sendMessage(UnitTestUtil.toText("commands.list_movables.error.no_movables_found"));

        // Run it again, but now do so with admin permissions enabled.
        // As a result, we should NOT get the "No movables found!" message again.
        CommandTestingUtil.initCommandSenderPermissions(playerCommandSender, true, true);
        Assertions.assertDoesNotThrow(
            () -> factory.newListMovables(playerCommandSender, retriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(playerCommandSender)
               .sendMessage(UnitTestUtil.toText("commands.list_movables.error.no_movables_found"));


        Assertions.assertDoesNotThrow(
            () -> factory.newListMovables(serverCommandSender, retriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(serverCommandSender, Mockito.never())
               .sendMessage(UnitTestUtil.toText("commands.list_movables.error.no_movables_found"));
    }
}
