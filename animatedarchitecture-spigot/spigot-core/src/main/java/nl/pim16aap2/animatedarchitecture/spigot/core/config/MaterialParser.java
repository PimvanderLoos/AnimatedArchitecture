package nl.pim16aap2.animatedarchitecture.spigot.core.config;


import lombok.Builder;
import lombok.CustomLog;
import lombok.Singular;
import org.bukkit.Material;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a parser for materials from strings.
 */
@CustomLog
@Builder
public class MaterialParser
{
    private String context;

    @Builder.Default
    private @Nullable Boolean isSolid = null;

    @Singular
    private Set<Material> defaultMaterials;

    @Builder.Default
    private Action onInvalid = Action.WARN_AND_SKIP;

    private @Nullable Material getFirstDefaultMaterial()
    {
        return defaultMaterials.stream().findFirst().orElse(null);
    }

    private Material returnDefaultMaterial(@Nullable String name)
    {
        final Material defaultMaterial = getFirstDefaultMaterial();
        log.atDebug().log(
            "[%s] Failed to parse material '%s'. Using default material: %s",
            context,
            name,
            defaultMaterial
        );

        if (defaultMaterial == null)
        {
            throw new IllegalStateException("No default material configured for context: " + context);
        }

        return defaultMaterial;
    }

    /**
     * Parses a material name to a {@link Material} object.
     *
     * @param names
     *     The name of the material to parse.
     * @return The parsed material object, or null if the material could not be parsed.
     */
    public Set<Material> parse(@Nullable List<String> names, boolean silent)
    {
        if (names == null || names.isEmpty())
        {
            log.atDebug().log("No materials provided for %s. Using default materials: %s", context, defaultMaterials);
            return defaultMaterials;
        }

        return names.stream()
            .map(name -> parseMaterial(name, silent))
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(Material.class)));
    }

    /**
     * Parses a material name to a {@link Material} object.
     *
     * @param name
     *     The name of the material to parse.
     * @return The parsed material object, or the default material if the name is null or empty.
     */
    public Material parse(@Nullable String name, boolean silent)
    {
        if (name == null || name.isEmpty())
        {
            return returnDefaultMaterial(name);
        }

        final var material = parseMaterial(name, silent);
        if (material == null)
        {
            return returnDefaultMaterial(name);
        }

        return material;
    }

    private @Nullable Material parseMaterial(String name, boolean silent)
    {
        final var material = Material.getMaterial(name);
        if (material == null)
        {
            handleInvalidMaterial(name, silent);
            return null;
        }

        if (isSolid != null && isSolid != material.isSolid())
        {
            handleInvalidMaterial(name, silent);
            return null;
        }

        return material;
    }

    private void handleInvalidMaterial(String name, boolean silent)
    {
        switch (onInvalid)
        {
            case WARN_AND_SKIP ->
            {
                if (!silent)
                    log.atError().log("[%s] Invalid material: %s. Skipping.", context, name);
            }
            case FAIL -> throw new IllegalArgumentException("[%s] Invalid material: %s".formatted(context, name));
            case SKIP ->
            {
            }
        }
    }

    /**
     * Represents the action to take when a material cannot be parsed.
     */
    public enum Action
    {
        SKIP,
        WARN_AND_SKIP,
        FAIL
    }
}
