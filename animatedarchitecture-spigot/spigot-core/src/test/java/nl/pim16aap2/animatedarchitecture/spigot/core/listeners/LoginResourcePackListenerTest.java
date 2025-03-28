package nl.pim16aap2.animatedarchitecture.spigot.core.listeners;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.semver4j.Semver;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static nl.pim16aap2.animatedarchitecture.spigot.core.listeners.LoginResourcePackListener.ResourcePackDetails;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

class ResourcePackDetailsTest
{
    @Test
    void decodeHash_shouldThrowExceptionForInvalidHash()
    {
        assertThatThrownBy(() -> ResourcePackDetails.decodeHash("0123456789"))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("The hash must be 40 characters long! Got: 10 characters.");
    }

    @Test
    void getForVersion_shouldReturnLatestForVersionsAboveAllRanges()
    {
        final ResourcePackDetails latest = ResourcePackDetails.LATEST;

        assertThat(latest).isEqualTo(ResourcePackDetails.getForVersion(Semver.of(2, 0, 0)));
    }

    @Test
    void getForVersion_shouldThrowExceptionForVersionsBelow1_20_0()
    {
        assertThatThrownBy(() -> ResourcePackDetails.getForVersion(Semver.of(1, 19, 0)))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("Version '1.19.0' is lower than the lowest supported version '1.20.0'");
    }

    @ParameterizedTest
    @MethodSource("provideVersionsAndExpectedFormats")
    void getForVersion_shouldReturnCorrectFormatForVersionInRange(Semver version, ResourcePackDetails expected)
    {
        assertThat(expected).isEqualTo(ResourcePackDetails.getForVersion(version));
    }

    private static Stream<Arguments> provideVersionsAndExpectedFormats()
    {
        return Stream.of(
            // FORMAT_15 (1.20.0 - 1.20.1)
            Arguments.of(Semver.of(1, 20, 0), ResourcePackDetails.FORMAT_15),
            Arguments.of(Semver.of(1, 20, 1), ResourcePackDetails.FORMAT_15),
            Arguments.of(Semver.parse("1.20.1-rc1"), ResourcePackDetails.FORMAT_15),

            // FORMAT_18 (1.20.2)
            Arguments.of(Semver.of(1, 20, 2), ResourcePackDetails.FORMAT_18),
            Arguments.of(Semver.parse("1.20.2-SNAPSHOT"), ResourcePackDetails.FORMAT_18),

            // FORMAT_22 (1.20.3 - 1.20.4)
            Arguments.of(Semver.of(1, 20, 3), ResourcePackDetails.FORMAT_22),
            Arguments.of(Semver.of(1, 20, 4), ResourcePackDetails.FORMAT_22),

            // FORMAT_32 (1.20.5 - 1.20.6)
            Arguments.of(Semver.of(1, 20, 5), ResourcePackDetails.FORMAT_32),
            Arguments.of(Semver.of(1, 20, 6), ResourcePackDetails.FORMAT_32),

            // FORMAT_34 (1.21.0 - 1.21.3)
            Arguments.of(Semver.of(1, 21, 0), ResourcePackDetails.FORMAT_34),
            Arguments.of(Semver.of(1, 21, 1), ResourcePackDetails.FORMAT_34),
            Arguments.of(Semver.of(1, 21, 2), ResourcePackDetails.FORMAT_34),
            Arguments.of(Semver.of(1, 21, 3), ResourcePackDetails.FORMAT_34),

            // FORMAT_46 (1.21.4)
            Arguments.of(Semver.of(1, 21, 4), ResourcePackDetails.FORMAT_46),

            // FORMAT_51 (1.21.5)
            Arguments.of(Semver.of(1, 21, 5), ResourcePackDetails.FORMAT_51)
        );
    }

    @ParameterizedTest
    @EnumSource(ResourcePackDetails.class)
    void getUrl_shouldHaveCorrectUrlFormat(ResourcePackDetails detail)
    {
        assertThat(detail.getUrl())
            .isNotNull()
            .endsWith("dl=1")
            .contains("AnimatedArchitectureResourcePack-Format")
            .contains("dropbox.com");
    }

    @ParameterizedTest
    @EnumSource(ResourcePackDetails.class)
    void getHash_shouldReturnValidSha1Hash(LoginResourcePackListener.ResourcePackDetails detail)
    {
        byte[] hash = detail.getHash();
        assertEquals(20, hash.length, "SHA-1 hash should be 20 bytes long");
    }

    @Test
    void hashValues_shouldBeConsistentWithDefinedValues()
    {
        final byte[] format15Hash = ResourcePackDetails.FORMAT_15.getHash();
        final String format15HashHex = bytesToHex(format15Hash);
        assertThat(format15HashHex).isEqualTo("4694DFAB385719F08D74F6A15C720A065F4347D5");

        final byte[] format51Hash = ResourcePackDetails.FORMAT_51.getHash();
        final String format51HashHex = bytesToHex(format51Hash);
        assertThat(format51HashHex).isEqualTo("2D3E655DE27896F8C5C14661CB5EB8C13B678D07");
    }

    @Test
    void enum_shouldNotHaveDuplicateDownloadLinks()
    {
        final ResourcePackDetails[] values = ResourcePackDetails.values();
        final Set<String> urls = new HashSet<>();

        for (final ResourcePackDetails pack : values)
        {
            final String url = pack.getUrl();
            final boolean isNew = urls.add(url);

            assertThat(isNew)
                .as(
                    "Duplicate download URL found: %s in %s. Each resource pack should have a unique URL.",
                    url,
                    pack.name())
                .isTrue();
        }

        assertThat(urls)
            .as("Number of unique URLs should match number of enum values")
            .hasSize(values.length);
    }

    @Test
    void enum_shouldNotHaveOverlappingVersionRanges()
    {
        Semver previousMax = Semver.of(0, 0, 0);

        for (final var entry : ResourcePackDetails.VALUES)
        {
            final Semver min = entry.getMinVersion();
            final Semver max = entry.getMaxVersion();

            assertThat(previousMax.isLowerThan(min))
                .as(
                    "Version ranges should not overlap between %s (%s-%s) and %s (%s-%s)",
                    previousMax,
                    min,
                    entry.name(),
                    max)
                .isTrue();

            previousMax = max;
        }
    }

    @Test
    void enum_shouldNotHaveDownloadLinkEndingWithDl0()
    {
        for (ResourcePackDetails pack : ResourcePackDetails.values())
        {
            String url = pack.getUrl();

            assertThat(url.endsWith("dl=0")).as(
                "Download URL for %s should not end with 'dl=0': %s", pack.name(), url
            ).isFalse();

            assertThat(url.endsWith("dl=1")).as(
                "Download URL for %s should end with 'dl=1': %s", pack.name(), url
            ).isTrue();
        }
    }

    @Test
    void allEnumEntries_shouldHaveNonEmptyUrls()
    {
        for (ResourcePackDetails detail : ResourcePackDetails.VALUES)
        {
            assertThat(detail.getUrl()).isNotNull();
            assertThat(detail.getHash()).isNotNull();
        }
    }

    private static String bytesToHex(byte[] bytes)
    {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes)
            result.append(String.format("%02X", b));
        return result.toString();
    }
}
