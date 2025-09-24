package nl.pim16aap2.animatedarchitecture.spigot.core.config;

import nl.altindag.log.LogCaptor;
import nl.pim16aap2.testing.annotations.WithLogCapture;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static nl.pim16aap2.testing.assertions.LogCaptorAssert.assertThatLogCaptor;
import static org.assertj.core.api.Assertions.*;

@WithLogCapture
@ExtendWith(MockitoExtension.class)
class MaterialParserTest
{
    private static final String INVALID_MATERIAL = "INVALID_MATERIAL";

    @Test
    void parse_list_shouldReturnParsedMaterialsForValidInput()
    {
        // setup
        final MaterialParser parser = MaterialParser.builder()
            .context("TestContext")
            .defaultMaterial(Material.BEDROCK)
            .build();
        final List<String> materials = Arrays.asList(
            Material.STONE.name(),
            Material.DIRT.name(),
            Material.IRON_BLOCK.name()
        );

        // execute
        final Set<Material> result = parser.parse(materials, false);

        // verify
        assertThat(result)
            .containsExactlyInAnyOrder(Material.STONE, Material.DIRT, Material.IRON_BLOCK);
    }

    @Test
    void parse_list_shouldReturnDefaultMaterialsForNullInput()
    {
        // setup
        final MaterialParser parser = MaterialParser.builder()
            .context("TestContext")
            .defaultMaterial(Material.STONE)
            .defaultMaterial(Material.DIRT)
            .build();

        // execute
        final Set<Material> result = parser.parse((List<String>) null, false);

        // verify
        assertThat(result)
            .containsExactlyInAnyOrder(Material.STONE, Material.DIRT);
    }

    @Test
    void parse_list_shouldReturnDefaultMaterialsForEmptyInput(LogCaptor logCaptor)
    {
        // setup
        final Set<Material> defaults = EnumSet.of(Material.STONE, Material.DIRT);
        final MaterialParser parser = MaterialParser.builder()
            .context("TestContext")
            .defaultMaterials(defaults)
            .build();
        final List<String> materials = List.of();

        // execute
        final Set<Material> result = parser.parse(materials, false);

        // verify
        assertThat(result).isEqualTo(defaults);
        assertThatLogCaptor(logCaptor)
            .atDebug()
            .singleWithMessageExactly(
                "No materials provided for %s. Using default materials: %s",
                "TestContext",
                defaults
            );
    }

    @Test
    void parse_list_shouldSkipInvalidAndLogInvalidMaterialsUsingWarnAndSkipAction(LogCaptor logCaptor)
    {
        // setup
        final MaterialParser parser = MaterialParser.builder()
            .context("TestContext")
            .onInvalid(MaterialParser.Action.WARN_AND_SKIP)
            .build();
        final List<String> materials = Arrays.asList(
            Material.STONE.name(),
            INVALID_MATERIAL,
            Material.DIRT.name()
        );

        // execute
        final Set<Material> result = parser.parse(materials, false);

        // verify
        assertThat(result)
            .containsExactlyInAnyOrder(Material.STONE, Material.DIRT);
        assertThatLogCaptor(logCaptor)
            .atError()
            .singleWithMessageExactly("[%s] Invalid material: %s. Skipping.", "TestContext", INVALID_MATERIAL);
    }

    @Test
    void parse_list_shouldFilterMaterialsBySolidityConstraint()
    {
        // setup
        final MaterialParser parser = MaterialParser.builder()
            .context("TestContext")
            .isSolid(true)
            .build();
        final List<String> materials = Arrays.asList(
            Material.STONE.name(),
            Material.AIR.name(),
            Material.DIRT.name()
        );

        // execute
        final Set<Material> result = parser.parse(materials, false);

        // verify
        assertThat(result)
            .contains(Material.STONE, Material.DIRT)
            .doesNotContain(Material.AIR);
    }

    @Test
    void parse_list_shouldRejectMaterialsNotMatchingSolidityConstraint(LogCaptor logCaptor)
    {
        // setup
        final MaterialParser parser = MaterialParser.builder()
            .context("TestContext")
            .isSolid(false)
            .onInvalid(MaterialParser.Action.WARN_AND_SKIP)
            .build();
        final List<String> materials = Collections.singletonList(Material.STONE.name());

        // execute
        final Set<Material> result = parser.parse(materials, false);

        // verify
        assertThat(result).isEmpty();
        assertThatLogCaptor(logCaptor)
            .atError()
            .singleWithMessageExactly("[%s] Invalid material: %s. Skipping.", "TestContext", Material.STONE);
    }

