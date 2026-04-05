package com.coloncmd.hotpot.dsl

import com.coloncmd.hotpot.model.HotPotResponse
import com.coloncmd.hotpot.notification.NotifyDefinition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ListenScopeTest :
    FunSpec({

        test("post registers a RouteDefinition.Post") {
            // arrange
            val scope = ListenScope(basePath = "/base", saveRequestResponse = true)

            // act
            scope.post(path = "/callback") { HotPotResponse.ok() }

            // assert
            scope.routes shouldHaveSize 1
            scope.routes.first().shouldBeInstanceOf<RouteDefinition.Post>()
            scope.routes.first().path shouldBe "/callback"
        }

        test("get registers a RouteDefinition.Get") {
            // arrange
            val scope = ListenScope(basePath = "/base", saveRequestResponse = true)

            // act
            scope.get(path = "/status") { HotPotResponse.ok() }

            // assert
            scope.routes shouldHaveSize 1
            scope.routes.first().shouldBeInstanceOf<RouteDefinition.Get>()
        }

        test("post inherits saveRequestResponse = null by default (inherits from group)") {
            // arrange
            val scope = ListenScope(basePath = "/base", saveRequestResponse = true)

            // act
            scope.post(path = "/callback") { HotPotResponse.ok() }

            // assert
            scope.routes.first().saveRequestResponse shouldBe null
        }

        test("post saveRequestResponse override is stored") {
            // arrange
            val scope = ListenScope(basePath = "/base", saveRequestResponse = true)

            // act
            scope.post(path = "/callback", saveRequestResponse = false) { HotPotResponse.ok() }

            // assert
            scope.routes.first().saveRequestResponse shouldBe false
        }

        test("notify with target registers a NotifyDefinition.Proxy") {
            // arrange
            val scope = ListenScope(basePath = "/base", saveRequestResponse = true)

            // act
            scope.notify(path = "/capture-succeeded", target = "http://service/cb")

            // assert
            scope.notifyRoutes shouldHaveSize 1
            val notify = scope.notifyRoutes.first()
            notify.shouldBeInstanceOf<NotifyDefinition.Proxy>()
            notify.path shouldBe "/capture-succeeded"
            notify.target shouldBe "http://service/cb"
        }

        test("notify with handler registers a NotifyDefinition.Custom") {
            // arrange
            val scope = ListenScope(basePath = "/base", saveRequestResponse = true)

            // act
            scope.notify(path = "/complex") { _ -> }

            // assert
            scope.notifyRoutes shouldHaveSize 1
            scope.notifyRoutes.first().shouldBeInstanceOf<NotifyDefinition.Custom>()
        }

        test("multiple routes and notify registrations are collected independently") {
            // arrange
            val scope = ListenScope(basePath = "/base", saveRequestResponse = true)

            // act
            scope.post(path = "/a") { HotPotResponse.ok() }
            scope.get(path = "/b") { HotPotResponse.ok() }
            scope.notify(path = "/trigger", target = "http://x")

            // assert
            scope.routes shouldHaveSize 2
            scope.notifyRoutes shouldHaveSize 1
        }
    })
