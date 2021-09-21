package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetrieverFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

class ListDoorsTest
{
    private List<AbstractDoor> doors;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer playerCommandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPServer serverCommandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private ListDoors.IFactory factory;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        final int size = 3;
        doors = new ArrayList<>(size);
        for (int idx = 0; idx < size; ++idx)
            doors.add(Mockito.mock(AbstractDoor.class));

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newListDoors(Mockito.any(ICommandSender.class),
                                          Mockito.any(DoorRetriever.class)))
               .thenAnswer(invoc -> new ListDoors(invoc.getArgument(0, ICommandSender.class), localizer,
                                                  invoc.getArgument(1, DoorRetriever.class)));
    }

    @Test
    @SneakyThrows
    void testBypass()
    {
        DoorRetriever retriever = DoorRetrieverFactory.ofDoors(doors);

        // No doors will be found, because the command sender is not an owner of any them.
        CommandTestingUtil.initCommandSenderPermissions(playerCommandSender, true, false);
        Assertions.assertTrue(factory.newListDoors(playerCommandSender, retriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(playerCommandSender).sendMessage("commands.list_doors.error.no_doors_found");

        // Run it again, but now do so with admin permissions enabled.
        // As a result, we should NOT get the "No doors found!" message again.
        CommandTestingUtil.initCommandSenderPermissions(playerCommandSender, true, true);
        Assertions.assertTrue(factory.newListDoors(playerCommandSender, retriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(playerCommandSender).sendMessage("commands.list_doors.error.no_doors_found");


        Assertions.assertTrue(factory.newListDoors(serverCommandSender, retriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(serverCommandSender, Mockito.never()).sendMessage("commands.list_doors.error.no_doors_found");
    }
}
