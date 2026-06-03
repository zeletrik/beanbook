package eu.zeletrik.beanbook

import org.springframework.boot.tomcat.TomcatContextCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WebConfig {
    @Bean
    fun webManifestMimeType(): TomcatContextCustomizer =
        TomcatContextCustomizer { context ->
            context.addMimeMapping("webmanifest", "application/manifest+json")
            context.addMimeMapping("svg", "image/svg+xml")
        }
}
