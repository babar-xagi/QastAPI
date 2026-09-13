package io.qastapi.core

enum class Environment(val identifier: String) {
    DEVELOPMENT("development"),
    TEST("test"),
    STAGING("staging"),
    PRODUCTION("production");

    fun isDev(): Boolean = this == DEVELOPMENT
    fun isProd(): Boolean = this == PRODUCTION
    fun isTest(): Boolean = this == TEST

    companion object {
        fun current(): Environment {
            val env = System.getenv("QAST_ENV")
                ?: System.getenv("ENV")
                ?: System.getProperty("qast.env")
                ?: "development"
            return fromString(env)
        }

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
