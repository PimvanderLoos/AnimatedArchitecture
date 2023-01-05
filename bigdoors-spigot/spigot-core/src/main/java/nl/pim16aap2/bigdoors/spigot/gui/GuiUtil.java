package nl.pim16aap2.bigdoors.spigot.gui;

import lombok.extern.flogger.Flogger;

@Flogger final class GuiUtil
{
    private GuiUtil()
    {
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
            log.atSevere().log("Invalid header line: '%s'! Line too long!", headerLine);
            return headerLine.substring(0, 9);
        }
        else if (headerLine.length() < 9)
        {
            log.atSevere().log("Invalid header line: '%s'! Line too short!", headerLine);
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
}
