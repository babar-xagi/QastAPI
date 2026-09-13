package io.qastapi.core

/**
 * Standard runtime environment profiles for QastAPI applications.
 */
enum class Environment(val identifier: String) {
    DEVELOPMENT("development"),
    TEST("test"),
    STAGING("staging"),
    PRODUCTION("production");

    fun isDev(): Boolean = this == DEVELOPMENT
    fun isProd(): Boolean = this == PRODUCTION
    fun isTest(): Boolean = this == TEST
    fun isStaging(): Boolean = this == STAGING

    companion object {
        @Volatile
        private var overrideProfile: String? = null

        /**
         * Explicitly set the active environment profile (useful for testing or programmatic bootstrap).
         */
        fun setProfile(profile: String?) {
            overrideProfile = profile
        }

        /**
         * Returns the active profile name as a string, checking override, environment variables,
         * system properties, and falling back to "development".
         */
        fun currentProfile(): String {
            return overrideProfile
                ?: System.getenv("QAST_PROFILE")
                ?: System.getenv("QAST_ENV")
                ?: System.getenv("ENV")
                ?: System.getProperty("qast.profile")
                ?: System.getProperty("qast.env")
                ?: "development"
        }

        /**
         * Resolves the current runtime [Environment] based on [currentProfile].
         */
        fun current(): Environment {
            return fromString(currentProfile())
        }

        /**
         * Parses an environment string into a standard [Environment] value.
         */
        fun fromString(value: String): Environment {
            return when (value.trim().lowercase()) {
                "prod", "production" -> PRODUCTION
                "test", "testing" -> TEST
                "stage", "staging" -> STAGING
                else -> DEVELOPMENT
            }
        }
    }
}
