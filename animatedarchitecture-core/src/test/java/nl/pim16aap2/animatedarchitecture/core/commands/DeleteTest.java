package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeleteTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer commandSender;

    @Mock
    private DatabaseManager databaseManager;

    private StructureRetriever doorRetriever;

    @Mock
    private Structure door;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private Delete.IFactory factory;

    @BeforeEach
    void init()
    {
        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, true);

        final StructureType doorType = Mockito.mock(StructureType.class);
        Mockito.when(doorType.getLocalizationKey()).thenReturn("DoorType");
        Mockito.when(door.getType()).thenReturn(doorType);

        Mockito.when(door.isOwner(Mockito.any(UUID.class), Mockito.any())).thenReturn(true);
        Mockito.when(door.isOwner(Mockito.any(IPlayer.class), Mockito.any())).thenReturn(true);
        doorRetriever = StructureRetrieverFactory.ofStructure(door);

        Mockito.when(databaseManager.deleteStructure(Mockito.any(), Mockito.any()))
            .thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newDelete(
                Mockito.any(ICommandSender.class),
                Mockito.any(StructureRetriever.class)))
            .thenAnswer(invoc -> new Delete(
                invoc.getArgument(0, ICommandSender.class),
                localizer,
                ITextFactory.getSimpleTextFactory(),
                invoc.getArgument(1, StructureRetriever.class),
                databaseManager)
            );
    }

    @Test
    void testServer()
    {
        final IServer server = Mockito.mock(IServer.class, Answers.CALLS_REAL_METHODS);
        Assertions.assertDoesNotThrow(() -> factory.newDelete(server, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager).deleteStructure(door, null);
    }

    @Test
    void testExecution()
    {
        // No permissions, so not allowed.
        CommandTestingUtil.initCommandSenderPermissions(commandSender, false, false);
        Assertions.assertDoesNotThrow(
            () -> factory.newDelete(commandSender, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager, Mockito.never()).deleteStructure(door, commandSender);

        // Has user permission, but not an owner, so not allowed.
        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, false);
        Assertions.assertDoesNotThrow(
            () -> factory.newDelete(commandSender, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager, Mockito.never()).deleteStructure(door, commandSender);

        // Has user permission, and is owner, so allowed.
        Mockito.when(door.getOwner(commandSender)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerCreator));
        Assertions.assertDoesNotThrow(
            () -> factory.newDelete(commandSender, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager, Mockito.times(1)).deleteStructure(door, commandSender);

        // Admin permission, so allowed, despite not being owner.
        Mockito.when(door.getOwner(commandSender)).thenReturn(Optional.empty());
        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, true);
        Assertions.assertDoesNotThrow(
            () -> factory.newDelete(commandSender, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager, Mockito.times(2)).deleteStructure(door, commandSender);
    }
}
