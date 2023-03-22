package nl.pim16aap2.animatedarchitecture.core.util.versioning;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ProjectVersionTest
{
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
    void compareVersions()
    {
        Assertions.assertTrue(ProjectVersion.isNewer("0", "1"));
        Assertions.assertTrue(ProjectVersion.isNewer("1.0", "1.1"));
        Assertions.assertTrue(ProjectVersion.isNewer("0.01", "0.1"));
        Assertions.assertTrue(ProjectVersion.isNewer("0.9", "0.11"));
        Assertions.assertTrue(ProjectVersion.isNewer("0.9", "1.0"));
        Assertions.assertTrue(ProjectVersion.isNewer("01", "1"));
        Assertions.assertTrue(ProjectVersion.isNewer("0.01", "0.1"));
        Assertions.assertTrue(ProjectVersion.isNewer("0.0.2", "0.0.3"));

        Assertions.assertFalse(ProjectVersion.isNewer("0.1", "0.01"));
        Assertions.assertFalse(ProjectVersion.isNewer("0.11", "0.9"));
        Assertions.assertFalse(ProjectVersion.isNewer("1", "0"));
        Assertions.assertFalse(ProjectVersion.isNewer("1", "1"));
        Assertions.assertFalse(ProjectVersion.isNewer("0.0.0.0.01", "0.0.0.0.01"));
    }
}
