# Demostración de OAuth2 Client Credentials Grant en token opaco

## Protocolo OAuth2
El protocolo OAuth2 es un protocolo de autorización que permite a una aplicación obtener acceso a los recursos de un propietario del recurso en nombre del propietario del recurso. OAuth2 define cuatro roles: propietario del recurso, servidor de recursos, cliente y servidor de autorización.

## Actores de OAuth2

1. *propietario del recurso*: Una entidad capaz de otorgar acceso a un recurso protegido. Cuando el propietario del recurso es una persona, se le denomina usuario final.

2. *servidor de recursos*: El servidor que alberga los recursos protegidos, capaz de aceptar y responder a solicitudes de recursos protegidos utilizando tokens de acceso.

3. *Cliente*: Una aplicación que realiza solicitudes de recursos protegidos en nombre del propietario del recurso y con su autorización. El término "cliente" no implica ninguna característica de implementación particular (por ejemplo, si la aplicación se ejecuta en un servidor, un escritorio u otros dispositivos). 

4. *servidor de autorización*: El servidor que emite tokens de acceso al cliente después de autenticarcon éxito al propietario del recurso y obtener autorización.

## Formatos de token OAuth2

### Self-contained - JSON Web Token (JWT)

Es una cadena de texto JSON codificado en base 64 con información de acceso (issuer, subject, rol, permisos, algoritmo cifrado, … , firma digital (hash del json cifrado con la llave privada del issuer)). Para verificar solo basta que el cliente utilice la llave publica del issuer para validar la firma del JWT.

### Reference (Opaco)

Es un token que no contiene información de acceso, solo un identificador único que el servidor de recursos puede validar en su base de datos para otorgar acceso a los recursos protegidos. Para verificar el token, el servidor de recursos debe hacer una consulta al servidor de autorización.

## Tipos de flujo de autorización OAuth2 (Grant type)

The most common OAuth grant types are listed below.

1. Authorization Code Grant: Utilizado principalmente por aplicaciones web del lado del servidor. El cliente recibe un código de autorización que luego se intercambia por un token de acceso.

2. Proof Key for Code Exchange (PKCE): Extensión del Authorization Code Grant, diseñado para aplicaciones públicas y móviles. Añade un paso adicional en el que el cliente crea un código secreto y un código de verificación, y solo el cliente conoce el código secreto.

3. Client Credentials Grant (Application Access): Utilizado por aplicaciones que necesitan acceder a sus propios recursos en lugar de los recursos de un usuario. El cliente se autentica utilizando sus propias credenciales.

4. Device Authorization Grant: Utilizado por aplicaciones en dispositivos con recursos limitados (como televisores, dispositivos de juegos, etc.) que no pueden interactuar directamente con un navegador. El usuario ingresa un código en un dispositivo y el dispositivo lo intercambia por un token de acceso.

5. Refresh Token Grant: Permite a las aplicaciones obtener un nuevo token de acceso utilizando un token de actualización, sin necesidad de que el usuario vuelva a autenticarse.

## Componentes de OAuth2 en Spring Security

### Cliente registrado `RegisteredClient`
* Archivo: *org.springframework.security.oauth2.server.authorization.client.RegisteredClient* 
* Artefacto dependencia: **spring-security-oauth2-authorization-serve**

Un cliente registrado es una aplicación que se ha registrado en el servidor de autorización. Un cliente debe estar registrado antes de poder solicitar tokens de acceso. Durante el registro del cliente, se le asigna un `client_id` y un `client_secret` (opcional para algunos flujos de autorización).
<https://datatracker.ietf.org/doc/html/rfc6749#section-2>

