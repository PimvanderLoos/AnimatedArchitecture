package nl.pim16aap2.bigdoors.api;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a list of sounds.
 */
public enum PSound
{
    THUD("bd.thud"),
    DRAGGING("bd.dragging2"),
    CLOSING_VAULT_DOOR("bd.dragging2"),
    DRAWBRIDGE_RATTLING("bd.drawbridge-rattling"),
    ;

    @NotNull
    private final String name;

    PSound(final @NotNull String name)
    {
        this.name = name;
    }

    /**
     * Gets the name of a {@link PSound}.
     *
     * @param sound The {@link PSound}.
     * @return The name of the {@link PSound}.
     */
    @NotNull
    public static String getSoundName(final @NotNull PSound sound)
    {
        return sound.name;
    }
}
