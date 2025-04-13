package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.exceptions.InvalidCommandInputException;
import nl.pim16aap2.animatedarchitecture.core.exceptions.NoAccessToStructureCommandException;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureOwner;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.testing.AssistedFactoryMocker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static nl.pim16aap2.animatedarchitecture.core.UnitTestUtil.assertThatMessageable;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class RemoveOwnerTest
{
    @Mock
    private DatabaseManager databaseManager;

    @Mock
    private Structure structure;

    @Mock
    private ICommandSender commandSender;

    @Mock
    private IPlayer target;

    private StructureRetriever structureRetriever;

    private AssistedFactoryMocker<RemoveOwner, RemoveOwner.IFactory> assistedFactoryMocker;

    @BeforeEach
    void init()
        throws NoSuchMethodException
    {
        structureRetriever = StructureRetrieverFactory.ofStructure(structure);

        assistedFactoryMocker = new AssistedFactoryMocker<>(RemoveOwner.class, RemoveOwner.IFactory.class)
            .injectParameter(DatabaseManager.class, databaseManager);
    }

    @AfterEach
    void afterEach()
    {
        verifyNoMoreInteractions(databaseManager);
    }

    @Test
    void getCommand_shouldReturnRemoveOwner()
    {
        assertEquals(
            CommandDefinition.REMOVE_OWNER,
            assistedFactoryMocker
                .getFactory()
                .newRemoveOwner(commandSender, structureRetriever, target)
                .getCommand()
        );
    }

    @Test
    void availableForNonPlayers_shouldReturnTrue()
    {
        assertTrue(
            assistedFactoryMocker
                .getFactory()
                .newRemoveOwner(commandSender, structureRetriever, target)
                .availableForNonPlayers()
        );
    }

    @Test
    void handleDatabaseActionResult_shouldMessageCommandSenderAndTargetOnSuccess()
    {
        // Setup
        UnitTestUtil.setStructureLocalization(structure);
        UnitTestUtil.setPersonalizedLocalizer(commandSender, target);

        // execute
        assistedFactoryMocker
            .getFactory()
            .newRemoveOwner(commandSender, structureRetriever, target)
            .handleDatabaseActionSuccess(structure);

        // Verify
        assertThatMessageable(commandSender)
            .sentSuccessMessage("commands.remove_owner.success")
            .withArgs(target.getName(), structure.getType().getLocalizationKey());

        assertThatMessageable(target)
            .sentInfoMessage("commands.remove_owner.removed_player_notification")
            .withArgs(structure.getType().getLocalizationKey(), "null (0)");
    }

    @Test
    void performAction_shouldPropagateExceptionFromDatabase()
    {
        when(commandSender.getPlayer()).thenReturn(Optional.empty());

        when(databaseManager.removeOwner(structure, target, null)).thenThrow(new RuntimeException("TEST"));

        final RuntimeException exception = UnitTestUtil.assertRootCause(
            RuntimeException.class,
            () -> assistedFactoryMocker
                .getFactory()
                .newRemoveOwner(commandSender, structureRetriever, target)
                .performAction(structure)
        );

        Assertions.assertEquals("TEST", exception.getMessage());
    }

    @Test
    void performAction_shouldUnwrapCommandSenderIfPlayer()
    {
        // Setup
        final IPlayer playerCommandSender = mock();
        when(commandSender.getPlayer()).thenReturn(Optional.of(playerCommandSender));

        when(databaseManager
            .removeOwner(structure, target, playerCommandSender))
            .thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));

        UnitTestUtil.setStructureLocalization(structure);
        UnitTestUtil.setPersonalizedLocalizer(commandSender, target);

        // execute
        assistedFactoryMocker
            .getFactory()
            .newRemoveOwner(commandSender, structureRetriever, target)
            .performAction(structure)
            .join();

        // Verify
        verify(databaseManager).removeOwner(structure, target, playerCommandSender);
    }

    @Test
    void performAction_shouldCallHandleDatabaseResult()
    {
        final var databaseResult = DatabaseManager.ActionResult.CANCELLED;

        when(commandSender.getPlayer()).thenReturn(Optional.empty());
        when(databaseManager
            .removeOwner(structure, target, null))
            .thenReturn(CompletableFuture.completedFuture(databaseResult));

        final var removeOwner = spy(
            assistedFactoryMocker
                .getFactory()
                .newRemoveOwner(commandSender, structureRetriever, target)
        );
        removeOwner.performAction(structure).join();

        verify(databaseManager).removeOwner(structure, target, null);
        verify(removeOwner).handleDatabaseActionResult(databaseResult, structure);
    }

    @Test
    void isAllowed_shouldWorkForStructureOwnerWithAdminPermission()
    {
        final IPlayer playerCommandSender = mock();
        final StructureOwner structureOwner = new StructureOwner(12, PermissionLevel.ADMIN, mock());
        final StructureOwner targetOwner = new StructureOwner(12, PermissionLevel.USER, mock());

        when(commandSender.isPlayer()).thenReturn(true);
        when(commandSender.getPlayer()).thenReturn(Optional.of(playerCommandSender));
        when(structure.getOwner(playerCommandSender)).thenReturn(Optional.of(structureOwner));
        when(structure.getOwner(target)).thenReturn(Optional.of(targetOwner));

        assertDoesNotThrow(() ->
            assistedFactoryMocker
                .getFactory()
                .newRemoveOwner(commandSender, structureRetriever, target)
                .isAllowed(structure, false)
        );
    }

    @Test
    void isAllowed_shouldThrowExceptionForNonStructureOwner()
    {
        final IPlayer playerCommandSender = mock();

        when(commandSender.isPlayer()).thenReturn(true);
        when(commandSender.getPlayer()).thenReturn(Optional.of(playerCommandSender));
        when(structure.getOwner(playerCommandSender)).thenReturn(Optional.empty());

        UnitTestUtil.setStructureLocalization(structure);

        final InvalidCommandInputException exception = UnitTestUtil.assertRootCause(
            InvalidCommandInputException.class,
            () -> assistedFactoryMocker
                .getFactory()
                .newRemoveOwner(commandSender, structureRetriever, target)
                .isAllowed(structure, false)
        );

        assertTrue(exception.isUserInformed());
        assertEquals(
            String.format("Player %s is not an owner of structure %s", commandSender, null),
            exception.getMessage()
        );

        assertThatMessageable(commandSender)
            .sentErrorMessage("commands.remove_owner.error.not_an_owner")
            .withArgs(structure.getType().getLocalizationKey());
    }

    @Test
    void isAllowed_shouldWorkForNonStructureOwnerWithBypass()
    {
        final IPlayer playerCommandSender = mock();
        final StructureOwner targetOwner = new StructureOwner(12, PermissionLevel.USER, mock());

        when(commandSender.isPlayer()).thenReturn(true);
        when(commandSender.getPlayer()).thenReturn(Optional.of(playerCommandSender));
        when(structure.getOwner(playerCommandSender)).thenReturn(Optional.empty());
        when(structure.getOwner(target)).thenReturn(Optional.of(targetOwner));

        assertDoesNotThrow(() ->
            assistedFactoryMocker
                .getFactory()
                .newRemoveOwner(commandSender, structureRetriever, target)
                .isAllowed(structure, true)
        );
    }

    @Test
    void isAllowed_shouldDefaultToCreatorNonPlayers()
    {
        final StructureOwner targetOwner = new StructureOwner(12, PermissionLevel.ADMIN, mock());

        when(commandSender.isPlayer()).thenReturn(false);
        when(commandSender.getPlayer()).thenReturn(Optional.empty());
        when(structure.getOwner(target)).thenReturn(Optional.of(targetOwner));

        // It would throw an exception if the permission level was anything other
        // than CREATOR, as the target is ADMIN (so only CREATOR can remove them).
        assertDoesNotThrow(() ->
            assistedFactoryMocker
                .getFactory()
                .newRemoveOwner(commandSender, structureRetriever, target)
                .isAllowed(structure, false)
        );
    }

    @Test
    void isAllowed_shouldThrowExceptionWhenCommandSenderHasNoPermissionForAttribute()
    {
        final var permissionLevel = PermissionLevel.USER;
        // Sanity check
        assertFalse(StructureAttribute.REMOVE_OWNER.canAccessWith(permissionLevel));

        final IPlayer playerCommandSender = mock();
        final StructureOwner structureOwner = new StructureOwner(12, permissionLevel, mock());

        when(commandSender.isPlayer()).thenReturn(true);
        when(commandSender.getPlayer()).thenReturn(Optional.of(playerCommandSender));
        when(structure.getOwner(playerCommandSender)).thenReturn(Optional.of(structureOwner));

        UnitTestUtil.setStructureLocalization(structure);

        final NoAccessToStructureCommandException exception = UnitTestUtil.assertRootCause(
            NoAccessToStructureCommandException.class,
            () -> assistedFactoryMocker
                .getFactory()
                .newRemoveOwner(commandSender, structureRetriever, target)
                .isAllowed(structure, false)
        );

        assertTrue(exception.isUserInformed());
        assertEquals(
            String.format("Player %s does not have permission to remove an owner", commandSender),
            exception.getMessage()
        );

        assertThatMessageable(commandSender)
            .sentErrorMessage("commands.remove_owner.error.not_allowed")
            .withArgs(structure.getType().getLocalizationKey());
    }

    @Test
    void isAllowed_shouldThrowExceptionWhenTargetIsNotAnOwner()
    {
        final IPlayer playerCommandSender = mock();
        final StructureOwner structureOwner = new StructureOwner(12, PermissionLevel.ADMIN, mock());

        when(commandSender.isPlayer()).thenReturn(true);
        when(commandSender.getPlayer()).thenReturn(Optional.of(playerCommandSender));
        when(structure.getOwner(playerCommandSender)).thenReturn(Optional.of(structureOwner));
        when(structure.getOwner(target)).thenReturn(Optional.empty());

        UnitTestUtil.setStructureLocalization(structure);

        final NoAccessToStructureCommandException exception = UnitTestUtil.assertRootCause(
            NoAccessToStructureCommandException.class,
            () -> assistedFactoryMocker
                .getFactory()
                .newRemoveOwner(commandSender, structureRetriever, target)
                .isAllowed(structure, false)
        );

        assertTrue(exception.isUserInformed());
        assertEquals(
            String.format("Player %s cannot remove non-owner %s structure %s", commandSender, target, null),
            exception.getMessage()
        );

        assertThatMessageable(commandSender)
            .sentErrorMessage("commands.remove_owner.error.target_not_an_owner")
            .withArgs(target.asString(), structure.getType().getLocalizationKey(), structure.getBasicInfo());
    }

    @Test
    void isAllowed_shouldThrowExceptionWhenTargetHasSamePermissionLevel()
    {
        final IPlayer playerCommandSender = mock();
        final StructureOwner structureOwner = new StructureOwner(12, PermissionLevel.ADMIN, mock());
        final StructureOwner targetOwner = new StructureOwner(12, PermissionLevel.ADMIN, mock());

        when(commandSender.isPlayer()).thenReturn(true);
        when(commandSender.getPlayer()).thenReturn(Optional.of(playerCommandSender));
        when(structure.getOwner(playerCommandSender)).thenReturn(Optional.of(structureOwner));
        when(structure.getOwner(target)).thenReturn(Optional.of(targetOwner));

        final NoAccessToStructureCommandException exception = UnitTestUtil.assertRootCause(
            NoAccessToStructureCommandException.class,
            () -> assistedFactoryMocker
                .getFactory()
                .newRemoveOwner(commandSender, structureRetriever, target)
                .isAllowed(structure, false)
        );

        assertTrue(exception.isUserInformed());
        assertEquals("Player cannot remove an owner with equal or lower permission level", exception.getMessage());

        verify(commandSender).sendError("commands.remove_owner.error.cannot_remove_lower_permission");
    }
}
