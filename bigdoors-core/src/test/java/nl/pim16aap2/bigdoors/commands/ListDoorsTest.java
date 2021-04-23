package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import lombok.val;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
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

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initCommandSenderPermissions;
import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initPlatform;

class ListDoorsTest
{
    private IBigDoorsPlatform platform;

    private List<AbstractDoorBase> doors;

    private List<AbstractDoorBase> emptyList = Collections.unmodifiableList(new ArrayList<>(0));

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer playerCommandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPServer serverCommandSender;

    @BeforeEach
    void init()
    {
        platform = initPlatform();
        MockitoAnnotations.openMocks(this);

        final int size = 3;
        doors = new ArrayList<>(size);
        for (int idx = 0; idx < size; ++idx)
            doors.add(Mockito.mock(AbstractDoorBase.class));

        Mockito.when(platform.getDelayedCommandInputManager()).thenReturn(new DelayedCommandInputManager());
    }

    @Test
    @SneakyThrows
    void testBypass()
    {
        val retriever = Mockito.mock(DoorRetriever.class);
        Mockito.when(retriever.getDoors()).thenReturn(CompletableFuture.completedFuture(doors));
        Mockito.when(retriever.getDoors(Mockito.any())).thenReturn(CompletableFuture.completedFuture(emptyList));

        initCommandSenderPermissions(playerCommandSender, true, false);
        Assertions.assertTrue(ListDoors.run(playerCommandSender, retriever).get(1, TimeUnit.SECONDS));
        Mockito.verify(playerCommandSender).sendMessage("No doors found!");

        // Run it again, byt now do so with admin permissions enabled.
        // As a result, we should NOT get the "No doors found!" message again.
        initCommandSenderPermissions(playerCommandSender, true, true);
        Assertions.assertTrue(ListDoors.run(playerCommandSender, retriever).get(1, TimeUnit.SECONDS));
        Mockito.verify(playerCommandSender).sendMessage("No doors found!");


        Assertions.assertTrue(ListDoors.run(serverCommandSender, retriever).get(1, TimeUnit.SECONDS));
        Mockito.verify(serverCommandSender, Mockito.never()).sendMessage("No doors found!");
    }
}
