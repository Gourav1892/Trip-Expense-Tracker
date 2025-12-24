package com.example.tripexpensetracker.ui.expenses

object CategorySuggester {
    private val foodKeywords = setOf("lunch", "dinner", "breakfast", "coffee", "cafe", "restaurant", "market", "groceries", "meal", "drink", "bar", "snack")
    private val transportKeywords = setOf("taxi", "uber", "lyft", "bus", "train", "flight", "gas", "fuel", "parking", "subway", "metro", "ticket")
    private val lodgingKeywords = setOf("hotel", "airbnb", "hostel", "room", "stay", "resort")
    private val entertainmentKeywords = setOf("movie", "cinema", "museum", "ticket", "tour", "park", "show", "concert", "game")

    fun suggestCategory(title: String): String? {
        val lowerTitle = title.lowercase()
        return when {
            foodKeywords.any { lowerTitle.contains(it) } -> "Food"
            transportKeywords.any { lowerTitle.contains(it) } -> "Transport"
            lodgingKeywords.any { lowerTitle.contains(it) } -> "Lodging"
            entertainmentKeywords.any { lowerTitle.contains(it) } -> "Entertainment"
            else -> null
        }
    }
}
