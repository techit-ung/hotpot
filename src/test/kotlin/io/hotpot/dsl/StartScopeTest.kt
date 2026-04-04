package io.hotpot.dsl

import io.hotpot.model.HotPotResponse
import io.hotpot.storage.InMemoryStorage
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class StartScopeTest : FunSpec({

    fun scope() = StartScope(InMemoryStorage())

    test("listen registers a route group with the given basePath") {
        val scope = scope()
        scope.listen(path = "/paymob") {}
        scope.routeGroups shouldHaveSize 1
        scope.routeGroups.first().basePath shouldBe "/paymob"
    }

    test("listen defaults saveRequestResponse to true") {
        val scope = scope()
        scope.listen(path = "/p") {}
        scope.routeGroups.first().saveRequestResponse shouldBe true
    }

    test("listen respects saveRequestResponse = false") {
        val scope = scope()
        scope.listen(path = "/p", saveRequestResponse = false) {}
        scope.routeGroups.first().saveRequestResponse shouldBe false
    }

    test("multiple listen blocks register multiple groups") {
        val scope = scope()
        scope.listen(path = "/a") {}
        scope.listen(path = "/b") {}
        scope.routeGroups shouldHaveSize 2
    }
})
