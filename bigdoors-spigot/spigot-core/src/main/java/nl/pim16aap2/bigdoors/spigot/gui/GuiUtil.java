package nl.pim16aap2.bigdoors.spigot.gui;

class GuiUtil
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
                final String line = header[rowIdx];
                if (line.length() > 9)
                    throw new IllegalArgumentException("Invalid header line: '");
                rows[rowIdx] = header[rowIdx];
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
}
