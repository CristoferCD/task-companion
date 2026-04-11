package es.cristcd.taskcompanion.persistence.model

import es.cristcd.taskcompanion.redmine.model.CustomField
import es.cristcd.taskcompanion.redmine.model.ExtendedIssue
import es.cristcd.taskcompanion.redmine.model.MultipleCustomField
import es.cristcd.taskcompanion.redmine.model.SimpleCustomField
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.json
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object RedmineIssue : IntIdTable() {
    val data = json<ExtendedIssue>("data", Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        serializersModule = SerializersModule {
            polymorphic(CustomField::class) {
                subclass(MultipleCustomField::class)
                defaultDeserializer { SimpleCustomField.serializer() }
            }
        }
    }).nullable()
    val updatedAt = timestamp("updated_at")
}