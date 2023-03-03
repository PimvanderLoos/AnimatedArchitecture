package nl.pim16aap2.animatedarchitecture.core.text;

import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TextTest
{
    final ColorScheme<Style> colorScheme =
        ColorScheme.<Style>builder()
                   .addStyle(TextType.ERROR, new Style("<err>", "</err>"))
                   .addStyle(TextType.INFO, new Style("<info>", "</info>"))
                   .setDefaultStyle(new Style("", ""))
                   .build();

    final TextComponentFactory textComponentFactory = new TextComponentFactory(colorScheme);

    @Test
    void subsection()
    {
        final Text text = new Text(textComponentFactory).append("123456789", TextType.HIGHLIGHT);

        Assertions.assertEquals("123", text.subsection(0, 3).toString());
        Assertions.assertEquals("456", text.subsection(3, 6).toString());
        Assertions.assertEquals("789", text.subsection(6, 9).toString());

        Assertions.assertThrows(RuntimeException.class, () -> text.subsection(-1, 4));
        Assertions.assertThrows(RuntimeException.class, () -> text.subsection(0, 11));
        Assertions.assertThrows(RuntimeException.class, () -> text.subsection(4, 3));
        Assertions.assertThrows(RuntimeException.class, () -> text.subsection(4, 4));
    }

    @Test
    void styledSubsection()
    {
        final Text text = new Text(textComponentFactory)
            .append("123", TextType.ERROR)
            .append("456", TextType.INFO)
            .append("789", TextType.HIGHLIGHT);

        Assertions.assertEquals("<err>1</err>", text.subsection(0, 1).render(new Renderer()));
        Assertions.assertEquals("<err>123</err><info>4</info>", text.subsection(0, 4).render(new Renderer()));
        Assertions.assertEquals("<info>56</info>789", text.subsection(4, 9).render(new Renderer()));
    }

    @Test
    void toStringTest()
    {
        final Text textA = new Text(textComponentFactory);
        final Text textB = new Text(textComponentFactory);

        textA.append("abc", TextType.ERROR);
        textB.append("def", TextType.INFO);
        Assertions.assertEquals("<err>abc</err><info>def</info>", textA.append(textB).render(new Renderer()));
    }

    @Test
    void append()
    {
        final Text textA = new Text(textComponentFactory).append("abcdef");
        final Text textB = new Text(textComponentFactory).append("ghifjk", TextType.INFO);

        final Text result = new Text(textA).append(textB);

        Assertions.assertEquals("abcdefghifjk", result.toString());
        Assertions.assertEquals("abcdef<info>ghifjk</info>", result.render(new Renderer()));
        Assertions.assertEquals(12, result.getLength());
    }

    @Test
    void prepend()
    {
        final Text textA = new Text(textComponentFactory).append("abc", TextType.ERROR);
        final Text textB = new Text(textComponentFactory).append("def", TextType.INFO);

        final Text result = new Text(textA).prepend(textB);

        Assertions.assertEquals(result, new Text(textB).append(textA));
        Assertions.assertEquals("<info>def</info><err>abc</err>", result.render(new Renderer()));
        Assertions.assertEquals("defabc", result.toString());
        Assertions.assertEquals(6, result.getLength());
    }

    @Test
    void addStyled()
    {
        final Text textA = new Text(textComponentFactory);
        final Text textB = new Text(textComponentFactory);

        textA.append("abc", TextType.ERROR);
        textB.append("def", TextType.INFO);

        Assertions.assertEquals(3, textA.getLength());
        Assertions.assertEquals(3, textB.getLength());

        final Text textAB = new Text(textA).append(textB);
        Assertions.assertEquals("abcdef", textAB.toString());
        Assertions.assertEquals("<err>abc</err><info>def</info>", textAB.render(new Renderer()));
    }

    @Test
    void testEquals()
    {
        final Text textA = new Text(textComponentFactory);
        final Text textB = new Text(textComponentFactory);

        textA.append("A", TextType.INFO);
        textB.append("B", TextType.ERROR);

        textA.append("B", TextType.ERROR);
        textB.prepend(new Text(textComponentFactory).append("A", TextType.INFO));
        Assertions.assertEquals(textA, textB);

        final Text textC = new Text(textA).append("C", TextType.HIGHLIGHT);
        final Text textD = new Text(textB).append("C", TextType.INFO);
        Assertions.assertNotEquals(textC, textD);

        final Text textE = new Text(textA).append("C");
        final Text textF = new Text(textB).append("D");
        Assertions.assertNotEquals(textE, textF);
    }

    @Test
    void testCommand()
    {
        Assertions.assertEquals(
            "<a href=\"my_url\">CLICK HERE</a>",
            new Text(textComponentFactory)
                .appendClickableText("CLICK HERE", null, "my_url", null)
                .render(new Renderer()));

        Assertions.assertEquals(
            "<a href=\"my_url\"><info>CLICK HERE</info></a>",
            new Text(textComponentFactory)
                .appendClickableText("CLICK HERE", TextType.INFO, "my_url", null)
                .render(new Renderer()));

        Assertions.assertEquals(
            "<a href=\"my_url\" title=\"click for help\"><info>CLICK HERE</info></a>",
            new Text(textComponentFactory)
                .appendClickableText("CLICK HERE", TextType.INFO, "my_url", "click for help")
                .render(new Renderer()));
    }

    @Test
    void testArguments()
    {
        final Text text = new Text(textComponentFactory);
        text.append("Click {1}, {2}, or {0} to do {3}!", TextType.INFO,
                    new TextArgument("HERE0", text.getClickableTextComponent(TextType.INFO, "url0", null)),
                    new TextArgument("HERE1", text.getClickableTextComponent(TextType.ERROR, "url1", null)),
                    new TextArgument("HERE2", text.getClickableTextComponent(TextType.ERROR, "url2", "hi")),
                    new TextArgument("something", text.getTextComponent(null)));

        final String result =
            "<info>Click </info>" +
                "<a href=\"url1\"><err>HERE1</err></a>" +
                "<info>, </info>" +
                "<a href=\"url2\" title=\"hi\"><err>HERE2</err></a>" +
                "<info>, or </info>" +
                "<a href=\"url0\"><info>HERE0</info></a>" +
                "<info> to do </info>" +
                "something" +
                "<info>!</info>";

        Assertions.assertEquals(result, text.render(new Renderer()));
    }

    @Test
    void testHashCode()
    {
        final Text textA = new Text(textComponentFactory);
        final Text textB = new Text(textComponentFactory);

        textA.append("A", TextType.INFO);
        textB.append("B", TextType.ERROR);

        textA.append("B", TextType.ERROR);
        textB.prepend(new Text(textComponentFactory).append("A", TextType.INFO));
        Assertions.assertEquals(textA.hashCode(), textB.hashCode());

        final Text textC = new Text(textA).append("C", TextType.HIGHLIGHT);
        final Text textD = new Text(textB).append("C", TextType.INFO);
        Assertions.assertNotEquals(textC.hashCode(), textD.hashCode());

        final Text textE = new Text(textA).append("C");
        final Text textF = new Text(textB).append("D");
        Assertions.assertNotEquals(textE.hashCode(), textF.hashCode());
    }

    @ToString
    private static final class TextComponentFactory implements ITextComponentFactory
    {
        private final ColorScheme<Style> colorScheme;

        public TextComponentFactory(ColorScheme<Style> colorScheme)
        {
            this.colorScheme = colorScheme;
        }

        @Override
        public TextComponent newComponent(@Nullable TextType type)
        {
            return new TextComponent(new StyleDecorator(colorScheme.getStyle(type)));
        }

        @Override
        public TextComponent newTextCommandComponent(@Nullable TextType type, String command, @Nullable String info)
        {
            return new TextComponent(
                new StyleDecorator(colorScheme.getStyle(type)),
                new CommandDecorator(command, info));
        }
    }

    private interface ITextTestDecorator extends ITextDecorator
    {
        String apply(String input);
    }

    private static final class Renderer implements ITextRenderer<String>
    {
        private final StringBuilder sb = new StringBuilder();

        @Override
        public void process(String text)
        {
            sb.append(text);
        }

        @Override
        public void process(String text, TextComponent component)
        {
            String result = text;
            for (final var decorator : getDecoratorsOfType(ITextTestDecorator.class, component).toList())
                result = decorator.apply(result);
            sb.append(result);
        }

        @Override
        public String getRendered()
        {
            return sb.toString();
        }
    }

    private record StyleDecorator(Style style) implements ITextTestDecorator
    {
        @Override
        public String apply(String input)
        {
            return style.on() + input + style.off();
        }
    }

    private record CommandDecorator(String command, @Nullable String info) implements ITextTestDecorator
    {
        @Override
        public String apply(String input)
        {
            final String title = info == null ? "" : (" title=\"" + info + "\"");
            return String.format("<a href=\"%s\"%s>%s</a>", command, title, input);
        }
    }

    private record Style(String on, String off)
    {
    }
}
