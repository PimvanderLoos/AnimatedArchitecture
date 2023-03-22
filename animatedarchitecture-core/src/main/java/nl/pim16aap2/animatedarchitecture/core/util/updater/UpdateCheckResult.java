package nl.pim16aap2.animatedarchitecture.core.util.updater;

/**
 * Represents the result status of checking for updates.
 */
public enum UpdateCheckResult
{
    /**
     * An unknown error occurred.
     */
    ERROR(true),

    /**
     * The returned json was invalid.
     */
    INVALID_JSON(true),

    /**
     * A newer version is available.
     */
    UPDATE_AVAILABLE(false),

    /**
     * The current version is up-to-date.
     */
    UP_TO_DATE(false);

    private final boolean isError;

    UpdateCheckResult(boolean isError)
    {
        this.isError = isError;
    }

    /**
     * Checks if the result is an error state.
     *
     * @return True if this result is an error state.
     */
    public boolean isError()
    {
        return this.isError;
    }
}
