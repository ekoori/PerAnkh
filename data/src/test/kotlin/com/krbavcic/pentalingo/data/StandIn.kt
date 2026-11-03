package com.krbavcic.pentalingo.data

import java.io.Closeable
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * A stand-in for the report server: a socket that answers every request with [status] and
 * [reply], and keeps what it was sent. (Android's unit-test classpath has no JDK HTTP server.)
 * The same as BushLines' stand-in for the server the two apps share.
 */
class StandIn : Closeable {

    class Request(val line: String, val headers: Map<String, String>, val body: ByteArray)

    @Volatile var status = 200
    @Volatile var reply = ""

    private val socket = ServerSocket(0)
    private val requests = mutableListOf<Request>()

    /** What it was sent, oldest first. Header names are lower case. */
    val received: List<Request> get() = synchronized(requests) { requests.toList() }

    fun url(path: String) = "http://127.0.0.1:${socket.localPort}$path"

    init {
        thread(isDaemon = true) {
            while (!socket.isClosed) {
                val client = runCatching { socket.accept() }.getOrNull() ?: break
                client.use(::answer)
            }
        }
    }

    override fun close() = socket.close()

    private fun answer(client: Socket) {
        val input = client.getInputStream().buffered()
        val line = readLine(input)
        val headers = generateSequence { readLine(input).takeIf { it.isNotEmpty() } }
            .associate { it.substringBefore(':').trim().lowercase() to it.substringAfter(':').trim() }
        val body = ByteArray(headers["content-length"]?.toInt() ?: 0) { input.read().toByte() }
        synchronized(requests) { requests += Request(line, headers, body) }
        val out = reply.toByteArray()
        client.getOutputStream().apply {
            write("HTTP/1.1 $status X\r\nContent-Type: application/json\r\nContent-Length: ${out.size}\r\nConnection: close\r\n\r\n".toByteArray())
            write(out)
            flush()
        }
    }

    private fun readLine(input: InputStream): String =
        generateSequence { input.read().takeIf { it >= 0 } }
            .takeWhile { it != '\n'.code }.map { it.toChar() }.joinToString("").trimEnd('\r')
}
