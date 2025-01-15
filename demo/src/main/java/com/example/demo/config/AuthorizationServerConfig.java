package com.example.demo.config;

import java.time.Duration;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class AuthorizationServerConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        return http.formLogin(Customizer.withDefaults()).build();
    }

    /**
     * Ejemplo de configuración de un repositorio de clientes registrados en una
     * base de datos
     *
     * @param dataSource
     * @return
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(DataSource dataSource) {

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        JdbcRegisteredClientRepository registeredClientRepository = new JdbcRegisteredClientRepository(jdbcTemplate);

        String clientId = "client-id";
        RegisteredClient existingClient = registeredClientRepository.findByClientId(clientId);

        if (existingClient == null) {
            RegisteredClient registeredClient = RegisteredClient
                    // The ID that uniquely identifies the RegisteredClient.
                    .withId(UUID.randomUUID().toString())
                    // The client identifier.
                    .clientId("client-id")
                    // The client’s secret. The value should be encoded using Spring Security’s
                    // PasswordEncoder. `{noop}` is used to indicate that the client’s secret is not
                    // encoded.
                    .clientSecret("{noop}client-secret")
                    // The authorization grant types that the client is authorized to use.
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    // The scopes that the client is authorized to use.
                    .scope("read")
                    .scope("write")
                    // The redirect URIs that the client is authorized to use.
                    .redirectUri("https://your-redirect-uri")
                    // The settings for the client’s tokens.
                    .tokenSettings(
                            TokenSettings.builder()
                                    // Set the format of the access token to opaque.
                                    .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                                    // Set the time-to-live for the access token to 5 minutes.
                                    .accessTokenTimeToLive(Duration.ofMinutes(300))
                                    // Set the time-to-live for the refresh token to 10 minutes.
                                    .refreshTokenTimeToLive(Duration.ofMinutes(600))
                                    // Set the reuse of the refresh token to false. This implies that a new refresh
                                    // token will be issued each time a refresh token is requested.
                                    .reuseRefreshTokens(false)
                                    .build())
                    .build();
            registeredClientRepository.save(registeredClient);
        }

        return registeredClientRepository;
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(DataSource dataSource, RegisteredClientRepository registeredClientRepository) {
        JdbcOperations jdbcOperations = new JdbcTemplate(dataSource);
        return new JdbcOAuth2AuthorizationService(jdbcOperations, registeredClientRepository);
    }

    /**
     * Ejemplo de configuración de los ajustes del servidor de autorización
     * https://docs.spring.io/spring-authorization-server/reference/configuration-model.html#configuring-authorization-server-settings
     *
     * @return
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                // se define el emisor del token
                .issuer("http://localhost:8080")
                // se define el endpoint de solicitud de token
                .tokenEndpoint("/oauth2/token")
                // se define el endpoint de introspección de token
                .tokenIntrospectionEndpoint("/oauth2/introspect")
                // se define el endpoint de revocación de token
                .tokenRevocationEndpoint("/oauth2/revoke")
                .build();
    }

}
