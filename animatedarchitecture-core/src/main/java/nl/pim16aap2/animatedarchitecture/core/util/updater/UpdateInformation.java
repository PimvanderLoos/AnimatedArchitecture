package nl.pim16aap2.animatedarchitecture.core.util.updater;

import org.jetbrains.annotations.Nullable;
import org.semver4j.Semver;

/**
 * Represents information about a potential update for this project.
 *
 * @param updateCheckResult
 *     The result of the update check.
 * @param newVersion
 *     The new version that is available as update.
 *     <p>
 *     This will be the same as the current version if the result is {@link UpdateCheckResult#UP_TO_DATE}.
 *     <p>
 *     In case an error occurred while checking for updates, this will be null.
 * @param updateUrl
 *     The url of the page where the update can be downloaded from.
 *     <p>
 *     In case an error occurred while checking for updates, this will be null.
 * @param updateName
 *     The name of the update.
 *     <p>
 *     In case an error occurred while checking for updates, this will be null.
 */
public record UpdateInformation(
    UpdateCheckResult updateCheckResult,
    @Nullable Semver newVersion,
    @Nullable String updateUrl,
    @Nullable String updateName
)
{
    /**
     * Create a new UpdateInformation object for an error state.
     *
     * @param updateCheckResult
     *     The result of the update check.
     * @throws IllegalArgumentException
     *     If the provided result is not an error-state. See {@link UpdateCheckResult#isError()}.
     */
    public static UpdateInformation ofErrorState(UpdateCheckResult updateCheckResult)
    {
        if (!updateCheckResult.isError())
            throw new IllegalArgumentException("Cannot set null values for non-error state update information!");
        return new UpdateInformation(updateCheckResult, null, null, null);
    }
}
