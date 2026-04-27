package com.whc06.trainer.ble

object WhC06Parser {
    const val MANUFACTURER_ID = 256
    private const val WEIGHT_OFFSET = 10
    private const val STABLE_OFFSET = 14
    private const val MIN_PAYLOAD_LEN = 16
    private const val MAX_RAW = 30_000

    enum class Unit(val raw: Int) {
        KG(1), LB(2), ST(3), JIN(4), UNKNOWN(0);
        companion object {
            fun from(raw: Int) = entries.firstOrNull { it.raw == raw } ?: UNKNOWN
        }
    }

    data class Sample(
        val kg: Double,
        val stable: Boolean,
        val unit: Unit,
        val timestampNanos: Long,
        val rssi: Int
    )

    fun parse(mfrData: ByteArray, ts: Long, rssi: Int = 0): Sample? {
        if (mfrData.size < MIN_PAYLOAD_LEN) return null
        val hi = mfrData[WEIGHT_OFFSET].toInt() and 0xFF
        val lo = mfrData[WEIGHT_OFFSET + 1].toInt() and 0xFF
        val raw = (hi shl 8) or lo
        if (raw > MAX_RAW) return null
        val stableByte = mfrData[STABLE_OFFSET].toInt() and 0xFF
        val stable = (stableByte and 0xF0) shr 4 != 0
        val unit = Unit.from(stableByte and 0x0F)
        return Sample(raw / 100.0, stable, unit, ts, rssi)
    }
}
