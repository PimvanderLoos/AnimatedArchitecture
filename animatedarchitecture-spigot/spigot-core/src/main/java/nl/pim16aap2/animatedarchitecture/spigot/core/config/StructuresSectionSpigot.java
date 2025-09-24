package nl.pim16aap2.animatedarchitecture.spigot.core.config;

import dagger.Lazy;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nl.pim16aap2.animatedarchitecture.core.config.IConfigSectionResult;
import nl.pim16aap2.animatedarchitecture.core.config.IStructureSubSection;
import nl.pim16aap2.animatedarchitecture.core.config.IStructureSubSectionFlag;
import nl.pim16aap2.animatedarchitecture.core.config.StructureTypeConfigurationOption;
import nl.pim16aap2.animatedarchitecture.core.config.StructuresSection;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import org.bukkit.Material;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Spigot-specific implementation of the StructuresSection.
 * <p>
 * This class extends the StructuresSection to provide additional comments and configurations specific to the Spigot
 * platform.
 */
@CustomLog
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class StructuresSectionSpigot extends StructuresSection<StructuresSectionSpigot.Result>
{
    private final Lazy<StructureTypeManager> lazyStructureTypeManager;

    @Getter
    private final @Nullable Consumer<Result> resultConsumer;

    @Override
    protected String getSectionComment()
    {
        return super.getSectionComment() + """
            
            GUI Material:
              The materials to use in the GUI when looking at the overview of all structures.
            """;
    }

    @Override
    protected IStructureSubSection createSubSection(StructureType structureType)
    {
        if (IStructureSubSectionFlag.STRUCTURE_TYPE_KEY.equals(structureType.getFullKey()))
            return new StructureSubSectionFlagSpigot(structureType);

        return new StructureSubSectionSpigot(structureType);
    }

    @Override
    protected Collection<StructureType> getRegisteredStructureTypes()
    {
        return lazyStructureTypeManager.get().getRegisteredStructureTypes();
    }

    @Override
    protected Result getResult(ConfigurationNode sectionNode, boolean silent)
    {
        return new Result(
            Collections.unmodifiableMap(getStructureResults(sectionNode, silent)),
            getFlagMovementFormula(sectionNode)
        );
    }

    private String getFlagMovementFormula(ConfigurationNode sectionNode)
    {
        return sectionNode
            .node(
                IStructureSubSectionFlag.STRUCTURE_TYPE_KEY,
                StructureSubSectionFlagSpigot.OPTION_MOVEMENT_FORMULA.name())
            .getString(StructureSubSectionFlagSpigot.OPTION_MOVEMENT_FORMULA.defaultValue());
    }

    private Map<StructureType, StructureResult> getStructureResults(ConfigurationNode sectionNode, boolean silent)
    {
        return lazyStructureTypeManager.get().getRegisteredStructureTypes()
            .stream()
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    structureType ->
                    {
                        try
                        {
                            return getStructureResult(
                                sectionNode.node(structureType.getFullKey()),
                                structureType,
                                silent
                            );
                        }
                        catch (SerializationException exception)
                        {
                            throw new RuntimeException(
                                "Failed to parse structure configuration for '%s': %s".formatted(
                                    structureType.getFullKey(),
                                    exception.getMessage()),
                                exception
                            );
                        }
                    }
                )
            );
    }

    private StructureResult getStructureResult(
        ConfigurationNode structureNode,
        StructureType structureType,
        boolean silent
    )
        throws SerializationException
    {
        if (structureNode.virtual())
        {
            if (!silent)
            {
                log.atWarn().log(
                    "Structure '%s' is not configured in the configuration file.",
                    structureType.getFullKey()
                );
            }

            return StructureResult.DEFAULT;
        }

        final double speedMultiplier =
            getConfigurationOption(structureNode, StructureTypeConfigurationOption.ANIMATION_SPEED_MULTIPLIER);

        final String price =
            getConfigurationOption(structureNode, StructureTypeConfigurationOption.PRICE_FORMULA);

        final Material guiMaterial = parseMaterial(structureNode, structureType, silent);

        return new StructureResult(
            speedMultiplier,
            price,
            guiMaterial
        );
    }

    private Material parseMaterial(ConfigurationNode structureNode, StructureType structureType, boolean silent)
        throws SerializationException
    {
        final Material defaultMaterial =
            StructureSubSectionSpigot.getDefaultGuiMaterial(structureType);

        final String guiMaterialName =
            getConfigurationOption(structureNode, StructureSubSectionSpigot.getGuiMaterialOption(defaultMaterial));

        return MaterialParser.builder()
            .defaultMaterial(defaultMaterial)
            .build()
            .parse(guiMaterialName, silent);
    }

    private <T> T getConfigurationOption(
        ConfigurationNode structureNode,
        StructureTypeConfigurationOption<T> option
    )
        throws SerializationException
    {
        return structureNode.node(option.name()).get(option.type(), option.defaultValue());
    }

    /**
     * Represents the result of the StructuresSection configuration.
     *
     * @param structureResults
     *     A map of structure types to their respective results.
     * @param flagMovementFormula
     *     The formula used for the movement of flag structures.
     */
    public record Result(
        Map<StructureType, StructureResult> structureResults,
        String flagMovementFormula
    ) implements IConfigSectionResult
    {
        /**
         * The default result used when no data is available.
         */
        public static final Result DEFAULT = new Result(
            Map.of(),
            StructureSubSectionFlagSpigot.OPTION_MOVEMENT_FORMULA.defaultValue()
        );

        public Result
        {
            structureResults = Map.copyOf(structureResults);
        }

        /**
         * Retrieves the {@link StructureResult} for a specific structure type.
         *
         * @param structureType
         *     The structure type for which to retrieve the result.
         * @return The {@link StructureResult} for the specified structure type, or {@link StructureResult#DEFAULT}
         * result if not found.
         */
        public StructureResult of(StructureType structureType)
        {
            return structureResults.getOrDefault(structureType, StructureResult.DEFAULT);
        }

        @SuppressWarnings("unused") // It's used by Lombok's @Delegate annotation
        public String priceFormula(StructureType type)
        {
            return of(type).priceFormula();
        }

        @SuppressWarnings("unused") // It's used by Lombok's @Delegate annotation
        public double animationTimeMultiplier(StructureType type)
        {
            return Math.max(0.0001D, of(type).animationSpeedMultiplier());
        }

        @SuppressWarnings("unused") // It's used by Lombok's @Delegate annotation
        public Material guiMaterial(StructureType type)
        {
            return of(type).guiMaterial();
        }
    }

    /**
     * Represents the result of a specific structure configuration.
     *
     * @param animationSpeedMultiplier
     *     The speed multiplier for the structure.
     * @param priceFormula
     *     The priceFormula formula for the structure, as a string.
     * @param guiMaterial
     *     The material used in the GUI for this structure.
     */
    public record StructureResult(
        double animationSpeedMultiplier,
        String priceFormula,
        Material guiMaterial
    )
    {
        /**
         * The default structure result, used when no specific configuration is provided.
         */
        public static final StructureResult DEFAULT = new StructureResult(
            StructureTypeConfigurationOption.ANIMATION_SPEED_MULTIPLIER.defaultValue(),
            StructureTypeConfigurationOption.PRICE_FORMULA.defaultValue(),
            StructureSubSectionSpigot.DEFAULT_GUI_MATERIAL
        );
    }
}
