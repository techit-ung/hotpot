package com.coloncmd.hotpot.dsl

import com.coloncmd.hotpot.storage.InMemoryStorage
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class StartScopeTest :
    FunSpec({

        fun scope() = StartScope(InMemoryStorage())

        test("listen registers a route group with the given basePath") {
            // arrange
            val scope = scope()

            // act
            scope.listen(path = "/paymob") {}

            // assert
            scope.routeGroups shouldHaveSize 1
            scope.routeGroups.first().basePath shouldBe "/paymob"
        }

        test("listen defaults saveRequestResponse to true") {
            // arrange
            val scope = scope()

            // act
            scope.listen(path = "/p") {}

            // assert
            scope.routeGroups.first().saveRequestResponse shouldBe true
        }

        test("listen respects saveRequestResponse = false") {
            // arrange
            val scope = scope()

            // act
            scope.listen(path = "/p", saveRequestResponse = false) {}

            // assert
            scope.routeGroups.first().saveRequestResponse shouldBe false
        }

        test("multiple listen blocks register multiple groups") {
            // arrange
            val scope = scope()

            // act
            scope.listen(path = "/a") {}
            scope.listen(path = "/b") {}

            // assert
            scope.routeGroups shouldHaveSize 2
        }
    })
