package nl.pim16aap2.bigdoors.text;

import lombok.ToString;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a colorscheme that can be used to add styles to text.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
@ToString
public final class ColorScheme
{
    private final Map<TextType, TextComponent> styleMap;
    private final TextComponent defaultStyle;

    ColorScheme(Map<TextType, TextComponent> styleMap, @Nullable TextComponent defaultStyle)
    {
        this.styleMap = styleMap;
        this.defaultStyle = defaultStyle == null ? TextComponent.EMPTY : defaultStyle;
    }

    /**
     * Gets the style associated with a certain {@link TextType}.
     *
     * @param type
     *     The {@link TextType} for which to find its style.
     * @return The style associated with the given {@link TextType}.
     */
    public TextComponent getStyle(TextType type)
    {
        return styleMap.getOrDefault(type, defaultStyle);
    }

    /**
     * Builds a new {@link ColorScheme}.
     *
     * @return A new {@link ColorSchemeBuilder}.
     */
    public static ColorSchemeBuilder builder()
    {
        return new ColorSchemeBuilder();
    }

    @SuppressWarnings("unused")
    @ToString
    public static class ColorSchemeBuilder
    {
        private final Map<TextType, TextComponent> styleMap = new IdentityHashMap<>();

        private @Nullable TextComponent defaultStyle = null;

        private @Nullable String defaultDisable = null;

        private ColorSchemeBuilder()
        {
        }

        /**
         * Sets the String that disables all active styles in one go.
         * <p>
         * When set to anything other than null, this will be used as the default value for all {@link TextComponent}s
         * when the on value is not empty and the off value is an empty string.
         *
         * @param str
         *     The string that disables all active styles.
         * @return This {@link ColorSchemeBuilder} instance.
         */
        @Contract("_ -> this")
        public ColorSchemeBuilder setDefaultDisable(@Nullable String str)
        {
            defaultDisable = str;
            return this;
        }

        /**
         * Sets the default style that is to be used when requesting the style for an unmapped text type.
         *
         * @param defaultStyle
         *     The default style to fall back to when none is mapped for a given text type. Defaults to
         *     {@link TextComponent#EMPTY} when null.
         * @return This {@link ColorSchemeBuilder} instance.
         */
        @Contract("_ -> this")
        public ColorSchemeBuilder setDefaultStyle(@Nullable TextComponent defaultStyle)
        {
            this.defaultStyle = defaultStyle;
            return this;
        }

        /**
         * Adds a style to a given {@link TextType}.
         *
         * @param type
         *     The {@link TextType} to add a style for.
         * @param style
         *     The style to add.
         * @return This {@link ColorSchemeBuilder} instance.
         */
        @Contract("_, _-> this")
        public ColorSchemeBuilder addStyle(TextType type, TextComponent style)
        {
            styleMap.put(type, style);
            return this;
        }

        /**
         * Adds a style to a given {@link TextType}.
         *
         * @param type
         *     The {@link TextType} to add a style for.
         * @param on
         *     The String that is used to enable this component. E.g. {@code <it>}.
         * @param off
         *     The String that is used to disable this component. E.g. {@code </it>}.
         * @return This {@link ColorSchemeBuilder} instance.
         */
        @Contract("_, _, _-> this")
        public ColorSchemeBuilder addStyle(TextType type, String on, String off)
        {
            styleMap.put(type, new TextComponent(on, off));
            return this;
        }

        /**
         * Adds a style to a given {@link TextType}.
         * <p>
         * This uses the default disable set via {@link #setDefaultDisable(String)}.
         *
         * @param type
         *     The {@link TextType} to add a style for.
         * @param on
         *     The String that is used to enable this component. E.g. {@code <it>}.
         * @return This {@link ColorSchemeBuilder} instance.
         */
        @Contract("_, _-> this")
        public ColorSchemeBuilder addStyle(TextType type, String on)
        {
            styleMap.put(type, new TextComponent(
                on, Objects.requireNonNull(defaultDisable, "defaultDisable is null! Has it been set?")));
            return this;
        }

        /**
         * Constructs the new {@link ColorScheme}.
         *
         * @return The new {@link ColorScheme}.
         */
        public ColorScheme build()
        {
            prepareBuild(styleMap, defaultDisable);
            return new ColorScheme(styleMap, defaultStyle);
        }

        /**
         * Prepares this object for building.
         *
         * @param styleMap
         *     The {@link #styleMap} to prepare for building.
         * @param defaultDisable
         *     See {@link #defaultDisable}.
         */
        private static void prepareBuild(Map<TextType, TextComponent> styleMap, @Nullable String defaultDisable)
        {
            // If defaultDisable was set, apply this default value to any components
            // that do not have an 'off' value yet.
            if (defaultDisable != null)
                for (final Map.Entry<TextType, TextComponent> entry : styleMap.entrySet())
                {
                    final TextComponent component = entry.getValue();
                    // If 'on' is set, but off isn't,
                    if ((!"".equals(component.getOn())) && "".equals(component.getOff()))
                        styleMap.put(entry.getKey(), new TextComponent(component.getOn(), defaultDisable));
                }
        }
    }
}
