package nl.pim16aap2.animatedarchitecture.spigot.core.comands;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.IRestartable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

/**
 * Represents an argument parser that can be used to retrieve and suggest structure types.
 *
 * @author Pim
 */
public class StructureTypeParser implements ArgumentParser<ICommandSender, StructureType>, IRestartable
{
    private final StructureTypeManager structureTypeManager;
    private final ILocalizer localizer;
    private Map<String, StructureType> suggestions = Collections.emptyMap();

    @Inject StructureTypeParser(
        RestartableHolder restartableHolder, StructureTypeManager structureTypeManager, ILocalizer localizer)
    {
        restartableHolder.registerRestartable(this);
        this.structureTypeManager = structureTypeManager;
        this.localizer = localizer;
    }

    @Override
    public ArgumentParseResult<StructureType> parse(
        CommandContext<ICommandSender> commandContext, Queue<String> inputQueue)
    {
        final @Nullable String input = inputQueue.peek();
        final @Nullable StructureType structureTypeType =
            input == null ? null : suggestions.get(input.toLowerCase(Locale.ROOT));

        if (structureTypeType == null)
            return ArgumentParseResult.failure(
                new IllegalStateException("Failed to parse structure type from input: '" + input + "'"));

        inputQueue.remove();
        return ArgumentParseResult.success(structureTypeType);
    }

    @Override
    public List<String> suggestions(CommandContext<ICommandSender> commandContext, String input)
    {
        return suggestions.keySet().stream().filter(val -> startsWith(input, val)).toList();
    }

    /**
     * @param base
     *     The base String to use.
     * @param test
     *     The String to compare against the base String.
     * @return True if the test String has the base string as its base.
     */
    static boolean startsWith(String base, String test)
    {
        return test.toLowerCase(Locale.ROOT).startsWith(base.toLowerCase(Locale.ROOT));
    }

    @Override
    public synchronized void initialize()
    {
        final Collection<StructureType> types = structureTypeManager.getEnabledStructureTypes();
        this.suggestions = new LinkedHashMap<>(MathUtil.ceil(1.25 * types.size()));
        types.forEach(type -> suggestions.put(format(type), type));
    }

    private String format(StructureType type)
    {
        return localizer.getMessage(type.getLocalizationKey()).toLowerCase(Locale.ENGLISH).replaceAll("\\s+", "");
    }

    @Override
    public synchronized void shutDown()
    {
        suggestions = Collections.emptyMap();
    }
}

