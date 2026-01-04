package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.InventoryGui;
import lombok.CustomLog;
import lombok.experimental.UtilityClass;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.WrappedPlayer;

import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class for GUI-related operations.
 */
@CustomLog
@UtilityClass
public final class GuiUtil
{
    /**
     * Closes all {@link InventoryGui}s that are currently open for a player.
     *
     * @param player
     *     The player whose guis to close.
     */
    public static void closeAllGuis(WrappedPlayer player)
    {
        final Deque<InventoryGui> history = InventoryGui.getHistory(player.getBukkitPlayer());
        InventoryGui finalGui = null;
        while (!history.isEmpty())
        {
            final InventoryGui gui = history.removeLast();
            gui.close(false);

            final var closeAction = gui.getCloseAction();
            if (closeAction != null)
            {
                closeAction.onClose(new InventoryGui.Close(
                    player.getBukkitPlayer(),
                    gui,
                    null
                ));
            }

            finalGui = gui;
        }
        if (finalGui != null)
            finalGui.destroy();
        InventoryGui.clearHistory(player.getBukkitPlayer());
    }

    /**
     * Closes all {@link InventoryGui}s that are currently open for a player, up to and including the provided guiPage.
     * <p>
     * This method will loop through the history of the player's guis and close them until the provided guiPage is
     * encountered. Note that the provided guiPage will also be closed.
     *
     * @param guiPage
     *     The guiPage to close up to.
     * @param player
     *     The player whose guis to close.
     */
    public static void closeGuiPage(InventoryGui guiPage, WrappedPlayer player)
    {
        final Deque<InventoryGui> history = InventoryGui.getHistory(player.getBukkitPlayer());
        while (!history.isEmpty())
        {
            final InventoryGui gui = history.removeLast();
            gui.close(false);
            // We don't use the close thing anywhere anyway.
            gui.getCloseAction().onClose(null);

            if (gui == guiPage) //NOPMD - Intentional reference comparison
                break;
        }
    }

    /**
     * Creates an array of 9-character-wide strings.
     * <p>
     * Each line contains the same character, up until 'count' characters have been placed (in total).
     * <p>
     * E.g., with a count of 12 and a character 's', the output would be ["sssssssss", "sss      "].
     * <p>
     * At most 5 lines can be used, including a potential header.
     *
     * @param ch
     *     The character to repeat.
     * @param count
     *     The number of times to repeat the character.
     * @param header
     *     The header lines to use. Each line is only allowed to be at most 9 characters wide.
     * @return The constructed array of strings.
     *
     * @throws IllegalArgumentException
     *     When a line in the header is encountered that is more than 9 characters wide.
     */
    public static String[] fillLinesWithChar(char ch, int count, String... header)
        throws IllegalArgumentException
    {
        // 5 lines = max inv size.
        final int rowCount = Math.min(5, 1 + (count / 9) + header.length);
        final String[] rows = new String[rowCount];

        int characterCount = 0;
        for (int rowIdx = 0; rowIdx < rowCount; ++rowIdx)
        {
            if (rowIdx < header.length)
            {
                rows[rowIdx] = getNormalizedHeaderLine(header[rowIdx]);
                continue;
            }

            final char[] row = new char[9];
            for (int col = 0; col < 9; ++col)
            {
                if (characterCount++ < count)
                    row[col] = ch;
                else
                    row[col] = ' ';
            }
            rows[rowIdx] = new String(row);
        }
        return rows;
    }

    private static String getNormalizedHeaderLine(String headerLine)
    {
        if (headerLine.length() > 9)
        {
            log.atError().log("Invalid header line: '%s'! Line too long!", headerLine);
            return headerLine.substring(0, 9);
        }
        else if (headerLine.length() < 9)
        {
            log.atError().log("Invalid header line: '%s'! Line too short!", headerLine);
            final char[] padding = new char[9];
            int idx = 0;
            for (; idx < headerLine.length(); ++idx)
                padding[idx] = headerLine.charAt(idx);
            for (; idx < 9; ++idx)
                padding[idx] = ' ';
            return new String(padding);
        }
        return headerLine;
    }

    /**
     * Creates a list of GUI elements for the deletable properties, alphabetically sorted by their localized button
     * titles.
     *
     * @param inputElements
     *     The collection of elements to create GUI elements for.
     * @param mapper
     *     The mapper to create the GUI elements from the provided input elements.
     * @param <T>
     *     The type of the input elements.
     * @return The sorted list of GUI elements.
     */
    public static <T> List<GuiElement> getGuiElementsSortedByTitle(
        Collection<T> inputElements,
        Function<T, NamedGuiElement> mapper
    )
    {
        return inputElements.stream()
            .map(mapper)
            .sorted(Comparator.comparing(NamedGuiElement::name))
            .map(NamedGuiElement::element)
            .collect(Collectors.toList());
    }

    /**
     * The mapper interface for creating named GUI elements.
     *
     * @param <T>
     *     The type of the input element.
     */
    public interface NamedGuiElementMapper<T>
    {
        /**
         * Creates a named GUI element for the given input element.
         *
         * @param slotChar
         *     The slot character where the element will be placed.
         * @param entry
         *     The input element to create the GUI element for.
         * @return The created named GUI element.
         */
        NamedGuiElement create(char slotChar, T entry);
    }
}
