package io.hotpot.dsl

import io.hotpot.notification.NotifyDefinition

data class RouteGroup(
    val basePath: String,
    val saveRequestResponse: Boolean,
    val routes: List<RouteDefinition>,
    val notifyRoutes: List<NotifyDefinition>,
)
