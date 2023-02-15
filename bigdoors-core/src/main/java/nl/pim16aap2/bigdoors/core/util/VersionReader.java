package nl.pim16aap2.bigdoors.core.util;

import lombok.EqualsAndHashCode;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.bigdoors.core.api.debugging.IDebuggable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * This class allows reading the commit hash and actions ID.
 */
@Singleton
@Flogger
@EqualsAndHashCode
public final class VersionReader implements IDebuggable
{
    private final VersionInfo versionInfo;

    @Inject public VersionReader(DebuggableRegistry debuggableRegistry)
    {
        this.versionInfo = getVersionInfo();
        debuggableRegistry.registerDebuggable(this);
    }

    private VersionInfo getVersionInfo0()
        throws Exception
    {
        try (InputStream inputStream = Objects.requireNonNull(
            this.getClass().getClassLoader().getResourceAsStream("version"));
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader))
        {
            final Object[] lines = bufferedReader.lines().toArray();
            if (lines.length != 2)
                throw new IllegalArgumentException("Failed to parse version from data: " + Arrays.toString(lines));
            return new VersionInfo((String) lines[0], (String) lines[1]);
        }
    }

    private VersionInfo getVersionInfo()
    {
        try
        {
            return getVersionInfo0();
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to read version of plugin!");
            return new VersionInfo("ERROR", "ERROR");
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
        return String.format("Commit: %s\nbuild ID: %s", versionInfo.git, versionInfo.build);
    }

    private record VersionInfo(String git, String build)
    {}
}
