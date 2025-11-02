package nl.pim16aap2.animatedarchitecture.spigot.core.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GeneralSectionSpigotTest
{
    @Test
    void result_shouldSetPrimaryCommandNameFromFirstAlias()
    {
        // setup
        final List<String> aliases = List.of("animatedarchitecture", "aa", "aarch");

        // execute
        final GeneralSectionSpigot.Result result = new GeneralSectionSpigot.Result(
            Set.of(),
            true,
            aliases,
            aliases.getFirst()
        );

        // verify
        assertThat(result.primaryCommandName()).isEqualTo("animatedarchitecture");
    }

    @Test
    void result_shouldSetPrimaryCommandNameFromSingleAlias()
    {
        // setup
        final List<String> aliases = List.of("aa");

        // execute
        final GeneralSectionSpigot.Result result = new GeneralSectionSpigot.Result(
            Set.of(),
            true,
            aliases,
            aliases.getFirst()
        );

        // verify
        assertThat(result.primaryCommandName()).isEqualTo("aa");
    }

    @Test
    void defaultResult_shouldHaveCorrectPrimaryCommandName()
    {
        // execute & verify
        assertThat(GeneralSectionSpigot.Result.DEFAULT.primaryCommandName())
            .isEqualTo("animatedarchitecture");
    }
}
