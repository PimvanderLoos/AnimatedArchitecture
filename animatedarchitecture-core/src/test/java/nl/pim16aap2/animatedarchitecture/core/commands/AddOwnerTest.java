package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
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
import org.mockito.stubbing.Answer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AddOwnerTest
{
    private ILocalizer localizer;

    @Mock
    private DatabaseManager databaseManager;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private AddOwner.IFactory factory;

    private StructureRetriever doorRetriever;

    @Mock
    private Structure door;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer commandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer target;

    private AddOwner addOwnerCreator;
    private AddOwner addOwnerAdmin;
    private AddOwner addOwnerUser;

    @BeforeEach
    void init()
    {
        final StructureType doorType = Mockito.mock(StructureType.class);
        Mockito.when(doorType.getLocalizationKey()).thenReturn("DoorType");
        Mockito.when(door.getType()).thenReturn(doorType);
        Mockito.when(door.isOwner(commandSender, StructureAttribute.ADD_OWNER.getPermissionLevel())).thenReturn(true);
        Mockito.when(door.isOwner(commandSender.getUUID(), StructureAttribute.ADD_OWNER.getPermissionLevel()))
            .thenReturn(true);

        localizer = UnitTestUtil.initLocalizer();

        Mockito.when(databaseManager.addOwner(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));

        Mockito.when(factory.newAddOwner(
                Mockito.any(ICommandSender.class),
                Mockito.any(StructureRetriever.class),
                Mockito.any(IPlayer.class),
                Mockito.any(PermissionLevel.class)))
            .thenAnswer((Answer<AddOwner>) invoc -> new AddOwner(
                invoc.getArgument(0, ICommandSender.class),
                localizer,
                ITextFactory.getSimpleTextFactory(),
                invoc.getArgument(1, StructureRetriever.class),
                invoc.getArgument(2, IPlayer.class),
                invoc.getArgument(3, PermissionLevel.class),
                databaseManager)
            );

        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, true);
        doorRetriever = StructureRetrieverFactory.ofStructure(door);

        addOwnerCreator = factory.newAddOwner(commandSender, doorRetriever, target, PermissionLevel.CREATOR);
        addOwnerAdmin = factory.newAddOwner(commandSender, doorRetriever, target, PermissionLevel.ADMIN);
        addOwnerUser = factory.newAddOwner(commandSender, doorRetriever, target, PermissionLevel.USER);
    }

    @Test
    void testInputValidity()
    {
        Assertions.assertFalse(addOwnerCreator.validInput());
        Assertions.assertTrue(addOwnerAdmin.validInput());
        Assertions.assertTrue(addOwnerUser.validInput());
        Assertions.assertFalse(
            factory.newAddOwner(commandSender, doorRetriever, target, PermissionLevel.NO_PERMISSION).validInput());
    }

    @Test
    void testIsAllowed()
    {
        Assertions.assertTrue(addOwnerAdmin.isAllowed(door, true));
        Assertions.assertFalse(addOwnerAdmin.isAllowed(door, false));

        Mockito.when(door.getOwner(commandSender)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerCreator));
        Assertions.assertFalse(addOwnerCreator.isAllowed(door, false));
        Assertions.assertTrue(addOwnerAdmin.isAllowed(door, false));
        Assertions.assertTrue(addOwnerUser.isAllowed(door, false));

        Mockito.when(door.getOwner(commandSender)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerAdmin));
        Assertions.assertFalse(addOwnerCreator.isAllowed(door, false));
        Assertions.assertFalse(addOwnerAdmin.isAllowed(door, false));
        Assertions.assertTrue(addOwnerUser.isAllowed(door, false));

        Mockito.when(door.getOwner(commandSender)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerUser));
        Assertions.assertFalse(addOwnerCreator.isAllowed(door, false));
        Assertions.assertFalse(addOwnerAdmin.isAllowed(door, false));
        Assertions.assertFalse(addOwnerUser.isAllowed(door, false));
    }

    @Test
    void nonPlayer()
    {
        final ICommandSender server = Mockito.mock(ICommandSender.class, Answers.CALLS_REAL_METHODS);
        final AddOwner addOwner = factory.newAddOwner(server, doorRetriever, target, PermissionLevel.CREATOR);

        Mockito.when(door.getOwner(target)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerCreator));
        Assertions.assertFalse(addOwner.isAllowed(door, false));

        Mockito.when(door.getOwner(target)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerAdmin));
        Assertions.assertTrue(addOwner.isAllowed(door, false));
    }

    @Test
    void testIsAllowedExistingTarget()
    {
        Mockito.when(door.getOwner(commandSender)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerCreator));
        Mockito.when(door.getOwner(target)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerAdmin));
        Assertions.assertFalse(addOwnerAdmin.isAllowed(door, false));
        Assertions.assertTrue(addOwnerUser.isAllowed(door, false));

        Mockito.when(door.getOwner(commandSender)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerAdmin));
        Mockito.when(door.getOwner(target)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerAdmin));
        Assertions.assertFalse(addOwnerAdmin.isAllowed(door, false));
        Assertions.assertFalse(addOwnerUser.isAllowed(door, false));

        Mockito.when(door.getOwner(commandSender)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerCreator));
        Mockito.when(door.getOwner(target)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerUser));
        Assertions.assertTrue(addOwnerAdmin.isAllowed(door, false));
        Assertions.assertFalse(addOwnerUser.isAllowed(door, false));

        Mockito.when(door.getOwner(commandSender)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerAdmin));
        Mockito.when(door.getOwner(target)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerCreator));
        Assertions.assertFalse(addOwnerAdmin.isAllowed(door, false));
        Assertions.assertFalse(addOwnerUser.isAllowed(door, false));

        // It should never be possible to re-assign level 0 ownership, even with bypass enabled.
        Mockito.when(door.getOwner(target)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerCreator));
        Assertions.assertFalse(addOwnerAdmin.isAllowed(door, true));
        Assertions.assertFalse(addOwnerUser.isAllowed(door, true));
    }

    @Test
    void testDatabaseInteraction()
    {
        Mockito.when(door.getOwner(commandSender)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerCreator));
        Mockito.when(door.getOwner(target)).thenReturn(Optional.of(CommandTestingUtil.structureOwnerAdmin));
        Mockito.when(door.isOwner(Mockito.any(UUID.class), Mockito.any())).thenReturn(true);
        Mockito.when(door.isOwner(Mockito.any(IPlayer.class), Mockito.any())).thenReturn(true);

        final CompletableFuture<?> result =
            factory.newAddOwner(commandSender, doorRetriever, target, AddOwner.DEFAULT_PERMISSION_LEVEL).run();

        Assertions.assertDoesNotThrow(() -> result.get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager, Mockito.times(1))
            .addOwner(door, target, AddOwner.DEFAULT_PERMISSION_LEVEL, commandSender.getPlayer().orElse(null));
    }
}
