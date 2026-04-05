package com.coloncmd.hotpot.routing

import com.coloncmd.hotpot.openapi.HotPotOpenApi.describeEmptyResponse
import com.coloncmd.hotpot.openapi.HotPotOpenApi.describeJsonResponse
import com.coloncmd.hotpot.storage.Storage
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.routing.openapi.describe

object QueryRouter {
    fun install(app: Application, storage: Storage): Route = app.routing {
        route("/hotpot") {
            get("/requests") {
                val path = call.parameters["path"]
                val method = call.parameters["method"]
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 50
                call.respond(storage.findRequests(path = path, method = method, limit = limit))
            }.describe {
                summary = "List captured requests"
                description = "Returns stored inbound requests, optionally filtered by path and method."
                operationId = "listCapturedRequests"
                tag("HotPot")
                parameters {
                    query("path") { description = "Filter by request path." }
                    query("method") { description = "Filter by HTTP method." }
                    query("limit") { description = "Maximum number of results." }
                }
                describeJsonResponse(HttpStatusCode.OK, "Captured requests.")
            }

            get("/requests/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val req = storage.findRequest(id) ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respond(req)
            }.describe {
                summary = "Get captured request"
                description = "Returns one stored request by id."
                operationId = "getCapturedRequest"
                tag("HotPot")
                parameters {
                    path("id") {
                        description = "Captured request id."
                        required = true
                    }
                }
                describeJsonResponse(HttpStatusCode.OK, "Captured request.")
                describeEmptyResponse(HttpStatusCode.BadRequest, "Missing request id.")
                describeEmptyResponse(HttpStatusCode.NotFound, "Request not found.")
            }

            get("/requests/{id}/response") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val resp = storage.findResponseFor(id) ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respond(resp)
            }.describe {
                summary = "Get stored response"
                description = "Returns the stored response paired with a captured request."
                operationId = "getCapturedResponse"
                tag("HotPot")
                parameters {
                    path("id") {
                        description = "Captured request id."
                        required = true
                    }
                }
                describeJsonResponse(HttpStatusCode.OK, "Stored response.")
                describeEmptyResponse(HttpStatusCode.BadRequest, "Missing request id.")
                describeEmptyResponse(HttpStatusCode.NotFound, "Response not found.")
            }

            delete("/requests") {
                storage.clear()
                call.respond(HttpStatusCode.NoContent)
            }.describe {
                summary = "Clear captured requests"
                description = "Deletes all stored requests and responses."
                operationId = "clearCapturedRequests"
                tag("HotPot")
                describeEmptyResponse(HttpStatusCode.NoContent, "Stored data cleared.")
            }
        }
    }
}
