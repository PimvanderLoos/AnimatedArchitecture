package nl.pim16aap2.bigDoors.compatibility;

public interface IProtectionCompatDefinition
{
    /**
     * Get the class of the given hook for a specific version of the plugin to load the compat for.
     *
     * @param version
     *     The version of the plugin to load the hook for.
     * @return The {@link IProtectionCompat} class of the compat.
     */
    Class<? extends IProtectionCompat> getClass(String version);

    /**
     * Get the name of the plugin the given compat hooks into.
     *
     * @return The name of the plugin the given compat hooks into.
     */
    String getName();
}
