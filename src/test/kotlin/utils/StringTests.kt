package tests.util

import kotlin.test.Test
import kotlin.test.assertEquals
import util.singleLine

class StringTests {
    @Test
    fun `remove multiple lines`() {
        val result =
            """
            Hello
            World
        """.singleLine()

        assertEquals(result, "Hello World")
    }

    @Test
    fun `remove start and end whitespace`() {
        val result =
            """

            Hello
            World

        """.singleLine()

        assertEquals(result, "Hello World")
    }
}
