package es.cristcd.taskcompanion.filter

import es.cristcd.taskcompanion.filter.dto.ColumnSelectionDto
import es.cristcd.taskcompanion.filter.dto.toColumnSelectionDto
import es.cristcd.taskcompanion.filter.form.ColumnSelectionForm
import es.cristcd.taskcompanion.persistence.model.VisibleTableColumns
import es.cristcd.taskcompanion.redmine.model.MultipleCustomField
import es.cristcd.taskcompanion.redmine.model.Project
import es.cristcd.taskcompanion.redmine.model.SimpleCustomField
import es.cristcd.taskcompanion.util.toDefaultFormatString
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert

object VisibleColumnsService {
    fun listColumnPreferences() : List<ColumnSelectionDto> {
        return transaction {
            VisibleTableColumns.selectAll()
                .orderBy(
                    VisibleTableColumns.index to SortOrder.ASC,
                    VisibleTableColumns.label to SortOrder.ASC
                ).map { it.toColumnSelectionDto() }
        }
    }

    fun updatePreferences(form: List<ColumnSelectionForm>) {
        transaction {
            val selectedColumns = form.filter { it.selectedIndex != null }
            VisibleTableColumns.batchUpsert(selectedColumns, VisibleTableColumns.label) { selectionForm ->
                this[VisibleTableColumns.index] = selectionForm.selectedIndex!!
                this[VisibleTableColumns.label] = selectionForm.label
            }
            val deletedColumns = form.filter { it.selectedIndex == null }.map { it.label }
            VisibleTableColumns.deleteWhere { VisibleTableColumns.label inList deletedColumns }
        }
    }

    fun loadVisibleColumns(project: Project?) : List<ColumnDefinition> {
        val projectFields = project?.issueCustomFields?.mapNotNull { it.name } ?: emptyList()
        val projectColumns = projectFields.map { field ->
            ColumnDefinition(field) {
                val fieldValue = it.customFields.firstOrNull { f -> f.name == field }
                when (fieldValue) {
                    is SimpleCustomField -> fieldValue.value ?: ""
                    is MultipleCustomField -> fieldValue.value?.joinToString(", ") ?: ""
                    null -> ""
                }
            }
        }
        val defaultColumns = listOf(
            ColumnDefinition( "Asignado a", -50) { it.assignedTo?.name ?: "" },
            ColumnDefinition( "Actualizado", -40) { it.updatedOn?.toDefaultFormatString() ?: ""},
            ColumnDefinition("Proyecto", null) { it.project.name ?: "" },
            ColumnDefinition("Tipo", null) { it.tracker.name ?: "" },
            ColumnDefinition("Autor", null) { it.author.name ?: "" },
            ColumnDefinition("Fecha de inicio", null) { it.startDate.toString() },
            ColumnDefinition("Fecha fin", null) { it.dueDate.toString() },
            ColumnDefinition("% Realizado", null) { it.doneRatio.toString() },
            ColumnDefinition("Tiempo estimado", null) { it.estimatedHours.toString() },
            ColumnDefinition("Creado", null) { it.createdOn?.toDefaultFormatString() ?: "" },
            ColumnDefinition("Cerrada", null) { it.closedOn?.toDefaultFormatString() ?: "" },
            ColumnDefinition("Versión prevista", null) { it.fixedVersion?.name ?: "" },
            ColumnDefinition("Categoría", null) { it.category?.name ?: "" },
        )
        val userPreferences = listColumnPreferences()

        return (defaultColumns + projectColumns).map { columnDefinition ->
            val preferenceSet = userPreferences.find { it.label == columnDefinition.label }
            if (preferenceSet != null) {
                columnDefinition.copy(visibleIndex = preferenceSet.index)
            } else {
                columnDefinition
            }
        }
    }

}