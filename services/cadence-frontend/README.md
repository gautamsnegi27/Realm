# Cadence Frontend

A modern React frontend application for the Realm microservices platform, featuring secure authentication with both basic auth and OAuth2 social login capabilities.

## Features

- 🔐 **Secure Authentication**: JWT-based authentication with OAuth2 social login (Google, GitHub)
- 🎨 **Modern UI**: Built with React, TypeScript, and Tailwind CSS
- 📱 **Responsive Design**: Mobile-first responsive design
- 🛡️ **Protected Routes**: Route-based authentication and authorization
- 🔄 **Real-time Feedback**: Toast notifications and loading states
- 🚀 **Performance**: Optimized build with code splitting and lazy loading

## Tech Stack

- **Frontend**: React 18, TypeScript, Vite
- **Styling**: Tailwind CSS
- **Routing**: React Router v6
- **Forms**: React Hook Form
- **HTTP Client**: Axios
- **Icons**: Lucide React
- **Notifications**: React Hot Toast

## Getting Started

### Prerequisites

- Node.js 18+ and npm 9+
- Backend services running (Gateway, User Service, Keycloak)

### Installation

1. **Clone and navigate to the frontend directory**:
   ```bash
   cd services/cadence-frontend
   ```

2. **Install dependencies**:
   ```bash
   npm install
   ```

3. **Set up environment variables**:
   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

4. **Start the development server**:
   ```bash
   npm run dev
   ```

   The application will be available at `http://localhost:3000`

### Available Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run preview` - Preview production build
- `npm run lint` - Run ESLint
- `npm run lint:fix` - Fix ESLint issues
- `npm run type-check` - Run TypeScript type checking

## Project Structure

```
src/
├── components/          # Reusable UI components
│   ├── LoadingSpinner.tsx
│   ├── Navbar.tsx
│   ├── OAuth2LoginButtons.tsx
│   └── ProtectedRoute.tsx
├── contexts/           # React contexts
│   └── AuthContext.tsx
├── pages/              # Page components
│   ├── DashboardPage.tsx
│   ├── LandingPage.tsx
│   ├── LoginPage.tsx
│   ├── OAuth2CallbackPage.tsx
│   ├── ProfilePage.tsx
│   └── SignupPage.tsx
├── services/           # API and business logic
│   ├── api.ts
│   └── auth.ts
├── types/              # TypeScript type definitions
│   └── auth.ts
├── utils/              # Utility functions
│   └── cn.ts
├── App.tsx             # Main app component
├── index.css           # Global styles
└── main.tsx            # App entry point
```

## Authentication Flow

### Basic Authentication

1. User enters credentials on login/signup page
2. Frontend sends request to `/api/v1/auth/login` or `/api/v1/auth/signup`
3. Backend validates credentials and returns JWT token
4. Frontend stores token and redirects to dashboard

### OAuth2 Social Login

1. User clicks social login button (Google/GitHub)
2. Frontend redirects to Keycloak authorization URL
3. User authenticates with social provider
4. Keycloak redirects back to `/auth/callback` with authorization code
5. Frontend exchanges code for tokens and sends user info to `/api/v1/auth/oauth2/login`
6. Backend creates/updates user and returns JWT token
7. Frontend stores token and redirects to dashboard

## API Integration

The frontend integrates with the following backend endpoints:

### Authentication Endpoints
- `POST /api/v1/auth/signup` - User registration
- `POST /api/v1/auth/login` - User login
- `GET /api/v1/auth/validate` - Token validation
- `POST /api/v1/auth/oauth2/login` - OAuth2 login
- `GET /api/v1/auth/oauth2/providers` - Get OAuth2 providers

### User Endpoints
- `GET /api/v1/user/profile` - Get user profile
- `PUT /api/v1/user/profile` - Update user profile

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `VITE_API_BASE_URL` | Backend API base URL | `http://localhost:8111/api/v1` |
| `VITE_GATEWAY_URL` | Gateway service URL | `http://localhost:8111` |
| `VITE_KEYCLOAK_URL` | Keycloak server URL | `http://localhost:9191` |
| `VITE_KEYCLOAK_REALM` | Keycloak realm name | `realm-service` |
| `VITE_KEYCLOAK_CLIENT_ID` | Keycloak client ID | `user-service-client` |

## Docker Deployment

### Build Docker Image

```bash
docker build -t cadence-frontend .
```

### Run Container

```bash
docker run -p 3000:80 cadence-frontend
```

### Docker Compose

The frontend is included in the main `docker-compose.yml` file:

```yaml
cadence-frontend:
  container_name: ms_cadence_frontend
  build:
    context: ./services/cadence-frontend
    dockerfile: Dockerfile
  ports:
    - "3000:80"
  networks:
    - microservices-net
  depends_on:
    - gateway
```

## Development Guidelines

### Code Style

- Use TypeScript for type safety
- Follow React best practices and hooks patterns
- Use functional components with hooks
- Implement proper error handling
- Add loading states for async operations

### Component Guidelines

- Keep components small and focused
- Use composition over inheritance
- Implement proper prop types
- Add proper accessibility attributes
- Use semantic HTML elements

### State Management

- Use React Context for global state (auth)
- Use local state for component-specific data
- Implement proper error boundaries
- Handle loading and error states

## Troubleshooting

### Common Issues

1. **CORS Errors**: Ensure the gateway service is running and CORS is properly configured
2. **OAuth2 Redirect Issues**: Check Keycloak client configuration and redirect URIs
3. **Token Expiration**: Implement token refresh logic or handle 401 responses
4. **Build Errors**: Check TypeScript types and dependencies

### Debug Mode

Enable debug mode by setting `VITE_LOG_LEVEL=debug` in your `.env` file.

## Contributing

1. Follow the existing code style and patterns
2. Add proper TypeScript types
3. Include error handling and loading states
4. Test authentication flows thoroughly
5. Update documentation for new features

## License

This project is part of the Realm microservices platform.