    @Test
    void parse_list_shouldThrowExceptionOnFirstInvalidMaterialWithFailAction()
    {
        // setup
        final MaterialParser parser = MaterialParser.builder()
            .context("TestContext")
            .onInvalid(MaterialParser.Action.FAIL)
            .build();
        final List<String> materials = Arrays.asList(Material.STONE.name(), INVALID_MATERIAL, Material.DIRT.name());

        // execute & verify
        assertThatThrownBy(() -> parser.parse(materials, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("[TestContext] Invalid material: %s", INVALID_MATERIAL);
    }

    @Test
    void parse_string_shouldReturnParsedMaterialForValidInput()
    {
        // setup
        final Material input = Material.DIRT;
        final MaterialParser parser = MaterialParser.builder()
            .context("TestContext")
            .defaultMaterial(Material.STONE)
            .build();

        // execute
        final Material result = parser.parse(input.name(), false);

        // verify
        assertThat(result).isEqualTo(input);
    }

    @Test
    void parse_string_shouldReturnDefaultMaterialForNullInput(LogCaptor logCaptor)
    {
        // setup
        final MaterialParser parser = MaterialParser.builder()
            .context("TestContext")
            .defaultMaterial(Material.STONE)
            .build();

        // execute
        final Material result = parser.parse((String) null, false);

        // verify
        assertThat(result).isEqualTo(Material.STONE);

        //noinspection DataFlowIssue
        assertThatLogCaptor(logCaptor)
            .atDebug()
            .singleWithMessageExactly(
                "[%s] Failed to parse material '%s'. Using default material: %s",
                "TestContext",
                null,
                Material.STONE
            );
    }

    @Test
    void parse_string_shouldReturnDefaultMaterialForEmptyInput(LogCaptor logCaptor)
    {
        // setup
        final MaterialParser parser = MaterialParser.builder()
            .context("TestContext")
            .defaultMaterial(Material.DIRT)
            .build();

        // execute
        final Material result = parser.parse("", false);

        // verify
        assertThat(result).isEqualTo(Material.DIRT);
        assertThatLogCaptor(logCaptor)
            .atDebug()
            .singleWithMessageExactly(
                "[%s] Failed to parse material '%s'. Using default material: %s",
                "TestContext",
                "",
                Material.DIRT
            );
    }

    @Test
    void parse_string_shouldThrowExceptionForInvalidMaterialWithFailAction()
    {
        // setup
        final MaterialParser parser = MaterialParser.builder()
            .context("TestContext")
            .onInvalid(MaterialParser.Action.FAIL)
            .build();

        // execute & verify
        assertThatThrownBy(() -> parser.parse(INVALID_MATERIAL, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("[TestContext] Invalid material: %s", INVALID_MATERIAL);
    }

    @Test
    void parse_string_shouldReturnDefaultMaterialForInvalidMaterialWithSkipAction(LogCaptor logCaptor)
    {
        // setup
        final MaterialParser parser = MaterialParser.builder()
            .context("TestContext")
            .defaultMaterial(Material.STONE)
            .onInvalid(MaterialParser.Action.SKIP)
            .build();

        // execute
        final Material result = parser.parse(INVALID_MATERIAL, false);

        // verify
        assertThat(result).isEqualTo(Material.STONE);
        assertThatLogCaptor(logCaptor)
            .atAllLevels()
            .hasNoneWithMessageContaining("Invalid material");
    }

    @Test
    void parse_string_shouldNotLogErrorsInSilentMode(LogCaptor logCaptor)
    {
        // setup
        final MaterialParser parser = MaterialParser.builder()
            .context("TestContext")
            .defaultMaterial(Material.STONE)
            .onInvalid(MaterialParser.Action.WARN_AND_SKIP)
            .build();

        // execute
        final Material result = parser.parse(INVALID_MATERIAL, true);

        // verify
        assertThat(result).isEqualTo(Material.STONE);
        assertThatLogCaptor(logCaptor)
            .atError()
            .isEmpty();
    }

    @Test
    void parse_string_shouldThrowExceptionWhenNoDefaultMaterialConfigured()
    {
        // setup
        final MaterialParser parser = MaterialParser.builder()
            .context("TestContext")
            .build();

        // execute & verify
        assertThatThrownBy(() -> parser.parse((String) null, false))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No default material configured for context: TestContext");
    }
}