Por ejemplo, se puede registrar un cliente con los siguientes atributos:
* `withId(uuid)`: Identificador único del cliente.
* `clientId(clientId)`: Identificador del cliente.
* `clientSecret(clientSecret)`: Secreto del cliente.
* `authorizationGrantType(authorizationGrantType)`: The authorization grant type(s) that the client can use. The supported values are `authorization_code`, `client_credentials`, `refresh_token`, `urn:ietf:params:oauth:grant-type:device_code`, and `urn:ietf:params:oauth:grant-type:token-exchange`.
* `scope(scope)`: The scope(s) that the client can use. 
* `redirectUri(redirectUri)`: The redirect URI(s) that the client can use.	

Mas información en <https://docs.spring.io/spring-authorization-server/reference/core-model-components.html#registered-client>

### Repositorio de clientes registrados `RegisteredClientRepository`
* Archivo: **org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository**
* Artefacto dependencia: **spring-security-oauth2-authorization-serve**

La interfaz `RegisteredClientRepository` permite gestionar los clientes registrados en el servidor de autorización OAuth2. Es requerido inyectar un bean de tipo `RegisteredClientRepository` en el contexto de Spring para poder utilizar el servidor de autorización OAuth2.

Mas información en <https://docs.spring.io/spring-authorization-server/reference/core-model-components.html#registered-client-repository>

### Repositorio de clientes registrados en memoria `InMemoryRegisteredClientRepository`
* Archivo **org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository**
* Artefacto dependencia: **spring-security-oauth2-authorization-serve**

La clase `InMemoryRegisteredClientRepository` es una implementación de `RegisteredClientRepository` que almacena los clientes registrados en memoria. Por ejemplo, se puede registrar clientes de la siguiente manera:

```java
@Bean
public RegisteredClientRepository registeredClientRepository() {
	List<RegisteredClient> registrations = ...
	return new InMemoryRegisteredClientRepository(registrations);
}
```

### Repositorio de clientes registrados en base de datos `JdbcRegisteredClientRepository`
Archivo: **org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository**
Artefacto dependencia: **spring-security-oauth2-authorization-serve**

La clase `JdbcRegisteredClientRepository` es una implementación de `RegisteredClientRepository` que almacena los clientes registrados en una base de datos. En la documentación encontramos la siguiente advertencia:

> IMPORTANT: This RegisteredClientRepository depends on the table definition described in "classpath:org/springframework/security/oauth2/server/authorization/client/oauth2-registered-client-schema.sql" and therefore MUST be defined in the database schema. 

Este archivo de esquema se puede obtener descomprimiendo el archivo `spring-security-oauth2-authorization-server-0.2.0.jar` y se encuentra en la ruta `org/springframework/security/oauth2/server/authorization/client/oauth2-registered-client-schema.sql`. El contenido del archivo es el siguiente:

```sql
CREATE TABLE oauth2_registered_client (
    id varchar(100) NOT NULL,
    client_id varchar(100) NOT NULL,
    client_id_issued_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_secret varchar(200) DEFAULT NULL,
    client_secret_expires_at timestamp DEFAULT NULL,
    client_name varchar(200) NOT NULL,
    client_authentication_methods varchar(1000) NOT NULL,
    authorization_grant_types varchar(1000) NOT NULL,
    redirect_uris varchar(1000) DEFAULT NULL,
    post_logout_redirect_uris varchar(1000) DEFAULT NULL,
    scopes varchar(1000) NOT NULL,
    client_settings varchar(2000) NOT NULL,
    token_settings varchar(2000) NOT NULL,
    PRIMARY KEY (id)
);
```
> https://github.com/spring-projects/spring-authorization-server/blob/main/oauth2-authorization-server/src/main/resources/org/springframework/security/oauth2/server/authorization/client/oauth2-registered-client-schema.sql

Para utilizar esta implementación se debe configurar un `DataSource` y se debe registrar un `JdbcRegisteredClientRepository` con un `org.springframework.jdbc.core.JdbcOperations jdbcOperations` en el contexto de Spring. Por ejemplo:

```java
@Bean
public RegisteredClientRepository registeredClientRepository(DataSource dataSource) {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    JdbcRegisteredClientRepository registeredClientRepository = new JdbcRegisteredClientRepository(jdbcTemplate);
    return registeredClientRepository;
}
```

