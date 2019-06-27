package nl.pim16aap2.bigdoors.util;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that loads key/value pairs used for translations.
 *
 * @author Pim
 */
public final class Messages
{
    private HashMap<String, String> messageMap = new HashMap<>();
    private final PLogger logger;
    private final File fileDir;
    private File textFile;
    private static final String DEFAULTFILENAME = "en_US.txt";

    /**
     * Constructor for Messages object.
     *
     * @param fileDir  The directory the messages file(s) will be in.
     * @param fileName The name of the file that will be loaded, if it exists. Extension excluded.
     * @param logger   The {@link PLogger} object that will be used for logging.
     */
    public Messages(final File fileDir, String fileName, final PLogger logger)
    {
        if (!fileDir.exists())
            fileDir.mkdirs();

        this.logger = logger;
        this.fileDir = fileDir;

        textFile = new File(fileDir, fileName + ".txt");
        if (!textFile.exists())
        {
            logger.warn("Failed to load language file: \"" + textFile
                + "\": File not found! Using default file instead!");
            textFile = new File(fileDir, DEFAULTFILENAME);
        }
        writeDefaultFile();
        readFile();
    }

    private void writeDefaultFile()
    {
        File defaultFile = new File(fileDir, DEFAULTFILENAME);
        if (defaultFile.exists() && !defaultFile.setWritable(true))
            logger.severe("Failed to make file \"" + defaultFile + "\" writable!");

        // Load the DEFAULTFILENAME from the resources folder.
        InputStream in = null;
        BufferedReader br = null;
        BufferedWriter bw = null;
        try
        {
            URL url = this.getClass().getClassLoader().getResource(DEFAULTFILENAME);
            if (url == null)
                logger.logMessage("Failed to read resources file from the jar! "
                    + "The default translation file cannot be generated! Please contact pim16aap2");
            else
            {
                URLConnection connection = url.openConnection();
                connection.setUseCaches(false);
                in = connection.getInputStream();
                java.nio.file.Files.copy(in, defaultFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (Exception e)
        {
            logger.logException(e, "Failed to write default file to \"" + textFile + "\".");
        }
        finally
        {
            try
            {
                if (in != null)
                    in.close();
            }
            catch (IOException e)
            {
            }
            try
            {
                if (br != null)
                    br.close();
            }
            catch (IOException e)
            {
            }
            try
            {
                if (bw != null)
                    bw.close();
            }
            catch (IOException e)
            {
            }
        }

        defaultFile.setWritable(false);
    }

    // Read locale file.
    private void readFile()
    {
        try (BufferedReader br = new BufferedReader(new FileReader(textFile)))
        {
            String sCurrentLine;

            while ((sCurrentLine = br.readLine()) != null)
            {
                // Ignore comments.
                if (sCurrentLine.startsWith("#"))
                    continue;
                String key, value;
                String[] parts = sCurrentLine.split("=", 2);
                key = parts[0];
                value = parts[1].replaceAll("&((?i)[0-9a-fk-or])", "\u00A7$1");
                String[] newLineSplitter = value.split("\\\\n"); // Wut? Can I haz more backslash?

                String values = newLineSplitter[0];

                for (int idx = 1; idx < newLineSplitter.length; ++idx)
                    values += "\n" + newLineSplitter[idx];

                messageMap.put(key, values);
            }
        }
        catch (FileNotFoundException e)
        {
            logger.logException(e, "Locale file \"" + textFile + "\" does not exist!");
        }
        catch (IOException e)
        {
            logger.logException(e, "Could not read locale file! \"" + textFile + "\"");
        }
    }

    public String getString(String key)
    {
        String value = messageMap.get(key);
        if (value != null)
            return value;

        logger.warn("Failed to get the translation for key " + key);
        return "Translation for key \"" + key + "\" not found! Contact server admin!";
    }

    public String getStringReverse(String value)
    {
        return messageMap.entrySet().stream().filter(e -> e.getValue().equals(value)).map(Map.Entry::getKey).findFirst()
            .orElse(null);
    }
}
