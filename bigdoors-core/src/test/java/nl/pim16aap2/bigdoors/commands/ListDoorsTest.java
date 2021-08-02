package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import lombok.val;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.managers.DelayedCommandInputManager;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.bigdoors.UnitTestUtil.initPlatform;
import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initCommandSenderPermissions;

class ListDoorsTest
{
    private List<AbstractDoor> doors;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer playerCommandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPServer serverCommandSender;

    @BeforeEach
    void init()
    {
        val platform = initPlatform();
        MockitoAnnotations.openMocks(this);

        final int size = 3;
        doors = new ArrayList<>(size);
        for (int idx = 0; idx < size; ++idx)
            doors.add(Mockito.mock(AbstractDoor.class));

        Mockito.when(platform.getDelayedCommandInputManager()).thenReturn(new DelayedCommandInputManager());
    }

    @Test
    @SneakyThrows
    void testBypass()
    {
        val retriever = Mockito.mock(DoorRetriever.class);
        Mockito.when(retriever.getDoors()).thenReturn(CompletableFuture.completedFuture(doors));
        Mockito.when(retriever.getDoors(Mockito.any()))
               .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

        initCommandSenderPermissions(playerCommandSender, true, false);
        Assertions.assertTrue(ListDoors.run(playerCommandSender, retriever).get(1, TimeUnit.SECONDS));
        Mockito.verify(playerCommandSender).sendMessage("commands.list_doors.error.no_doors_found");

        // Run it again, byt now do so with admin permissions enabled.
        // As a result, we should NOT get the "No doors found!" message again.
        initCommandSenderPermissions(playerCommandSender, true, true);
        Assertions.assertTrue(ListDoors.run(playerCommandSender, retriever).get(1, TimeUnit.SECONDS));
        Mockito.verify(playerCommandSender).sendMessage("commands.list_doors.error.no_doors_found");


        Assertions.assertTrue(ListDoors.run(serverCommandSender, retriever).get(1, TimeUnit.SECONDS));
        Mockito.verify(serverCommandSender, Mockito.never()).sendMessage("commands.list_doors.error.no_doors_found");
    }
}
