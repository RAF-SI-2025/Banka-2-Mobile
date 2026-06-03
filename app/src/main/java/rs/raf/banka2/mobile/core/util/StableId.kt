package rs.raf.banka2.mobile.core.util

/**
 * Stabilan, deterministican 64-bitni id iz proizvoljnog String-a (FNV-1a).
 *
 * R2-1482: inter-bank OTC ponude/ugovori/listinzi imaju STRING id-eve (UUID iz
 * partner banke). UI zahteva Long `id` (paritet sa intra-bank tipovima) i koristi
 * ga kao Compose list key. Ranije se koristio `String.hashCode().toLong()`, ali
 * `String.hashCode()` vraca 32-bitni Int (~4.3 mlrd vrednosti, ozbiljan rizik
 * kolizije po Birthday paradoksu vec na hiljadama UUID-a) — dva razlicita
 * `offerId`-a mogu dati isti key → Compose "Key was already used" crash.
 *
 * FNV-1a sa 64-bitnom sirinom drasticno smanjuje verovatnocu kolizije i ostaje
 * deterministican preko sesija (isti UUID → isti id).
 */
fun String.stableLongId(): Long {
    var hash = -0x340d631b7bdddcdbL // 14695981039346656037 (FNV offset basis, 64-bit)
    for (ch in this) {
        hash = hash xor (ch.code.toLong() and 0xff)
        hash *= 0x100000001b3L // FNV prime, 64-bit
    }
    return hash
}