### Configuración del servidor de autorización `AuthorizationServerSettings` 
* Archivo: **org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings**
* Artefacto dependencia: **spring-security-oauth2-authorization-serve**

La clase `AuthorizationServerSettings` permite configurar el servidor de autorización OAuth2. Por ejemplo para la autorización de tipo `client_credentials` se puede configurar los siguientes atributos:
* `issuer(uri)`: Define la URI del emisor de los tokens. Esta URI se utiliza para identificar el servidor de autorización que emite los tokens.
* `tokenEndpoint(path)`: Define la URI del punto de acceso de los tokens de acceso.
* `tokenIntrospectionEndpoint(path)`: Define la URI del punto de acceso de la introspección de los tokens.
* `tokenRevocationEndpoint(path)`: Define la URI del punto de acceso de la revocación de los tokens.

### Configuraciones de Token  `TokenSettings`
Archivo: **org.springframework.security.oauth2.server.authorization.settings.TokenSettings**
Artefacto dependencia: **spring-security-oauth2-authorization-serve**

La clase `TokenSettings` permite configurar los tokens de acceso y de actualización. Por ejemplo, se puede configurar el formato (entre `OAuth2TokenFormat.SELF_CONTAINED` y `OAuth2TokenFormat.REFERENCE`), la duración de los tokens de acceso y de actualización de la siguiente manera:
    
```java
TokenSettings tokenSettings = TokenSettings.builder()
    .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
    .accessTokenTimeToLive(Duration.ofHours(1))
    .refreshTokenTimeToLive(Duration.ofDays(1))
    .build();
```

Mas información en <https://docs.spring.io/spring-authorization-server/reference/core-model-components.html#oauth2-token-generator>

### Servicio de autorización OAuth2 `OAuth2AuthorizationService`
Archivo: **org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationServic**
Artefacto dependencia: **spring-security-oauth2-authorization-serve**

La interfaz `OAuth2AuthorizationService` permite gestionar las autorizaciones OAuth2. Es requerido inyectar un bean de tipo `OAuth2AuthorizationService` en el contexto de Spring para poder utilizar el servidor de autorización OAuth2.

Mas información en <https://docs.spring.io/spring-authorization-server/reference/core-model-components.html#oauth2-authorization-service>

### Servicio de autorización OAuth2 en memoria `InMemoryOAuth2AuthorizationService`
Archivo: **org.springframework.security.oauth2.server.authorization.InMemoryOAuth2Authorization
Artefacto dependencia: **spring-security-oauth2-authorization-serve**

La clase `InMemoryOAuth2AuthorizationService` es una implementación de `OAuth2AuthorizationService` que almacena las autorizaciones en memoria. 

### Servicio de autorización OAuth2 en base de datos `JdbcOAuth2AuthorizationService`
Archivo: **org.springframework.security.oauth2.server.authorization.JdbcOAuth2Authorization**
Artefacto dependencia: **spring-security-oauth2-authorization-serve**

La clase `JdbcOAuth2AuthorizationService` es una implementación de `OAuth2AuthorizationService` que almacena las autorizaciones en una base de datos. En la documentación encontramos la siguiente advertencia:

> IMPORTANT: This OAuth2AuthorizationService depends on the table definition described in "classpath:org/springframework/security/oauth2/server/authorization/oauth2-authorization-schema.sql" and therefore MUST be defined in the database schema.

Este archivo de esquema se puede obtener descomprimiendo el archivo `spring-security-oauth2-authorization-server-0.2.0.jar` y se encuentra en la ruta `org/springframework/security/oauth2/server/authorization/oauth2-authorization-schema.sql`. El contenido del archivo es el siguiente:

