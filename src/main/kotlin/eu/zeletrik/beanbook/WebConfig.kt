package eu.zeletrik.beanbook

import org.springframework.boot.tomcat.TomcatContextCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Web server configuration that registers static-asset MIME mappings for the embedded Tomcat. */
@Configuration
class WebConfig {
    /**
     * Registers MIME mappings on the Tomcat context so PWA assets are served with the correct
     * content types: `.webmanifest` as `application/manifest+json` and `.svg` as `image/svg+xml`.
     */
    @Bean
    fun webManifestMimeType(): TomcatContextCustomizer =
        TomcatContextCustomizer { context ->
            context.addMimeMapping("webmanifest", "application/manifest+json")
            context.addMimeMapping("svg", "image/svg+xml")
        }
}
