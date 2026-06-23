package eu.zeletrik.beanbook.ui

/**
 * Bottom-navigation destinations, in display order. Use [MainView.navigateTo] with these
 * instead of raw tab indices so a nav reorder can't silently mis-route a caller.
 */
enum class AppTab { PURCHASES, ANALYTICS, ADD, WISHLIST, SETTINGS }
