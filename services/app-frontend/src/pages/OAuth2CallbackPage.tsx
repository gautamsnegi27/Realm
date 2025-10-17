import React, { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import LoadingSpinner from '../components/LoadingSpinner'
import { apiService } from '../services/api'
import { AuthService } from '../services/auth'
import toast from 'react-hot-toast'

const OAuth2CallbackPage: React.FC = () => {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const { login } = useAuth()
  const [status, setStatus] = useState<'processing' | 'success' | 'error'>('processing')
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    const handleOAuth2Callback = async () => {
      const code = searchParams.get('code')
      if (!code) return

      // Check if this code has already been processed
      const processedKey = `oauth2_processed_${code}`
      if (sessionStorage.getItem(processedKey)) {
        console.log('OAuth2 callback already processed for this code, skipping...')
        setStatus('success')
        setTimeout(() => {
          navigate('/dashboard', { replace: true })
        }, 1000)
        return
      }

      // Mark this code as being processed immediately
      sessionStorage.setItem(processedKey, 'true')
      try {
        const code = searchParams.get('code')
        const state = searchParams.get('state')
        const error = searchParams.get('error')

        if (error) {
          throw new Error(`OAuth2 error: ${error}`)
        }

        if (!code || !state) {
          throw new Error('Missing authorization code or state parameter')
        }

        // Verify state parameter
        const storedState = sessionStorage.getItem('oauth2_state')
        const storedProvider = sessionStorage.getItem('oauth2_provider')
        
        if (!storedState || !storedProvider || state !== storedState) {
          throw new Error('Invalid state parameter')
        }

        // Extract provider from state
        const provider = storedProvider

        // Send authorization code to backend for secure token exchange
        const requestBody = {
          code,
          state,
          provider,
          redirectUri: `${window.location.origin}/auth/callback`
        }

        const authResponse = await fetch('/api/v1/auth/oauth2/callback', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(requestBody),
        })

        if (!authResponse.ok) {
          const errorText = await authResponse.text()
          console.error('OAuth2 callback failed:', errorText)
          throw new Error('OAuth2 authentication failed')
        }

        const authData = await authResponse.json()
        
        // Store tokens and user info
        AuthService.setToken(authData.token)
        AuthService.setUser({
          id: authData.userId,
          username: authData.username,
          email: authData.email,
        })

        // Clean up session storage
        sessionStorage.removeItem('oauth2_state')
        sessionStorage.removeItem('oauth2_provider')

        setStatus('success')
        toast.success(`Welcome! You've been signed in with ${provider}.`)
        
        // Redirect to landing page
        setTimeout(() => {
          navigate('/', { replace: true })
        }, 1000)

      } catch (error) {
        console.error('OAuth2 callback error:', error)
        setStatus('error')
        setErrorMessage(error instanceof Error ? error.message : 'OAuth2 authentication failed')

        // Clean up session storage
        sessionStorage.removeItem('oauth2_state')
        sessionStorage.removeItem('oauth2_provider')

        // Redirect to login after delay
        setTimeout(() => {
          navigate('/login', { replace: true })
        }, 3000)
      }
    }

    handleOAuth2Callback()
  }, [searchParams, navigate, login])



  return (
    <div className="min-h-screen bg-gray-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10">
          <div className="text-center">
            {status === 'processing' && (
              <>
                <LoadingSpinner size="lg" className="mb-4" />
                <h2 className="text-xl font-semibold text-gray-900 mb-2">
                  Completing sign in...
                </h2>
                <p className="text-gray-600">
                  Please wait while we complete your authentication.
                </p>
              </>
            )}

            {status === 'success' && (
              <>
                <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
                  <svg className="w-8 h-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                </div>
                <h2 className="text-xl font-semibold text-gray-900 mb-2">
                  Sign in successful!
                </h2>
                <p className="text-gray-600">
                  Redirecting you to your dashboard...
                </p>
              </>
            )}

            {status === 'error' && (
              <>
                <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
                  <svg className="w-8 h-8 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </div>
                <h2 className="text-xl font-semibold text-gray-900 mb-2">
                  Authentication failed
                </h2>
                <p className="text-gray-600 mb-4">
                  {errorMessage}
                </p>
                <p className="text-sm text-gray-500">
                  Redirecting you back to the login page...
                </p>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

export default OAuth2CallbackPage
