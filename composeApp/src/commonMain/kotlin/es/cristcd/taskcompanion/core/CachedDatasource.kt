@file:OptIn(ExperimentalTime::class)

package es.cristcd.taskcompanion.core

import kotlinx.coroutines.flow.flow
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

fun <T> loadCaching(fromDb: JdbcTransaction.() -> Pair<T, Instant>?, fromApi: suspend () -> T, onUpdate: (T) -> Unit) = flow {
    emit(CachedResult.Loading)
    val dbItem = transaction {
        fromDb()
    }
    if (dbItem != null) {
        emit(CachedResult.FromDb(dbItem.first, dbItem.second))
    }
    val apiItem = fromApi()
    emit(CachedResult.FromApi(apiItem))
    transaction {
        onUpdate(apiItem)
    }
}

sealed interface CachedResult<out T> {
    data object Loading : CachedResult<Nothing>
    data class FromDb<T>(val issue: T, val updatedAt: Instant) : CachedResult<T>
    data class FromApi<T>(val issue: T) : CachedResult<T>
}