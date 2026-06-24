package eu.zeletrik.beanbook.ai.internal

private const val MAX_REDUCED_CHARS = 16_000

private val DROP_BLOCKS = Regex(
    "<(script|style|nav|header|footer|svg|noscript)\\b[^>]*>.*?</\\1>",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)
private val TAGS = Regex("<[^>]+>")
private val WHITESPACE = Regex("\\s+")

/**
 * Crudely reduces an HTML page to visible text for the LLM: drops script/style/chrome blocks, strips the
 * remaining tags, unescapes a few common entities, collapses whitespace, and caps the length so the prompt
 * (and cost) stay bounded. Best-effort — enough to feed a model, not a real DOM parse.
 */
internal fun reduceHtml(html: String): String =
    html.replace(DROP_BLOCKS, " ")
        .replace(TAGS, " ")
        .let(::unescapeEntities)
        .replace(WHITESPACE, " ")
        .trim()
        .take(MAX_REDUCED_CHARS)

private fun unescapeEntities(text: String): String =
    text.replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ")
