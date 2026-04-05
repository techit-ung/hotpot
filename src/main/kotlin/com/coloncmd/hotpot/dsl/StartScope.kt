package com.coloncmd.hotpot.dsl

import com.coloncmd.hotpot.notification.NotificationService
import com.coloncmd.hotpot.storage.Storage

class StartScope internal constructor(
    internal val storage: Storage,
    internal val notificationService: NotificationService = NotificationService.create(),
) {
    internal val routeGroups = mutableListOf<RouteGroup>()

    fun listen(
        path: String = "",
        saveRequestResponse: Boolean = true,
        block: ListenScope.() -> Unit,
    ) {
        val scope = ListenScope(basePath = path, saveRequestResponse = saveRequestResponse)
        scope.block()
        routeGroups +=
            RouteGroup(
                basePath = path,
                saveRequestResponse = saveRequestResponse,
                routes = scope.routes.toList(),
                notifyRoutes = scope.notifyRoutes.toList(),
            )
    }
}
