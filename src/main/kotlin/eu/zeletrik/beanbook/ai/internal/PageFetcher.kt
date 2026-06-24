package eu.zeletrik.beanbook.ai.internal

/**
 * Seam over fetching a web page's HTML, so [eu.zeletrik.beanbook.ai.AiExtractionService] can be tested
 * against fixture HTML without real network I/O. Returns null on any fetch failure.
 */
fun interface PageFetcher {
    fun fetch(url: String): String?
}
