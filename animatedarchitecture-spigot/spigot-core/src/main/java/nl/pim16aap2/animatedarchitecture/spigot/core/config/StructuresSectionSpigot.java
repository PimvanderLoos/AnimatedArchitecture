package nl.pim16aap2.animatedarchitecture.spigot.core.config;

import dagger.Lazy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.flogger.Flogger;
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
@Flogger
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
    protected Result getResult(ConfigurationNode sectionNode)
    {
        return new Result(
            Collections.unmodifiableMap(getStructureResults(sectionNode)),
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

    private Map<StructureType, StructureResult> getStructureResults(ConfigurationNode sectionNode)
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
                            return getStructureResult(sectionNode.node(structureType.getFullKey()), structureType);
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

    private StructureResult getStructureResult(ConfigurationNode structureNode, StructureType structureType)
        throws SerializationException
    {
        if (structureNode.virtual())
        {
            log.atWarning().log(
                "Structure '%s' is not configured in the configuration file.",
                structureType.getFullKey()
            );

            return new StructureResult(
                StructureTypeConfigurationOption.SPEED_MULTIPLIER.defaultValue(),
                StructureTypeConfigurationOption.PRICE.defaultValue(),
                StructureSubSectionSpigot.getDefaultGuiMaterial(structureType)
            );
        }

        final double speedMultiplier =
            getConfigurationOption(structureNode, StructureTypeConfigurationOption.SPEED_MULTIPLIER);

        final String price =
            getConfigurationOption(structureNode, StructureTypeConfigurationOption.PRICE);

        final Material guiMaterial = parseMaterial(structureNode, structureType);

        return new StructureResult(
            speedMultiplier,
            price,
            guiMaterial
        );
    }

    private Material parseMaterial(ConfigurationNode structureNode, StructureType structureType)
        throws SerializationException
    {
        final Material defaultMaterial =
            StructureSubSectionSpigot.getDefaultGuiMaterial(structureType);

        final String guiMaterialName =
            getConfigurationOption(structureNode, StructureSubSectionSpigot.getGuiMaterialOption(defaultMaterial));

        return MaterialParser.builder()
            .defaultMaterial(defaultMaterial)
            .build()
            .parse(guiMaterialName);
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
    ) implements IConfigSectionResult {}

    /**
     * Represents the result of a specific structure configuration.
     *
     * @param speedMultiplier
     *     The speed multiplier for the structure.
     * @param price
     *     The price formula for the structure, as a string.
     * @param guiMaterial
     *     The material used in the GUI for this structure.
     */
    public record StructureResult(
        double speedMultiplier,
        String price,
        Material guiMaterial
    ) {}
}
