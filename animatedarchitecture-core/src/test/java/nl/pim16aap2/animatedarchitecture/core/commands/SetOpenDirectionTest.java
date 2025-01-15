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
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.animatedarchitecture.core.commands.CommandTestingUtil.initCommandSenderPermissions;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SetOpenDirectionTest
{
    @Mock
    private Structure structure;

    @Mock
    private StructureType structureType;

    private StructureRetriever structureRetriever;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer commandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private SetOpenDirection.IFactory factory;

    @BeforeEach
    void init()
    {
        Mockito.when(structure.syncData())
            .thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));
        Mockito.when(structure.getType()).thenReturn(structureType);

        initCommandSenderPermissions(commandSender, true, true);
        structureRetriever = StructureRetrieverFactory.ofStructure(structure);

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newSetOpenDirection(
                Mockito.any(ICommandSender.class),
                Mockito.any(StructureRetriever.class),
                Mockito.any(MovementDirection.class),
                Mockito.anyBoolean()))
            .thenAnswer(invoc -> new SetOpenDirection(
                invoc.getArgument(0, ICommandSender.class),
                invoc.getArgument(1, StructureRetriever.class),
                invoc.getArgument(2, MovementDirection.class),
                invoc.getArgument(3, Boolean.class),
                localizer,
                ITextFactory.getSimpleTextFactory(),
                Mockito.mock(CommandFactory.class))
            );
    }

    @Test
    void testOpenDirValidity()
    {
        final MovementDirection movementDirection = MovementDirection.CLOCKWISE;

        Mockito.when(structureType.isValidOpenDirection(Mockito.any())).thenReturn(false);
        final SetOpenDirection command =
            factory.newSetOpenDirection(commandSender, structureRetriever, movementDirection);

        Assertions.assertDoesNotThrow(() -> command.performAction(structure).get(1, TimeUnit.SECONDS));
        Mockito.verify(structure, Mockito.never()).syncData();
        Mockito.verify(structure, Mockito.never()).setOpenDir(movementDirection);

        Mockito.when(structureType.isValidOpenDirection(movementDirection)).thenReturn(true);
        Assertions.assertDoesNotThrow(() -> command.performAction(structure).get(1, TimeUnit.SECONDS));
        Mockito.verify(structure).setOpenDir(movementDirection);
        Mockito.verify(structure).syncData();
    }
}
