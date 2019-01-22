package com.murgupluoglu.timetable

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ExampleUnitTest {

    @Test
    fun testFortmatTimeToString() {
        assertEquals("01:23", formatTimeHHmm(1.384f))
    }

}