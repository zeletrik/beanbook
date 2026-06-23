package eu.zeletrik.beanbook.ui

/**
 * Captures [NotificationHelper] output for assertions. Call [install] in @BeforeEach and [reset] in
 * @AfterEach so the recorder doesn't leak into unrelated tests.
 */
internal object RecordedNotifications {
    val shown = mutableListOf<Pair<String, Boolean>>()

    fun install() {
        shown.clear()
        NotificationHelper.recorder = { text, isError -> shown.add(text to isError) }
    }

    fun reset() {
        NotificationHelper.recorder = null
        shown.clear()
    }
}
