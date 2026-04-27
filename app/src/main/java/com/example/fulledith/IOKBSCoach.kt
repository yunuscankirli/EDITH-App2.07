package com.example.fulledith

object IOKBSCoach {

    data class Soru(
        val ders: String,
        val metin: String,
        val dogruCevap: String,
        val aciklama: String
    )

    val matematik = listOf(
        Soru("Matematik", "Eksi 12 artı 5 işleminin sonucu kaçtır?", "eksi yedi|−7|-7", "−12 + 5 = −7."),
        Soru("Matematik", "İki x artı 5 eşittir 11 denkleminde x kaçtır?", "üç|x=3", "2x = 6, x = 3."),
        Soru("Matematik", "Bir araba 3 saatte 240 km giderse 5 saatte kaç km gider?", "dört yüz|400", "80 km/s × 5 = 400 km."),
        Soru("Matematik", "Bir üçgenin iki açısı 55 ve 70 derece ise üçüncü açı kaçtır?", "elli beş|55", "180 − 55 − 70 = 55."),
        Soru("Türkçe", "Kalbini kırdı ifadesinde hangi anlam vardır?", "mecaz|ikinci", "Mecaz anlam."),
        Soru("Türkçe", "De bağlacı ayrı mı bitişik mi yazılır?", "ayrı", "Daima ayrı yazılır."),
        Soru("Türkçe", "Güneş gülümsedi ifadesinde hangi söz sanatı vardır?", "kişileştirme|teşhis", "Kişileştirme."),
        Soru("Türkçe", "Hızlıca koştu cümlesinde zarf hangisidir?", "hızlıca", "Hızlıca hal zarfıdır."),
        Soru("Fen", "Güneşe en yakın gezegen hangisidir?", "merkür", "Merkür."),
        Soru("Fen", "Mitoz bölünme sonucu kaç yavru hücre oluşur?", "iki|2", "2 yavru hücre, özdeş DNA."),
        Soru("Fen", "Tuz ve su karışımı hangi yöntemle ayrılır?", "buharlaştırma", "Buharlaştırma."),
        Soru("Fen", "F eşittir m çarpı a formülünde 60N kuvvet 6kg kütleye etki ederse ivme kaçtır?", "on|10", "a = F/m = 10 m/s²."),
        Soru("Sosyal", "Ahilik teşkilatı hangi alanda faaliyet göstermiştir?", "esnaf|zanaat|ticaret", "Esnaf ve zanaatkar örgütüdür."),
        Soru("Sosyal", "Matbaanın Osmanlıya girişi hangi yüzyılda oldu?", "on sekizinci|18|1727", "1727'de İbrahim Müteferrika.")
    )

    fun rastgeleSoru(ders: String? = null): Soru {
        val havuz = if (ders != null)
            matematik.filter { it.ders.lowercase().contains(ders.lowercase()) }
        else matematik
        return havuz.random()
    }
}
