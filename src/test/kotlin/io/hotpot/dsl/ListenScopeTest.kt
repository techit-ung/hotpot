package io.hotpot.dsl

import io.hotpot.model.HotPotResponse
import io.hotpot.notification.NotifyDefinition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ListenScopeTest : FunSpec({

    test("post registers a RouteDefinition.Post") {
        val scope = ListenScope(basePath = "/base", saveRequestResponse = true)
        scope.post(path = "/callback") { HotPotResponse.ok() }
        scope.routes shouldHaveSize 1
        scope.routes.first().shouldBeInstanceOf<RouteDefinition.Post>()
        scope.routes.first().path shouldBe "/callback"
    }

    test("get registers a RouteDefinition.Get") {
        val scope = ListenScope(basePath = "/base", saveRequestResponse = true)
        scope.get(path = "/status") { HotPotResponse.ok() }
        scope.routes shouldHaveSize 1
        scope.routes.first().shouldBeInstanceOf<RouteDefinition.Get>()
    }

    test("post inherits saveRequestResponse = null by default (inherits from group)") {
        val scope = ListenScope(basePath = "/base", saveRequestResponse = true)
        scope.post(path = "/callback") { HotPotResponse.ok() }
        scope.routes.first().saveRequestResponse shouldBe null
    }

    test("post saveRequestResponse override is stored") {
        val scope = ListenScope(basePath = "/base", saveRequestResponse = true)
        scope.post(path = "/callback", saveRequestResponse = false) { HotPotResponse.ok() }
        scope.routes.first().saveRequestResponse shouldBe false
    }

    test("notify with target registers a NotifyDefinition.Proxy") {
        val scope = ListenScope(basePath = "/base", saveRequestResponse = true)
        scope.notify(path = "/capture-succeeded", target = "http://service/cb")
        scope.notifyRoutes shouldHaveSize 1
        val notify = scope.notifyRoutes.first()
        notify.shouldBeInstanceOf<NotifyDefinition.Proxy>()
        notify.path shouldBe "/capture-succeeded"
        (notify as NotifyDefinition.Proxy).target shouldBe "http://service/cb"
    }

    test("notify with handler registers a NotifyDefinition.Custom") {
        val scope = ListenScope(basePath = "/base", saveRequestResponse = true)
        scope.notify(path = "/complex") { _ -> }
        scope.notifyRoutes shouldHaveSize 1
        scope.notifyRoutes.first().shouldBeInstanceOf<NotifyDefinition.Custom>()
    }

    test("multiple routes and notify registrations are collected independently") {
        val scope = ListenScope(basePath = "/base", saveRequestResponse = true)
        scope.post(path = "/a") { HotPotResponse.ok() }
        scope.get(path = "/b") { HotPotResponse.ok() }
        scope.notify(path = "/trigger", target = "http://x")
        scope.routes shouldHaveSize 2
        scope.notifyRoutes shouldHaveSize 1
    }
})
