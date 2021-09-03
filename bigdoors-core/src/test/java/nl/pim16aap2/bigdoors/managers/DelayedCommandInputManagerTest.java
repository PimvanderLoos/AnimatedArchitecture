package nl.pim16aap2.bigdoors.managers;

import nl.pim16aap2.bigdoors.commands.DelayedCommandInputRequest;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class DelayedCommandInputManagerTest
{
    @Mock
    ICommandSender commandSender;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRegistration()
    {
        final var manager = new DelayedCommandInputManager();
        final var request = Mockito.mock(DelayedCommandInputRequest.class);

        manager.register(commandSender, request);

        final var returned = manager.getInputRequest(commandSender);
        Assertions.assertTrue(returned.isPresent());
        Assertions.assertEquals(request, returned.get());
        Mockito.verify(request, Mockito.never()).cancel();
    }

    @Test
    void testCommandSenders()
    {
        final var manager = new DelayedCommandInputManager();
        final var request = Mockito.mock(DelayedCommandInputRequest.class);
        final var altSender = Mockito.mock(ICommandSender.class);

        manager.register(commandSender, request);
        Assertions.assertTrue(manager.getInputRequest(altSender).isEmpty());
        Assertions.assertTrue(manager.getInputRequest(commandSender).isPresent());
        Mockito.verify(request, Mockito.never()).cancel();
    }

    @Test
    void testDeregistration()
    {
        final var manager = new DelayedCommandInputManager();
        final var first = Mockito.mock(DelayedCommandInputRequest.class);
        final var second = Mockito.mock(DelayedCommandInputRequest.class);
        final var altSender = Mockito.mock(ICommandSender.class);

        manager.register(commandSender, first);

        manager.deregister(commandSender, second);
        Assertions.assertTrue(manager.getInputRequest(commandSender).isPresent());

        manager.deregister(altSender, first);
        Assertions.assertTrue(manager.getInputRequest(commandSender).isPresent());

        manager.deregister(commandSender, first);
        Assertions.assertTrue(manager.getInputRequest(commandSender).isEmpty());

        Mockito.verify(first, Mockito.never()).cancel();
        Mockito.verify(second, Mockito.never()).cancel();
    }

    @Test
    void testCancelAll()
    {
        final var manager = new DelayedCommandInputManager();
        final var first = Mockito.mock(DelayedCommandInputRequest.class);

        manager.register(commandSender, first);
        manager.cancelAll(commandSender);
        Assertions.assertTrue(manager.getInputRequest(commandSender).isEmpty());
        Mockito.verify(first).cancel();
    }

    @Test
    void testDeregisterAll()
    {
        final var manager = new DelayedCommandInputManager();

        final var first = Mockito.mock(DelayedCommandInputRequest.class);

        manager.register(commandSender, first);
        manager.deregisterAll(commandSender);
        Assertions.assertTrue(manager.getInputRequest(commandSender).isEmpty());
        Mockito.verify(first, Mockito.never()).cancel();
    }

    @Test
    void testOverride()
    {
        final var manager = new DelayedCommandInputManager();
        final var first = Mockito.mock(DelayedCommandInputRequest.class);
        final var second = Mockito.mock(DelayedCommandInputRequest.class);

        manager.register(commandSender, first);
        manager.register(commandSender, second);

        Mockito.verify(first).cancel();

        final var returned = manager.getInputRequest(commandSender);
        Assertions.assertTrue(returned.isPresent());
        Assertions.assertNotEquals(first, returned.get());
        Assertions.assertEquals(second, returned.get());
        Mockito.verify(second, Mockito.never()).cancel();
    }
}
