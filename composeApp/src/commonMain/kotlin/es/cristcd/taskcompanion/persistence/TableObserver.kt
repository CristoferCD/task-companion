package es.cristcd.taskcompanion.persistence

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class TableObserver {
    private val revision = MutableStateFlow(0)

    fun <T> writeTransaction(statement: JdbcTransaction.() -> T): T  {
        val result = transaction {
            statement()
        }
        revision.update { it + 1 }
        return result
    }

    fun <T> queryAsFlow(statement: JdbcTransaction.() -> T): Flow<T> = revision.map {
        transaction {
            statement()
        }
    }
}