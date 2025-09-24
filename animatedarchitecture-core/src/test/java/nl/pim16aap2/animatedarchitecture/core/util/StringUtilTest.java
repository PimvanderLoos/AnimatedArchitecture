package nl.pim16aap2.animatedarchitecture.core.util;

import it.unimi.dsi.fastutil.ints.IntImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class StringUtilTest
{
    @Test
    void testRandomString()
    {
        final var result = StringUtil.randomString(10);
        assertEquals(10, result.length());
    }

    @Test
    void testHasTrailingNewLine()
    {
        assertTrue(StringUtil.hasTrailingNewLine("Hello\n"));
        assertTrue(StringUtil.hasTrailingNewLine("\n"));
        assertFalse(StringUtil.hasTrailingNewLine("Hello"));
        assertFalse(StringUtil.hasTrailingNewLine(""));
    }

    @Test
    void testRemoveTrailingNewLinesStringBuilder()
    {
        StringBuilder sb = new StringBuilder("Hello\n");
        assertEquals("Hello", StringUtil.removeTrailingNewLines(sb).toString());

        sb = new StringBuilder("Hello\nHello\n");
        assertEquals("Hello\nHello", StringUtil.removeTrailingNewLines(sb).toString());

        sb = new StringBuilder("\nHello\nHello\n\n\n");
        assertEquals("\nHello\nHello", StringUtil.removeTrailingNewLines(sb).toString());

        sb = new StringBuilder("Hello");
        assertEquals("Hello", StringUtil.removeTrailingNewLines(sb).toString());

        sb = new StringBuilder();
        assertEquals("", StringUtil.removeTrailingNewLines(sb).toString());
    }

    @Test
    void testRemoveTrailingNewLinesString()
    {
        assertEquals("Hello", StringUtil.removeTrailingNewLines("Hello\n"));
        assertEquals("Hello\nHello", StringUtil.removeTrailingNewLines("Hello\nHello\n"));
        assertEquals("\nHello\nHello", StringUtil.removeTrailingNewLines("\nHello\nHello\n\n\n"));
        assertEquals("Hello", StringUtil.removeTrailingNewLines("Hello"));
        assertEquals("", StringUtil.removeTrailingNewLines(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"3-21", "mydoor", "MyDoor", "A", "a", "a0", "0a0"})
    void isValidStructureName(String name)
    {
        assertTrue(StringUtil.isValidStructureName(name));
    }

    @ParameterizedTest
    @CsvSource(
        {
            "hello, Hello",
            "WORLD, WORLD",
            "a, A",
        }
    )
    void testCapitalizeFirstLetter(String input, String expected)
    {
        assertEquals(expected, StringUtil.capitalizeFirstLetter(input));
    }

    @Test
    void testCountPatternOccurrences()
    {
        Pattern pattern = Pattern.compile("\\?");
        assertEquals(3, StringUtil.countPatternOccurrences(pattern, "How are you? Where? Why?"));
        assertEquals(0, StringUtil.countPatternOccurrences(pattern, "No questions here."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "0", "0999", "321", "my door", "myDoor!", "myDoor?"})
    void isInvalidStructureName(String name)
    {
        assertFalse(StringUtil.isValidStructureName(name));
    }

    @Test
    void isInvalidStructureNameNull()
    {
        assertFalse(StringUtil.isValidStructureName(null));
    }

    @Test
    void testStringCollector()
    {
        final var collector = StringUtil.stringCollector("\n  - ", "{}");

        List<String> items = Arrays.asList("1", "2", "3");
        String result = items.stream().collect(collector);
        assertEquals("\n  - 1\n  - 2\n  - 3", result);

        items = List.of();
        //noinspection RedundantOperationOnEmptyContainer
        result = items.stream().collect(collector);
        assertEquals("{}", result);
    }

    @Test
    void testGetVariableIndicesMissingCharacter()
    {
        Assertions.assertTrue(StringUtil.getVariableIndices("Hello, World!", 'q').isEmpty());
    }

    @Test
    void testGetVariableIndices()
    {
        Assertions.assertEquals(
            IntImmutableList.of(4, 8),
            StringUtil.getVariableIndices("Hello, World!", 'o')
        );
    }
}
