/*
 * Copyright [2024] [envite consulting GmbH]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.kadai.adapter.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Configures an embedded OAuth2 Authorization Server for showcasing the client-credentials flow
 * against the Camunda 7 Outbox REST API.
 *
 * <p>This configuration sets up a self-contained authorization server within the Spring Boot
 * application. The token endpoint is available at {@code /oauth2/token} on the application's own
 * port, making the example fully runnable without any external identity provider.
 *
 * <p>Pre-configured client:
 *
 * <ul>
 *   <li>Client ID: {@code kadai-adapter}
 *   <li>Client Secret: {@code super-secret}
 *   <li>Authentication Method: {@code client_secret_post}
 *   <li>Grant Type: {@code client_credentials}
 *   <li>Scopes: {@code outbox:read}, {@code outbox:write}
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class EmbeddedAuthorizationServerConfig {

  /**
   * Security filter chain for the OAuth2 Authorization Server endpoints (e.g. {@code
   * /oauth2/token}, {@code /oauth2/jwks}).
   *
   * @param http the {@link HttpSecurity} to configure
   * @return the configured {@link SecurityFilterChain}
   */
  @Bean
  @Order(1)
  public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) {
    OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
        new OAuth2AuthorizationServerConfigurer();
    RequestMatcher endpointsMatcher = authorizationServerConfigurer.getEndpointsMatcher();
    http.securityMatcher(endpointsMatcher)
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .csrf(csrf -> csrf.ignoringRequestMatchers(endpointsMatcher))
        .with(authorizationServerConfigurer, Customizer.withDefaults());
    return http.build();
  }

  /**
   * Security filter chain that protects the Outbox REST endpoints ({@code /outbox-rest/**}) with
   * OAuth2 JWT Bearer token authentication. The same {@link JwtDecoder} used by the embedded
   * authorization server validates incoming tokens without any external HTTP call.
   *
   * @param http the {@link HttpSecurity} to configure
   * @return the configured {@link SecurityFilterChain}
   * @throws Exception if the security configuration fails
   */
  @Bean
  @Order(2)
  public SecurityFilterChain outboxResourceServerFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher(request -> request.getRequestURI().startsWith("/outbox-rest"))
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .csrf(AbstractHttpConfigurer::disable);
    return http.build();
  }

  /**
   * Default security filter chain that permits all other requests without authentication, suitable
   * for a local showcase application.
   *
   * @param http the {@link HttpSecurity} to configure
   * @return the configured {@link SecurityFilterChain}
   * @throws Exception if the security configuration fails
   */
  @Bean
  @Order(3)
  public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .csrf(AbstractHttpConfigurer::disable);
    return http.build();
  }

  /**
   * In-memory repository of OAuth2 clients registered with the embedded authorization server.
   *
   * @return the {@link RegisteredClientRepository}
   */
  @Bean
  public RegisteredClientRepository registeredClientRepository() {
    RegisteredClient kadaiAdapterClient =
        RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("kadai-adapter")
            .clientSecret("{noop}super-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .scope("outbox:read")
            .scope("outbox:write")
            .tokenSettings(
                TokenSettings.builder().accessTokenTimeToLive(Duration.ofHours(1)).build())
            .build();
    return new InMemoryRegisteredClientRepository(kadaiAdapterClient);
  }

  /**
   * JWK source backed by a freshly generated RSA key-pair, used to sign JWT access tokens.
   *
   * @return the {@link JWKSource}
   */
  @Bean
  public JWKSource<SecurityContext> jwkSource() {
    RSAKey rsaKey = generateRsaKey();
    JWKSet jwkSet = new JWKSet(rsaKey);
    return new ImmutableJWKSet<>(jwkSet);
  }

  /**
   * JWT decoder used to validate tokens issued by this authorization server (e.g. for token
   * introspection).
   *
   * @param jwkSource the JWK source to use for decoding
   * @return the {@link JwtDecoder}
   */
  @Bean
  public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
    return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
  }

  /**
   * Authorization server settings with default endpoint paths ({@code /oauth2/token}, {@code
   * /oauth2/jwks}, etc.).
   *
   * @return the {@link AuthorizationServerSettings}
   */
  @Bean
  public AuthorizationServerSettings authorizationServerSettings() {
    return AuthorizationServerSettings.builder().build();
  }

  private static RSAKey generateRsaKey() {
    KeyPair keyPair = generateRsaKeyPair();
    return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
        .privateKey((RSAPrivateKey) keyPair.getPrivate())
        .keyID(UUID.randomUUID().toString())
        .build();
  }

  private static KeyPair generateRsaKeyPair() {
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      return keyPairGenerator.generateKeyPair();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(
          "Failed to generate RSA key pair for OAuth2 token signing", e);
    }
  }
}
