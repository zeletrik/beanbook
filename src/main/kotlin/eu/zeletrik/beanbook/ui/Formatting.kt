package eu.zeletrik.beanbook.ui

import eu.zeletrik.beanbook.beans.BeanPurchase
import java.math.BigDecimal
import java.math.RoundingMode

private const val ONE_STAR = 1
private const val TWO_STARS = 2
private const val THREE_STARS = 3
private const val FOUR_STARS = 4
private const val FIVE_STARS = 5

/** "MEDIUM" → "Medium". Single home for the enum-label formatting used across the UI. */
internal fun Enum<*>.displayName(): String = name.lowercase().replaceFirstChar(Char::uppercase)

/** Star rating as filled/empty stars; empty string for null or 0. */
internal fun Int?.toStars(): String = when (this) {
    ONE_STAR -> "★☆☆☆☆"; TWO_STARS -> "★★☆☆☆"; THREE_STARS -> "★★★☆☆"
    FOUR_STARS -> "★★★★☆"; FIVE_STARS -> "★★★★★"
    else -> ""
}

/** Formats a money amount with the given currency symbol, e.g. "€18.50". */
internal fun BigDecimal.formatPrice(currency: String = "€"): String =
    "$currency${this.setScale(2, RoundingMode.HALF_UP).toPlainString()}"

/** Origin with its optional second-level region, e.g. "Colombia" or "Colombia, Huila". Trims both so
 *  stray whitespace (from imports or typed input) doesn't show as "Colombia ,  Huila". */
internal fun BeanPurchase.originLabel(): String {
    val country = origin.trim()
    val sub = region?.trim()?.takeIf { it.isNotEmpty() }
    return if (sub == null) country else "$country, $sub"
}

/**
 * Normalises a user-entered link into a safe href: prepends `https://` when no scheme is present,
 * and rejects any non-http(s) scheme (e.g. `javascript:`, `mailto:`) so a stored value can't smuggle
 * an unsafe anchor target. Returns null for blank input.
 */
internal fun String?.toHref(): String? {
    val raw = this?.trim().orEmpty()
    if (raw.isBlank()) return null
    val lower = raw.lowercase()
    return when {
        lower.startsWith("http://") || lower.startsWith("https://") -> raw
        raw.contains("://") || lower.startsWith("javascript:") || lower.startsWith("mailto:") -> null
        else -> "https://$raw"
    }
}

/** Strips the scheme and any www. prefix for compact display of a link, e.g. "roaster.com/beans". */
internal fun String.toDisplayLink(): String =
    trim().removePrefix("https://").removePrefix("http://").removePrefix("www.").trimEnd('/')
