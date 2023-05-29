package nl.pim16aap2.animatedarchitecture.core.util.versioning;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ProjectVersionTest
{
    @Test
    void parse()
    {
        Assertions.assertEquals(new ProjectVersion("1", false), ProjectVersion.parse("1"));
        Assertions.assertEquals(new ProjectVersion("1.0", false), ProjectVersion.parse("1.0"));
        Assertions.assertEquals(new ProjectVersion("1.0.0", false), ProjectVersion.parse("1.0.0"));
        Assertions.assertEquals(new ProjectVersion("1", true), ProjectVersion.parse("1-SNAPSHOT"));
        Assertions.assertEquals(new ProjectVersion("1.0", true), ProjectVersion.parse("1.0-SNAPSHOT"));
        Assertions.assertEquals(new ProjectVersion("1.0.0", true), ProjectVersion.parse("1.0.0-SNAPSHOT"));
    }

    @Test
    void testStringToInteger()
    {
        Assertions.assertEquals(1, ProjectVersion.versionSectionToInt("1", 1, true));
        Assertions.assertEquals(10, ProjectVersion.versionSectionToInt("1", 2, true));
        Assertions.assertEquals(100, ProjectVersion.versionSectionToInt("1", 3, true));

        Assertions.assertEquals(1, ProjectVersion.versionSectionToInt("1", 1, false));
        Assertions.assertEquals(1, ProjectVersion.versionSectionToInt("1", 2, false));
        Assertions.assertEquals(1, ProjectVersion.versionSectionToInt("1", 3, false));
    }

    @Test
    void isNewer()
    {
        Assertions.assertTrue(ProjectVersion.isNewer("0", "1"));
        Assertions.assertTrue(ProjectVersion.isNewer("1.0", "1.1"));
        Assertions.assertTrue(ProjectVersion.isNewer("0.01", "0.1"));
        Assertions.assertTrue(ProjectVersion.isNewer("0.9", "0.11"));
        Assertions.assertTrue(ProjectVersion.isNewer("0.9", "1.0"));
        Assertions.assertTrue(ProjectVersion.isNewer("01", "1"));
        Assertions.assertTrue(ProjectVersion.isNewer("0.01", "0.1"));
        Assertions.assertTrue(ProjectVersion.isNewer("0.0.2", "0.0.3"));

        Assertions.assertFalse(ProjectVersion.isNewer("1", "1.0.00000.0"));
        Assertions.assertFalse(ProjectVersion.isNewer("0.1", "0.01"));
        Assertions.assertFalse(ProjectVersion.isNewer("0.11", "0.9"));
        Assertions.assertFalse(ProjectVersion.isNewer("1", "0"));
        Assertions.assertFalse(ProjectVersion.isNewer("1", "1"));
        Assertions.assertFalse(ProjectVersion.isNewer("0.0.0.0.01", "0.0.0.0.01"));
    }

    @Test
    void isNewerThan()
    {
        Assertions.assertTrue(ProjectVersion.parse("1.0").isNewerThan("0"));
        Assertions.assertTrue(ProjectVersion.parse("1").isNewerThan("0.1-SNAPSHOT"));
        Assertions.assertFalse(ProjectVersion.parse("1.0").isNewerThan("1.0"));
        Assertions.assertTrue(ProjectVersion.parse("1.0").isNewerThan("1.0-SNAPSHOT"));

        Assertions.assertTrue(ProjectVersion.parse("1.0-SNAPSHOT").isNewerThan("0"));
        Assertions.assertFalse(ProjectVersion.parse("1.0-SNAPSHOT").isNewerThan("1.0"));
        Assertions.assertFalse(ProjectVersion.parse("1.0-SNAPSHOT").isNewerThan("1"));
    }

    @Test
    void isAtLeast()
    {
        Assertions.assertTrue(ProjectVersion.parse("1.0").isAtLeast("0"));
        Assertions.assertTrue(ProjectVersion.parse("1").isAtLeast("0.1-SNAPSHOT"));
        Assertions.assertTrue(ProjectVersion.parse("1.0").isAtLeast("1.0"));
        Assertions.assertTrue(ProjectVersion.parse("1.0").isAtLeast("1.0-SNAPSHOT"));

        Assertions.assertTrue(ProjectVersion.parse("1.0-SNAPSHOT").isAtLeast("0"));
        Assertions.assertFalse(ProjectVersion.parse("1.0-SNAPSHOT").isAtLeast("1.0"));
        Assertions.assertFalse(ProjectVersion.parse("1.0-SNAPSHOT").isAtLeast("1"));
    }

    @Test
    void trimInsignificantZeroes()
    {
        Assertions.assertEquals("1", ProjectVersion.trimInsignificantZeroes("1.0.0"));
        Assertions.assertEquals("1000", ProjectVersion.trimInsignificantZeroes("1000"));
        Assertions.assertEquals("1.0.0.1", ProjectVersion.trimInsignificantZeroes("1.0.0.1"));
        Assertions.assertEquals("10", ProjectVersion.trimInsignificantZeroes("10.0.0"));
        Assertions.assertEquals("0.0.0.00001", ProjectVersion.trimInsignificantZeroes("0.0.0.00001"));
    }
}
