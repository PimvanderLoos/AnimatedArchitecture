package nl.pim16aap2.animatedarchitecture.spigot.core.config;


import lombok.Builder;
import lombok.Singular;
import lombok.extern.flogger.Flogger;
import org.bukkit.Material;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Flogger
@Builder
public class MaterialParser
{
    private String context;

    @Builder.Default
    private @Nullable Boolean isSolid = null;

    @Singular
    private Set<Material> defaultMaterials;

    @Builder.Default
    private Action onDuplicate = Action.SKIP;

    @Builder.Default
    private Action onInvalid = Action.WARN_AND_SKIP;

    /**
     * Parses a material name to a {@link Material} object.
     *
     * @param names
     *     The name of the material to parse.
     * @return The parsed material object, or null if the material could not be parsed.
     */
    public Set<Material> parse(@Nullable List<String> names)
    {
        if (names == null || names.isEmpty())
        {
            log.atFine().log("No materials provided for %s. Using default materials: %s", context, defaultMaterials);
            return defaultMaterials;
        }

        return names.stream()
            .map(this::parseMaterial)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private @Nullable Material getFirstDefaultMaterial()
    {
        return defaultMaterials.stream().findFirst().orElse(null);
    }

    private Material returnDefaultMaterial(@Nullable String name)
    {
        final Material defaultMaterial = getFirstDefaultMaterial();
        log.atFine().log(
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
     * @param name
     *     The name of the material to parse.
     * @return The parsed material object, or the default material if the name is null or empty.
     */
    public Material parse(@Nullable String name)
    {
        if (name == null || name.isEmpty())
        {
            return returnDefaultMaterial(name);
        }

        final var material = parseMaterial(name);
        if (material == null)
        {
            return returnDefaultMaterial(name);
        }

        return material;
    }

    private @Nullable Material parseMaterial(String name)
    {
        final var material = Material.getMaterial(name);
        if (material == null)
        {
            handleInvalidMaterial(name);
            return null;
        }

        if (isSolid != null && isSolid != material.isSolid())
        {
            handleInvalidMaterial(name);
            return null;
        }

        return material;
    }

    private void handleInvalidMaterial(String name)
    {
        switch (onInvalid)
        {
            case WARN_AND_SKIP -> log.atSevere().log("[%s] Invalid material: %s. Skipping.", context, name);
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
