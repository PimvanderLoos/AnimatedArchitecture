package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Timeout(1)
class RemoveOwnerTest
{
    private StructureRetriever doorRetriever;

    @Mock
    private AbstractStructure door;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer commandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer target;

    @Mock
    private DatabaseManager databaseManager;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private RemoveOwner.IFactory factory;

    @BeforeEach
    void beforeEach()
    {
        MockitoAnnotations.openMocks(this);

        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, true);

        final StructureType doorType = Mockito.mock(StructureType.class);
        Mockito.when(doorType.getLocalizationKey()).thenReturn("DoorType");
        Mockito.when(door.getType()).thenReturn(doorType);

        Mockito.when(door.isOwner(Mockito.any(UUID.class), Mockito.any())).thenReturn(true);
        Mockito.when(door.isOwner(Mockito.any(IPlayer.class), Mockito.any())).thenReturn(true);
        doorRetriever = StructureRetrieverFactory.ofStructure(door);

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(databaseManager.removeOwner(Mockito.any(AbstractStructure.class), Mockito.any(IPlayer.class),
                                                 Mockito.any(IPlayer.class)))
               .thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));

        Mockito.when(factory.newRemoveOwner(Mockito.any(ICommandSender.class),
                                            Mockito.any(StructureRetriever.class),
                                            Mockito.any(IPlayer.class)))
               .thenAnswer(invoc -> new RemoveOwner(invoc.getArgument(0, ICommandSender.class), localizer,
                                                    ITextFactory.getSimpleTextFactory(),
                                                    invoc.getArgument(1, StructureRetriever.class),
                                                    invoc.getArgument(2, IPlayer.class), databaseManager));
    }

    /**
     * Ensure that even with the bypass permission, certain invalid actions are not allowed. (e.g. removing the original
     * creator).
     */
    @Test
    void testBypassLimitations()
    {
        final RemoveOwner removeOwner = factory.newRemoveOwner(commandSender, doorRetriever, target);

        // The target is not an owner, so this should be false despite the bypass permission.
        Assertions.assertFalse(removeOwner.isAllowed(door, true));

        // Removing the level0 owner is not allowed even with bypass enabled!
        Mockito.when(door.getOwner(target)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerCreator));
        Assertions.assertFalse(removeOwner.isAllowed(door, true));

        // Removing level>0 owners IS allowed with bypass even if
        Mockito.when(door.getOwner(target)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerAdmin));
        Assertions.assertTrue(removeOwner.isAllowed(door, true));
    }

    @Test
    void testSuccess()
    {
        final RemoveOwner removeOwner = factory.newRemoveOwner(commandSender, doorRetriever, target);

        Mockito.when(door.getOwner(commandSender)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerCreator));
        Mockito.when(door.getOwner(target)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerAdmin));
        Assertions.assertTrue(removeOwner.isAllowed(door, false));

        Mockito.when(door.getOwner(target)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerUser));
        Assertions.assertTrue(removeOwner.isAllowed(door, false));
    }

    @Test
    void testIsAllowed()
    {
        final RemoveOwner removeOwner = factory.newRemoveOwner(commandSender, doorRetriever, target);

        Mockito.when(door.getOwner(target)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerCreator));
        Mockito.when(door.getOwner(commandSender)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerCreator));
        Assertions.assertFalse(removeOwner.isAllowed(door, false));

        Mockito.when(door.getOwner(commandSender)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerAdmin));
        Assertions.assertFalse(removeOwner.isAllowed(door, false));

        Mockito.when(door.getOwner(commandSender)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerAdmin));
        Assertions.assertFalse(removeOwner.isAllowed(door, false));

        Mockito.when(door.getOwner(target)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerAdmin));
        Assertions.assertFalse(removeOwner.isAllowed(door, false));

        Mockito.when(door.getOwner(target)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerUser));
        Assertions.assertTrue(removeOwner.isAllowed(door, false));

        Mockito.when(door.getOwner(target)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerUser));
        Assertions.assertTrue(removeOwner.isAllowed(door, false));

        Mockito.when(door.getOwner(commandSender)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerUser));
        Assertions.assertFalse(removeOwner.isAllowed(door, true));

        Mockito.when(door.getOwner(commandSender)).thenReturn(Optional.empty());
        Assertions.assertFalse(removeOwner.isAllowed(door, false));
    }

    @Test
    void testDatabaseInteraction()
    {
        Mockito.when(door.getOwner(commandSender)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerCreator));
        Mockito.when(door.getOwner(target)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerAdmin));

        final CompletableFuture<?> result = factory.newRemoveOwner(commandSender, doorRetriever, target).run();
        Assertions.assertDoesNotThrow(() -> result.get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager, Mockito.times(1)).removeOwner(door, target,
                                                                      commandSender.getPlayer().orElse(null));
    }
}
