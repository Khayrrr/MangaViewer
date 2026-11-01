package com.khayrrr.mangaviewer.util

/**
 * Natural alphanumeric comparator for strings (e.g. "page 2.jpg" before "page 10.jpg").
 * Splits strings into digit and non-digit chunks for comparison.
 */
object NaturalOrderComparator : Comparator<String> {

    override fun compare(s1: String?, s2: String?): Int {
        if (s1 == null && s2 == null) return 0
        if (s1 == null) return -1
        if (s2 == null) return 1

        var idx1 = 0
        var idx2 = 0
        val len1 = s1.length
        val len2 = s2.length

        while (idx1 < len1 && idx2 < len2) {
            val c1 = s1[idx1]
            val c2 = s2[idx2]

            if (c1.isDigit() && c2.isDigit()) {
                // Find complete numeric run for both strings
                val start1 = idx1
                while (idx1 < len1 && s1[idx1].isDigit()) {
                    idx1++
                }
                val start2 = idx2
                while (idx2 < len2 && s2[idx2].isDigit()) {
                    idx2++
                }

                val numStr1 = s1.substring(start1, idx1).trimStart('0')
                val numStr2 = s2.substring(start2, idx2).trimStart('0')

                // Compare numeric values
                if (numStr1.length != numStr2.length) {
                    return numStr1.length - numStr2.length
                }
                val numDiff = numStr1.compareTo(numStr2)
                if (numDiff != 0) {
                    return numDiff
                }
            } else {
                val charDiff = c1.lowercaseChar().compareTo(c2.lowercaseChar())
                if (charDiff != 0) {
                    return charDiff
                }
                idx1++
                idx2++
            }
        }

        return len1 - len2
    }
}
