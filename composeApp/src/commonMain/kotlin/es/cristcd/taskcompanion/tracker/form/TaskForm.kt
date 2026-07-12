package es.cristcd.taskcompanion.tracker.form

data class TaskForm(val categoryId: Int, val code: String, val description: String, val redmineId: Long? = null) {
}