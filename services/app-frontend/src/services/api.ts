import axios, { AxiosInstance, AxiosError } from 'axios'
import toast from 'react-hot-toast'
import { AuthResponse, LoginRequest, SignupRequest, OAuth2ProvidersResponse, ApiError } from '../types/auth'

class ApiService {
  private api: AxiosInstance

  constructor() {
    this.api = axios.create({
      baseURL: '/api/v1', // Proxied through Vite to gateway
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
    })

    // Request interceptor to add auth token
    this.api.interceptors.request.use(
      (config) => {
        const token = localStorage.getItem('authToken')
        if (token) {
          config.headers.Authorization = `Bearer ${token}`
        }
        return config
      },
      (error) => Promise.reject(error)
    )

    // Response interceptor for error handling
    this.api.interceptors.response.use(
      (response) => response,
      (error: AxiosError<ApiError>) => {
        const message = error.response?.data?.error || error.message || 'An error occurred'
        
        if (error.response?.status === 401) {
          // Unauthorized - clear token and redirect to login
          localStorage.removeItem('authToken')
          localStorage.removeItem('user')
          window.location.href = '/login'
          toast.error('Session expired. Please login again.')
        } else if (error.response?.status >= 500) {
          toast.error('Server error. Please try again later.')
        } else {
          toast.error(message)
        }
        
        return Promise.reject(error)
      }
    )
  }

  // Authentication endpoints
  async login(credentials: LoginRequest): Promise<AuthResponse> {
    const response = await this.api.post<AuthResponse>('/auth/login', credentials)
    return response.data
  }

  async signup(userData: SignupRequest): Promise<AuthResponse> {
    const response = await this.api.post<AuthResponse>('/auth/signup', userData)
    return response.data
  }

  async validateToken(): Promise<{ valid: boolean; username?: string }> {
    const response = await this.api.get('/auth/validate')
    return response.data
  }

  // OAuth2 endpoints
  async getOAuth2Providers(): Promise<OAuth2ProvidersResponse> {
    const response = await this.api.get<OAuth2ProvidersResponse>('/auth/oauth2/providers')
    return response.data
  }

  // User endpoints
  async getCurrentUser(): Promise<any> {
    const response = await this.api.get('/user/profile')
    return response.data
  }

  async updateProfile(userData: any): Promise<any> {
    const response = await this.api.put('/user/profile', userData)
    return response.data
  }
}

export const apiService = new ApiService()
export default apiService
