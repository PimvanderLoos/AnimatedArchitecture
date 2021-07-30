package nl.pim16aap2.bigdoors.localization;

import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

class LocalizationGeneratorTest
{
    @Test
    void testGetLocaleFilesInDirectory()
    {
        final @NotNull String baseName = "Translation";
        final @NotNull List<Path> paths = new ArrayList<>(5);

        val path0 = Paths.get("./" + baseName + ".properties");
        val path1 = Paths.get("./" + baseName + "_en_US.properties");

        paths.add(path0);
        paths.add(path1);
        // Ignored because it doesn't start with the correct base name.
        paths.add(Paths.get("./randomFile_nl_NL.properties"));
        // Ignored because we only look for ".properties" files.
        paths.add(Paths.get("./" + baseName + "_nl_NL.txt"));
        // Ignored because the basename isn't followed by either ".properties"
        // (default file) or by _[locale].
        paths.add(Paths.get("./" + baseName + "nl_NL.txt"));


        @NotNull val localeFiles = LocalizationGenerator.getLocaleFiles(baseName, paths);
        System.out.println(localeFiles);
        Assertions.assertEquals(2, localeFiles.size());
        Assertions.assertEquals(new LocalizationGenerator.LocaleFile(path0, ""), localeFiles.get(0));
        Assertions.assertEquals(new LocalizationGenerator.LocaleFile(path1, "en_US"), localeFiles.get(1));
    }

    @Test
    void testGetKeyFromLine()
    {
        Assertions.assertEquals("key", LocalizationGenerator.getKeyFromLine("key=value"));
        Assertions.assertEquals("key", LocalizationGenerator.getKeyFromLine("key=value=another_value"));
        Assertions.assertNull(LocalizationGenerator.getKeyFromLine("key"));
        Assertions.assertNull(LocalizationGenerator.getKeyFromLine(""));
    }

    @Test
    void testGetKeySet()
    {
        final List<String> input = new ArrayList<>(5);
        input.add("key=value");
        input.add("key=value");
        input.add("key_value");
        input.add("key2=value2");
        input.add("key3=======");

        val output = LocalizationGenerator.getKeySet(input);
        Assertions.assertEquals(3, output.size());
        Assertions.assertTrue(output.contains("key"));
        Assertions.assertTrue(output.contains("key2"));
        Assertions.assertTrue(output.contains("key3"));
    }

    @Test
    void testAppendableLocales()
    {
        // Existing: Unique set
        final List<String> existing = new ArrayList<>(10);
        existing.add("key0=value0");
        existing.add("key1=value1");
        existing.add("key2=value2");
        existing.add("key3=value3");
        existing.add("key4=value4");
        existing.add("key5=value5");
        existing.add("key6=value6");
        existing.add("key7=value7");
        existing.add("key8=value8");
        existing.add("key9=value9");

        // NewLines: Overlap with existing
        final List<String> lstB = new ArrayList<>(5);
        lstB.add("key5=value10");
        lstB.add("key6=value11");
        lstB.add("key7=value12");
        lstB.add("key8=value13");
        lstB.add("key9=value14");

        // NewLines: Unique set
        final List<String> lstC = new ArrayList<>(5);
        lstC.add("key10=value15");
        lstC.add("key11=value16");
        lstC.add("key12=value17");
        lstC.add("key13=value18");
        lstC.add("key14=value19");

        final List<String> newLines = new ArrayList<>(10);
        newLines.addAll(lstB);
        newLines.addAll(lstC);

        val appendable = LocalizationGenerator.getAppendable(existing, newLines);
        // Real appendable exists of only list C as all keys from lists B already exist in the "existing" list.
        Assertions.assertEquals(lstC, appendable);
    }
}
