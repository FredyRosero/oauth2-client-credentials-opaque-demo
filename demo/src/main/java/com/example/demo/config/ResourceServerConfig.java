package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class ResourceServerConfig {

    @Value("${spring.security.oauth2.resourceserver.opaquetoken.introspection-uri}")
	String introspectionUri;

	@Value("${spring.security.oauth2.resourceserver.opaquetoken.client-id}")
	String clientId;

	@Value("${spring.security.oauth2.resourceserver.opaquetoken.client-secret}")
	String clientSecret;
    
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // disable CSRF: CROSS-SITE REQUEST FORGERY is a type of attack that occurs when a malicious web site, email, blog, instant message, or program causes a user's web browser to perform an unwanted action on a trusted site when the user is authenticated
            .csrf(csrf -> csrf.disable())
            // authorize any request: any request must be authenticated
            .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers("/actuator/**").permitAll()
                    .anyRequest().authenticated()
            )
            // configure OAuth2 Resource Server: opaque token
            .oauth2ResourceServer((oauth2) -> oauth2
                    .opaqueToken((opaque) -> opaque
                            .introspectionUri(this.introspectionUri)
                            .introspectionClientCredentials(this.clientId, this.clientSecret)
                    )
            );
        return http.build();
	}
    
}
 