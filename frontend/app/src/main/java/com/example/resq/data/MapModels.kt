package com.example.resq.data

/**
 * Categories shown as filter chips on the Map screen.
 */
enum class ResponderCategory(
    val label    : String,
    val emoji    : String,
) {
    HOSPITAL  ("Hospital",   "🏥"),
    POLICE    ("Police",     "🚔"),
    AMBULANCE ("Ambulance",  "🚑"),
    FIRE      ("Damkar",     "🚒"),
    CONTACT   ("Contact",    "📞"),
}

/**
 * A single nearby responder pin shown on the map and in the result list.
 */
data class NearbyResponder(
    val id          : Int,
    val name        : String,
    val address     : String,
    val category    : ResponderCategory,
    val lat         : Double,
    val lng         : Double,
    val distanceKm  : Double,   // straight-line from user
    val etaMinutes  : Int,      // estimated travel time
)

/**
 * Dummy data — clustered around a Jakarta center point
 * (Jl. Jend. Sudirman area: -6.2088, 106.8456).
 *
 * When backend is ready, replace this object with a repository call.
 */
object DummyResponders {

    private val BASE_LAT = -6.2088
    private val BASE_LNG = 106.8456

    val all: List<NearbyResponder> = listOf(

        // ── Hospitals ────────────────────────────────────────────────────────
        NearbyResponder(1,  "Bhakti Asih General Hospital",    "Jl. Raya Kesehatan No. 12", ResponderCategory.HOSPITAL,  BASE_LAT + 0.012, BASE_LNG + 0.008, 1.2, 5),
        NearbyResponder(2,  "RS Cipto Mangunkusumo",           "Jl. Diponegoro No. 71",     ResponderCategory.HOSPITAL,  BASE_LAT - 0.009, BASE_LNG + 0.014, 1.8, 7),
        NearbyResponder(3,  "RS MMC Jakarta",                  "Jl. HR Rasuna Said Kav. C-21", ResponderCategory.HOSPITAL, BASE_LAT + 0.021, BASE_LNG - 0.011, 2.4, 10),
        NearbyResponder(4,  "RS Jakarta",                      "Jl. Jend. Sudirman Kav. 49", ResponderCategory.HOSPITAL, BASE_LAT - 0.018, BASE_LNG - 0.006, 2.0, 8),
        NearbyResponder(5,  "Klinik Medika Utama",             "Jl. Fatmawati Raya No. 5",  ResponderCategory.HOSPITAL,  BASE_LAT + 0.030, BASE_LNG + 0.020, 3.5, 14),

        // ── Police ───────────────────────────────────────────────────────────
        NearbyResponder(6,  "Polsek Tanah Abang",              "Jl. KH Mas Mansyur No. 25", ResponderCategory.POLICE,    BASE_LAT + 0.007, BASE_LNG - 0.015, 1.4, 6),
        NearbyResponder(7,  "Polres Jakarta Pusat",            "Jl. Kramat Raya No. 303",   ResponderCategory.POLICE,    BASE_LAT - 0.014, BASE_LNG + 0.021, 2.8, 11),
        NearbyResponder(8,  "Polsek Setiabudi",                "Jl. HR Rasuna Said No. 1",  ResponderCategory.POLICE,    BASE_LAT + 0.025, BASE_LNG + 0.004, 2.6, 10),
        NearbyResponder(9,  "Polda Metro Jaya",                "Jl. Jend. Sudirman No. 55", ResponderCategory.POLICE,    BASE_LAT - 0.005, BASE_LNG - 0.022, 1.9, 8),

        // ── Ambulance ────────────────────────────────────────────────────────
        NearbyResponder(10, "Ambulans 118 – Pos Sudirman",     "Jl. Jend. Sudirman Km. 2",  ResponderCategory.AMBULANCE, BASE_LAT + 0.004, BASE_LNG + 0.003, 0.6, 3),
        NearbyResponder(11, "PMI Jakarta Pusat",               "Jl. Medan Merdeka Timur 6", ResponderCategory.AMBULANCE, BASE_LAT - 0.022, BASE_LNG + 0.009, 2.5, 10),
        NearbyResponder(12, "RSUD Tarakan – Ambulans",         "Jl. Kyai Caringin No. 7",   ResponderCategory.AMBULANCE, BASE_LAT + 0.016, BASE_LNG - 0.018, 2.1, 9),

        // ── Fire (Damkar) ─────────────────────────────────────────────────────
        NearbyResponder(13, "Damkar Pos Tanah Abang",          "Jl. Jati Baru Raya No. 1",  ResponderCategory.FIRE,      BASE_LAT - 0.011, BASE_LNG - 0.013, 1.5, 6),
        NearbyResponder(14, "Damkar Pos Setiabudi",            "Jl. Mampang Prapatan No. 7",ResponderCategory.FIRE,      BASE_LAT + 0.028, BASE_LNG + 0.016, 3.2, 13),
        NearbyResponder(15, "Damkar Pos Gambir",               "Jl. Medan Merdeka Utara 8", ResponderCategory.FIRE,      BASE_LAT - 0.019, BASE_LNG + 0.025, 3.0, 12),

        // ── Contact (Emergency contacts dari UserSession) ─────────────────────
        // Placeholder — akan diisi dari UserSessionManager di fragment
        NearbyResponder(16, "Kontak Darurat 1",                "Tersimpan di profil Anda",  ResponderCategory.CONTACT,   BASE_LAT + 0.003, BASE_LNG + 0.007, 0.8, 4),
        NearbyResponder(17, "Kontak Darurat 2",                "Tersimpan di profil Anda",  ResponderCategory.CONTACT,   BASE_LAT - 0.007, BASE_LNG - 0.004, 1.1, 5),
    )

    fun byCategory(category: ResponderCategory) =
        all.filter { it.category == category }.sortedBy { it.distanceKm }
}
