package dev.schlaubi.openid.helper.util

import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext
import io.ktor.utils.io.*

data class ProviderRouteSelector(val name: String) : RouteSelector() {
    override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation {
        if (context.segments.take(2) == listOf("providers", name)) {
            return RouteSelectorEvaluation.Constant
        }
        return RouteSelectorEvaluation.FailedParameter
    }

    override fun toString(): String = "[provider = $name]"
}

@KtorDsl
fun Route.provider(name: String, build: Route.() -> Unit): Route {
    val selector = ProviderRouteSelector(name)
    return createChild(selector).apply(build)
}
