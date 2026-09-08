package io.ktor.client.plugins.logging

interface Logger {
    fun log(message: String)
}
