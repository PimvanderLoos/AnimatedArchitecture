package nl.pim16aap2.bigdoors.core.text;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TextTest
{
    final ColorScheme colorScheme = ColorScheme.builder()
                                               .addStyle(TextType.ERROR, "!", "?")
                                               .addStyle(TextType.INFO, "~~", "||")
                                               .addStyle(TextType.HIGHLIGHT, "___", "---")
                                               .build();

    @Test
    void subsection()
    {
        final Text text = new Text(colorScheme).append("123456789", TextType.HIGHLIGHT);

        Assertions.assertEquals("123", text.subsection(0, 3).toPlainString());
        Assertions.assertEquals("456", text.subsection(3, 6).toPlainString());
        Assertions.assertEquals("789", text.subsection(6, 9).toPlainString());

        Assertions.assertThrows(RuntimeException.class, () -> text.subsection(-1, 4));
        Assertions.assertThrows(RuntimeException.class, () -> text.subsection(0, 11));
        Assertions.assertThrows(RuntimeException.class, () -> text.subsection(4, 3));
        Assertions.assertThrows(RuntimeException.class, () -> text.subsection(4, 4));
    }

    @Test
    void styledSubsection()
    {
        final Text text = new Text(colorScheme)
            .append("123", TextType.ERROR)
            .append("456", TextType.INFO)
            .append("789", TextType.HIGHLIGHT);

        Assertions.assertEquals("!1?", text.subsection(0, 1).toString());
        Assertions.assertEquals("!123?~~4||", text.subsection(0, 4).toString());
        Assertions.assertEquals("~~56||___789---", text.subsection(4, 9).toString());
    }

    @Test
    void toStringTest()
    {
        final Text textA = new Text(colorScheme);
        final Text textB = new Text(colorScheme);

        textA.append("abc", TextType.ERROR);
        textB.append("def", TextType.INFO);
        Assertions.assertEquals("!abc?~~def||", textA.append(textB).toString());
    }

    @Test
    void append()
    {
        final Text textA = new Text(colorScheme).append("abcdef");
        final Text textB = new Text(colorScheme).append("ghifjk", TextType.INFO);

        final Text result = new Text(textA).append(textB);

        Assertions.assertEquals("abcdefghifjk", result.toPlainString());
        Assertions.assertEquals("abcdef~~ghifjk||", result.toString());
        Assertions.assertEquals(12, result.getLength());
        Assertions.assertEquals(16, result.getStyledLength());
    }

    @Test
    void prepend()
    {
        final Text textA = new Text(colorScheme).append("abc", TextType.ERROR);
        final Text textB = new Text(colorScheme).append("def", TextType.INFO);

        final Text result = new Text(textA).prepend(textB);

        Assertions.assertEquals(result, new Text(textB).append(textA));
        Assertions.assertEquals("~~def||!abc?", result.toString());
        Assertions.assertEquals("defabc", result.toPlainString());
        Assertions.assertEquals(6, result.getLength());
        Assertions.assertEquals(12, result.getStyledLength());
    }

    @Test
    void addStyled()
    {
        final Text textA = new Text(colorScheme);
        final Text textB = new Text(colorScheme);

        textA.append("abc", TextType.ERROR);
        textB.append("def", TextType.INFO);

        Assertions.assertEquals(3, textA.getLength());
        Assertions.assertEquals(3 + 2, textA.getStyledLength()); // +2 for the style.

        Assertions.assertEquals(3, textB.getLength());
        Assertions.assertEquals(3 + 4, textB.getStyledLength()); // +4 for the style.

        final Text textAB = new Text(textA).append(textB);
        Assertions.assertEquals("abcdef", textAB.toPlainString());
        Assertions.assertEquals("!abc?~~def||", textAB.toString());
    }

    @Test
    void testEquals()
    {
        final Text textA = new Text(colorScheme);
        final Text textB = new Text(colorScheme);

        textA.append("A", TextType.INFO);
        textB.append("B", TextType.ERROR);

        textA.append("B", TextType.ERROR);
        textB.prepend(new Text(colorScheme).append("A", TextType.INFO));
        Assertions.assertEquals(textA, textB);

        final Text textC = new Text(textA).append("C", TextType.HIGHLIGHT);
        final Text textD = new Text(textB).append("C", TextType.INFO);
        Assertions.assertNotEquals(textC, textD);

        final Text textE = new Text(textA).append("C");
        final Text textF = new Text(textB).append("D");
        Assertions.assertNotEquals(textE, textF);
    }

    @Test
    void testHashCode()
    {
        final Text textA = new Text(colorScheme);
        final Text textB = new Text(colorScheme);

        textA.append("A", TextType.INFO);
        textB.append("B", TextType.ERROR);

        textA.append("B", TextType.ERROR);
        textB.prepend(new Text(colorScheme).append("A", TextType.INFO));
        Assertions.assertEquals(textA.hashCode(), textB.hashCode());

        final Text textC = new Text(textA).append("C", TextType.HIGHLIGHT);
        final Text textD = new Text(textB).append("C", TextType.INFO);
        Assertions.assertNotEquals(textC.hashCode(), textD.hashCode());

        final Text textE = new Text(textA).append("C");
        final Text textF = new Text(textB).append("D");
        Assertions.assertNotEquals(textE.hashCode(), textF.hashCode());
    }
}
