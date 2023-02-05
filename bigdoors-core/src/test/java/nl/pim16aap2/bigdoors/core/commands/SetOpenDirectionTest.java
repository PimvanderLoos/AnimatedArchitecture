package nl.pim16aap2.bigdoors.core.commands;

import nl.pim16aap2.bigdoors.core.UnitTestUtil;
import nl.pim16aap2.bigdoors.core.api.IPPlayer;
import nl.pim16aap2.bigdoors.core.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.core.localization.ILocalizer;
import nl.pim16aap2.bigdoors.core.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.structuretypes.StructureType;
import nl.pim16aap2.bigdoors.core.util.MovementDirection;
import nl.pim16aap2.bigdoors.core.util.structureretriever.StructureRetriever;
import nl.pim16aap2.bigdoors.core.util.structureretriever.StructureRetrieverFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.bigdoors.core.commands.CommandTestingUtil.initCommandSenderPermissions;

@Timeout(1)
class SetOpenDirectionTest
{
    @Mock
    private AbstractStructure structure;

    @Mock
    private StructureType structureType;

    private StructureRetriever structureRetriever;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private SetOpenDirection.IFactory factory;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        Mockito.when(structure.syncData())
               .thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));
        Mockito.when(structure.getType()).thenReturn(structureType);

        initCommandSenderPermissions(commandSender, true, true);
        structureRetriever = StructureRetrieverFactory.ofStructure(structure);

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newSetOpenDirection(Mockito.any(ICommandSender.class),
                                                 Mockito.any(StructureRetriever.class),
                                                 Mockito.any(MovementDirection.class)))
               .thenAnswer(invoc -> new SetOpenDirection(invoc.getArgument(0, ICommandSender.class), localizer,
                                                         ITextFactory.getSimpleTextFactory(),
                                                         invoc.getArgument(1, StructureRetriever.class),
                                                         invoc.getArgument(2, MovementDirection.class)));
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
