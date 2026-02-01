package es.cristcd.taskcompanion.redmine

import es.cristcd.taskcompanion.persistence.model.UserPreferences
import es.cristcd.taskcompanion.redmine.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object RedmineService {
    @OptIn(ExperimentalSerializationApi::class)
    var client: HttpClient? = configureClientFromDB()

    private fun configureClient(url: String, key: String): HttpClient {
        return HttpClient(CIO) {
            install(Logging) {
                level = LogLevel.ALL
            }
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    namingStrategy = JsonNamingStrategy.SnakeCase
                    serializersModule = SerializersModule {
                        polymorphic(CustomField::class) {
                            subclass(MultipleCustomField::class)
                            defaultDeserializer { SimpleCustomField.serializer() }
                        }
                    }
                })
            }
            install(HttpCache) {

            }
            expectSuccess = true
            defaultRequest {
                url(url)
                headers.appendIfNameAbsent("X-Redmine-API-Key", key)
            }
        }
    }

    private fun configureClientFromDB(): HttpClient? {
        val prefsDb = transaction {
            UserPreferences.selectAll().firstOrNull()
        }
        prefsDb?.let {
            val url = it[UserPreferences.redmineUrl]
            val key = it[UserPreferences.apiKey]

            if (!url.isNullOrBlank() && !key.isNullOrBlank()) {
                return configureClient(url, key)
            }
        }
        return null
    }

    suspend fun listIssues(versionId: Long): IssueList {
        return client!!.get("issues.json") {
            url {
                parameter("fixed_version_id", versionId)
                parameter("sort", "status")
                parameter("limit", 100) //TODO: paginacion
            }
        }.body()
    }

    suspend fun listIssuesAssignedToMe(): IssueList {
        return client!!.get("issues.json") {
            url {
                parameter("limit", 50)
                parameter("assigned_to_id", "me")
                parameter("sort", "status,updated_on:desc")
            }
        }.body()
    }

    suspend fun listIssuesByQuery(queryId: Long, projectId: Long?): IssueList {
        return client!!.get("issues.json") {
            parameter("query_id", queryId)
            if (projectId != null) {
                parameter("project_id", projectId)
            }
            parameter("limit", 50)
        }.body()
    }

    suspend fun listIssuesByProject(projectId: Long): IssueList {
        return client!!.get("issues.json") {
            parameter("project_id", projectId)
            parameter("sort", "updated_on:desc")
            parameter("limit", 50)
        }.body()
    }

    suspend fun listMonitoredIssues(): IssueList {
        return client!!.get("issues.json") {
            url {
                parameter("limit", 50)
                parameter("watcher_id", "me")
                parameter("sort", "updated_on:desc")
            }
        }.body()
    }

    suspend fun getIssue(id: Long): ExtendedIssue {
        return client!!.get("issues/$id.json") {
            url {
                parameter("include", "journals,watchers,allowed_statuses")
            }
        }.body<ExtendedIssueRoot>().issue
    }

    suspend fun getLoggedUser(): User {
        return client!!.get("my/account.json").body<UserRoot>().user
    }

    suspend fun watchIssue(id: Long, userId: Long) {
        client!!.post("issues/$id/watchers.json") {
            parameter("user_id", userId)
        }
    }

    suspend fun stopWatchingIssue(id: Long, userId: Long) {
        client!!.delete("issues/$id/watchers/$userId.json")
    }

    suspend fun getVersion(id: Long): Version {
        return client!!.get("versions/$id.json")
            .body<VersionRoot>().version
    }

    suspend fun listOpenProjectVersions(projectId: Long): List<Version> {
        return client!!.get("projects/$projectId/versions.json")
            .body<VersionList>().versions.filter { it.status == "open" }
    }

    suspend fun listProjectVersions(projectId: Long): List<Version> {
        return client!!.get("projects/$projectId/versions.json")
            .body<VersionList>().versions
    }

    suspend fun updateCredentials(url: String, apiKey: String): User {
        val newClient = configureClient(url, apiKey)
        val user = newClient.get("my/account.json").body<UserRoot>().user
        client = newClient
        return user
    }

    suspend fun getProject(id: Long): Project {
        return client!!.get("projects/$id.json") {
            parameter("include", "trackers,issue_categories,enabled_modules,time_entry_activities,issue_custom_fields")
        }.body<ProjectRoot>().project
    }

    suspend fun listProjects(): List<Project> {
        return client!!.get("projects.json").body<ProjectList>().projects
    }

    suspend fun updateIssueAttribute(issueId: Long, form: IssueForm) {
        client!!.put("issues/$issueId.json") {
            contentType(ContentType.Application.Json)
            setBody(IssueFormRoot(form))
        }
    }

    suspend fun listStatus(): List<IssueStatus> {
        return client!!.get("issue_statuses.json").body<IssueStatusList>().issueStatuses
    }

    suspend fun listAllQueries(): List<Query> {
        val result = mutableListOf<Query>()
        val limit = 100
        var offset = 0
        do {
            val items = client!!.get("queries.json") {
                parameter("limit", limit)
                parameter("offset", offset)
            }.body<QueriesRoot>()

            result.addAll(items.queries)
            offset += items.queries.size

        } while (items.queries.isNotEmpty())

        return result
    }

    fun clearCredentials() {
        client = null
    }
}