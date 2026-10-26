package dev.cubecrafttd.ui

enum class TdLanguage {
    EN_US, ZH_CN;

    companion object {
        fun parse(value: String): TdLanguage? = when (value.lowercase().replace('-', '_')) {
            "zh", "zh_cn", "简体中文" -> ZH_CN
            "en", "en_us", "english" -> EN_US
            else -> null
        }
    }
}

/** Presentation-only localization. Never rewrite action IDs, configuration keys or player data. */
object TdText {
    private val placeholder = Regex("\\{([a-zA-Z0-9_]+)(!)?}")
    private data class Template(val pattern: Regex, val names: List<Pair<String, Boolean>>, val target: String)
    private val exact = linkedMapOf<String, String>()
    private val templates = mutableListOf<Template>()

    init {
        val stream = requireNotNull(TdText::class.java.getResourceAsStream("/lang/zh_CN.tsv")) {
            "Missing bundled Chinese translations"
        }
        stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.filter { it.isNotBlank() && !it.startsWith('#') }.forEach { line ->
                val parts = line.split('\t')
                require(parts.size == 2) { "Invalid translation entry: $line" }
                val (source, target) = parts
                val fields = placeholder.findAll(source).toList()
                if (fields.isEmpty()) {
                    require(exact.put(source, target) == null) { "Duplicate translation: $source" }
                } else {
                    require(fields.map { it.groupValues[1] }.toSet().size == fields.size)
                    val names = fields.map { it.groupValues[1] to (it.groupValues[2] == "!") }
                    require(placeholder.findAll(target).all { match -> names.any { it.first == match.groupValues[1] } })
                    var offset = 0
                    val pattern = buildString {
                        fields.forEach { field ->
                            append(Regex.escape(source.substring(offset, field.range.first)))
                            append("([\\s\\S]*?)")
                            offset = field.range.last + 1
                        }
                        append(Regex.escape(source.substring(offset)))
                    }
                    templates += Template(Regex(pattern), names, target)
                }
            }
        }
    }

    fun translate(text: String, language: TdLanguage): String = translate(text, language, 0)

    private fun translate(text: String, language: TdLanguage, depth: Int): String {
        if (language == TdLanguage.EN_US || text.isEmpty() || depth > 6) return text
        exact[text]?.let { return it }
        templates.forEach { template ->
            val match = template.pattern.matchEntire(text) ?: return@forEach
            val values = template.names.mapIndexed { index, (name, localized) ->
                val value = match.groupValues[index + 1]
                name to if (localized) translate(value, language, depth + 1) else value
            }.toMap()
            return placeholder.replace(template.target) { values.getValue(it.groupValues[1]) }
        }
        return text // Unknown diagnostics remain searchable, including their original error code.
    }

    /** Re-map recovered emphasis after translation changes UTF-16 offsets. */
    fun spans(text: String, spans: List<MenuBoldSpan>, language: TdLanguage): List<MenuBoldSpan> {
        if (language == TdLanguage.EN_US) return spans
        val translated = translate(text, language)
        var offset = 0
        return spans.mapNotNull { span ->
            val highlight = translate(text.substring(span.start, span.endExclusive), language)
            val start = translated.indexOf(highlight, offset)
            if (start < 0 || highlight.isEmpty()) null else {
                offset = start + highlight.length
                MenuBoldSpan(start, offset, span.color)
            }
        }
    }
}
