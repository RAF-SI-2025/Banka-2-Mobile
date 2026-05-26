package rs.raf.banka2.mobile.core.network

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import java.math.BigDecimal

/**
 * ME-11: Moshi nema nativni adapter za `java.math.BigDecimal` — moramo ga
 * eksplicitno registrovati pre KotlinJsonAdapterFactory.
 *
 * Spec C2 §255 zahteva BigDecimal precision za novcana polja. Backend (Jackson)
 * vraca BigDecimal kao JSON number i prima oba JSON number i JSON string na
 * deserialization-u.
 *
 * Ovaj adapter:
 *  - DESERIALIZE: cita JSON number ILI string preko `reader.nextString()` koji
 *    na NUMBER token-u vraca raw tekstualnu reprezentaciju (preserves precision
 *    bez Double round-trip-a).
 *  - SERIALIZE: pise BigDecimal kao JSON number preko `JsonWriter.value(Number)`
 *    overload-a (ne kao JSON string). BE Jackson uvek prepoznaje number format.
 */
class BigDecimalAdapter {

    @FromJson
    fun fromJson(reader: JsonReader): BigDecimal? {
        return when (reader.peek()) {
            JsonReader.Token.NULL -> reader.nextNull()
            JsonReader.Token.NUMBER, JsonReader.Token.STRING -> {
                val raw = reader.nextString()
                runCatching { BigDecimal(raw) }.getOrNull()
            }
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: BigDecimal?) {
        if (value == null) {
            writer.nullValue()
        } else {
            // Number overload — Moshi ce upisati kao JSON number, ne string.
            writer.value(value as Number)
        }
    }
}
