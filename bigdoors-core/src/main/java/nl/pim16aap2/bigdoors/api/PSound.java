package nl.pim16aap2.bigdoors.api;


import org.jetbrains.annotations.NotNull;

/**
 * Represents a list of sounds.
 */
public enum PSound
{
    THUD("bd.thud", 5),
    DRAGGING("bd.dragging2", 15),
    CLOSING_VAULT_DOOR("bd.dragging2", 15),
    DRAWBRIDGE_RATTLING("bd.drawbridge-rattling", 15),
    ;


    private final @NotNull String name;

    /**
     * The duration of the sound, measured in ticks.
     */
    private final int duration;

    PSound(final @NotNull String name, final int duration)
    {
        this.name = name;
        this.duration = duration;
    }

    /**
     * Gets the name of a {@link PSound}.
     *
     * @param sound The {@link PSound}.
     * @return The name of the {@link PSound}.
     */
    public static @NotNull String getSoundName(final @NotNull PSound sound)
    {
        return sound.name;
    }

    /**
     * Gets the duration of the {@link PSound}, measured in ticks.
     *
     * @param sound The {@link PSound}.
     * @return The duration of the {@link PSound}.
     */
    public static int getDuration(final @NotNull PSound sound)
    {
        return sound.duration;
    }
}
