package dk.clausr.kanpla.extensions

import dk.clausr.kanpla.model.Menu
import dk.clausr.kanpla.model.Product

val Product.productNameWithEmojis: String
    get() = "${name.getProductNameEmoji()} ${name.substringBefore('(').trim()}"

val Menu.productNameWithEmojis: String
    get() = "${productName.getProductNameEmoji()} ${name.substringBefore('(').trim().ifBlank { productName }}"

private fun String.getProductNameEmoji(): String = with(this.lowercase()) {
    return when {
        contains("lilla") -> "🟣"
        contains("grøn") -> "🟢"
        contains("orange") -> "🟠"
        contains("rød") -> "🔴"
        contains("håndmadder") -> ""
        contains("salat") -> {
            buildString {
                when {
                    this@with.contains("protein") -> append("💪")
                    this@with.contains("vegetar") -> append("🐰")
                }
                append("🥗")
            }
        }

        contains("sandwich") -> {
            buildString {
                when {
                    this@with.contains("kød") -> append("🥩")
                    this@with.contains("vegetar") -> append("🐰")
                }
                append("🥪")
            }
        }

        else -> "🧊"
    }
}
