package com.khayrrr.mangaviewer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NaturalOrderComparatorTest {

    @Test
    fun testNaturalOrderNumericSorting() {
        val list = listOf("page 10.jpg", "page 1.jpg", "page 2.jpg", "page 20.jpg", "page 03.jpg")
        val sorted = list.sortedWith(NaturalOrderComparator)
        assertEquals(
            listOf("page 1.jpg", "page 2.jpg", "page 03.jpg", "page 10.jpg", "page 20.jpg"),
            sorted
        )
    }

    @Test
    fun testEqualStrings() {
        assertEquals(0, NaturalOrderComparator.compare("abc", "abc"))
        assertEquals(0, NaturalOrderComparator.compare("123", "123"))
    }

    @Test
    fun testCaseInsensitive() {
        assertTrue(NaturalOrderComparator.compare("Apple", "banana") < 0)
        assertTrue(NaturalOrderComparator.compare("banana", "Apple") > 0)
    }
}
