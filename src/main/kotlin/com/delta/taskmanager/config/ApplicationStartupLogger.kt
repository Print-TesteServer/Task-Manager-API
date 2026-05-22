package com.delta.taskmanager.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ApplicationStartupLogger(
    @Value("\${server.port}") private val port: Int,
    @Value("\${server.servlet.context-path:}") private val contextPath: String
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        val base = buildBaseUrl()
        val swaggerUrl = "$base/swagger-ui/index.html"
        val apiDocsUrl = "$base/api-docs"

        val lines = listOf(
            "",
            "============================================================",
            "  Task Manager API — pronta para uso",
            "============================================================",
            "  Abra no navegador (Swagger UI):",
            "  $swaggerUrl",
            "",
            "  OpenAPI (JSON):",
            "  $apiDocsUrl",
            "",
            "  Rotas REST retornam JSON (nao abrem como pagina no navegador).",
            "  Use o Swagger, curl ou Postman para testar /api/auth e /api/tasks.",
            "============================================================",
            ""
        )

        log.info(lines.joinToString("\n"))
    }

    private fun buildBaseUrl(): String {
        val path = contextPath.trim().removeSuffix("/")
        return if (path.isEmpty()) "http://localhost:$port" else "http://localhost:$port$path"
    }
}
