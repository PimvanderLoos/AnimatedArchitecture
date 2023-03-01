package nl.pim16aap2.bigdoors.core.text;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import nl.pim16aap2.bigdoors.core.util.Util;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a colorscheme that can be used to add styles to text.
 */
@ToString
@EqualsAndHashCode
public final class ColorScheme<T>
{
    private final Map<TextType, T> styleMap;
    private final T defaultStyle;

    ColorScheme(Map<TextType, T> styleMap, T defaultStyle)
    {
        this.styleMap = styleMap;
        this.defaultStyle = defaultStyle;
    }

    /**
     * Gets the style associated with a certain {@link TextType}.
     *
     * @param type
     *     The {@link TextType} for which to find its style.
     * @return The style associated with the given {@link TextType}.
     */
    public T getStyle(@Nullable TextType type)
    {
        return type == null ? defaultStyle : styleMap.getOrDefault(type, defaultStyle);
    }

    /**
     * Builds a new {@link ColorScheme}.
     *
     * @return A new {@link ColorSchemeBuilder}.
     */
    public static <T> ColorSchemeBuilder<T> builder()
    {
        return new ColorSchemeBuilder<>();
    }

    @SuppressWarnings("unused")
    @ToString
    public static class ColorSchemeBuilder<T>
    {
        private final Map<TextType, T> styleMap = new HashMap<>();

        private @Nullable T defaultStyle = null;

        private ColorSchemeBuilder()
        {
        }

        /**
         * Sets the default style that is to be used when requesting the style for an unmapped text type.
         *
         * @param defaultStyle
         *     The default style to fall back to when none is mapped for a given text type.
         * @return This {@link ColorSchemeBuilder} instance.
         */
        @Contract("_ -> this")
        public ColorSchemeBuilder<T> setDefaultStyle(T defaultStyle)
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
        public ColorSchemeBuilder<T> addStyle(TextType type, T style)
        {
            styleMap.put(type, style);
            return this;
        }

        /**
         * Constructs the new {@link ColorScheme}.
         *
         * @return The new {@link ColorScheme}.
         */
        public ColorScheme<T> build()
        {
            return new ColorScheme<>(styleMap, Util.requireNonNull(defaultStyle, "defaultStyle"));
        }
    }
}
