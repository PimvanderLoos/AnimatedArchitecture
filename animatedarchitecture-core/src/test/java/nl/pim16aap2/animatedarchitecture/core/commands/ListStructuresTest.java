package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.testing.MockInjector;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ListStructuresTest
{
    private List<Structure> structures;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer playerCommandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IServer serverCommandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private ListStructures.IFactory factory;

    @BeforeEach
    void init()
    {
        final int size = 3;
        structures = new ArrayList<>(size);
        for (int idx = 0; idx < size; ++idx)
            structures.add(Mockito.mock(Structure.class));

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newListStructures(
                Mockito.any(ICommandSender.class),
                Mockito.any(StructureRetriever.class)))
            .thenAnswer(invoc -> new ListStructures(
                invoc.getArgument(0, ICommandSender.class),
                localizer,
                ITextFactory.getSimpleTextFactory(),
                invoc.getArgument(1, StructureRetriever.class))
            );
    }

    @Test
    void testBypass()
    {
        final var retriever = MockInjector.injectInto(StructureRetrieverFactory.class).ofStructures(structures);

        // No structures will be found, because the command sender is not an owner of any them.
        CommandTestingUtil.initCommandSenderPermissions(playerCommandSender, true, false);
        Assertions.assertDoesNotThrow(
            () -> factory.newListStructures(playerCommandSender, retriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(playerCommandSender)
            .sendMessage(UnitTestUtil.textArgumentMatcher("commands.list_structures.error.no_structures_found"));

        // Run it again, but now do so with admin permissions enabled.
        // As a result, we should NOT get the "No structures found!" message again.
        CommandTestingUtil.initCommandSenderPermissions(playerCommandSender, true, true);
        Assertions.assertDoesNotThrow(
            () -> factory.newListStructures(playerCommandSender, retriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(playerCommandSender)
            .sendMessage(UnitTestUtil.textArgumentMatcher("commands.list_structures.error.no_structures_found"));


        Assertions.assertDoesNotThrow(
            () -> factory.newListStructures(serverCommandSender, retriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(serverCommandSender, Mockito.never())
            .sendMessage(UnitTestUtil.textArgumentMatcher("commands.list_structures.error.no_structures_found"));
    }
}
