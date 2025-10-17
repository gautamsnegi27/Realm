export interface User {
  id: string
  username: string
  email: string
  firstName?: string
  lastName?: string
  isVerified?: boolean
  createdAt?: string
  lastLoginAt?: string
}

export interface AuthResponse {
  token: string
  tokenType: string
  expiresIn: number
  username: string
  email: string
  userId: string
}

export interface LoginRequest {
  usernameOrEmail: string
  password: string
}

export interface SignupRequest {
  username: string
  email: string
  password: string
  firstName?: string
  lastName?: string
}

export interface OAuth2LoginRequest {
  provider: string
  providerId: string
  email: string
  username: string
  firstName?: string
  lastName?: string
  accessToken: string
}

export interface OAuth2Provider {
  name: string
  displayName: string
  icon: string
  color: string
}

export interface OAuth2ProvidersResponse {
  providers: string[]
  keycloakAuthUrl: string
}

export interface ApiError {
  error: string
  message?: string
  status?: number
}
