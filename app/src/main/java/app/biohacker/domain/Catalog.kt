package app.biohacker.domain

/** The mods and myths, read from the TSVs in assets/content (see docs/CONTENT.md). */
class Catalog(val all: List<Mod>, val myths: List<Myth>) {
    private val byId = all.associateBy { it.id }
    private val bySys = all.groupBy { it.sys }
    private val mythById = myths.associateBy { it.id }

    operator fun get(id: String): Mod? = byId[id]
    fun myth(id: String): Myth? = mythById[id]
    fun of(s: Sys): List<Mod> = bySys[s].orEmpty()

    companion object {
        fun files() = Sys.entries.map { "content/${it.key}.tsv" }
        fun mythFiles() = Sys.entries.map { "content/myths-${it.key}.tsv" }

        private fun opt(s: String) = s.takeIf { it.isNotBlank() && it != "-" }

        fun parse(text: String): List<Mod> = text.lineSequence().mapNotNull { line ->
            val c = line.split('\t')
            if (c.size != 17) return@mapNotNull null
            val sys = Sys.of(c[1]) ?: return@mapNotNull null
            val tag = Tag.of(sys, c[2]) ?: return@mapNotNull null
            val ev = c[3].firstOrNull()?.takeIf { it in "ABCD" } ?: return@mapNotNull null
            Mod(
                c[0], sys, tag, ev, c[4].toIntOrNull()?.coerceIn(1, 3) ?: 1, c[5].toIntOrNull()?.coerceIn(1, 3) ?: 1,
                When.of(c[6]) ?: When.ANY, c[7].toIntOrNull() ?: 5, c[8].toIntOrNull()?.coerceIn(1, 7) ?: 7,
                c[9], c[10], c[11], c[12], c[13], c[14], opt(c[15]), opt(c[16]),
            )
        }.toList()

        fun parseMyths(text: String): List<Myth> = text.lineSequence().mapNotNull { line ->
            val c = line.split('\t')
            if (c.size != 6) return@mapNotNull null
            val sys = Sys.of(c[1]) ?: return@mapNotNull null
            Myth(c[0], sys, c[2], c[3], c[4], c[5])
        }.toList()

        fun of(mods: List<String>, myths: List<String>) = Catalog(mods.flatMap { parse(it) }, myths.flatMap { parseMyths(it) })
    }
}
