package es.cristcd.taskcompanion.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.cristcd.taskcompanion.issue.IssueService
import es.cristcd.taskcompanion.issue.dto.TagInfoDto
import es.cristcd.taskcompanion.issue.form.NewTagForm
import es.cristcd.taskcompanion.persistence.model.Category
import es.cristcd.taskcompanion.persistence.model.CategoryType
import es.cristcd.taskcompanion.persistence.model.Status
import es.cristcd.taskcompanion.persistence.model.UserPreferences
import es.cristcd.taskcompanion.redmine.RedmineService
import es.cristcd.taskcompanion.redmine.model.IssueStatus
import es.cristcd.taskcompanion.redmine.model.Project
import es.cristcd.taskcompanion.redmine.model.User
import es.cristcd.taskcompanion.tracker.SettingsCache
import es.cristcd.taskcompanion.tracker.TrackerService
import es.cristcd.taskcompanion.tracker.dto.CategoryDto
import es.cristcd.taskcompanion.tracker.dto.StatusDto
import es.cristcd.taskcompanion.tracker.form.CategoryForm
import es.cristcd.taskcompanion.tracker.form.StatusForm
import es.cristcd.taskcompanion.updater.GithubRelease
import es.cristcd.taskcompanion.updater.UpdaterService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.core.dao.id.EntityIDFunctionProvider
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.awt.Desktop
import java.net.URI

class SettingsViewmodel : ViewModel() {
    private val log = KotlinLogging.logger {}

    val redmineUser: StateFlow<RedmineUserResult>
        field = MutableStateFlow<RedmineUserResult>(RedmineUserResult.Loading)

    val categories: StateFlow<List<CategoryDto>>
        field = MutableStateFlow(emptyList<CategoryDto>())

    val projects: StateFlow<List<Project>>
        field = MutableStateFlow(emptyList<Project>())

    val statuses: StateFlow<List<StatusDto>>
        field = MutableStateFlow(emptyList<StatusDto>())

    val redmineStatuses: StateFlow<List<IssueStatus>>
        field = MutableStateFlow(emptyList<IssueStatus>())

    val tags: StateFlow<List<TagInfoDto>>
        field = MutableStateFlow(emptyList())

    val appVersion: StateFlow<AppVersionResult>
        field = MutableStateFlow<AppVersionResult>(AppVersionResult.Loading)

    val scale: StateFlow<ScaleSettings>
        field = MutableStateFlow(ScaleSettings(0, 0))

    fun loadRedmineInfo() {
        viewModelScope.launch {
            //TODO: handle unauthorized
            try {
                val user = RedmineService.getLoggedUser()
                redmineUser.emit(RedmineUserResult.Ok(user))
            } catch (e: Exception) {
                redmineUser.emit(RedmineUserResult.NotLoggedIn)
            }
        }
    }

