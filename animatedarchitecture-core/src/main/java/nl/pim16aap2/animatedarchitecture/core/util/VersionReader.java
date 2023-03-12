package nl.pim16aap2.animatedarchitecture.core.util;

import lombok.EqualsAndHashCode;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * This class allows reading the commit hash and actions ID.
 */
@Singleton
@Flogger
@EqualsAndHashCode
public final class VersionReader implements IDebuggable
{
    private final VersionInfo versionInfo;

    @Inject VersionReader(DebuggableRegistry debuggableRegistry)
    {
        this.versionInfo = readVersionInfo();
        debuggableRegistry.registerDebuggable(this);
    }

    /**
     * Getter for the parsed version info.
     *
     * @return The parsed {@link VersionInfo}.
     */
    @SuppressWarnings("unused")
    public VersionInfo getVersionInfo()
    {
        return versionInfo;
    }

    private VersionInfo readVersionInfo0()
        throws Exception
    {
        try (InputStream inputStream = Objects.requireNonNull(
            this.getClass().getClassLoader().getResourceAsStream("version"));
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader))
        {
            final Object[] lines = bufferedReader.lines().toArray();
            if (lines.length != 3)
                throw new IllegalArgumentException("Failed to parse version from data: " + Arrays.toString(lines));
            return new VersionInfo((String) lines[0],
                                   parseInt("build number", (String) lines[1]),
                                   parseInt("build id", (String) lines[2]));
        }
    }

    private int parseInt(String name, String str)
    {
        final OptionalInt ret = Util.parseInt(str);
        if (ret.isEmpty())
            log.atSevere().log("Failed to parse %s from input: '%s'", name, str);
        return ret.orElse(-1);
    }

    private VersionInfo readVersionInfo()
    {
        try
        {
            return readVersionInfo0();
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to read version of plugin!");
            return new VersionInfo("ERROR", -1, -1);
        }
    }

    @Override
    public String getDebugInformation()
    {
        return this.toString();
    }

    @Override
    public String toString()
    {
        return String.format("Commit: %s\nBuild number: %d\nBuild id: %d",
                             versionInfo.git, versionInfo.buildNumber, versionInfo.buildId);
    }

    /**
     * A description of the current version of this project.
     *
     * @param git
     *     The git data with the branch, commit hash, dirty status (i.e. modified version of the commit), and commit
     *     time.
     * @param buildNumber
     *     The number of the build. Using GitHub Actions (which we do), this refers to GITHUB_RUN_NUMBER. This number is
     *     incremented for each new run of a particular workflow.
     * @param buildId
     *     The unique id of the build. Using GitHub Actions, this refers to GITHUB_RUN_ID.
     */
    public record VersionInfo(String git, int buildNumber, int buildId)
    {}
}
