import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

object BackupService {
    private const val API = "http://127.0.0.1:8082/api/v1"
    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 300000
        }
        install(ContentNegotiation) {
            json()
        }
    }

    suspend fun addNewFile(path: String): String {
        val uniqueName = UUID.randomUUID().toString()
        postFile(path, uniqueName)
        return uniqueName
    }

    suspend fun getPath(uniqueName: String): String {
        println("getPath start")
        val response = client.get(API) {
            url {
                parameter("uniqueName", uniqueName)
            }
        }
        println("getPath end")
        return response.bodyAsText()
    }

    private suspend fun postFile(path: String, uniqueName: String) {
        println("postFile start")
        val file = File(path)
        val fileName = file.name

        client.post(API) {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("file", ChannelProvider(file.length()) {
                            file.readChannel()
                        }, Headers.build {
                            append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                            append("uniqueName", uniqueName)
                            append("path", file.absolutePath)
                        })
                    },
                    boundary = "WebAppBoundary"
                )
            )
//                onUpload { bytesSentTotal, contentLength ->
//                    println("Sent $bytesSentTotal bytes from $contentLength")
//                }
            println("postFile end")
        }
    }

    suspend fun deleteFile(uniqueName: String) {
        client.delete(API) {
            url {
                parameter("uniqueName", uniqueName)
            }
        }
    }

    suspend fun renameFile(oldName: String, newName: String) {
        client.patch(API) {
            url {
                parameter("oldName", oldName)
                parameter("newName", newName)
            }
        }
    }

    suspend fun getPaths(uniqueNames: List<String>): List<String> {
        return client.get("$API/list") {
            contentType(ContentType.Application.Json)
            setBody(uniqueNames)
        }.body()
    }

    private suspend fun getAllPaths(): List<String> {
        return client.get("$API/paths").body()
    }

    private suspend fun getAllFiles(): List<Pair<String, String>> {
        return client.get("$API/all").body()
    }

    // TODO: 16.11.2022 Сделать проверку по времени
    suspend fun createBackup() {
        val paths = getAllFiles()
        paths.forEach {
            postFile(it.first, it.second)
        }
    }

    suspend fun restoreFile(uniqueName: String, preferredPath: String? = null) {
        client.prepareGet("$API/restore") {
            url {
                parameter("uniqueName", uniqueName)
            }
        }.execute {

            val pathFromServer = it.headers.flattenEntries().toMap()["path"]!! // TODO: 16.11.2022
            val path = if (preferredPath == null) {
                pathFromServer
            } else {
                "$preferredPath/${pathFromServer.substringAfterLast('\\')}"
            }
            val file = File(path)
            file.delete()

            val channel: ByteReadChannel = it.body()
            while (!channel.isClosedForRead) {
                val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                while (!packet.isEmpty) {
                    val bytes = packet.readBytes()
                    file.appendBytes(bytes)
                }
            }
        }
    }

    suspend fun restoreFiles(path: String? = null) {
        withContext(Dispatchers.IO) {
            getAllFiles().forEach {
                launch {
                    restoreFile(it.second, path)
                }
            }
        }
    }
}