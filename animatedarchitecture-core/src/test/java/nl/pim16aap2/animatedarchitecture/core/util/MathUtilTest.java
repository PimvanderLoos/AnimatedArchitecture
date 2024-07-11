package nl.pim16aap2.animatedarchitecture.core.util;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.*;

public class MathUtilTest
{
    @Test
    void isNumerical()
    {
        assertTrue(MathUtil.isNumerical("-1"));
        assertTrue(MathUtil.isNumerical("1"));
        assertTrue(MathUtil.isNumerical("9999999"));

        assertFalse(MathUtil.isNumerical(null));
        assertFalse(MathUtil.isNumerical(""));
        assertFalse(MathUtil.isNumerical("-"));
        assertFalse(MathUtil.isNumerical("1-"));
        assertFalse(MathUtil.isNumerical("a"));
        assertFalse(MathUtil.isNumerical(":"));
        assertFalse(MathUtil.isNumerical("/"));
        assertFalse(MathUtil.isNumerical("99999999 "));
        assertFalse(MathUtil.isNumerical("99999999a"));
    }

    @Test
    void testClampAngleRad()
    {
        assertEquals(0, MathUtil.clampAngleRad(0), MathUtil.EPS);
        assertEquals(Math.PI, MathUtil.clampAngleRad(Math.PI), MathUtil.EPS);
        assertEquals(Math.PI, MathUtil.clampAngleRad(3 * Math.PI), MathUtil.EPS);
        assertEquals(-Math.PI, MathUtil.clampAngleRad(-3 * Math.PI), MathUtil.EPS);
        assertEquals(MathUtil.HALF_PI, MathUtil.clampAngleRad(MathUtil.HALF_PI), MathUtil.EPS);
    }

    @Test
    void testClampAngleDeg()
    {
        assertEquals(0, MathUtil.clampAngleDeg(0), MathUtil.EPS);
        assertEquals(0, MathUtil.clampAngleDeg(360), MathUtil.EPS);
        assertEquals(0, MathUtil.clampAngleDeg(720), MathUtil.EPS);
        assertEquals(0, MathUtil.clampAngleDeg(-720), MathUtil.EPS);

        assertEquals(90, MathUtil.clampAngleDeg(90), MathUtil.EPS);

        assertEquals(359, MathUtil.clampAngleDeg(359), MathUtil.EPS);
        assertEquals(1, MathUtil.clampAngleDeg(361), MathUtil.EPS);

        assertEquals(-359, MathUtil.clampAngleDeg(-359), MathUtil.EPS);
        assertEquals(-1, MathUtil.clampAngleDeg(-361), MathUtil.EPS);
    }

    @Test
    void testParseInt()
    {
        assertEquals(OptionalInt.of(123), MathUtil.parseInt("123"));
        assertEquals(OptionalInt.empty(), MathUtil.parseInt("abc"));
        assertEquals(OptionalInt.empty(), MathUtil.parseInt((String) null));
    }

    @Test
    void testParseIntOptional()
    {
        assertEquals(OptionalInt.of(123), MathUtil.parseInt(Optional.of("123")));
        assertEquals(OptionalInt.empty(), MathUtil.parseInt(Optional.of("abc")));
        assertEquals(OptionalInt.empty(), MathUtil.parseInt(Optional.empty()));
    }

    @Test
    void testParseDouble()
    {
        assertOptionalDoubleEquals(OptionalDouble.of(123.45), MathUtil.parseDouble("123.45"));
        assertOptionalDoubleEquals(OptionalDouble.empty(), MathUtil.parseDouble("abc"));
        assertOptionalDoubleEquals(OptionalDouble.empty(), MathUtil.parseDouble((String) null));
    }

    @Test
    void testParseDoubleOptional()
    {
        assertOptionalDoubleEquals(OptionalDouble.of(123.45), MathUtil.parseDouble(Optional.of("123.45")));
        assertOptionalDoubleEquals(OptionalDouble.empty(), MathUtil.parseDouble(Optional.of("abc")));
        assertOptionalDoubleEquals(OptionalDouble.empty(), MathUtil.parseDouble(Optional.empty()));
    }

    @Test
    void testParseLong()
    {
        assertEquals(OptionalLong.of(123456789L), MathUtil.parseLong("123456789"));
        assertEquals(OptionalLong.empty(), MathUtil.parseLong("abc"));
        assertEquals(OptionalLong.empty(), MathUtil.parseLong((String) null));
    }

    @Test
    void testParseLongOptional()
    {
        assertEquals(OptionalLong.of(123456789L), MathUtil.parseLong(Optional.of("123456789")));
        assertEquals(OptionalLong.empty(), MathUtil.parseLong(Optional.of("abc")));
        assertEquals(OptionalLong.empty(), MathUtil.parseLong(Optional.empty()));
    }

    @Test
    void testIsNumerical()
    {
        assertTrue(MathUtil.isNumerical("123"));
        assertTrue(MathUtil.isNumerical("-123"));

        assertFalse(MathUtil.isNumerical("123-"));
        assertFalse(MathUtil.isNumerical("--123"));
        assertFalse(MathUtil.isNumerical("abc"));
        assertFalse(MathUtil.isNumerical(null));
        assertFalse(MathUtil.isNumerical(""));
    }

    @Test
    void testBetweenInt()
    {
        assertTrue(MathUtil.between(5, 1, 10));
        assertTrue(MathUtil.between(1, 1, 10));
        assertTrue(MathUtil.between(10, 1, 10));

        assertFalse(MathUtil.between(0, 1, 10));
        assertFalse(MathUtil.between(11, 1, 10));
    }

    @Test
    void testBetweenDouble()
    {
        assertTrue(MathUtil.between(5.5, 1.0, 10.0));
        assertTrue(MathUtil.between(1.0, 1.0, 10.0));
        assertTrue(MathUtil.between(10.0, 1.0, 10.0));

        assertFalse(MathUtil.between(0.5, 1.0, 10.0));
        assertFalse(MathUtil.between(10.5, 1.0, 10.0));
    }

    void assertOptionalDoubleEquals(OptionalDouble expected, OptionalDouble actual)
    {
        if (expected.isEmpty())
        {
            assertTrue(actual.isEmpty());
            return;
        }

        assertTrue(actual.isPresent());
        assertEquals(expected.getAsDouble(), actual.getAsDouble(), MathUtil.EPS);
    }
}
