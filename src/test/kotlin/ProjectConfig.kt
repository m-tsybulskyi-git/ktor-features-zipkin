package mjs.ktor.features.zipkin

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.extensions.junitxml.JunitXmlReporter

class ProjectConfig : AbstractProjectConfig() {
    override fun listeners() = listOf(
        JunitXmlReporter(
            includeContainers = true,
            useTestPathAsName = true,
        )
    )
}