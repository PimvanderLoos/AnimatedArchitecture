package nl.pim16aap2.animatedarchitecture.spigot.util.implementations;

import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class WrappedPlayerTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer player;

    @Test
    void formatCommand_shouldAddLeadingSlashForPlayers()
    {
        // execute & verify
        assertThat(player.formatCommand("animatedarchitecture", "help"))
            .isEqualTo("/animatedarchitecture help");
    }

    @Test
    void formatCommand_shouldFormatWithArguments()
    {
        // execute & verify
        assertThat(player.formatCommand("cmd", "%s %d", "arg", 123))
            .isEqualTo("/cmd arg 123");
    }

    @Test
    void formatCommand_shouldNotAddTrailingSpaceWhenSubCommandIsEmpty()
    {
        // execute & verify
        assertThat(player.formatCommand("animatedarchitecture", ""))
            .isEqualTo("/animatedarchitecture");
    }

    @ParameterizedTest
    @MethodSource("LocaleParserInputOutputPairTestCases")
    void parseLocale_parameterizedTests(LocaleParserInputOutputPair testCase)
    {
        Locale result = WrappedPlayer.parseLocale(testCase.input());
        assertThat(result).isEqualTo(testCase.expected());
    }

    private static Stream<LocaleParserInputOutputPair> LocaleParserInputOutputPairTestCases()
    {
        return Stream.of(
            new LocaleParserInputOutputPair(null, null),
            new LocaleParserInputOutputPair("", null),
            new LocaleParserInputOutputPair("en", Locale.forLanguageTag("en")),
            new LocaleParserInputOutputPair("en_US", Locale.forLanguageTag("en-us")),
            new LocaleParserInputOutputPair("en-US", Locale.forLanguageTag("en-us"))
        );
    }

    private record LocaleParserInputOutputPair(@Nullable String input, @Nullable Locale expected)
    {
    }
}
