import { apiService } from './api'
import { AuthResponse, LoginRequest, SignupRequest, User, OAuth2Provider } from '../types/auth'

export class AuthService {
  private static readonly TOKEN_KEY = 'authToken'
  private static readonly USER_KEY = 'user'

  // Token management
  static getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY)
  }

  static setToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token)
  }

  static removeToken(): void {
    localStorage.removeItem(this.TOKEN_KEY)
    localStorage.removeItem(this.USER_KEY)
  }

  static isAuthenticated(): boolean {
    const token = this.getToken()
    if (!token) return false

    try {
      // Basic JWT expiration check
      const payload = JSON.parse(atob(token.split('.')[1]))
      const currentTime = Date.now() / 1000
      return payload.exp > currentTime
    } catch {
      return false
    }
  }

  // User management
  static getUser(): User | null {
    const userStr = localStorage.getItem(this.USER_KEY)
    return userStr ? JSON.parse(userStr) : null
  }

  static setUser(user: User): void {
    localStorage.setItem(this.USER_KEY, JSON.stringify(user))
  }

  // Authentication methods
  static async login(credentials: LoginRequest): Promise<AuthResponse> {
    const response = await apiService.login(credentials)
    
    this.setToken(response.token)
    this.setUser({
      id: response.userId,
      username: response.username,
      email: response.email,
    })
    
    return response
  }

  static async signup(userData: SignupRequest): Promise<AuthResponse> {
    const response = await apiService.signup(userData)
    
    this.setToken(response.token)
    this.setUser({
      id: response.userId,
      username: response.username,
      email: response.email,
    })
    
    return response
  }

  static async logout(): Promise<void> {
    this.removeToken()
    window.location.href = '/'
  }

  static async validateToken(): Promise<boolean> {
    try {
      const response = await apiService.validateToken()
      return response.valid
    } catch {
      this.removeToken()
      return false
    }
  }

  // OAuth2 methods
  static getOAuth2Providers(): OAuth2Provider[] {
    return [
      {
        name: 'google',
        displayName: 'Google',
        icon: '🔍',
        color: 'bg-red-500 hover:bg-red-600'
      },
      {
        name: 'github',
        displayName: 'GitHub',
        icon: '🐙',
        color: 'bg-gray-800 hover:bg-gray-900'
      }
    ]
  }

  static async initiateOAuth2Login(provider: string): Promise<void> {
    try {
      const providersResponse = await apiService.getOAuth2Providers()
      const keycloakUrl = providersResponse.keycloakAuthUrl
      
      // Construct OAuth2 URL for the specific provider
      const clientId = 'user-service-client'
      const redirectUri = `${window.location.origin}/auth/callback`
      const scope = 'openid email profile'
      const state = `${provider}_${Date.now()}`
      
      const authUrl = `${keycloakUrl}?` +
        `client_id=${clientId}&` +
        `redirect_uri=${encodeURIComponent(redirectUri)}&` +
        `scope=${encodeURIComponent(scope)}&` +
        `response_type=code&` +
        `state=${state}&` +
        `kc_idp_hint=${provider}`
      
      // Store provider in session for callback handling
      sessionStorage.setItem('oauth2_provider', provider)
      sessionStorage.setItem('oauth2_state', state)
      
      // Redirect to OAuth2 provider
      window.location.href = authUrl
    } catch (error) {
      console.error('Failed to initiate OAuth2 login:', error)
      throw error
    }
  }
}

export default AuthService
