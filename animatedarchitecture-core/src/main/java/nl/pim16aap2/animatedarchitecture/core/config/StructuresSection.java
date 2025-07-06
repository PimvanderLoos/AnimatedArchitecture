package nl.pim16aap2.animatedarchitecture.core.config;

import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Collection;
import java.util.Comparator;

/**
 * Represents a section in the configuration with per-structure type settings.
 */
public abstract class StructuresSection implements IConfigSection
{
    public static final String SECTION_TITLE = "structures";

    private static final String SECTION_COMMENT = """
        Change the properties of each structure type.
        
        Multiplier:
          Changes the animation time of each structure type.
          The higher the value, the more time an animation will take.
        
          For example, we have a structure with a default animation duration of 10 seconds.
          With a multiplier of 1.5, the animation will take 15 seconds, while a multiplier of 0.5 will result in a
          duration of 5 seconds.
        
          Note that the maximum speed of the animated blocks is limited by 'maxBlockSpeed', so there is a limit to
          how fast you can make the structures.
        
        Price:
          When an economy hook is present, you can set the price of creation here for each type of structure.
          You can use the word "blockCount" (without quotation marks, case-sensitive) as a variable that will be
          replaced by the actual blockCount.
        
          You can use the following operators:
            -, +, *, /, sqrt(), ^, %, min(a,b), max(a,b), abs(), and parentheses.
        
          For example: "price='max(10, sqrt(16)^4/100*blockCount)'
          would return 10 for a blockCount of 0 to 3 and 10.24 for a blockCount of 4.
          You must always put the formula or simple value or whatever in quotation marks!
        
          If no economy hook is present, the price will be ignored and the structure can be created for free.
        """;

    /**
     * Returns the comment for this section.
     * <p>
     * This comment is used to describe the purpose and settings of the section in the configuration file.
     * <p>
     * Subclasses can override this method to customize the comment for their specific section.
     *
     * @return The comment for this section.
     */
    protected String getSectionComment()
    {
        return SECTION_COMMENT;
    }

    @Override
    public CommentedConfigurationNode buildInitialLimitsNode()
        throws SerializationException
    {
        return CommentedConfigurationNode.root()
            .comment(getSectionComment())
            .set("[]");
    }

    /**
     * Creates a subsection for the given structure type.
     *
     * @param structureType
     *     The structure type to create a subsection for.
     * @return An instance of {@link IStructureSubSection} for the given structure type.
     */
    protected abstract IStructureSubSection createSubSection(StructureType structureType);

    /**
     * Gets the subsection for the structures section in the configuration.
     * <p>
     * If the subsection does not exist, it will be created.
     *
     * @param root
     *     The root configuration node to get the structures subsection from.
     * @return The structures subsection node.
     *
     * @throws SerializationException
     *     If there is an error during serialization.
     */
    private CommentedConfigurationNode getStructuresSubSection(CommentedConfigurationNode root)
        throws SerializationException
    {
        final CommentedConfigurationNode node = root.node(getSectionTitle());
        if (node.virtual())
            return buildInitialLimitsNode();
        return node;
    }

    private void writeConfigurationOptionToNode(
        CommentedConfigurationNode node,
        StructureTypeConfigurationOption option)
    {
        try
        {
            final var optionNode = node.node(option.name());

            // If the node already exists, we should not overwrite it.
            if (!optionNode.virtual())
                return;

            optionNode
                .comment(option.comment())
                .set(option.defaultValue());
        }
        catch (SerializationException exception)
        {
            throw new RuntimeException(
                String.format("Failed to write configuration option: %s", option),
                exception
            );
        }
    }

    private void writeConfigurationOptionsToNode(
        CommentedConfigurationNode node,
        Collection<StructureTypeConfigurationOption> options)
    {
        options.forEach(option -> writeConfigurationOptionToNode(node, option));
    }

    private void writeSubSectionToNode(
        CommentedConfigurationNode structuresNode,
        IStructureSubSection subSection)
    {
        structuresNode
            .node(subSection.getSectionTitle())
            .act(node -> writeConfigurationOptionsToNode(node, subSection.getConfigurationOptions()));
    }

    /**
     * Gets the registered structure types.
     *
     * @return A collection of registered structure types.
     */
    protected abstract Collection<StructureType> getRegisteredStructureTypes();

    /**
     * Populates the structures section with the registered structure types.
     *
     * @param root
     *     The root configuration node to populate.
     */
    public final void populateStructures(CommentedConfigurationNode root)
        throws SerializationException
    {
        final var node = getStructuresSubSection(root);
        getRegisteredStructureTypes().stream()
            .map(this::createSubSection)
            .sorted(Comparator.comparing(IStructureSubSection::getSectionTitle))
            .forEach(section -> this.writeSubSectionToNode(node, section));
    }

    @Override
    public String getSectionTitle()
    {
        return SECTION_TITLE;
    }
}
