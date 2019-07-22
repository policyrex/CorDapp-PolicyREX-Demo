package com.policyrex.plugin

import com.policyrex.api.V1
import net.corda.core.messaging.CordaRPCOps
import net.corda.webserver.services.WebServerPluginRegistry
import java.util.function.Function

class WebServerPlugin : WebServerPluginRegistry {
    /**
     * A list of classes that expose web APIs.
     */
    override val webApis: List<Function<CordaRPCOps, out Any>> = listOf(Function(::V1))

    /**
     * A list of directories in the resources directory that will be served by Jetty under /web.
     */
    override val staticServeDirs = mapOf(
            // This will serve the insuranceWeb directory in resources to /web/demo
            "DemoVersion" to javaClass.classLoader.getResource("demo").toExternalForm()
    )
}