```sql
CREATE TABLE oauth2_authorization (
    id varchar(100) NOT NULL,
    registered_client_id varchar(100) NOT NULL,
    principal_name varchar(200) NOT NULL,
    authorization_grant_type varchar(100) NOT NULL,
    authorized_scopes varchar(1000) DEFAULT NULL,
    attributes blob DEFAULT NULL,
    state varchar(500) DEFAULT NULL,
    authorization_code_value blob DEFAULT NULL,
    authorization_code_issued_at timestamp DEFAULT NULL,
    authorization_code_expires_at timestamp DEFAULT NULL,
    authorization_code_metadata blob DEFAULT NULL,
    access_token_value blob DEFAULT NULL,
    access_token_issued_at timestamp DEFAULT NULL,
    access_token_expires_at timestamp DEFAULT NULL,
    access_token_metadata blob DEFAULT NULL,
    access_token_type varchar(100) DEFAULT NULL,
    access_token_scopes varchar(1000) DEFAULT NULL,
    oidc_id_token_value blob DEFAULT NULL,
    oidc_id_token_issued_at timestamp DEFAULT NULL,
    oidc_id_token_expires_at timestamp DEFAULT NULL,
    oidc_id_token_metadata blob DEFAULT NULL,
    refresh_token_value blob DEFAULT NULL,
    refresh_token_issued_at timestamp DEFAULT NULL,
    refresh_token_expires_at timestamp DEFAULT NULL,
    refresh_token_metadata blob DEFAULT NULL,
    user_code_value blob DEFAULT NULL,
    user_code_issued_at timestamp DEFAULT NULL,
    user_code_expires_at timestamp DEFAULT NULL,
    user_code_metadata blob DEFAULT NULL,
    device_code_value blob DEFAULT NULL,
    device_code_issued_at timestamp DEFAULT NULL,
    device_code_expires_at timestamp DEFAULT NULL,
    device_code_metadata blob DEFAULT NULL,
    PRIMARY KEY (id)
);
```
> https://github.com/spring-projects/spring-authorization-server/blob/main/oauth2-authorization-server/src/main/resources/org/springframework/security/oauth2/server/authorization/oauth2-authorization-schema.sql

Es posible registrar un `JdbcOAuth2AuthorizationService` con un `org.springframework.jdbc.core.JdbcOperations jdbcOperations` en el contexto de Spring. Por ejemplo:
```java
  @Bean
  public OAuth2AuthorizationService authorizationService(DataSource dataSource, RegisteredClientRepository registeredClientRepository) {
      JdbcOperations jdbcOperations = new JdbcTemplate(dataSource);
      return new JdbcOAuth2AuthorizationService(jdbcOperations, registeredClientRepository);
  }
```

### Generador de tokens `OAuth2TokenGenerator`
Archivo: **org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator**
Artefacto dependencia: **spring-security-oauth2-authorization-serve**

La interfaz `OAuth2TokenGenerator` permite generar tokens de acceso y de actualización. Para utilizar esta interfaz se debe inyectar un bean de tipo `OAuth2TokenGenerator` en el contexto de Spring. 

El `OAuth2TokenGenerator` es utilizado principalmente por componentes que implementan el procesamiento de concesiones de autorización, por ejemplo, `authorization_code`, `client_credentials` y `refresh_token`. 
Las implementaciones proporcionadas son `OAuth2AccessTokenGenerator`, `OAuth2RefreshTokenGenerator` y `JwtGenerator`. El `OAuth2AccessTokenGenerator` genera un token de acceso "opaco" (`OAuth2TokenFormat.REFERENCE`), y el `JwtGenerator` genera un Jwt (`OAuth2TokenFormat.SELF_CONTAINED`).

[NOTA]
> El OAuth2TokenGenerator es un componente OPCIONAL y por defecto es un DelegatingOAuth2TokenGenerator compuesto por un OAuth2AccessTokenGenerator y un OAuth2RefreshTokenGenerator.

[NOTA]
> Si se registra un JwtEncoder como @Bean o un JWKSource<SecurityContext> como @Bean, entonces un JwtGenerator se compone adicionalmente en el DelegatingOAuth2TokenGenerator.