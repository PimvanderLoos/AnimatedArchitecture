package nl.pim16aap2.bigdoors.core.commands;

import nl.pim16aap2.bigdoors.core.UnitTestUtil;
import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.core.localization.ILocalizer;
import nl.pim16aap2.bigdoors.core.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.tooluser.PowerBlockRelocator;
import nl.pim16aap2.bigdoors.core.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.core.util.structureretriever.StructureRetriever;
import nl.pim16aap2.bigdoors.core.util.structureretriever.StructureRetrieverFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Timeout(1)
class MovePowerBlockTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer commandSender;

    @Mock
    private ToolUser toolUser;

    @Mock
    private ToolUserManager toolUserManager;

    private StructureRetriever doorRetriever;

    @Mock
    private AbstractStructure door;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private MovePowerBlock.IFactory factory;

    @BeforeEach
    void init()
    {
        final UUID uuid = UUID.randomUUID();

        MockitoAnnotations.openMocks(this);

        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, true);
        doorRetriever = StructureRetrieverFactory.ofStructure(door);
        Mockito.when(door.isOwner(uuid)).thenReturn(true);
        Mockito.when(door.isOwner(Mockito.any(IPlayer.class))).thenReturn(true);
        Mockito.when(commandSender.getUUID()).thenReturn(uuid);
        Mockito.when(toolUserManager.getToolUser(uuid)).thenReturn(Optional.of(toolUser));

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        final PowerBlockRelocator.IFactory powerBlockRelocatorFactory =
            Mockito.mock(PowerBlockRelocator.IFactory.class);
        Mockito.when(powerBlockRelocatorFactory.create(Mockito.any(), Mockito.any()))
               .thenReturn(Mockito.mock(PowerBlockRelocator.class));

        Mockito.when(factory.newMovePowerBlock(Mockito.any(ICommandSender.class),
                                               Mockito.any(StructureRetriever.class)))
               .thenAnswer(invoc -> new MovePowerBlock(invoc.getArgument(0, ICommandSender.class), localizer,
                                                       ITextFactory.getSimpleTextFactory(),
                                                       invoc.getArgument(1, StructureRetriever.class),
                                                       toolUserManager, powerBlockRelocatorFactory));
    }

    @Test
    void testServer()
    {
        final IServer server = Mockito.mock(IServer.class, Answers.CALLS_REAL_METHODS);
        Assertions.assertDoesNotThrow(
            () -> factory.newMovePowerBlock(server, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager, Mockito.never()).startToolUser(Mockito.any(), Mockito.anyInt());
    }

    @Test
    void testExecution()
    {
        Assertions.assertDoesNotThrow(
            () -> factory.newMovePowerBlock(commandSender, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager).startToolUser(Mockito.any(), Mockito.anyInt());
    }
}
