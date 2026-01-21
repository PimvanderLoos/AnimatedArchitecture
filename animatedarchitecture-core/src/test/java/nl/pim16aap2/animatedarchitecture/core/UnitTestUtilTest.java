package nl.pim16aap2.animatedarchitecture.core;

import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.text.ITextComponentFactory;
import nl.pim16aap2.animatedarchitecture.core.text.Text;
import nl.pim16aap2.animatedarchitecture.core.text.TextArgument;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.exceptions.verification.NoInteractionsWanted;
import org.mockito.junit.jupiter.MockitoExtension;

import static nl.pim16aap2.animatedarchitecture.core.UnitTestUtil.textArgumentMatcher;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class UnitTestUtilTest
{
    @Mock
    private ICommandSender commandSender;

    @Test
    void verifyNoMoreMessagesSent_throwsNpeWhenNullValueProvided()
    {
        //noinspection DataFlowIssue
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> UnitTestUtil.verifyNoMoreMessagesSent(commandSender, null));
    }

    @ParameterizedTest
    @EnumSource(value = SendMessageMethod.class)
    void verifyNoMoreMessagesSent_shouldThrowExceptionForUnverifiedMessages(SendMessageMethod method)
    {
        // Setup
        method.sendMessage(commandSender);

        // Execute & Verify
        assertThatExceptionOfType(NoInteractionsWanted.class)
            .isThrownBy(() -> UnitTestUtil.verifyNoMoreMessagesSent(commandSender))
            .withMessageContaining(
                "-> at nl.pim16aap2.animatedarchitecture.core.UnitTestUtilTest$SendMessageMethod$%d.sendMessage(",
                1 + method.ordinal())
            .withMessageContaining("Actually, above is the only interaction with this mock.");
    }

    @ParameterizedTest
    @EnumSource(value = SendMessageMethod.class)
    void verifyNoMoreMessagesSent_shouldNotThrowExceptionForVerifiedMessages(SendMessageMethod method)
    {
        // Setup
        method.sendMessage(commandSender);

        // Execute
        method.verifyMessageSent(commandSender);

        // Verify
        assertThatCode(() -> UnitTestUtil.verifyNoMoreMessagesSent(commandSender))
            .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = SendMessageMethod.class)
    @SuppressWarnings("DirectInvocationOnMock")
    void verifyNoMoreMessagesSent_shouldNotThrowExceptionForOtherUnverifiedMethods(SendMessageMethod method)
    {
        // Setup
        method.sendMessage(commandSender);
        method.verifyMessageSent(commandSender);

        // Execute
        commandSender.getPlayer();

        // Verify
        assertThatCode(() -> UnitTestUtil.verifyNoMoreMessagesSent(commandSender))
            .doesNotThrowAnyException();
    }

    @Test
    void verifyNoMoreMessagesSent_shouldNotThrowExceptionWithoutAnyInteractions()
    {
        // Execute & Verify
        assertThatCode(() -> UnitTestUtil.verifyNoMoreMessagesSent(commandSender))
            .doesNotThrowAnyException();
    }

    @Test
    void asserThatAllSendMessagesAreTested()
    {
        assertThat(UnitTestUtil.SEND_MESSAGE_METHODS)
            .hasSize(SendMessageMethod.values().length);
    }

    private enum SendMessageMethod
    {
        /**
         * {@link ICommandSender#sendMessage(Text)}.
         */
        SEND_MESSAGE_TEXT
            {
                @Override
                void sendMessage(ICommandSender commandSender)
                {
                    commandSender.sendMessage(
                        new Text(ITextComponentFactory.SimpleTextComponentFactory.INSTANCE, null).append(name()));
                }

                @Override
                void verifyMessageSent(ICommandSender commandSender)
                {
                    verify(commandSender).sendMessage(textArgumentMatcher(name()));
                }
            },

        /**
         * {@link ICommandSender#sendMessage(TextType, String, Text.ArgumentCreator...)}.
         */
        SEND_MESSAGE_MANUAL
            {
                @Override
                void sendMessage(ICommandSender commandSender)
                {
                    commandSender.sendMessage(
                        TextType.CLICKABLE,
                        name(),
                        getArgumentCreator(this)
                    );
                }

                @Override
                void verifyMessageSent(ICommandSender commandSender)
                {
                    verify(commandSender).sendMessage(
                        TextType.CLICKABLE,
                        name(),
                        getArgumentCreator(this)
                    );
                }
            },

        /**
         * {@link ICommandSender#sendError(String, Text.ArgumentCreator...)}.
         */
        SEND_ERROR
            {
                @Override
                void sendMessage(ICommandSender commandSender)
                {
                    commandSender.sendError(name(), getArgumentCreator(this));
                }

                @Override
                void verifyMessageSent(ICommandSender commandSender)
                {
                    verify(commandSender).sendError(name(), getArgumentCreator(this));
                }
            },

        /**
         * {@link ICommandSender#sendSuccess(String, Text.ArgumentCreator...)}.
         */
        SEND_SUCCESS
            {
                @Override
                void sendMessage(ICommandSender commandSender)
                {
                    commandSender.sendSuccess(name(), getArgumentCreator(this));
                }

                @Override
                void verifyMessageSent(ICommandSender commandSender)
                {
                    verify(commandSender).sendSuccess(name(), getArgumentCreator(this));
                }
            },

        /**
         * {@link ICommandSender#sendInfo(String, Text.ArgumentCreator...)}.
         */
        SEND_INFO
            {
                @Override
                void sendMessage(ICommandSender commandSender)
                {
                    commandSender.sendInfo(name(), getArgumentCreator(this));
                }

                @Override
                void verifyMessageSent(ICommandSender commandSender)
                {
                    verify(commandSender).sendInfo(name(), getArgumentCreator(this));
                }
            },
        ;

        /**
         * Sends a message to the command sender.
         *
         * @param commandSender
         *     The command sender to send the message to.
         */
        abstract void sendMessage(ICommandSender commandSender);

        /**
         * Verifies that a message was sent to the command sender.
         *
         * @param commandSender
         *     The command sender to verify the message was sent to.
         */
        abstract void verifyMessageSent(ICommandSender commandSender);

        private final Text.ArgumentCreator argumentCreator;

        SendMessageMethod()
        {
            argumentCreator = ignored -> new TextArgument("[" + ordinal() + "] arg1");
        }

        private static Text.ArgumentCreator getArgumentCreator(SendMessageMethod sendMessageMethod)
        {
            return sendMessageMethod.argumentCreator;
        }
    }
}
