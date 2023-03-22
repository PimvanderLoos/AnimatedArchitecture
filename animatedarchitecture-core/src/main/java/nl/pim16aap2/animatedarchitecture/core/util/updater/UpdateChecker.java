package nl.pim16aap2.animatedarchitecture.core.util.updater;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.versioning.ProjectVersion;
import org.jetbrains.annotations.Nullable;

import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Represents an update checker that checks for new releases on GitHub.
 */
@Flogger
@Singleton
public final class UpdateChecker implements IDebuggable
{
    private static final @Nullable URL UPDATE_URL =
        createUrl("https://api.github.com/repos/PimvanderLoos/AnimatedArchitecture/releases/latest");

    private final ProjectVersion currentVersion;

    /**
     * The most recently retrieve update information.
     */
    @Getter
    private volatile @Nullable UpdateInformation updateInformation;

    /**
     * Instantiates a new update checker.
     * <p>
     * Immediately runs a new check.
     *
     * @param currentVersion
     *     The current version of the project to compare potential updates against.
     */
    public UpdateChecker(ProjectVersion currentVersion)
    {
        this.currentVersion = currentVersion;
        checkForUpdates();
    }

    /**
     * Runs a new update check.
     *
     * @return The resulting update information.
     */
    public CompletableFuture<@Nullable UpdateInformation> checkForUpdates()
    {
        return CompletableFuture.supplyAsync(this::checkForUpdates0).exceptionally(Util::exceptionally);
    }

    /**
     * Tries to update {@link #updateInformation}.
     * <p>
     * If the information is null or if the update check is an error state (see {@link UpdateCheckResult#isError()}),
     * this will be logged as a severe problem.
     * <p>
     * If an update is available, this will be logged as well.
     * <p>
     * Note that this method will try to retain valid update information. This means that a null value as input will not
     * set the current information to null, and error state information will not override existing information.
     *
     * @param newInformation
     *     The new update information.
     * @return The resulting update information.
     */
    private @Nullable UpdateInformation setCurrentUpdateInformation(@Nullable UpdateInformation newInformation)
    {
        final @Nullable UpdateInformation current = this.updateInformation;
        if (newInformation == null)
        {
            log.atWarning().log("Received invalid update information!");
            return current;
        }

        final UpdateCheckResult status = newInformation.updateCheckResult();
        if (status != UpdateCheckResult.UPDATE_AVAILABLE && status != UpdateCheckResult.UP_TO_DATE)
        {
            log.atSevere().log("Received unexpected update state: '%s' for URL: '%s'!", status, UPDATE_URL);
            if (current != null)
                return current;
        }

        if (status == UpdateCheckResult.UPDATE_AVAILABLE)
            log.atInfo().log("A new version is available: '%s'! Get it here: %s",
                             newInformation.updateName(), newInformation.updateUrl());

        this.updateInformation = newInformation;
        return newInformation;
    }

    /**
     * Private method that runs the updates.
     * <p>
     * Ensure that this method is not called from the main thread!
     *
     * @return The resulting update information. See {@link #setCurrentUpdateInformation(UpdateInformation)}.
     */
    private @Nullable UpdateInformation checkForUpdates0()
    {
        final @Nullable JsonElement element = readUrl();
        if (element == null)
            return setCurrentUpdateInformation(new UpdateInformation(UpdateCheckResult.ERROR));

        if (!element.isJsonObject())
            return setCurrentUpdateInformation(new UpdateInformation(UpdateCheckResult.INVALID_JSON));
        final JsonObject jsonObject = element.getAsJsonObject();

        final @Nullable ProjectVersion newVersion = getNewVersion(jsonObject);
        if (newVersion == null)
            return setCurrentUpdateInformation(new UpdateInformation(UpdateCheckResult.ERROR));

        return setCurrentUpdateInformation(getUpdateInformation(newVersion, jsonObject));
    }

    /**
     * Processes the new project version and checks if it is up-to-date.
     *
     * @param newVersion
     *     The version to process.
     * @return {@link UpdateCheckResult#UPDATE_AVAILABLE} if the new version is newer than {@link #currentVersion} or
     * {@link UpdateCheckResult#UP_TO_DATE} if not.
     */
    private UpdateCheckResult processNewVersion(ProjectVersion newVersion)
    {
        if (this.currentVersion.equals(newVersion))
            return UpdateCheckResult.UP_TO_DATE;

        return newVersion.isNewerThan(this.currentVersion) ?
               UpdateCheckResult.UPDATE_AVAILABLE :
               UpdateCheckResult.UP_TO_DATE;
    }

    /**
     * Parses the update information from the json data.
     *
     * @param newVersion
     *     The new version that was previously parsed from the same json data.
     * @param element
     *     The json data.
     * @return The parsed update information, or null if an error occurred.
     */
    private @Nullable UpdateInformation getUpdateInformation(ProjectVersion newVersion, JsonObject element)
    {
        try
        {
            final UpdateCheckResult result = processNewVersion(newVersion);
            final String updateUrl = element.get("html_url").getAsString();
            final String updateName = element.get("name").getAsString();
            return new UpdateInformation(result, newVersion, updateUrl, updateName);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to parse json data!");
            return null;
        }
    }

    /**
     * Parses the new project version from the json data.
     *
     * @param element
     *     The json element containing the project version.
     * @return The parsed project version, or null if an error occurred.
     */
    private @Nullable ProjectVersion getNewVersion(JsonObject element)
    {
        try
        {
            return ProjectVersion.parse(element.get("tag_name").getAsString());
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to parse json data!");
            return null;
        }
    }

    /**
     * Reads the {@link #UPDATE_URL} (if it is not null) and parses the json element from it.
     *
     * @return The parsed json element or null if the page could not be parsed.
     */
    private @Nullable JsonElement readUrl()
    {
        if (UPDATE_URL == null)
            return null;

        try (var reader = new InputStreamReader(UPDATE_URL.openStream()))
        {
            return JsonParser.parseReader(reader);
        }
        catch (IOException e)
        {
            log.atSevere().withCause(e).log("Failed to read URL '%s'", UPDATE_URL);
            return null;
        }
    }

    /**
     * Creates a new URL object from a path.
     *
     * @param path
     *     The path to create the URL from.
     * @return The URL if it could be constructed or null if an error occurred.
     */
    private static @Nullable URL createUrl(String path)
    {
        try
        {
            return new URL(path);
        }
        catch (MalformedURLException e)
        {
            log.atSevere().withCause(e).log("Failed to get URL from path '%s'", path);
            return null;
        }
    }

    @Override
    public String getDebugInformation()
    {
        return Objects.toString(updateInformation);
    }
}
