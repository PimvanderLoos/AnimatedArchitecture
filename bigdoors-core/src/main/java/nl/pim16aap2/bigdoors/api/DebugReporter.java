package nl.pim16aap2.bigdoors.api;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.Nullable;

@AllArgsConstructor
public class DebugReporter
{
    protected final @Nullable IBigDoorsPlatform platform;

    /**
     * Gets the data-dump containing useful information for debugging issues.
     */
    public String getDump()
    {
        return new StringBuilder()
            .append("Java home: ").append(System.getProperty("java.home"))
            .append('\n')
            .append("Java compiler: ").append(System.getProperty("java.compiler"))
            .append('\n')
            .append("Java vendor: ").append(System.getProperty("java.vendor"))
            .append('\n')
            .append("Java version: ").append(System.getProperty("java.version"))
            .append('\n')
            .append("Java Runtime version: ").append(System.getProperty("java.runtime.version"))
            .append('\n')
            .append("JVM name: ").append(System.getProperty("java.vm.name"))
            .append('\n')
            .append("JVM version: ").append(System.getProperty("java.vm.version"))
            .append('\n')
            .append("JVM specification version: ").append(System.getProperty("java.vm.specification.version"))
            .append('\n')
            .append("JVM specification name: ").append(System.getProperty("java.vm.specification.name"))
            .append('\n')
            .append("OS Name: ").append(System.getProperty("os.name"))
            .append('\n')
            .append("OS Arch: ").append(System.getProperty("os.arch"))
            .append('\n')
            .append("OS Version: ").append(System.getProperty(" os.version"))
            .append('\n')
            .append("TMP: ").append(System.getProperty("java.io.tmpdir"))
            .append('\n')
            .append("TMP SQLite: ").append(System.getProperty("org.sqlite.tmpdir"))
            .append('\n')
            .append("Registered Platform: ").append(platform == null ? "null" : platform.getClass().getName())
            .append('\n')

            .toString();
    }
}
