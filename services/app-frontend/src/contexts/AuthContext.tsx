import React, { createContext, useContext, useEffect, useState, ReactNode } from 'react'
import { User, LoginRequest, SignupRequest } from '../types/auth'
import { AuthService } from '../services/auth'
import toast from 'react-hot-toast'

interface AuthContextType {
  user: User | null
  loading: boolean
  isAuthenticated: boolean
  login: (credentials: LoginRequest) => Promise<void>
  signup: (userData: SignupRequest) => Promise<void>
  logout: () => Promise<void>
  refreshAuth: () => Promise<void>
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

interface AuthProviderProps {
  children: ReactNode
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  const isAuthenticated = !!user && AuthService.isAuthenticated()

  // Initialize auth state on mount
  useEffect(() => {
    const initializeAuth = async () => {
      try {
        const token = AuthService.getToken()
        const storedUser = AuthService.getUser()

        if (token && storedUser) {
          // Validate token with backend
          const isValid = await AuthService.validateToken()
          if (isValid) {
            setUser(storedUser)
          } else {
            AuthService.removeToken()
          }
        }
      } catch (error) {
        console.error('Auth initialization error:', error)
        AuthService.removeToken()
      } finally {
        setLoading(false)
      }
    }

    initializeAuth()
  }, [])

  const login = async (credentials: LoginRequest) => {
    try {
      setLoading(true)
      const response = await AuthService.login(credentials)
      
      const newUser: User = {
        id: response.userId,
        username: response.username,
        email: response.email,
      }
      
      setUser(newUser)
      toast.success(`Welcome back, ${response.username}!`)
    } catch (error) {
      console.error('Login error:', error)
      throw error
    } finally {
      setLoading(false)
    }
  }

  const signup = async (userData: SignupRequest) => {
    try {
      setLoading(true)
      const response = await AuthService.signup(userData)
      
      const newUser: User = {
        id: response.userId,
        username: response.username,
        email: response.email,
      }
      
      setUser(newUser)
      toast.success(`Welcome to Realm, ${response.username}!`)
    } catch (error) {
      console.error('Signup error:', error)
      throw error
    } finally {
      setLoading(false)
    }
  }

  const logout = async () => {
    try {
      setLoading(true)
      await AuthService.logout()
      setUser(null)
      toast.success('Logged out successfully')
    } catch (error) {
      console.error('Logout error:', error)
    } finally {
      setLoading(false)
    }
  }

  const refreshAuth = async () => {
    try {
      const isValid = await AuthService.validateToken()
      if (!isValid) {
        setUser(null)
        AuthService.removeToken()
      }
    } catch (error) {
      console.error('Auth refresh error:', error)
      setUser(null)
      AuthService.removeToken()
    }
  }

  const value: AuthContextType = {
    user,
    loading,
    isAuthenticated,
    login,
    signup,
    logout,
    refreshAuth,
  }

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}

export default AuthContext
