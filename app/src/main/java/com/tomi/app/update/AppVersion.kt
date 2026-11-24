package com.tomi.app.update

data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String? = null,
) : Comparable<AppVersion> {
    init {
        require(major >= 0 && minor >= 0 && patch >= 0)
    }

    fun isNewerThan(other: AppVersion): Boolean = this > other

    override fun compareTo(other: AppVersion): Int {
        major.compareTo(other.major).let { if (it != 0) return it }
        minor.compareTo(other.minor).let { if (it != 0) return it }
        patch.compareTo(other.patch).let { if (it != 0) return it }
        return when {
            preRelease == null && other.preRelease == null -> 0
            preRelease == null -> 1
            other.preRelease == null -> -1
            else -> comparePreRelease(preRelease, other.preRelease)
        }
    }

    private fun comparePreRelease(left: String, right: String): Int {
        val leftParts = left.split('.')
        val rightParts = right.split('.')
        val count = maxOf(leftParts.size, rightParts.size)
        repeat(count) { index ->
            val a = leftParts.getOrNull(index)
            val b = rightParts.getOrNull(index)
            if (a == null && b == null) return 0
            if (a == null) return -1
            if (b == null) return 1
            val aNumber = a.toIntOrNull()
            val bNumber = b.toIntOrNull()
            val comparison = when {
                aNumber != null && bNumber != null -> aNumber.compareTo(bNumber)
                aNumber != null -> -1
                bNumber != null -> 1
                else -> a.compareTo(b)
            }
            if (comparison != 0) return comparison
        }
        return 0
    }

    companion object {
        private val pattern = Regex(
            "^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:-([0-9A-Za-z.-]+))?(?:\\+[0-9A-Za-z.-]+)?$",
        )

        fun parse(value: String): AppVersion {
            val match = pattern.matchEntire(value.trim())
                ?: throw IllegalArgumentException("Invalid app version: $value")
            return AppVersion(
                major = match.groupValues[1].toInt(),
                minor = match.groupValues[2].toInt(),
                patch = match.groupValues[3].toInt(),
                preRelease = match.groupValues[4].takeIf { it.isNotBlank() },
            )
        }
    }
}
