package io.qastapi.core

open class QastException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

open class HttpException(
    val statusCode: Int,
    override val message: String,
    val details: Any? = null,
    cause: Throwable? = null
) : QastException(message, cause)

class BadRequestException(message: String = "Bad Request", details: Any? = null, cause: Throwable? = null) :
    HttpException(400, message, details, cause)

class UnauthorizedException(message: String = "Unauthorized", details: Any? = null, cause: Throwable? = null) :
    HttpException(401, message, details, cause)

class ForbiddenException(message: String = "Forbidden", details: Any? = null, cause: Throwable? = null) :
    HttpException(403, message, details, cause)

class NotFoundException(message: String = "Not Found", details: Any? = null, cause: Throwable? = null) :
    HttpException(404, message, details, cause)

class MethodNotAllowedException(message: String = "Method Not Allowed", details: Any? = null, cause: Throwable? = null) :
    HttpException(405, message, details, cause)

class ConflictException(message: String = "Conflict", details: Any? = null, cause: Throwable? = null) :
    HttpException(409, message, details, cause)

class UnprocessableEntityException(message: String = "Unprocessable Entity", details: Any? = null, cause: Throwable? = null) :
    HttpException(422, message, details, cause)

class InternalServerErrorException(message: String = "Internal Server Error", details: Any? = null, cause: Throwable? = null) :
    HttpException(500, message, details, cause)
