package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionCause;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionType;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableToggleRequest;
import nl.pim16aap2.bigdoors.movable.MovableToggleRequestBuilder;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetrieverFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initCommandSenderPermissions;

class ToggleTest
{
    private MovableRetriever movableRetriever;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock
    private AbstractMovable movable;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private MovableToggleRequest.IFactory movableToggleRequestFactory;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private Toggle.IFactory factory;

    private MovableToggleRequestBuilder movableToggleRequestBuilder;

    @Mock
    private IMessageable messageableServer;

    @Mock
    private MovableToggleRequest movableToggleRequest;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        initCommandSenderPermissions(commandSender, true, true);

        Mockito.when(movable.isOwner(Mockito.any(UUID.class))).thenReturn(true);
        Mockito.when(movable.isOwner(Mockito.any(IPPlayer.class))).thenReturn(true);

        movableRetriever = MovableRetrieverFactory.ofMovable(movable);

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(movableToggleRequestFactory.create(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                                                        Mockito.nullable(Double.class), Mockito.anyBoolean(),
                                                        Mockito.any()))
               .thenReturn(movableToggleRequest);

        movableToggleRequestBuilder = new MovableToggleRequestBuilder(movableToggleRequestFactory, messageableServer,
                                                                      Mockito.mock(IPPlayerFactory.class));

