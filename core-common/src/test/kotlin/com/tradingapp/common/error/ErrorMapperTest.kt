package com.tradingapp.common.error

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ErrorMapperTest {

    @Test
    fun `UnknownHostException maps to no-internet message`() {
        val message = ErrorMapper.toUserMessage(UnknownHostException("host not found"))
        assertEquals("No internet connection. Showing cached data.", message)
    }

    @Test
    fun `SocketTimeoutException maps to timeout message`() {
        val message = ErrorMapper.toUserMessage(SocketTimeoutException("timeout"))
        assertEquals("Request timed out. Please retry.", message)
    }

    @Test
    fun `IOException maps to connection error message`() {
        val message = ErrorMapper.toUserMessage(IOException("connection reset"))
        assertEquals("Connection error. Please retry.", message)
    }

    @Test
    fun `RuntimeException with message uses localizedMessage`() {
        val message = ErrorMapper.toUserMessage(RuntimeException("Something broke"))
        assertEquals("Something broke", message)
    }

    @Test
    fun `RuntimeException with blank message falls back to generic`() {
        val message = ErrorMapper.toUserMessage(RuntimeException(""))
        assertEquals("An unexpected error occurred.", message)
    }

    @Test
    fun `output is never empty for any throwable`() {
        val result = ErrorMapper.toUserMessage(Throwable())
        assert(result.isNotBlank()) { "ErrorMapper must never return a blank message" }
    }
}
