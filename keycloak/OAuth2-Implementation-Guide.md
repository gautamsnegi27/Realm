# OAuth2 Implementation with Keycloak

## Table of Contents
- [OAuth2 Fundamentals](#oauth2-fundamentals)
- [Authorization Code Flow](#authorization-code-flow)
- [Keycloak Integration](#keycloak-integration)
- [Security Implementation](#security-implementation)
- [Token Management](#token-management)
- [Architecture Overview](#architecture-overview)

## OAuth2 Fundamentals

### What is OAuth2?
OAuth2 (Open Authorization 2.0) is an authorization framework that enables applications to obtain limited access to user accounts on an HTTP service. It works by delegating user authentication to the service that hosts the user account and authorizing third-party applications to access the user account.

### Key Components
- **Resource Owner**: The user who authorizes an application to access their account
- **Client**: The application that wants to access the user's account
- **Resource Server**: The server hosting the protected resources (APIs)
- **Authorization Server**: The server that authenticates the user and issues access tokens

### OAuth2 Grant Types
1. **Authorization Code Grant** (Most Secure - Used in our implementation)
2. **Implicit Grant** (Deprecated for security reasons)
3. **Resource Owner Password Credentials Grant**
4. **Client Credentials Grant**

## Authorization Code Flow

### Step-by-Step Process

#### 1. Authorization Request
```
GET /auth?response_type=code&client_id=CLIENT_ID&redirect_uri=REDIRECT_URI&scope=SCOPE&state=STATE
```

#### 2. User Authorization
- User is redirected to authorization server (Keycloak)
- User authenticates with their credentials
- User grants permission to the application

#### 3. Authorization Code Response
```
HTTP/1.1 302 Found
Location: REDIRECT_URI?code=AUTHORIZATION_CODE&state=STATE
```

#### 4. Access Token Request
```
POST /token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&
code=AUTHORIZATION_CODE&
redirect_uri=REDIRECT_URI&
client_id=CLIENT_ID&
client_secret=CLIENT_SECRET
```

#### 5. Access Token Response
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "scope": "openid email profile"
}
```

## Keycloak Integration

### Configuration
- **Keycloak Server**: `http://localhost:9191`
- **Realm**: `realm-service`
- **Client ID**: `user-service-client`
- **Client Type**: Confidential (requires client secret)
- **Valid Redirect URIs**: `http://localhost:3000/auth/callback`

### Supported Identity Providers
- Google OAuth2
- GitHub OAuth2
- Custom OIDC providers

### Client Scopes
- `openid`: Required for OpenID Connect
- `email`: Access to user's email address
- `profile`: Access to user's profile information (name, etc.)

## Security Implementation

### Frontend Security
```typescript
// Duplicate request prevention
const processedKey = `oauth2_processed_${code}`
if (sessionStorage.getItem(processedKey)) {
  return // Prevent duplicate processing
}
sessionStorage.setItem(processedKey, 'true')
```

### Backend Security
```java
// Client authentication with secret
headers.setBasicAuth("user-service-client", clientSecret);

// Secure token exchange
MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
body.add("grant_type", "authorization_code");
body.add("code", request.getCode());
body.add("redirect_uri", request.getRedirectUri());
```

### Security Best Practices Implemented
1. **Client Secret Protection**: Never exposed to frontend
2. **State Parameter**: CSRF protection (handled by Keycloak)
3. **HTTPS in Production**: Secure token transmission
4. **Token Validation**: Verify tokens before use
5. **Scope Limitation**: Request minimal required permissions

## Token Management

### Access Token
- **Purpose**: API access authorization
- **Lifetime**: 1 hour (configurable)
- **Format**: JWT (JSON Web Token)
- **Usage**: Bearer token in Authorization header

### Refresh Token
- **Purpose**: Obtain new access tokens without re-authentication
- **Lifetime**: 30 days (configurable)
- **Security**: Stored securely, rotated on use

### ID Token (OpenID Connect)
- **Purpose**: User identity information
- **Contains**: User claims (sub, email, name, etc.)
- **Verification**: Signature validation required

### Token Storage
```typescript
// Frontend token storage
localStorage.setItem('authToken', response.token)
localStorage.setItem('user', JSON.stringify(userData))
```

## Architecture Overview

### Flow Diagram
```
[Frontend] → [Keycloak] → [Backend] → [Database]
     ↓           ↓           ↓           ↓
  Initiate    Authenticate  Exchange   Store User
   OAuth2        User       Tokens      Data
```

### Component Responsibilities

#### Frontend (React)
- Initiate OAuth2 flow
- Handle callback with authorization code
- Send code to backend for secure exchange
- Store application JWT token

#### Keycloak (Authorization Server)
- User authentication
- Social provider integration
- Token issuance and validation
- User session management

#### Backend (Spring Boot)
- Secure token exchange with client secret
- User information retrieval from Keycloak
- Application JWT token generation
- User account creation/synchronization

#### Database (MongoDB)
- User profile storage
- OAuth2 provider mapping
- Application-specific user data

### Error Handling

#### Common Error Scenarios
1. **Invalid Authorization Code**: Code expired or already used
2. **Invalid Client Credentials**: Wrong client ID or secret
3. **Invalid Redirect URI**: Mismatch with registered URI
4. **Insufficient Scope**: Requested permissions not granted
5. **Network Failures**: Timeout or connectivity issues

#### Error Response Format
```json
{
  "error": "invalid_grant",
  "error_description": "The provided authorization grant is invalid"
}
```

### Performance Considerations
- **Token Caching**: Cache valid tokens to reduce API calls
- **Connection Pooling**: Reuse HTTP connections for token requests
- **Async Processing**: Non-blocking token validation
- **Rate Limiting**: Prevent abuse of token endpoints

### Monitoring and Logging
- **Token Exchange Events**: Log successful/failed exchanges
- **User Authentication**: Track login attempts and success rates
- **Error Tracking**: Monitor OAuth2 flow failures
- **Performance Metrics**: Token exchange response times

## Implementation Details

### Frontend Implementation
```typescript
// OAuth2 initiation
const initiateOAuth2Login = async (provider: string) => {
  const params = new URLSearchParams({
    client_id: 'user-service-client',
    response_type: 'code',
    scope: 'openid email profile',
    redirect_uri: `${window.location.origin}/auth/callback`,
    state: generateRandomState()
  })

  const authUrl = `http://localhost:9191/realms/realm-service/protocol/openid-connect/auth?${params}`
  window.location.href = authUrl
}

// Callback handling
const handleCallback = async (code: string, state: string) => {
  const response = await fetch('/api/v1/auth/oauth2/callback', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      code,
      state,
      provider: 'google',
      redirectUri: `${window.location.origin}/auth/callback`
    })
  })

  const authData = await response.json()
  localStorage.setItem('authToken', authData.token)
}
```

### Backend Implementation
```java
// Token exchange service
@Service
public class OAuth2Service {

  @Value("${keycloak.credentials.secret}")
  private String clientSecret;

  public AuthResponse handleOAuth2Callback(OAuth2CallbackRequest request) {
    // 1. Exchange authorization code for tokens
    Map<String, Object> tokenResponse = exchangeCodeForTokens(request);

    // 2. Get user info from Keycloak
    Map<String, Object> userInfo = getUserInfoFromKeycloak(
      tokenResponse.get("access_token").toString()
    );

    // 3. Create or update user in database
    User user = createOrUpdateUser(userInfo, request.getProvider());

    // 4. Generate application JWT token
    String appToken = jwtUtil.generateToken(
      user.getId(), user.getUsername(), user.getEmail()
    );

    return AuthResponse.builder()
      .token(appToken)
      .username(user.getUsername())
      .email(user.getEmail())
      .build();
  }

  private Map<String, Object> exchangeCodeForTokens(OAuth2CallbackRequest request) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.setBasicAuth("user-service-client", clientSecret);

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", "authorization_code");
    body.add("code", request.getCode());
    body.add("redirect_uri", request.getRedirectUri());

    return restTemplate.exchange(
      keycloakTokenEndpoint,
      HttpMethod.POST,
      new HttpEntity<>(body, headers),
      Map.class
    ).getBody();
  }
}
```
