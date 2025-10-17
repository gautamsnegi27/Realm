# Keycloak Setup Instructions

This directory contains the Keycloak realm configuration for the Realm project.

## Quick Start

### 1. Start Keycloak

```bash
docker-compose up -d keycloak-postgres keycloak
```

Wait for Keycloak to start (check logs: `docker logs -f ms_keycloak`)

### 2. Access Keycloak Admin Console

- URL: http://localhost:9191
- Username: `admin`
- Password: `test`

### 3. Import Realm Configuration

**Option A: Via UI (Recommended for first-time setup)**

1. Log in to Keycloak Admin Console
2. Click on the realm dropdown (top-left, says "master")
3. Click "Create Realm"
4. Click "Browse" and select `keycloak/realm-service-export.json`
5. Click "Create"

**Option B: Via CLI (for automation)**

```bash
docker exec -it ms_keycloak /opt/keycloak/bin/kc.sh import \
  --file /tmp/realm-service-export.json \
  --override true
```

## Social Login Configuration

After importing the realm, you need to configure the identity providers with your OAuth2 credentials:

### Google OAuth2 Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable "Google+ API"
4. Create OAuth 2.0 credentials:
   - Application type: Web application
   - Authorized redirect URIs: `http://localhost:9191/realms/realm-service/broker/google/endpoint`
5. Copy Client ID and Client Secret

In Keycloak:
1. Navigate to: **Identity Providers** → **google**
2. Update:
   - Client ID: `<Your Google Client ID>`
   - Client Secret: `<Your Google Client Secret>`
3. Save

### GitHub OAuth2 Setup

1. Go to [GitHub Developer Settings](https://github.com/settings/developers)
2. Click "New OAuth App"
3. Fill in:
   - Application name: `Realm Service`
   - Homepage URL: `http://localhost:8112`
   - Authorization callback URL: `http://localhost:9191/realms/realm-service/broker/github/endpoint`
4. Click "Register application"
5. Generate a new client secret
6. Copy Client ID and Client Secret

In Keycloak:
1. Navigate to: **Identity Providers** → **github**
2. Update:
   - Client ID: `<Your GitHub Client ID>`
   - Client Secret: `<Your GitHub Client Secret>`
3. Save

## Environment Variables

For production, use environment variables instead of hardcoded values:

### Local Development (.env or docker-compose.yml)

```env
# Keycloak
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=test

# Google OAuth2
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# GitHub OAuth2
GITHUB_CLIENT_ID=your-github-client-id
GITHUB_CLIENT_SECRET=your-github-client-secret
```

### Production (AWS Secrets Manager)

Store these as secrets in AWS Secrets Manager and reference them in your ECS task definitions.

## Testing Social Login

### Via Keycloak Hosted UI

1. Navigate to: http://localhost:9191/realms/realm-service/account
2. You'll see "Sign in with Google" and "Sign in with GitHub" buttons
3. Click one to test the flow

### Via Your Application

Your frontend should redirect users to:
```
http://localhost:9191/realms/realm-service/protocol/openid-connect/auth?client_id=user-service-client&redirect_uri=http://localhost:3000/callback&response_type=code&scope=openid%20profile%20email
```

## Adding More Identity Providers

Keycloak supports many identity providers out of the box:
- Facebook
- Twitter
- LinkedIn
- Microsoft
- Apple
- Generic OpenID Connect
- Generic SAML 2.0

To add a new provider:
1. In Keycloak Admin Console, go to **Identity Providers**
2. Click "Add provider"
3. Select the provider type
4. Fill in the required configuration
5. Save

## Keycloak Admin CLI

Useful commands for automation:

### Export realm
```bash
docker exec ms_keycloak /opt/keycloak/bin/kc.sh export \
  --file /tmp/realm-export.json \
  --realm realm-service
```

### Create user via CLI
```bash
docker exec ms_keycloak /opt/keycloak/bin/kcadm.sh create users \
  -r realm-service \
  -s username=testuser \
  -s email=test@example.com \
  -s enabled=true
```

### Reset admin password
```bash
docker exec ms_keycloak /opt/keycloak/bin/kc.sh \
  export --users realm_file \
  --realm master
```

## Troubleshooting

### Keycloak won't start
- Check Docker logs: `docker logs ms_keycloak`
- Ensure PostgreSQL is running: `docker ps | grep keycloak-postgres`
- Verify port 9191 is not in use: `netstat -an | grep 9191`

### Social login redirect issues
- Verify redirect URIs match exactly in both provider and Keycloak
- Check browser console for CORS errors
- Ensure `KC_HOSTNAME_STRICT=false` in docker-compose.yml for local development

### Token validation errors
- Check JWK Set URI is accessible: http://localhost:9191/realms/realm-service/protocol/openid-connect/certs
- Verify token hasn't expired
- Check Gateway logs for detailed error messages

## Security Best Practices

1. **Change default admin password** in production
2. **Use HTTPS** for all Keycloak endpoints in production
3. **Rotate client secrets** regularly
4. **Enable MFA** for admin accounts
5. **Limit token lifespans** based on your security requirements
6. **Use separate realms** for different environments (dev, staging, prod)
7. **Enable audit logging** for compliance
8. **Regularly update** Keycloak to latest version

## References

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Social Identity Providers](https://www.keycloak.org/docs/latest/server_admin/#_identity_broker)
- [Securing Applications](https://www.keycloak.org/docs/latest/securing_apps/)



