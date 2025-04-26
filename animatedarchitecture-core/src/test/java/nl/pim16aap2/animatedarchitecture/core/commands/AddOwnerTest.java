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
import org.assertj.core.api.InstanceOfAssertFactories;
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

import static nl.pim16aap2.animatedarchitecture.core.UnitTestUtil.assertThatMessageable;
import static org.assertj.core.api.Assertions.*;
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
        assistedFactoryMocker = AssistedFactoryMocker.injectMocksFromTestClass(AddOwner.IFactory.class, this);

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
        // Setup
        final AddOwner addOwner = addOwnerWithDefaults(PermissionLevel.ADMIN);

        when(structure.getOwner(target)).thenReturn(Optional.empty());

        // Execute & Verify
        assertThat(addOwner.getExistingPermissionLevel(structure)).isEqualTo(PermissionLevel.NO_PERMISSION);
    }

    @Test
    void getExistingPermissionLevel_shouldReturnExistingPermissionLevel()
    {
        // Setup
        final PermissionLevel permissionLevel = PermissionLevel.USER;
        final StructureOwner structureOwner = new StructureOwner(12345L, permissionLevel, mock());
        final AddOwner addOwner = addOwnerWithDefaults(PermissionLevel.ADMIN);

        when(structure.getOwner(target)).thenReturn(Optional.of(structureOwner));

        // Execute & Verify
        assertThat(addOwner.getExistingPermissionLevel(structure)).isEqualTo(permissionLevel);
    }

    @Test
    void validateInput_shouldAcceptAdminTarget()
    {
        // Setup
        final AddOwner addOwner = addOwnerWithDefaults(PermissionLevel.ADMIN);

        // Execute & Verify
        assertDoesNotThrow(addOwner::validateInput);
    }

    @Test
    void validateInput_shouldAcceptUserTarget()
    {
        // Setup
        final AddOwner addOwner = addOwnerWithDefaults(PermissionLevel.USER);

        // Execute & Verify
        assertDoesNotThrow(addOwner::validateInput);
    }

    @Test
    void validateInput_shouldThrowExceptionForCreatorTarget()
    {
        // Setup
        final AddOwner addOwner = addOwnerWithDefaults(assistedFactoryMocker, PermissionLevel.CREATOR);

        UnitTestUtil.initMessageable(commandSender);

        // Execute & Verify
        assertThatExceptionOfType(InvalidCommandInputException.class)
            .isThrownBy(addOwner::validateInput)
            .withMessage(
                "The target permission level '%s' is invalid for the AddOwner command.",
                PermissionLevel.CREATOR)
            .extracting(InvalidCommandInputException::isUserInformed, InstanceOfAssertFactories.BOOLEAN)
            .isTrue();

        assertThatMessageable(commandSender)
            .sentErrorMessage("commands.add_owner.error.invalid_target_permission")
            .withArgs(PermissionLevel.CREATOR.getTranslationKey());
    }

    @Test
    void validateInput_shouldThrowExceptionForNoPermissionInput()
    {
        // Setup
        final AddOwner addOwner = addOwnerWithDefaults(assistedFactoryMocker, PermissionLevel.NO_PERMISSION);

        assertThatExceptionOfType(InvalidCommandInputException.class)
            .isThrownBy(addOwner::validateInput)
            .withMessage(
                "The target permission level '%s' is invalid for the AddOwner command.",
                PermissionLevel.NO_PERMISSION)
            .extracting(InvalidCommandInputException::isUserInformed, InstanceOfAssertFactories.BOOLEAN)
            .isTrue();

        assertThatMessageable(commandSender)
            .sentErrorMessage("commands.add_owner.error.invalid_target_permission")
            .withArgs(PermissionLevel.NO_PERMISSION.getTranslationKey());
    }

    @Test
    void isAllowed_shouldRespectBypassForNonOwnerPlayerCommandSender()
    {
        // Setup
        final AddOwner addOwner = addOwnerWithDefaults(assistedFactoryMocker, PermissionLevel.ADMIN);

        when(commandSender.isPlayer()).thenReturn(true);

        // Execute & Verify
        assertThrows(NoAccessToStructureCommandException.class, () -> addOwner.isAllowed(structure, false));
    }

    @Test
    void isAllowed_shouldBypassForNonPlayerCommandSender()
    {
        // Setup
        final AddOwner addOwner = addOwnerWithDefaults(PermissionLevel.ADMIN);

        when(commandSender.isPlayer()).thenReturn(false);

        // Execute & Verify
        assertDoesNotThrow(() -> addOwner.isAllowed(structure, false));
    }

    @Test
    void isAllowed_shouldThrowExceptionForNonOwnerCommandSender()
    {
        // Setup
        final AddOwner addOwner = addOwnerWithDefaults(assistedFactoryMocker, PermissionLevel.ADMIN);

        UnitTestUtil.initMessageable(commandSender);
        UnitTestUtil.setStructureLocalization(structure);

        when(commandSender.isPlayer()).thenReturn(true);
        when(commandSender.getPlayer()).thenReturn(Optional.of(commandSender));
        when(structure.getOwner(target)).thenReturn(Optional.empty());

        // Execute & Verify
        assertThatExceptionOfType(NoAccessToStructureCommandException.class)
            .isThrownBy(() -> addOwner.isAllowed(structure, false))
            .withMessage("The command sender is not an owner of the structure.")
            .extracting(NoAccessToStructureCommandException::isUserInformed, InstanceOfAssertFactories.BOOLEAN)
            .isTrue();

        assertThatMessageable(commandSender)
            .sentErrorMessage("commands.add_owner.error.not_an_owner")
            .withArgs("StructureType");
    }

    @Test
    void isAllowed_shouldThrowExceptionWhenTargetingPrimeOwnerWithBypass()
    {
        final AddOwner addOwner = addOwnerWithDefaults(assistedFactoryMocker, PermissionLevel.ADMIN);
        final StructureOwner targetOwner = new StructureOwner(1L, PermissionLevel.CREATOR, mock());

        when(structure.getOwner(target)).thenReturn(Optional.of(targetOwner));
        when(commandSender.isPlayer()).thenReturn(true);

        final NoAccessToStructureCommandException exception = assertThrows(
            NoAccessToStructureCommandException.class,
            () -> addOwner.isAllowed(structure, true)
        );

        assertThat(exception.isUserInformed()).isTrue();
        assertThat(exception.getMessage()).isEqualTo("Cannot target the prime owner of a structure.");
    }

    @Test
    void isAllowed_shouldThrowExceptionWhenTargetingPrimeOwnerForNonPlayerCommandSender()
    {
        final AddOwner addOwner = addOwnerWithDefaults(assistedFactoryMocker, PermissionLevel.ADMIN);
        final StructureOwner targetOwner = new StructureOwner(1L, PermissionLevel.CREATOR, mock());

        when(structure.getOwner(target)).thenReturn(Optional.of(targetOwner));
        when(commandSender.isPlayer()).thenReturn(false);

        assertThatExceptionOfType(NoAccessToStructureCommandException.class)
            .isThrownBy(() -> addOwner.isAllowed(structure, false))
            .withMessage("Cannot target the prime owner of a structure.")
            .extracting(NoAccessToStructureCommandException::isUserInformed, InstanceOfAssertFactories.BOOLEAN)
            .isTrue();
    }

    @Test
    void isAllowed_shouldThrowExceptionWhenCommandSenderDoesNotHaveAddOwnerPermission()
    {
        final AddOwner addOwner = addOwnerWithDefaults(assistedFactoryMocker, PermissionLevel.ADMIN);
        final StructureOwner senderOwner = new StructureOwner(1L, PermissionLevel.USER, mock());

        when(commandSender.isPlayer()).thenReturn(true);
        when(commandSender.getPlayer()).thenReturn(Optional.of(commandSender));

        when(structure.getOwner(target)).thenReturn(Optional.empty());
        when(structure.getOwner(commandSender)).thenReturn(Optional.of(senderOwner));

        assertThatExceptionOfType(NoAccessToStructureCommandException.class)
            .isThrownBy(() -> addOwner.isAllowed(structure, false))
            .withMessage("The command sender is not allowed to add owners.")
            .extracting(NoAccessToStructureCommandException::isUserInformed, InstanceOfAssertFactories.BOOLEAN)
            .isTrue();
    }

    @Test
    void isAllowed_shouldThrowExceptionWhenAssigningPermissionBelowOrEqualToSelf()
    {
        final AddOwner addOwner = addOwnerWithDefaults(assistedFactoryMocker, PermissionLevel.ADMIN);
        final StructureOwner senderOwner = new StructureOwner(1L, PermissionLevel.ADMIN, mock());

        when(commandSender.isPlayer()).thenReturn(true);
        when(commandSender.getPlayer()).thenReturn(Optional.of(commandSender));

        when(structure.getOwner(target)).thenReturn(Optional.empty());
        when(structure.getOwner(commandSender)).thenReturn(Optional.of(senderOwner));

        final NoAccessToStructureCommandException exception = assertThrows(
            NoAccessToStructureCommandException.class,
            () -> addOwner.isAllowed(structure, false)
        );

        assertThat(exception.isUserInformed()).isTrue();
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

        assertThat(exception.isUserInformed()).isTrue();
    }

    @Test
    void isAllowed_shouldThrowExceptionWhenTargetAlreadyHasSamePermission()
    {
        final AddOwner addOwner = addOwnerWithDefaults(assistedFactoryMocker, PermissionLevel.USER);
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
        assertThat(exception.isUserInformed()).isTrue();
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

    @Test
    void performAction_shouldCallDatabaseManagerAndHandleResult()
        throws ExecutionException, InterruptedException, TimeoutException
    {
        // Setup
        final AddOwner addOwner = addOwnerWithDefaults(assistedFactoryMocker, PermissionLevel.USER);

        final String targetName = "target-name";
        when(target.getName()).thenReturn(targetName);

        UnitTestUtil.setPersonalizedLocalizer(commandSender, target);
        UnitTestUtil.setStructureLocalization(structure);

        when(structure.getUid()).thenReturn(12L);
        when(structure.getName()).thenReturn("structure-name");
        when(commandSender.getPlayer()).thenReturn(Optional.of(commandSender));
        when(databaseManager
            .addOwner(structure, target, PermissionLevel.USER, commandSender))
            .thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));

        // execute
        addOwner
            .performAction(structure)
            .get(1, TimeUnit.SECONDS);

        // Verify
        assertThatMessageable(target)
            .sentInfoMessage("commands.add_owner.added_player_notification")
            .withArgs(
                PermissionLevel.USER.getTranslationKey(),
                "StructureType",
                "structure-name (12)"
            );
        assertThatMessageable(commandSender)
            .sentSuccessMessage("commands.add_owner.success")
            .withArgs(
                targetName,
                PermissionLevel.USER.getTranslationKey(),
                "StructureType"
            );

        verify(databaseManager).addOwner(structure, target, PermissionLevel.USER, commandSender);
    }

    @Test
    void handleDatabaseActionSuccess_shouldSendCorrectSuccessMessages()
    {
        // Setup
        final AddOwner addOwner = addOwnerWithDefaults(assistedFactoryMocker, PermissionLevel.USER);
        UnitTestUtil.setPersonalizedLocalizer(commandSender, target);
        UnitTestUtil.setStructureLocalization(structure);

        final String targetName = "target-name";
        when(target.getName()).thenReturn(targetName);

        when(structure.getName()).thenReturn("structure-name");
        when(structure.getUid()).thenReturn(12L);

        // Execute
        addOwner.handleDatabaseActionSuccess(structure);

        // Verify
        assertThatMessageable(target)
            .sentInfoMessage("commands.add_owner.added_player_notification")
            .withArgs(
                PermissionLevel.USER.getTranslationKey(),
                "StructureType",
                "structure-name (12)"
            );
        assertThatMessageable(commandSender)
            .sentSuccessMessage("commands.add_owner.success")
            .withArgs(
                targetName,
                PermissionLevel.USER.getTranslationKey(),
                "StructureType"
            );
    }

    @Test
    void getCommand_shouldReturnCorrectCommandDefinition()
    {
        final AddOwner addOwner = addOwnerWithDefaults(PermissionLevel.USER);

        assertThat(addOwner.getCommand()).isEqualTo(CommandDefinition.ADD_OWNER);
    }

    @Test
    void defaultConstructor_shouldUseDefaultPermissionLevel()
    {
        final var factory = assistedFactoryMocker.getFactory();
        final AddOwner addOwner = factory.newAddOwner(commandSender, structureRetriever, target);

        assertThat(addOwner.getTargetPermissionLevel()).isEqualTo(PermissionLevel.USER);
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
