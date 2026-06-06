package es.cristcd.taskcompanion.persistence.model

import org.jetbrains.exposed.v1.core.Table

object IssueTag : Table() {
    val issue = reference("issue", Issue)
    val tag = reference("tag", Tag)
    val blocked = bool("blocked").default(false)
        .comment("Prevents this tag from being shown when extracted from the title")
}