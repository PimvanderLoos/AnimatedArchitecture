package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationType;
import nl.pim16aap2.animatedarchitecture.core.api.IConfig;
import nl.pim16aap2.animatedarchitecture.core.api.IMessageable;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IPlayerFactory;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionCause;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionType;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAnimationRequest;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAnimationRequestBuilder;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureToggleResult;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Timeout(1)
class ToggleTest
{
    private StructureRetriever structureRetriever;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer commandSender;

    @Mock
    private AbstractStructure structure;

    @Mock
    private StructureType structureType;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private StructureAnimationRequest.IFactory structureToggleRequestFactory;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private Toggle.IFactory factory;

    private StructureAnimationRequestBuilder structureToggleRequestBuilder;

    @Mock
    private IMessageable messageableServer;

    @Mock
    private StructureAnimationRequest structureToggleRequest;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, true);

        Mockito.when(structureType.getLocalizationKey()).thenReturn("StructureType");

        Mockito.when(structure.isOwner(Mockito.any(UUID.class), Mockito.any())).thenReturn(true);
        Mockito.when(structure.isOwner(Mockito.any(IPlayer.class), Mockito.any())).thenReturn(true);
        Mockito.when(structure.getType()).thenReturn(structureType);

        structureRetriever = StructureRetrieverFactory.ofStructure(structure);

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(structureToggleRequest.execute())
               .thenReturn(CompletableFuture.completedFuture(StructureToggleResult.SUCCESS));

        Mockito.when(structureToggleRequestFactory.create(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                                                          Mockito.nullable(Double.class), Mockito.anyBoolean(),
                                                          Mockito.anyBoolean(), Mockito.any(), Mockito.any()))
               .thenReturn(structureToggleRequest);

        structureToggleRequestBuilder = new StructureAnimationRequestBuilder(
            structureToggleRequestFactory, messageableServer, Mockito.mock(IPlayerFactory.class),
            Mockito.mock(IConfig.class));

        Mockito.when(factory.newToggle(Mockito.any(ICommandSender.class), Mockito.any(StructureActionType.class),
                                       Mockito.any(AnimationType.class), Mockito.nullable(Double.class),
                                       Mockito.anyBoolean(), Mockito.any(StructureRetriever[].class)))
               .thenAnswer(
                   invoc ->
                   {
                       final StructureRetriever[] retrievers =
                           UnitTestUtil.arrayFromCapturedVarArgs(StructureRetriever.class, invoc, 5);
                       return new Toggle(
                           localizer,
                           ITextFactory.getSimpleTextFactory(),
                           messageableServer,
                           structureToggleRequestBuilder,
                           invoc.getArgument(0, ICommandSender.class),
                           invoc.getArgument(1, StructureActionType.class),
                           invoc.getArgument(2, AnimationType.class),
                           invoc.getArgument(3, Double.class),
                           invoc.getArgument(4, Boolean.class),
                           retrievers);
                   });
    }

    @Test
    void testSuccess()
        throws Exception
    {
        final Toggle toggle =
            factory.newToggle(commandSender, Toggle.DEFAULT_STRUCTURE_ACTION_TYPE,
                              Toggle.DEFAULT_ANIMATION_TYPE, structureRetriever);

        toggle.executeCommand(new PermissionsStatus(true, true)).get(1, TimeUnit.SECONDS);
        Mockito.verify(structureToggleRequestFactory)
               .create(structureRetriever, StructureActionCause.PLAYER, commandSender, commandSender, null, false,
                       true, StructureActionType.TOGGLE, AnimationType.MOVE_BLOCKS);

        Mockito.when(structure.getOwner(commandSender))
               .thenReturn(Optional.of(CommandTestingUtil.structureOwnerCreator));
        toggle.executeCommand(new PermissionsStatus(true, false)).get(1, TimeUnit.SECONDS);
        Mockito.verify(structureToggleRequestFactory, Mockito.times(2))
               .create(structureRetriever, StructureActionCause.PLAYER,
                       commandSender, commandSender,
                       null, false, true, StructureActionType.TOGGLE, AnimationType.MOVE_BLOCKS);
    }

    @Test
    void testExecution()
        throws Exception
    {
        // Ensure that supplying multiple structure retrievers properly attempts toggling all of them.
        final int count = 10;
        final StructureRetriever[] retrievers = new StructureRetriever[count];
        for (int idx = 0; idx < count; ++idx)
        {
            final StructureType type = Mockito.mock(StructureType.class);
            Mockito.when(type.getLocalizationKey()).thenReturn("StructureType" + idx);

            final AbstractStructure newStructure = Mockito.mock(AbstractStructure.class);
            Mockito.when(newStructure.isOwner(Mockito.any(UUID.class), Mockito.any())).thenReturn(true);
            Mockito.when(newStructure.isOwner(Mockito.any(IPlayer.class), Mockito.any())).thenReturn(true);
            Mockito.when(newStructure.getType()).thenReturn(type);

            retrievers[idx] = StructureRetrieverFactory.ofStructure(newStructure);
        }

        final Toggle toggle =
            factory.newToggle(commandSender, Toggle.DEFAULT_STRUCTURE_ACTION_TYPE,
                              Toggle.DEFAULT_ANIMATION_TYPE, null, false, retrievers);

        toggle.executeCommand(new PermissionsStatus(true, true)).get(1, TimeUnit.SECONDS);

        final Set<StructureRetriever> toggledStructures =
            Mockito.mockingDetails(structureToggleRequestFactory).getInvocations().stream()
                   .<StructureRetriever>map(invocation -> invocation.getArgument(0))
                   .collect(Collectors.toSet());

        Assertions.assertEquals(count, toggledStructures.size());
        for (int idx = 0; idx < count; ++idx)
            Assertions.assertTrue(toggledStructures.contains(retrievers[idx]));
    }

    @Test
    void testParameters()
    {
        Mockito.when(structure.isCloseable()).thenReturn(true);
        Mockito.when(structure.isOpenable()).thenReturn(true);

        Assertions.assertDoesNotThrow(
            () -> factory.newToggle(commandSender, structureRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(structureToggleRequestFactory, Mockito.times(1))
               .create(structureRetriever, StructureActionCause.PLAYER, commandSender, commandSender,
                       null, false, true, StructureActionType.TOGGLE, AnimationType.MOVE_BLOCKS);


        Assertions.assertDoesNotThrow(
            () -> factory.newToggle(commandSender, StructureActionType.TOGGLE,
                                    AnimationType.MOVE_BLOCKS, 3.141592653589793D, true, structureRetriever).run()
                         .get(1, TimeUnit.SECONDS));
        Mockito.verify(structureToggleRequestFactory, Mockito.times(1))
               .create(structureRetriever, StructureActionCause.PLAYER, commandSender, commandSender,
                       3.141592653589793D, false, true, StructureActionType.TOGGLE, AnimationType.MOVE_BLOCKS);


        Assertions.assertDoesNotThrow(
            () -> factory.newToggle(commandSender, StructureActionType.CLOSE,
                                    AnimationType.MOVE_BLOCKS, structureRetriever)
                         .run().get(1, TimeUnit.SECONDS));
        Mockito.verify(structureToggleRequestFactory, Mockito.times(1))
               .create(structureRetriever, StructureActionCause.PLAYER, commandSender, commandSender,
                       null, false, true, StructureActionType.CLOSE, AnimationType.MOVE_BLOCKS);


        Assertions.assertDoesNotThrow(
            () -> factory.newToggle(commandSender, StructureActionType.OPEN,
                                    AnimationType.MOVE_BLOCKS, 42D, true, structureRetriever).run()
                         .get(1, TimeUnit.SECONDS));
        Mockito.verify(structureToggleRequestFactory, Mockito.times(1))
               .create(structureRetriever, StructureActionCause.PLAYER, commandSender,
                       commandSender, 42D, false, true, StructureActionType.OPEN, AnimationType.MOVE_BLOCKS);
    }

    @Test
    void testServerCommandSender()
    {
        final IServer serverCommandSender = Mockito.mock(IServer.class, Answers.CALLS_REAL_METHODS);
        Assertions.assertDoesNotThrow(
            () -> factory.newToggle(
                             serverCommandSender, StructureActionType.TOGGLE,
                             AnimationType.PREVIEW, structureRetriever).run()
                         .get(1, TimeUnit.SECONDS));
        Mockito.verify(structureToggleRequestFactory, Mockito.times(1))
               .create(structureRetriever, StructureActionCause.SERVER, messageableServer, null,
                       null, false, true, StructureActionType.TOGGLE, AnimationType.PREVIEW);
    }

    private void verifyNoOpenerCalls()
    {
        Mockito.verify(structureToggleRequestFactory, Mockito.never())
               .create(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                       Mockito.anyDouble(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any(), Mockito.any());
    }

    @Test
    void testAbort()
    {
        Mockito.when(structure.isCloseable()).thenReturn(false);

        Assertions.assertDoesNotThrow(
            () -> factory.newToggle(commandSender, StructureActionType.CLOSE,
                                    AnimationType.MOVE_BLOCKS, structureRetriever).run()
                         .get(1, TimeUnit.SECONDS));
        verifyNoOpenerCalls();

        Mockito.when(structure.isCloseable()).thenReturn(true);
        CommandTestingUtil.initCommandSenderPermissions(commandSender, false, false);
        Mockito.when(structure.getOwner(Mockito.any(IPlayer.class))).thenReturn(Optional.empty());

        Assertions.assertDoesNotThrow(
            () -> factory.newToggle(commandSender, StructureActionType.CLOSE, AnimationType.MOVE_BLOCKS,
                                    structureRetriever)
                         .run().get(1, TimeUnit.SECONDS));
        verifyNoOpenerCalls();

        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, false);
        Assertions.assertDoesNotThrow(
            () -> factory.newToggle(commandSender, StructureActionType.CLOSE, AnimationType.MOVE_BLOCKS,
                                    structureRetriever)
                         .run().get(1, TimeUnit.SECONDS));
        verifyNoOpenerCalls();
    }
}
