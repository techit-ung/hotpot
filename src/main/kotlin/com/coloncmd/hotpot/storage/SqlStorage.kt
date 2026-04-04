package com.coloncmd.hotpot.storage

import com.coloncmd.hotpot.model.WebhookRequest
import com.coloncmd.hotpot.model.WebhookResponse
import com.coloncmd.hotpot.storage.schema.RequestsTable
import com.coloncmd.hotpot.storage.schema.ResponsesTable
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class SqlStorage(private val database: Database) : Storage {

    companion object {
        fun inMemory(): SqlStorage {
            val db = Database.connect(
                url = "jdbc:h2:mem:hotpot_${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver",
            )
            transaction(db) {
                SchemaUtils.create(RequestsTable, ResponsesTable)
            }
            return SqlStorage(db)
        }
    }

    private suspend fun <T> db(block: Transaction.() -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database, statement = block)

    override suspend fun saveRequest(request: WebhookRequest): String = db {
        val id = UUID.randomUUID().toString()
        RequestsTable.insert {
            it[RequestsTable.id]         = id
            it[RequestsTable.path]       = request.path
            it[RequestsTable.method]     = request.method
            it[RequestsTable.headers]    = Json.encodeToString(request.headers)
            it[RequestsTable.body]       = request.body
            it[RequestsTable.receivedAt] = request.receivedAt
        }
        id
    }

    override suspend fun saveResponse(requestId: String, response: WebhookResponse): Unit = db {
        ResponsesTable.insert {
            it[ResponsesTable.requestId] = requestId
            it[ResponsesTable.status]    = response.status
            it[ResponsesTable.body]      = response.body
            it[ResponsesTable.sentAt]    = response.sentAt
        }
    }

    override suspend fun findRequests(path: String?, method: String?, limit: Int): List<WebhookRequest> = db {
        RequestsTable
            .selectAll()
            .apply { path?.let { andWhere { RequestsTable.path eq path } } }
            .apply { method?.let { andWhere { RequestsTable.method eq method } } }
            .orderBy(RequestsTable.receivedAt, SortOrder.DESC)
            .limit(limit)
            .map { it.toWebhookRequest() }
    }

    override suspend fun findRequest(id: String): WebhookRequest? = db {
        RequestsTable
            .selectAll()
            .where { RequestsTable.id eq id }
            .singleOrNull()
            ?.toWebhookRequest()
    }

    override suspend fun findResponseFor(requestId: String): WebhookResponse? = db {
        ResponsesTable
            .selectAll()
            .where { ResponsesTable.requestId eq requestId }
            .singleOrNull()
            ?.toWebhookResponse()
    }

    override suspend fun clear(): Unit = db {
        ResponsesTable.deleteAll()
        RequestsTable.deleteAll()
    }

    @Suppress("UNCHECKED_CAST")
    private fun ResultRow.toWebhookRequest() = WebhookRequest(
        id         = this[RequestsTable.id],
        path       = this[RequestsTable.path],
        method     = this[RequestsTable.method],
        headers    = Json.decodeFromString<Map<String, List<String>>>(this[RequestsTable.headers]),
        body       = this[RequestsTable.body],
        receivedAt = this[RequestsTable.receivedAt],
    )

    private fun ResultRow.toWebhookResponse() = WebhookResponse(
        requestId = this[ResponsesTable.requestId],
        status    = this[ResponsesTable.status],
        body      = this[ResponsesTable.body],
        sentAt    = this[ResponsesTable.sentAt],
    )
}
