package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.testing.AssistedFactoryMocker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static nl.pim16aap2.animatedarchitecture.core.UnitTestUtil.assertThatMessageable;
import static nl.pim16aap2.animatedarchitecture.core.commands.CommandTestingUtil.initCommandSenderPermissions;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class SetOpenDirectionTest
{
    @Mock
    private Structure structure;

    @Mock
    private StructureType structureType;

    private StructureRetriever structureRetriever;

    @Mock
    private IPlayer commandSender;

    private AssistedFactoryMocker<SetOpenDirection, SetOpenDirection.IFactory> factory;

    @BeforeEach
    void init()
        throws NoSuchMethodException
    {
        initCommandSenderPermissions(commandSender, true, true);
        structureRetriever = StructureRetrieverFactory.ofStructure(structure);

        factory = AssistedFactoryMocker.injectMocksFromTestClass(SetOpenDirection.IFactory.class, this);
    }

    @Test
    void performAction_shouldSendErrorForInvalidMovementDirection()
    {
        // Setup
        when(structure.getType()).thenReturn(structureType);
        when(structureType.getLocalizationKey()).thenReturn("StructureType");

        final MovementDirection movementDirection = MovementDirection.CLOCKWISE;
        UnitTestUtil.initMessageable(commandSender);

        final String basicInfo = "basic-structure-info";
        when(structure.getBasicInfo()).thenReturn(basicInfo);

        final var command = factory.getFactory()
            .newSetOpenDirection(commandSender, structureRetriever, movementDirection);

        // Execute
        command.performAction(structure).join();

        // Verify
        verify(structure, never()).syncData();
        verify(structure, never()).setOpenDirection(movementDirection);
        assertThatMessageable(commandSender)
            .sentErrorMessage("commands.set_open_direction.error.invalid_rotation")
            .withArgs(movementDirection.getLocalizationKey(), "StructureType", basicInfo);
    }

    @Test
    void performAction_shouldWorkFineForValidMovementDirection()
    {
        // Setup
        final MovementDirection movementDirection = MovementDirection.CLOCKWISE;
        final IExecutor executor = mock();

        UnitTestUtil.initMessageable(commandSender);

        when(structure.getType()).thenReturn(structureType);
        when(structure.syncData()).thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));
        when(structure.getName()).thenReturn("structure-name");
        when(structure.getUid()).thenReturn(12L);
        when(structureType.getLocalizationKey()).thenReturn("StructureType");
        when(structureType.isValidOpenDirection(movementDirection)).thenReturn(true);
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        final var command = factory
            .injectParameters(executor)
            .getFactory()
            .newSetOpenDirection(commandSender, structureRetriever, movementDirection);

        // Execute
        command.performAction(structure).join();

        // Verify
        verify(structure).setOpenDirection(movementDirection);
        verify(structure).syncData();
        assertThatMessageable(commandSender)
            .sentSuccessMessage("commands.set_open_direction.success")
            .withArgs("StructureType", "structure-name (12)");
    }

    @Test
    void getCommand_shouldReturnCommandDefinition()
    {
        // Setup
        final var command = factory.getFactory()
            .newSetOpenDirection(commandSender, structureRetriever, MovementDirection.CLOCKWISE);

        // Execute
        final var result = command.getCommand();

        // Verify
        assertThat(result).isEqualTo(CommandDefinition.SET_OPEN_DIRECTION);
    }
}
