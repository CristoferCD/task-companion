package es.cristcd.taskcompanion.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.cristcd.taskcompanion.issue.IssueService
import es.cristcd.taskcompanion.issue.dto.TagDto
import es.cristcd.taskcompanion.issue.form.NewTagForm
import es.cristcd.taskcompanion.persistence.model.Category
import es.cristcd.taskcompanion.persistence.model.CategoryType
import es.cristcd.taskcompanion.persistence.model.Status
import es.cristcd.taskcompanion.persistence.model.UserPreferences
import es.cristcd.taskcompanion.redmine.RedmineService
import es.cristcd.taskcompanion.redmine.model.IssueStatus
import es.cristcd.taskcompanion.redmine.model.Project
import es.cristcd.taskcompanion.redmine.model.User
import es.cristcd.taskcompanion.tracker.TrackerService
import es.cristcd.taskcompanion.tracker.dto.CategoryDto
import es.cristcd.taskcompanion.tracker.dto.StatusDto
import es.cristcd.taskcompanion.tracker.dto.TaskDto
import es.cristcd.taskcompanion.tracker.form.CategoryForm
import es.cristcd.taskcompanion.tracker.form.StatusForm
import es.cristcd.taskcompanion.ui.screen.dashboard.DashboardGroup
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.core.dao.id.EntityIDFunctionProvider
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class SettingsViewmodel : ViewModel() {
    private val log = KotlinLogging.logger {}

    private val _redmineUser = MutableStateFlow<RedmineUserResult>(RedmineUserResult.Loading)
    val redmineUser = _redmineUser.asStateFlow()

    private val _categories = MutableStateFlow(emptyList<CategoryDto>())
    val categories = _categories.asStateFlow()

    private val _projects = MutableStateFlow(emptyList<Project>())
    val projects = _projects.asStateFlow()

    private val _statuses = MutableStateFlow(emptyList<StatusDto>())
    val statuses = _statuses.asStateFlow()

    private val _redmineStatuses = MutableStateFlow(emptyList<IssueStatus>())
    val redmineStatuses = _redmineStatuses.asStateFlow()

    val tags: StateFlow<List<TagDto>>
        field = MutableStateFlow(emptyList())

    fun loadRedmineInfo() {
        viewModelScope.launch {
            //TODO: handle unauthorized
            try {
                val user = RedmineService.getLoggedUser()
                _redmineUser.emit(RedmineUserResult.Ok(user))
            } catch (e: Exception) {
                _redmineUser.emit(RedmineUserResult.NotLoggedIn)
            }
        }
    }

    fun saveRedmineSettings(url: String, apiKey: String) {
        viewModelScope.launch {
            _redmineUser.emit(RedmineUserResult.Loading)
            try {
                val user = RedmineService.updateCredentials(url, apiKey)
                transaction {
                    val prefsDB = UserPreferences.selectAll().firstOrNull()
                    if (prefsDB != null) {
                        UserPreferences.update({ UserPreferences.id eq prefsDB[UserPreferences.id]}) {
                            it[UserPreferences.redmineId] = user.id
                            it[UserPreferences.redmineUrl] = url
                            it[UserPreferences.apiKey] = apiKey
                        }
                    } else {
                        UserPreferences.insert {
                            it[UserPreferences.redmineId] = user.id
                            it[UserPreferences.redmineUrl] = url
                            it[UserPreferences.apiKey] = apiKey
                        }
                    }
                }
                _redmineUser.emit(RedmineUserResult.Ok(user))
            } catch (e: Exception) {
                log.error(e) { "Failed to save redmine settings" }
                _redmineUser.emit(RedmineUserResult.NotLoggedIn)
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
            _redmineUser.emit(RedmineUserResult.NotLoggedIn)
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            _categories.emit(TrackerService.listCategories())
        }
    }

    fun loadProjects() {
        viewModelScope.launch {
            _projects.emit(RedmineService.listProjects())
        }
    }

    fun loadStatuses() {
        viewModelScope.launch {
            _statuses.emit(TrackerService.listStatuses())
        }
    }

    fun loadRedmineStatuses() {
        viewModelScope.launch {
            _redmineStatuses.emit(RedmineService.listStatus())
        }
    }

    fun loadTags() {
        viewModelScope.launch {
            tags.emit(IssueService.listTags())
        }
    }

    fun saveCategory(category: CategoryForm) {
        viewModelScope.launch {
            transaction {
                //TODO: upsert
                Category.upsert {
                    category.id?.let {categoryId -> it[id] = EntityIDFunctionProvider.createEntityID(categoryId, Category) }
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
        }
    }

    fun deleteStatus(statusId: Int) {
        viewModelScope.launch {
            transaction {
                Status.deleteWhere { Status.id eq statusId }
            }
            loadStatuses()
        }
    }

    //TODO: invalidate SettingsCache
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
        }
    }

    fun createTag(form: NewTagForm) {
        viewModelScope.launch {
            IssueService.createTag(form)
            tags.emit(IssueService.listTags())
        }
    }

    fun deleteTag(tagId: Int) {
        viewModelScope.launch {
            IssueService.deleteTag(tagId)
            tags.emit(IssueService.listTags())
        }
    }

}


sealed interface RedmineUserResult {
    data object Loading: RedmineUserResult
    data class Ok(val user: User): RedmineUserResult
    data object NotLoggedIn: RedmineUserResult
}