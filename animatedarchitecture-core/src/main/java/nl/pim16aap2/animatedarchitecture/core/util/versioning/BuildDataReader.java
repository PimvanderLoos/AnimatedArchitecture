package nl.pim16aap2.animatedarchitecture.core.util.versioning;

import lombok.EqualsAndHashCode;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;

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
 * This class allows reading the build data (e.g. commit hash and actions ID) from the 'build_data' file in the jar.
 */
@Singleton
@Flogger
@EqualsAndHashCode
public final class BuildDataReader implements IDebuggable
{
    private final BuildData buildData;

    @Inject
    BuildDataReader(DebuggableRegistry debuggableRegistry)
    {
        this.buildData = readBuildData();
        debuggableRegistry.registerDebuggable(this);
    }

    /**
     * Getter for the parsed build data.
     *
     * @return The parsed {@link BuildData}.
     */
    @SuppressWarnings("unused")
    public BuildData getBuildData()
    {
        return buildData;
    }

    private BuildData readBuildData0()
        throws Exception
    {
        try (
            InputStream inputStream = Objects.requireNonNull(
                this.getClass().getClassLoader().getResourceAsStream("build_data"));
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader))
        {
            final Object[] lines = bufferedReader.lines().toArray();
            if (lines.length != 3)
                throw new IllegalArgumentException("Failed to parse build data from input: " + Arrays.toString(lines));
            return new BuildData(
                (String) lines[0],
                parseInt("build number", (String) lines[1]),
                parseInt("build id", (String) lines[2]));
        }
    }

    private int parseInt(String name, String str)
    {
        final OptionalInt ret = MathUtil.parseInt(str);
        if (ret.isEmpty())
            log.atSevere().log("Failed to parse %s from input: '%s'", name, str);
        return ret.orElse(-1);
    }

    private BuildData readBuildData()
    {
        try
        {
            return readBuildData0();
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to read build data!");
            return new BuildData("ERROR", -1, -1);
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
            buildData.git, buildData.buildNumber, buildData.buildId);
    }

    /**
     * A description of the build data of the current build.
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
    public record BuildData(String git, int buildNumber, int buildId)
    {}
}
