package io.qastapi.routing

import io.qastapi.http.HttpMethod
import kotlin.math.min

/**
 * Intelligent route suggester for developer experience on 404 Not Found responses.
 * Calculates edit distances and path segment similarities to provide "Did you mean?" suggestions.
 */
object RouteSuggester {

    fun suggestRoutes(
        method: HttpMethod,
        path: String,
        allRoutes: List<Route>,
        maxSuggestions: Int = 3
    ): List<String> {
        if (allRoutes.isEmpty()) return emptyList()

        val normalizedPath = path.trim().removeSuffix("/").ifEmpty { "/" }

        val scored = allRoutes.map { route ->
            val routePath = route.pattern.rawPattern.trim().removeSuffix("/").ifEmpty { "/" }
            val distance = levenshteinDistance(normalizedPath.lowercase(), routePath.lowercase())
            val methodPenalty = if (route.method == method) 0 else 2
            val totalScore = distance + methodPenalty
            Triple(route, totalScore, distance)
        }
            .filter { (_, _, distance) ->
                // Only suggest if distance is reasonably close
                distance <= 5 || distance <= (normalizedPath.length / 2).coerceAtLeast(3)
            }
            .sortedBy { it.second }
            .take(maxSuggestions)

        return scored.map { (route, _, _) ->
            "${route.method.value} ${route.pattern.rawPattern}"
        }
    }

    fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}
