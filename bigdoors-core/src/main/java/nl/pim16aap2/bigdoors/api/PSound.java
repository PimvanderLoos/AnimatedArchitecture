package nl.pim16aap2.bigdoors.api;

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

    private final String name;

    /**
     * The duration of the sound, measured in ticks.
     */
    private final int duration;

    PSound(String name, int duration)
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
    public static String getSoundName(PSound sound)
    {
        return sound.name;
    }

    /**
     * Gets the duration of the {@link PSound}, measured in ticks.
     *
     * @param sound The {@link PSound}.
     * @return The duration of the {@link PSound}.
     */
    public static int getDuration(PSound sound)
    {
        return sound.duration;
    }
}
