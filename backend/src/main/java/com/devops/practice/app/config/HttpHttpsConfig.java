package com.devops.practice.app.config;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Adds a second Tomcat connector so the app listens on BOTH:
 *   - HTTP  on port {http.port}  (default 8080)
 *   - HTTPS on server.port       (default 8443, configured with SSL)
 *
 * Only active when SSL is ON. In production (SPRING_PROFILES_ACTIVE=production
 * sets server.ssl.enabled=false, because Nginx handles TLS), this extra
 * connector is skipped so the app has a single plain-HTTP connector on 8080.
 */
@Configuration
@ConditionalOnProperty(name = "server.ssl.enabled", havingValue = "true")
public class HttpHttpsConfig {

    @Value("${http.port:8080}")
    private int httpPort;

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> httpConnectorCustomizer() {
        return factory -> {
            Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            connector.setScheme("http");
            connector.setPort(httpPort);
            connector.setSecure(false);
            connector.setRedirectPort(httpPort);
            factory.addAdditionalTomcatConnectors(connector);
        };
    }
}