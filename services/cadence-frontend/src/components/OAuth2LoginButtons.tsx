import React, { useState } from 'react'
import { AuthService } from '../services/auth'
import LoadingSpinner from './LoadingSpinner'
import toast from 'react-hot-toast'

const OAuth2LoginButtons: React.FC = () => {
  const [loading, setLoading] = useState<string | null>(null)

  const handleOAuth2Login = async (provider: string) => {
    try {
      setLoading(provider)
      await AuthService.initiateOAuth2Login(provider)
    } catch (error) {
      console.error(`${provider} login error:`, error)
      toast.error(`Failed to initiate ${provider} login`)
      setLoading(null)
    }
  }

  const providers = AuthService.getOAuth2Providers()

  return (
    <div className="space-y-3">
      {providers.map((provider) => (
        <button
          key={provider.name}
          onClick={() => handleOAuth2Login(provider.name)}
          disabled={loading !== null}
          className={`w-full flex items-center justify-center space-x-3 px-4 py-3 border border-gray-300 rounded-lg text-white font-medium transition-colors ${provider.color} disabled:opacity-50 disabled:cursor-not-allowed`}
        >
          {loading === provider.name ? (
            <LoadingSpinner size="sm" />
          ) : (
            <>
              <span className="text-lg">{provider.icon}</span>
              <span>Continue with {provider.displayName}</span>
            </>
          )}
        </button>
      ))}
    </div>
  )
}

export default OAuth2LoginButtons
