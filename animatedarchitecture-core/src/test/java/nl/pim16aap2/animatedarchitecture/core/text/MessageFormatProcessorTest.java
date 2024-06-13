package nl.pim16aap2.animatedarchitecture.core.text;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class MessageFormatProcessorTest
{
    @Test
    void testFormatting()
    {
        Assertions.assertEquals("Test", new MessageFormatProcessor("Test").getFormattedString());
        Assertions.assertEquals("Test", new MessageFormatProcessor("Test", "Ignored").getFormattedString());
        Assertions.assertEquals(
            "Test 3.14",
            new MessageFormatProcessor("Test {0,number,#.##}", Math.PI).getFormattedString()
        );
    }

    @Test
    void testSections()
    {
        final var proc = new MessageFormatProcessor("Hello there, {0}!", "John Smith");

        Assertions.assertEquals(
            List.of(new MessageFormatProcessor.MessageFormatSection(0, 13, -1),
                new MessageFormatProcessor.MessageFormatSection(13, 23, 0),
                new MessageFormatProcessor.MessageFormatSection(23, 24, -1)),
            proc.getSections()
        );

        Assertions.assertEquals("Hello there, John Smith!", proc.getFormattedString());
    }

    @Test
    void testMixedParameterOrdering()
    {
        final var proc = new MessageFormatProcessor("{2} {1} {0}", "C", "B", "A");

        Assertions.assertEquals("A B C", proc.getFormattedString());

        Assertions.assertEquals(
            List.of(new MessageFormatProcessor.MessageFormatSection(0, 1, 2),
                new MessageFormatProcessor.MessageFormatSection(1, 2, -1),
                new MessageFormatProcessor.MessageFormatSection(2, 3, 1),
                new MessageFormatProcessor.MessageFormatSection(3, 4, -1),
                new MessageFormatProcessor.MessageFormatSection(4, 5, 0)),
            proc.getSections()
        );
    }
}