        Mockito.when(factory.newToggle(Mockito.any(ICommandSender.class), Mockito.any(MovableActionType.class),
                                       Mockito.nullable(Double.class), Mockito.any()))
               .thenAnswer(
                   invoc ->
                   {
                       final MovableRetriever[] retrievers =
                           UnitTestUtil.arrayFromCapturedVarArgs(MovableRetriever.class, invoc, 3);

                       return new Toggle(invoc.getArgument(0, ICommandSender.class), localizer,
                                         ITextFactory.getSimpleTextFactory(),
                                         invoc.getArgument(1, MovableActionType.class),
                                         invoc.getArgument(2, Double.class), movableToggleRequestBuilder,
                                         messageableServer, retrievers);
                   });
    }

    @Test
    void testSuccess()
        throws Exception
    {
        final Toggle toggle = factory.newToggle(commandSender, Toggle.DEFAULT_MOVABLE_ACTION_TYPE,
                                                (Double) null, movableRetriever);
        toggle.executeCommand(new PermissionsStatus(true, true)).get(1, TimeUnit.SECONDS);
        Mockito.verify(movableToggleRequestFactory).create(movableRetriever, MovableActionCause.PLAYER, commandSender,
                                                           commandSender, null, false, MovableActionType.TOGGLE);

        Mockito.when(movable.getOwner(commandSender))
               .thenReturn(Optional.of(CommandTestingUtil.movableOwnerCreator));
        toggle.executeCommand(new PermissionsStatus(true, false)).get(1, TimeUnit.SECONDS);
        Mockito.verify(movableToggleRequestFactory, Mockito.times(2))
               .create(movableRetriever, MovableActionCause.PLAYER,
                       commandSender, commandSender,
                       null, false, MovableActionType.TOGGLE);
    }

    @Test
    void testExecution()
        throws Exception
    {
        // Ensure that supplying multiple movable retrievers properly attempts toggling all of them.
        final int count = 10;
        final MovableRetriever[] retrievers = new MovableRetriever[count];
        for (int idx = 0; idx < count; ++idx)
        {
            final AbstractMovable newMovable = Mockito.mock(AbstractMovable.class);
            Mockito.when(newMovable.isOwner(Mockito.any(UUID.class))).thenReturn(true);
            Mockito.when(newMovable.isOwner(Mockito.any(IPPlayer.class))).thenReturn(true);
            retrievers[idx] = MovableRetrieverFactory.ofMovable(newMovable);
        }

        final Toggle toggle = factory.newToggle(commandSender, Toggle.DEFAULT_MOVABLE_ACTION_TYPE,
                                                null, retrievers);
        toggle.executeCommand(new PermissionsStatus(true, true)).get(1, TimeUnit.SECONDS);

        final Set<MovableRetriever> toggledMovables =
            Mockito.mockingDetails(movableToggleRequestFactory).getInvocations().stream()
                   .<MovableRetriever>map(invocation -> invocation.getArgument(0))
                   .collect(Collectors.toSet());

        Assertions.assertEquals(count, toggledMovables.size());
        for (int idx = 0; idx < count; ++idx)
            Assertions.assertTrue(toggledMovables.contains(retrievers[idx]));
    }

    @Test
    void testParameters()
        throws Exception
    {
        Mockito.when(movable.isCloseable()).thenReturn(true);
        Mockito.when(movable.isOpenable()).thenReturn(true);

        Assertions.assertTrue(factory.newToggle(commandSender, movableRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(movableToggleRequestFactory, Mockito.times(1))
               .create(movableRetriever, MovableActionCause.PLAYER, commandSender, commandSender,
                       null, false, MovableActionType.TOGGLE);


        Assertions.assertTrue(factory.newToggle(commandSender, 3.141592653589793D, movableRetriever).run()
                                     .get(1, TimeUnit.SECONDS));
        Mockito.verify(movableToggleRequestFactory, Mockito.times(1))
               .create(movableRetriever, MovableActionCause.PLAYER, commandSender, commandSender,
                       3.141592653589793D, false, MovableActionType.TOGGLE);


        Assertions.assertTrue(
            factory.newToggle(commandSender, MovableActionType.CLOSE, movableRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(movableToggleRequestFactory, Mockito.times(1))
               .create(movableRetriever, MovableActionCause.PLAYER, commandSender, commandSender,
                       null, false, MovableActionType.CLOSE);


        Assertions.assertTrue(factory.newToggle(commandSender, MovableActionType.OPEN, 42D, movableRetriever).run()
                                     .get(1, TimeUnit.SECONDS));
        Mockito.verify(movableToggleRequestFactory, Mockito.times(1))
               .create(movableRetriever, MovableActionCause.PLAYER, commandSender,
                       commandSender, 42D, false, MovableActionType.OPEN);
    }

    @Test
    void testServerCommandSender()
        throws Exception
    {
        final IPServer serverCommandSender = Mockito.mock(IPServer.class, Answers.CALLS_REAL_METHODS);
        Assertions.assertTrue(factory.newToggle(serverCommandSender, MovableActionType.TOGGLE, movableRetriever).run()
                                     .get(1, TimeUnit.SECONDS));
        Mockito.verify(movableToggleRequestFactory, Mockito.times(1))
               .create(movableRetriever, MovableActionCause.SERVER, messageableServer, null,
                       null, false, MovableActionType.TOGGLE);
    }

    private void verifyNoOpenerCalls()
    {
        Mockito.verify(movableToggleRequestFactory, Mockito.never())
               .create(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                       Mockito.anyDouble(), Mockito.anyBoolean(), Mockito.any());
    }

    @Test
    void testAbort()
        throws Exception
    {
        Mockito.when(movable.isCloseable()).thenReturn(false);

        Assertions.assertTrue(factory.newToggle(commandSender, MovableActionType.CLOSE, movableRetriever).run()
                                     .get(1, TimeUnit.SECONDS));
        verifyNoOpenerCalls();

        Mockito.when(movable.isCloseable()).thenReturn(true);
        initCommandSenderPermissions(commandSender, false, false);
        Mockito.when(movable.getOwner(Mockito.any(IPPlayer.class))).thenReturn(Optional.empty());

        Assertions.assertTrue(factory.newToggle(commandSender, MovableActionType.CLOSE, movableRetriever).run()
                                     .get(1, TimeUnit.SECONDS));
        verifyNoOpenerCalls();

        initCommandSenderPermissions(commandSender, true, false);
        Assertions.assertTrue(factory.newToggle(commandSender, MovableActionType.CLOSE, movableRetriever).run()
                                     .get(1, TimeUnit.SECONDS));
        verifyNoOpenerCalls();
    }
}