    fun saveRedmineSettings(url: String, apiKey: String) {
        viewModelScope.launch {
            redmineUser.emit(RedmineUserResult.Loading)
            try {
                val correctUrl = if (!url.endsWith('/')) "$url/" else url
                val user = RedmineService.updateCredentials(correctUrl, apiKey)
                transaction {
                    val prefsDB = UserPreferences.selectAll().firstOrNull()
                    UserPreferences.upsert {
                        prefsDB?.getOrNull(UserPreferences.id)?.let { id -> it[UserPreferences.id] = id }
                        it[UserPreferences.redmineId] = user.id
                        it[UserPreferences.redmineUrl] = correctUrl
                        it[UserPreferences.apiKey] = apiKey
                    }
                }
                importStatusFromRedmineAfterFirstLogin()
                redmineUser.emit(RedmineUserResult.Ok(user))
            } catch (e: Exception) {
                log.error(e) { "Failed to save redmine settings" }
                redmineUser.emit(RedmineUserResult.NotLoggedIn)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            transaction {
                UserPreferences.update() {
                    it[UserPreferences.redmineId] = null
                    it[UserPreferences.redmineUrl] = null
                    it[UserPreferences.apiKey] = null
                }
            }
            RedmineService.clearCredentials()
            redmineUser.emit(RedmineUserResult.NotLoggedIn)
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            categories.emit(TrackerService.listCategories())
        }
    }

    fun loadProjects() {
        viewModelScope.launch {
            projects.emit(RedmineService.listProjects())
        }
    }

    fun loadStatuses() {
        viewModelScope.launch {
            statuses.emit(TrackerService.listStatuses())
        }
    }

    fun loadRedmineStatuses() {
        viewModelScope.launch {
            redmineStatuses.emit(RedmineService.listStatus())
        }
    }

    fun loadTags() {
        viewModelScope.launch {
            tags.emit(IssueService.listTagsIncludingDeleted())
        }
    }

    fun loadAppVersion() {
        viewModelScope.launch {
            val currentVersion = UpdaterService.currentVersion()
            if (currentVersion == null) {
                appVersion.emit(AppVersionResult.NoInfo("N/A"))
                return@launch
            }
            val githubRelease = UpdaterService.latestGithubRelease()
            if (githubRelease == null) {
                appVersion.emit(AppVersionResult.NoInfo(currentVersion))
                return@launch
            }

            if (UpdaterService.isUpdateAvailable(githubRelease)) {
                appVersion.emit(AppVersionResult.UpdateAvailable(currentVersion, githubRelease))
            } else {
                appVersion.emit(AppVersionResult.LatestInstalled(githubRelease))
            }
        }
    }

    fun loadPreferences() {
        viewModelScope.launch {
            transaction {
                UserPreferences.select(UserPreferences.scalePercent, UserPreferences.fontScalePercent)
                    .firstOrNull()?.let {
                        ScaleSettings(
                            it[UserPreferences.scalePercent] ?: 100,
                            it[UserPreferences.fontScalePercent] ?: 100
                        )
                    }
            }?.let { scale.emit(it) }
        }
    }

    fun downloadNewVersion(release: GithubRelease) {
        Desktop.getDesktop().browse(URI(release.htmlUrl))
    }

    fun saveCategory(category: CategoryForm) {
        viewModelScope.launch {
            transaction {
                //TODO: upsert
                Category.upsert {
                    category.id?.let { categoryId ->
                        it[id] = EntityIDFunctionProvider.createEntityID(categoryId, Category)
                    }
                    it[name] = category.name
                    it[type] = if (category.work) CategoryType.WORK else CategoryType.REST
                    it[color] = category.color
                    it[redmineProjectId] = category.redmineId
                }
            }
            loadCategories()
        }
    }

    fun deleteCategory(categoryId: Int) {
        viewModelScope.launch {
            transaction {
                Category.deleteWhere { Category.id eq categoryId }
            }
            loadCategories()
        }
    }

    fun saveStatus(status: StatusForm) {
        viewModelScope.launch {
            transaction {
                Status.upsert {
                    status.id?.let { statusId -> it[id] = EntityIDFunctionProvider.createEntityID(statusId, Status) }
                    it[name] = status.name
                    it[color] = status.color
                    it[redmineStatusId] = status.redmineId
                }
            }
            loadStatuses()
            SettingsCache.invalidateStatusColorCache()
        }
    }

    fun deleteStatus(statusId: Int) {
        viewModelScope.launch {
            transaction {
                Status.deleteWhere { Status.id eq statusId }
            }
            loadStatuses()
            SettingsCache.invalidateStatusColorCache()
        }
    }

    private fun importStatusFromRedmineAfterFirstLogin() {
        val anyStatusSaved = transaction {
            Status.select(Status.id)
                .limit(1)
                .any()
        }

        if (!anyStatusSaved) {
            importStatusFromRedmine()
        }
    }

    fun importStatusFromRedmine() {
        val palette = listOf(
            0xffffb7ff,
            0xff4d908e,
            0xff90be6d,
            0xfff9c74f,
            0xff6a4c93,
            0xfff94144,
            0xffbde0fe,
            0xffef476f,
            0xffebd2b4,
        )
        viewModelScope.launch {
            RedmineService.listStatus().forEachIndexed { idx, redmineStatus ->
                transaction {
                    Status.insert {
                        it[name] = redmineStatus.name
                        it[color] = palette[idx % palette.size]
                        it[redmineStatusId] = redmineStatus.id
                    }
                }
            }
            loadStatuses()
            SettingsCache.invalidateStatusColorCache()
        }
    }

    fun createTag(form: NewTagForm) {
        viewModelScope.launch {
            IssueService.createTag(form)
            tags.emit(IssueService.listTagsIncludingDeleted())
        }
    }

    fun restoreTag(tagId: Int) {
        viewModelScope.launch {
            IssueService.restoreTag(tagId)
            tags.emit(IssueService.listTagsIncludingDeleted())
        }
    }

    fun deleteTag(tagId: Int) {
        viewModelScope.launch {
            IssueService.deleteTag(tagId)
            tags.emit(IssueService.listTagsIncludingDeleted())
        }
    }

    fun updateScale(uiScale: Long, fontScale: Long) {
        viewModelScope.launch {
            transaction {
                val prefsDB = UserPreferences.selectAll().firstOrNull()
                UserPreferences.upsert {
                    prefsDB?.getOrNull(UserPreferences.id)?.let { id -> it[UserPreferences.id] = id }
                    it[UserPreferences.scalePercent] = uiScale
                    it[UserPreferences.fontScalePercent] = fontScale
                }
            }
        }
    }

}


sealed interface RedmineUserResult {
    data object Loading : RedmineUserResult
    data class Ok(val user: User) : RedmineUserResult
    data object NotLoggedIn : RedmineUserResult
}

sealed interface AppVersionResult {
    data object Loading: AppVersionResult
    data class LatestInstalled(val latestRelease: GithubRelease) : AppVersionResult
    data class UpdateAvailable(val currentVersion: String, val latestRelease: GithubRelease) : AppVersionResult
    data class NoInfo(val currentVersion: String): AppVersionResult
}

data class ScaleSettings(val uiScale: Long, val fontScale: Long)