import exceptions.NameAlreadyTaken
import exceptions.UniqueNameNotFound
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
import java.io.FileNotFoundException
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

        val response = client.get(API) {
            url {
                parameter("uniqueName", uniqueName)
            }
        }

        if (response.status == HttpStatusCode.NotFound) {
            throw UniqueNameNotFound()
        }

        return response.bodyAsText()
    }

    private suspend fun postFile(path: String, uniqueName: String) {
        val file = File(path)
        val fileName = file.name

        if (file.isFile) {
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
            }
        } else {
            throw FileNotFoundException()
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
        val response = client.patch(API) {
            url {
                parameter("oldName", oldName)
                parameter("newName", newName)
            }
        }

        when(response.status) {
            HttpStatusCode.NotFound -> throw UniqueNameNotFound()
            HttpStatusCode.Conflict -> throw NameAlreadyTaken()
        }
    }

    suspend fun getPaths(uniqueNames: List<String>): List<Pair<String, String>> {
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

    suspend fun createBackup() {
        val paths = getAllFiles()
        paths.forEach {
            try {
                postFile(it.first, it.second)
            } catch (_: Exception) {

            }
        }
    }

    suspend fun restoreFile(uniqueName: String, preferredPath: String? = null) {

        client.prepareGet("$API/restore") {
            url {
                parameter("uniqueName", uniqueName)
            }
        }.execute {

            val pathFromServer = it.headers.flattenEntries().toMap()["path"]!! // TODO: 16.11.2022
            println(pathFromServer)

            val folders = File(preferredPath ?: pathFromServer.substringBeforeLast('\\'))
            folders.mkdirs()

            val file = File(folders, pathFromServer.substringAfterLast('\\'))
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