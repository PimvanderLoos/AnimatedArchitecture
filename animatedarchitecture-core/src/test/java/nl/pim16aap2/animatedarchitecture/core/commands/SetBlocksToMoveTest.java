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
import nl.pim16aap2.animatedarchitecture.core.structures.structurearchetypes.IDiscreteMovement;
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

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SetBlocksToMoveTest
{
    private AbstractStructure structure;

    private StructureRetriever structureRetriever;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer commandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private SetBlocksToMove.IFactory factory;

    @Mock
    private StructureType structureType;

    @BeforeEach
    void init()
    {
        structure = Mockito.mock(
            AbstractStructure.class,
            Mockito.withSettings().extraInterfaces(IDiscreteMovement.class)
        );

        Mockito.when(structure.syncData())
            .thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));

        Mockito.when(structureType.getLocalizationKey()).thenReturn("StructureType");
        Mockito.when(structure.getType()).thenReturn(structureType);

        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, true);
        structureRetriever = StructureRetrieverFactory.ofStructure(structure);

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newSetBlocksToMove(
                Mockito.any(ICommandSender.class),
                Mockito.any(StructureRetriever.class),
                Mockito.anyInt()))
            .thenAnswer(invoc -> new SetBlocksToMove(
                invoc.getArgument(0, ICommandSender.class),
                localizer,
                ITextFactory.getSimpleTextFactory(),
                invoc.getArgument(1, StructureRetriever.class),
                invoc.getArgument(2, Integer.class))
            );
    }

    @Test
    void testStructureTypes()
    {
        final int blocksToMove = 42;

        final SetBlocksToMove command = factory.newSetBlocksToMove(commandSender, structureRetriever, blocksToMove);
        final AbstractStructure altStructure = Mockito.mock(AbstractStructure.class);
        Mockito.when(altStructure.getType()).thenReturn(structureType);

        Assertions.assertDoesNotThrow(() -> command.performAction(altStructure).get(1, TimeUnit.SECONDS));
        Mockito.verify(altStructure, Mockito.never()).syncData();

        Assertions.assertDoesNotThrow(() -> command.performAction(structure).get(1, TimeUnit.SECONDS));
        Mockito.verify((IDiscreteMovement) structure).setBlocksToMove(blocksToMove);
        Mockito.verify(structure).syncData();
    }
}
