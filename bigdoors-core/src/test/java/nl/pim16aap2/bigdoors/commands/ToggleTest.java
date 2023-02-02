package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.events.structureaction.StructureActionCause;
import nl.pim16aap2.bigdoors.events.structureaction.StructureActionType;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.moveblocks.AnimationType;
import nl.pim16aap2.bigdoors.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.structures.StructureToggleRequest;
import nl.pim16aap2.bigdoors.structures.StructureToggleRequestBuilder;
import nl.pim16aap2.bigdoors.structuretypes.StructureType;
import nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever;
import nl.pim16aap2.bigdoors.util.structureretriever.StructureRetrieverFactory;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initCommandSenderPermissions;

@Timeout(1)
class ToggleTest
{
    private StructureRetriever structureRetriever;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock
    private AbstractStructure structure;

    @Mock
    private StructureType structureType;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private StructureToggleRequest.IFactory structureToggleRequestFactory;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private Toggle.IFactory factory;

    private StructureToggleRequestBuilder structureToggleRequestBuilder;

    @Mock
    private IMessageable messageableServer;

    @Mock
    private StructureToggleRequest structureToggleRequest;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        initCommandSenderPermissions(commandSender, true, true);

        Mockito.when(structureType.getLocalizationKey()).thenReturn("StructureType");

        Mockito.when(structure.isOwner(Mockito.any(UUID.class))).thenReturn(true);
        Mockito.when(structure.isOwner(Mockito.any(IPPlayer.class))).thenReturn(true);
        Mockito.when(structure.getType()).thenReturn(structureType);

        structureRetriever = StructureRetrieverFactory.ofStructure(structure);

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(structureToggleRequestFactory.create(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                                                          Mockito.nullable(Double.class), Mockito.anyBoolean(),
                                                          Mockito.any(), Mockito.any()))
               .thenReturn(structureToggleRequest);

        structureToggleRequestBuilder = new StructureToggleRequestBuilder(
            structureToggleRequestFactory, messageableServer, Mockito.mock(IPPlayerFactory.class),
            Mockito.mock(IConfigLoader.class));

        Mockito.when(factory.newToggle(Mockito.any(ICommandSender.class), Mockito.any(StructureActionType.class),
                                       Mockito.any(AnimationType.class), Mockito.nullable(Double.class),
                                       Mockito.any(StructureRetriever[].class)))
               .thenAnswer(
                   invoc ->
                   {
                       final StructureRetriever[] retrievers =
                           UnitTestUtil.arrayFromCapturedVarArgs(StructureRetriever.class, invoc, 4);

                       return new Toggle(invoc.getArgument(0, ICommandSender.class), localizer,
                                         ITextFactory.getSimpleTextFactory(),
                                         invoc.getArgument(1, StructureActionType.class),
                                         invoc.getArgument(2, AnimationType.class),
                                         invoc.getArgument(3, Double.class), structureToggleRequestBuilder,
                                         messageableServer, retrievers);
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
                       StructureActionType.TOGGLE, AnimationType.MOVE_BLOCKS);

        Mockito.when(structure.getOwner(commandSender))
               .thenReturn(Optional.of(CommandTestingUtil.structureOwnerCreator));
        toggle.executeCommand(new PermissionsStatus(true, false)).get(1, TimeUnit.SECONDS);
        Mockito.verify(structureToggleRequestFactory, Mockito.times(2))
               .create(structureRetriever, StructureActionCause.PLAYER,
                       commandSender, commandSender,
                       null, false, StructureActionType.TOGGLE, AnimationType.MOVE_BLOCKS);
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
            Mockito.when(newStructure.isOwner(Mockito.any(UUID.class))).thenReturn(true);
            Mockito.when(newStructure.isOwner(Mockito.any(IPPlayer.class))).thenReturn(true);
            Mockito.when(newStructure.getType()).thenReturn(type);

            retrievers[idx] = StructureRetrieverFactory.ofStructure(newStructure);
        }

        final Toggle toggle =
            factory.newToggle(commandSender, Toggle.DEFAULT_STRUCTURE_ACTION_TYPE,
                              Toggle.DEFAULT_ANIMATION_TYPE, null, retrievers);

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
                       null, false, StructureActionType.TOGGLE, AnimationType.MOVE_BLOCKS);


        Assertions.assertDoesNotThrow(
            () -> factory.newToggle(commandSender, StructureActionType.TOGGLE,
                                    AnimationType.MOVE_BLOCKS, 3.141592653589793D, structureRetriever).run()
                         .get(1, TimeUnit.SECONDS));
        Mockito.verify(structureToggleRequestFactory, Mockito.times(1))
               .create(structureRetriever, StructureActionCause.PLAYER, commandSender, commandSender,
                       3.141592653589793D, false, StructureActionType.TOGGLE, AnimationType.MOVE_BLOCKS);


        Assertions.assertDoesNotThrow(
            () -> factory.newToggle(commandSender, StructureActionType.CLOSE,
                                    AnimationType.MOVE_BLOCKS, structureRetriever)
                         .run().get(1, TimeUnit.SECONDS));
        Mockito.verify(structureToggleRequestFactory, Mockito.times(1))
               .create(structureRetriever, StructureActionCause.PLAYER, commandSender, commandSender,
                       null, false, StructureActionType.CLOSE, AnimationType.MOVE_BLOCKS);


        Assertions.assertDoesNotThrow(
            () -> factory.newToggle(commandSender, StructureActionType.OPEN,
                                    AnimationType.MOVE_BLOCKS, 42D, structureRetriever).run()
                         .get(1, TimeUnit.SECONDS));
        Mockito.verify(structureToggleRequestFactory, Mockito.times(1))
               .create(structureRetriever, StructureActionCause.PLAYER, commandSender,
                       commandSender, 42D, false, StructureActionType.OPEN, AnimationType.MOVE_BLOCKS);
    }

    @Test
    void testServerCommandSender()
    {
        final IPServer serverCommandSender = Mockito.mock(IPServer.class, Answers.CALLS_REAL_METHODS);
        Assertions.assertDoesNotThrow(
            () -> factory.newToggle(
                             serverCommandSender, StructureActionType.TOGGLE,
                             AnimationType.PREVIEW, structureRetriever).run()
                         .get(1, TimeUnit.SECONDS));
        Mockito.verify(structureToggleRequestFactory, Mockito.times(1))
               .create(structureRetriever, StructureActionCause.SERVER, messageableServer, null,
                       null, false, StructureActionType.TOGGLE, AnimationType.PREVIEW);
    }

    private void verifyNoOpenerCalls()
    {
        Mockito.verify(structureToggleRequestFactory, Mockito.never())
               .create(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                       Mockito.anyDouble(), Mockito.anyBoolean(), Mockito.any(), Mockito.any());
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
        initCommandSenderPermissions(commandSender, false, false);
        Mockito.when(structure.getOwner(Mockito.any(IPPlayer.class))).thenReturn(Optional.empty());

        Assertions.assertDoesNotThrow(
            () -> factory.newToggle(commandSender, StructureActionType.CLOSE, AnimationType.MOVE_BLOCKS,
                                    structureRetriever)
                         .run().get(1, TimeUnit.SECONDS));
        verifyNoOpenerCalls();

        initCommandSenderPermissions(commandSender, true, false);
        Assertions.assertDoesNotThrow(
            () -> factory.newToggle(commandSender, StructureActionType.CLOSE, AnimationType.MOVE_BLOCKS,
                                    structureRetriever)
                         .run().get(1, TimeUnit.SECONDS));
        verifyNoOpenerCalls();
    }
}
