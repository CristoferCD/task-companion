package es.cristcd.taskcompanion.persistence.model

import org.jetbrains.exposed.v1.core.Table

object IssueTag : Table() {
    val issue = reference("issue", Issue)
    val tag = reference("tag", Tag)
}