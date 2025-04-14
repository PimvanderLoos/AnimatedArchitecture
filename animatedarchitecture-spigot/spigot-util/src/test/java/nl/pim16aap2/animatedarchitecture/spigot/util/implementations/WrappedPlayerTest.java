package nl.pim16aap2.animatedarchitecture.spigot.util.implementations;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class WrappedPlayerTest
{
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
