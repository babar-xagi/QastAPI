package io.qastapi.routing

import io.qastapi.http.HttpMethod

/**
 * Marks a method as handling HTTP GET requests for the given path pattern.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Get(val path: String = "/")

/**
 * Marks a method as handling HTTP POST requests for the given path pattern.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Post(val path: String = "/")

/**
 * Marks a method as handling HTTP PUT requests for the given path pattern.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Put(val path: String = "/")

/**
 * Marks a method as handling HTTP PATCH requests for the given path pattern.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Patch(val path: String = "/")

/**
 * Marks a method as handling HTTP DELETE requests for the given path pattern.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Delete(val path: String = "/")

/**
 * Marks a method as handling HTTP HEAD requests for the given path pattern.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Head(val path: String = "/")

/**
 * Marks a method as handling HTTP OPTIONS requests for the given path pattern.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Options(val path: String = "/")

/**
 * Marks a method as handling HTTP requests for the given HTTP method and path pattern.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class HttpRoute(val method: String = "GET", val path: String = "/")

