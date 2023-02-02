package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.structures.AbstractStructure;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initCommandSenderPermissions;
import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.structureOwnerCreator;

@Timeout(1)
class DeleteTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock
    private DatabaseManager databaseManager;

    private StructureRetriever doorRetriever;

    @Mock
    private AbstractStructure door;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private Delete.IFactory factory;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        initCommandSenderPermissions(commandSender, true, true);

        final StructureType doorType = Mockito.mock(StructureType.class);
        Mockito.when(doorType.getLocalizationKey()).thenReturn("DoorType");
        Mockito.when(door.getType()).thenReturn(doorType);

        Mockito.when(door.isOwner(Mockito.any(UUID.class))).thenReturn(true);
        Mockito.when(door.isOwner(Mockito.any(IPPlayer.class))).thenReturn(true);
        doorRetriever = StructureRetrieverFactory.ofStructure(door);

        Mockito.when(databaseManager.deleteStructure(Mockito.any(), Mockito.any()))
               .thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newDelete(Mockito.any(ICommandSender.class),
                                       Mockito.any(StructureRetriever.class)))
               .thenAnswer(invoc -> new Delete(invoc.getArgument(0, ICommandSender.class), localizer,
                                               ITextFactory.getSimpleTextFactory(),
                                               invoc.getArgument(1, StructureRetriever.class),
                                               databaseManager));
    }

    @Test
    void testServer()
    {
        final IPServer server = Mockito.mock(IPServer.class, Answers.CALLS_REAL_METHODS);
        Assertions.assertDoesNotThrow(() -> factory.newDelete(server, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager).deleteStructure(door, null);
    }

    @Test
    void testExecution()
    {
        // No permissions, so not allowed.
        initCommandSenderPermissions(commandSender, false, false);
        Assertions.assertDoesNotThrow(
            () -> factory.newDelete(commandSender, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager, Mockito.never()).deleteStructure(door, commandSender);

        // Has user permission, but not an owner, so not allowed.
        initCommandSenderPermissions(commandSender, true, false);
        Assertions.assertDoesNotThrow(
            () -> factory.newDelete(commandSender, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager, Mockito.never()).deleteStructure(door, commandSender);

        // Has user permission, and is owner, so allowed.
        Mockito.when(door.getOwner(commandSender)).thenReturn(Optional.of(structureOwnerCreator));
        Assertions.assertDoesNotThrow(
            () -> factory.newDelete(commandSender, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager, Mockito.times(1)).deleteStructure(door, commandSender);

        // Admin permission, so allowed, despite not being owner.
        Mockito.when(door.getOwner(commandSender)).thenReturn(Optional.empty());
        initCommandSenderPermissions(commandSender, true, true);
        Assertions.assertDoesNotThrow(
            () -> factory.newDelete(commandSender, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager, Mockito.times(2)).deleteStructure(door, commandSender);
    }
}
