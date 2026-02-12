package nl.pim16aap2.animatedarchitecture.spigot.core.config;

import nl.pim16aap2.animatedarchitecture.core.config.StructureSubSection;
import nl.pim16aap2.animatedarchitecture.core.config.StructureTypeConfigurationOption;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import org.bukkit.Material;

import java.util.List;

/**
 * Spigot-specific implementation of {@link StructureSubSection}.
 * <p>
 * This class is used to define a subsection for a specific structure type in the configuration.
 */
public class StructureSubSectionSpigot extends StructureSubSection
{
    public static final String PATH_GUI_MATERIAL = "gui_material";

    public static final Material DEFAULT_GUI_MATERIAL = Material.WARPED_DOOR;

    public StructureSubSectionSpigot(StructureType structureType)
    {
        super(structureType);
    }

    @Override
    protected void appendConfigurationOptions(List<StructureTypeConfigurationOption<?>> options)
    {
        options.add(getGuiMaterialOption(getStructureType()));
    }

    private static StructureTypeConfigurationOption<String> getGuiMaterialOption(StructureType structureType)
    {
        return getGuiMaterialOption(getDefaultGuiMaterial(structureType));
    }

    static StructureTypeConfigurationOption<String> getGuiMaterialOption(Material guiMaterial)
    {
        return new StructureTypeConfigurationOption<>(
            PATH_GUI_MATERIAL,
            guiMaterial.name(),
            String.class
        );
    }

    static Material getDefaultGuiMaterial(StructureType structureType)
    {
        return switch (structureType.getFullKey())
        {
            case "bigdoor" -> Material.OAK_DOOR;
            case "clock" -> Material.CLOCK;
            case "drawbridge" -> Material.OAK_TRAPDOOR;
            case "flag" -> Material.WHITE_BANNER;
            case "garagedoor" -> Material.MINECART;
            case "portcullis" -> Material.IRON_BARS;
            case "revolvingdoor" -> Material.MUSIC_DISC_PIGSTEP;
            case "slidingdoor" -> Material.PISTON;
            case "windmill" -> Material.SUNFLOWER;
            default -> DEFAULT_GUI_MATERIAL;
        };
    }
}
