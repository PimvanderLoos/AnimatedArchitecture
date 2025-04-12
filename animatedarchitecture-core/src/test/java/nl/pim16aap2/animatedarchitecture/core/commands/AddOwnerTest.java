package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.exceptions.InvalidCommandInputException;
import nl.pim16aap2.animatedarchitecture.core.exceptions.NoAccessToStructureCommandException;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureOwner;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.testing.AssistedFactoryMocker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class AddOwnerTest
{
    private StructureRetriever structureRetriever;

    @Mock
    private DatabaseManager databaseManager;

    @Mock
    private Structure structure;

    @Mock
    private IPlayer commandSender;

    @Mock
    private IPlayer target;

    private AssistedFactoryMocker<AddOwner, AddOwner.IFactory> assistedFactoryMocker;

    @BeforeEach
    void init()
        throws NoSuchMethodException
    {
        assistedFactoryMocker = new AssistedFactoryMocker<>(AddOwner.class, AddOwner.IFactory.class)
            .setMock(DatabaseManager.class, databaseManager);

        structureRetriever = StructureRetrieverFactory.ofStructure(structure);
    }

    @AfterEach
    void afterEach()
    {
        verifyNoMoreInteractions(databaseManager);
    }

    @Test
    void getExistingPermissionLevel_defaultToNoPermission()
    {
        final AddOwner addOwner = addOwnerWithDefaults(PermissionLevel.ADMIN);

        when(structure.getOwner(target)).thenReturn(Optional.empty());

        assertEquals(PermissionLevel.NO_PERMISSION, addOwner.getExistingPermissionLevel(structure));
    }

    @Test
    void getExistingPermissionLevel_shouldReturnExistingPermissionLevel()
    {
        final PermissionLevel permissionLevel = PermissionLevel.USER;
        final StructureOwner structureOwner = new StructureOwner(12345L, permissionLevel, mock());
        final AddOwner addOwner = addOwnerWithDefaults(PermissionLevel.ADMIN);

        when(structure.getOwner(target)).thenReturn(Optional.of(structureOwner));

        assertEquals(permissionLevel, addOwner.getExistingPermissionLevel(structure));
    }

    @Test
    void validateInput_shouldAcceptAdminTarget()
    {
        final AddOwner addOwner = addOwnerWithDefaults(PermissionLevel.ADMIN);

        assertDoesNotThrow(addOwner::validateInput);
    }

    @Test
    void validateInput_shouldAcceptUserTarget()
    {
        final AddOwner addOwner = addOwnerWithDefaults(PermissionLevel.USER);

        assertDoesNotThrow(addOwner::validateInput);
    }

    @Test
    void validateInput_shouldThrowExceptionForCreatorTarget()
    {
        final AddOwner addOwner =
            addOwnerWithDefaults(assistedFactoryMocker, PermissionLevel.CREATOR);

        final InvalidCommandInputException exception =
            assertThrows(InvalidCommandInputException.class, addOwner::validateInput);

        assertTrue(exception.isUserInformed());
    }

    @Test
    void validateInput_shouldThrowExceptionForNoPermissionInput()
    {
        final AddOwner addOwner =
            addOwnerWithDefaults(assistedFactoryMocker, PermissionLevel.NO_PERMISSION);

        assertThrows(InvalidCommandInputException.class, addOwner::validateInput);
    }

    @Test
    void isAllowed_shouldRespectBypassForNonOwnerPlayerCommandSender()
    {
        final AddOwner addOwner =
            addOwnerWithDefaults(assistedFactoryMocker, PermissionLevel.ADMIN);
        UnitTestUtil.setStructureLocalization(structure);

        when(commandSender.isPlayer()).thenReturn(true);

        assertThrows(NoAccessToStructureCommandException.class, () -> addOwner.isAllowed(structure, false));
    }

    @Test
    void isAllowed_shouldBypassForNonPlayerCommandSender()
    {
        final AddOwner addOwner = addOwnerWithDefaults(PermissionLevel.ADMIN);

        when(commandSender.isPlayer()).thenReturn(false);

        assertDoesNotThrow(() -> addOwner.isAllowed(structure, false));
    }

    @Test
    void isAllowed_shouldThrowExceptionForNonOwnerCommandSender()
    {
        final AddOwner addOwner =
            addOwnerWithDefaults(assistedFactoryMocker, PermissionLevel.ADMIN);

        when(commandSender.isPlayer()).thenReturn(true);
        when(commandSender.getPlayer()).thenReturn(Optional.of(commandSender));
        when(structure.getOwner(target)).thenReturn(Optional.empty());

        UnitTestUtil.setStructureLocalization(structure);

        final NoAccessToStructureCommandException exception = assertThrows(
            NoAccessToStructureCommandException.class,
            () -> addOwner.isAllowed(structure, false)
        );

        assertTrue(exception.isUserInformed());
    }

    @Test
    void isAllowed_shouldThrowExceptionWhenTargetingPrimeOwnerWithBypass()
    {
        final AddOwner addOwner =
            addOwnerWithDefaults(assistedFactoryMocker, PermissionLevel.ADMIN);

        final StructureOwner targetOwner = new StructureOwner(1L, PermissionLevel.CREATOR, mock());

        when(structure.getOwner(target)).thenReturn(Optional.of(targetOwner));
        when(commandSender.isPlayer()).thenReturn(true);

        final NoAccessToStructureCommandException exception = assertThrows(
            NoAccessToStructureCommandException.class,
            () -> addOwner.isAllowed(structure, true)
        );

        assertTrue(exception.isUserInformed());
        assertEquals("Cannot target the prime owner of a structure.", exception.getMessage());
    }

    @Test
    void isAllowed_shouldThrowExceptionWhenTargetingPrimeOwnerForNonPlayerCommandSender()
    {
        final AddOwner addOwner =
            addOwnerWithDefaults(assistedFactoryMocker, PermissionLevel.ADMIN);

        final StructureOwner targetOwner = new StructureOwner(1L, PermissionLevel.CREATOR, mock());

        when(structure.getOwner(target)).thenReturn(Optional.of(targetOwner));
        when(commandSender.isPlayer()).thenReturn(false);

        final NoAccessToStructureCommandException exception = assertThrows(
            NoAccessToStructureCommandException.class,
            () -> addOwner.isAllowed(structure, false)
        );

        assertTrue(exception.isUserInformed());
        assertEquals("Cannot target the prime owner of a structure.", exception.getMessage());
    }

    @Test
    void isAllowed_shouldThrowExceptionWhenCommandSenderDoesNotHaveAddOwnerPermission()
    {
        final AddOwner addOwner =
            addOwnerWithDefaults(assistedFactoryMocker, PermissionLevel.ADMIN);

        final StructureOwner senderOwner = new StructureOwner(1L, PermissionLevel.USER, mock());

        UnitTestUtil.setStructureLocalization(structure);

        when(commandSender.isPlayer()).thenReturn(true);
        when(commandSender.getPlayer()).thenReturn(Optional.of(commandSender));

        when(structure.getOwner(target)).thenReturn(Optional.empty());
        when(structure.getOwner(commandSender)).thenReturn(Optional.of(senderOwner));

        final NoAccessToStructureCommandException exception = assertThrows(
            NoAccessToStructureCommandException.class,
            () -> addOwner.isAllowed(structure, false)
        );

        assertTrue(exception.isUserInformed());
    }

    @Test
    void isAllowed_shouldThrowExceptionWhenAssigningPermissionBelowOrEqualToSelf()
    {
        final AddOwner addOwner =
            addOwnerWithDefaults(assistedFactoryMocker, PermissionLevel.ADMIN);

        final StructureOwner senderOwner = new StructureOwner(1L, PermissionLevel.ADMIN, mock());

        when(commandSender.isPlayer()).thenReturn(true);
        when(commandSender.getPlayer()).thenReturn(Optional.of(commandSender));

        when(structure.getOwner(target)).thenReturn(Optional.empty());
        when(structure.getOwner(commandSender)).thenReturn(Optional.of(senderOwner));

        final NoAccessToStructureCommandException exception = assertThrows(
            NoAccessToStructureCommandException.class,
            () -> addOwner.isAllowed(structure, false)
        );

        assertTrue(exception.isUserInformed());
        assertEquals("Cannot assign a permission level below the command sender.", exception.getMessage());
    }

    @Test
    void isAllowed_shouldThrowExceptionWhenTargetAlreadyHasOwnershipBelowCommandSender()
    {
        final AddOwner addOwner = addOwnerWithDefaults(PermissionLevel.ADMIN);

        final StructureOwner senderOwner = new StructureOwner(1L, PermissionLevel.ADMIN, mock());
        final StructureOwner targetOwner = new StructureOwner(1L, PermissionLevel.USER, mock());

        when(commandSender.isPlayer()).thenReturn(true);
        when(commandSender.getPlayer()).thenReturn(Optional.of(commandSender));

        when(structure.getOwner(commandSender)).thenReturn(Optional.of(senderOwner));
        when(structure.getOwner(target)).thenReturn(Optional.of(targetOwner));

        final NoAccessToStructureCommandException exception = assertThrows(
            NoAccessToStructureCommandException.class,
            () -> addOwner.isAllowed(structure, false)
        );

        assertTrue(exception.isUserInformed());
    }

    @Test
    void isAllowed_shouldThrowExceptionWhenTargetAlreadyHasSamePermission()
    {
        final AddOwner addOwner =
            addOwnerWithDefaults(assistedFactoryMocker, PermissionLevel.USER);

        final StructureOwner senderOwner = new StructureOwner(1L, PermissionLevel.ADMIN, mock());
        final StructureOwner targetOwner = new StructureOwner(1L, PermissionLevel.USER, mock());

        when(commandSender.isPlayer()).thenReturn(true);
        when(commandSender.getPlayer()).thenReturn(Optional.of(commandSender));

        when(structure.getOwner(commandSender)).thenReturn(Optional.of(senderOwner));
        when(structure.getOwner(target)).thenReturn(Optional.of(targetOwner));

        UnitTestUtil.setStructureLocalization(structure);

        final NoAccessToStructureCommandException exception = assertThrows(
            NoAccessToStructureCommandException.class,
            () -> addOwner.isAllowed(structure, false)
        );
        assertTrue(exception.isUserInformed());
    }

    @Test
    void isAllowed_shouldAllowWhenAllConditionsAreMet()
    {
        final AddOwner addOwner = addOwnerWithDefaults(PermissionLevel.USER);
        final StructureOwner senderOwner = new StructureOwner(1L, PermissionLevel.ADMIN, mock());

        when(commandSender.isPlayer()).thenReturn(true);
        when(commandSender.getPlayer()).thenReturn(Optional.of(commandSender));

        when(structure.getOwner(commandSender)).thenReturn(Optional.of(senderOwner));
        when(structure.getOwner(target)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> addOwner.isAllowed(structure, false));
    }

//    @Test
//    void isAllowed


    @Test
    void performAction_shouldCallDatabaseManagerAndHandleResult()
        throws ExecutionException, InterruptedException, TimeoutException
    {
        final AddOwner addOwner = addOwnerWithDefaults(
            assistedFactoryMocker,
            PermissionLevel.USER
        );

        UnitTestUtil.setStructureLocalization(structure);

        when(commandSender.getPlayer()).thenReturn(Optional.of(commandSender));

        when(databaseManager
            .addOwner(structure, target, PermissionLevel.USER, commandSender))
            .thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));

        addOwner
            .performAction(structure)
            .get(1, TimeUnit.SECONDS);

        verify(commandSender).sendMessage(UnitTestUtil.textArgumentMatcher("commands.add_owner.success"));
        verify(target).sendMessage(UnitTestUtil.textArgumentMatcher("commands.add_owner.added_player_notification"));

        verify(databaseManager).addOwner(structure, target, PermissionLevel.USER, commandSender);
    }

    @Test
    void handleDatabaseActionSuccess_shouldSendCorrectSuccessMessages()
    {
        // setup
        final AddOwner addOwner =
            addOwnerWithDefaults(assistedFactoryMocker, PermissionLevel.USER);
        UnitTestUtil.setPersonalizedLocalizer(commandSender);
        UnitTestUtil.setStructureLocalization(structure);

        when(structure.getName()).thenReturn("structure-name");
        when(structure.getUid()).thenReturn(12L);

        // execute
        addOwner.handleDatabaseActionSuccess(structure);

        // verify
        UnitTestUtil.assertSendInfo(
            target,
            "commands.add_owner.added_player_notification",
            PermissionLevel.USER.getTranslationKey(),
            structure.getType().getLocalizationKey(),
            "structure-name (12)"
        );
        UnitTestUtil.assertSendSuccess(
            commandSender,
            "commands.add_owner.success",
            target.getName(),
            PermissionLevel.USER.getTranslationKey(),
            structure.getType().getLocalizationKey()
        );
    }

    @Test
    void getCommand_shouldReturnCorrectCommandDefinition()
    {
        final AddOwner addOwner = addOwnerWithDefaults(PermissionLevel.USER);

        assertEquals(CommandDefinition.ADD_OWNER, addOwner.getCommand());
    }

    @Test
    void defaultConstructor_shouldUseDefaultPermissionLevel()
    {
        final var factory = assistedFactoryMocker.getFactory();
        final AddOwner addOwner = factory.newAddOwner(commandSender, structureRetriever, target);

        assertEquals(PermissionLevel.USER, addOwner.getTargetPermissionLevel());
    }

    private AddOwner addOwnerWithDefaults(PermissionLevel permissionLevel)
    {
        return addOwnerWithDefaults(assistedFactoryMocker, permissionLevel);
    }

    /**
     * Creates a new AddOwner instance with the default mocks and the given permission level.
     * <p>
     * It uses {@link #commandSender} (a player) for the command sender, {@link #structureRetriever} for the structure
     * retriever, and {@link #target} for the target player.
     *
     * @param assistedFactoryMocker
     *     The mocker to get the factory from that will create the AddOwner instance.
     * @param permissionLevel
     *     The target permission level of the AddOwner instance.
     * @return The new AddOwner instance.
     */
    private AddOwner addOwnerWithDefaults(
        AssistedFactoryMocker<AddOwner, AddOwner.IFactory> assistedFactoryMocker,
        PermissionLevel permissionLevel)
    {
        final var factory = assistedFactoryMocker.getFactory();
        return factory.newAddOwner(commandSender, structureRetriever, target, permissionLevel);
    }
}
