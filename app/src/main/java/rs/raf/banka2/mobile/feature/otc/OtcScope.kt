package rs.raf.banka2.mobile.feature.otc

/**
 * Tab koji bira da li gledamo OTC unutar nase banke ili kroz inter-bank protokol.
 * Koristi se i u Discovery i u Ponude/Ugovori ekranu.
 */
enum class OtcScope(val inter: Boolean, val label: String) {
    Domestic(false, "Domace (intra)"),
    Foreign(true, "Inostrane (inter)")
}
