import React, { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useAuth } from '../contexts/AuthContext'
import Navbar from '../components/Navbar'
import LoadingSpinner from '../components/LoadingSpinner'
import { User, Mail, Calendar, Shield, Edit3, Save, X } from 'lucide-react'
import toast from 'react-hot-toast'

interface ProfileFormData {
  firstName?: string
  lastName?: string
  email: string
  username: string
}

const ProfilePage: React.FC = () => {
  const { user } = useAuth()
  const [isEditing, setIsEditing] = useState(false)
  const [isLoading, setIsLoading] = useState(false)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<ProfileFormData>({
    defaultValues: {
      firstName: user?.firstName || '',
      lastName: user?.lastName || '',
      email: user?.email || '',
      username: user?.username || '',
    },
  })

  const onSubmit = async (data: ProfileFormData) => {
    try {
      setIsLoading(true)
      // TODO: Implement profile update API call
      console.log('Updating profile:', data)
      
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 1000))
      
      toast.success('Profile updated successfully!')
      setIsEditing(false)
    } catch (error) {
      console.error('Profile update error:', error)
      toast.error('Failed to update profile')
    } finally {
      setIsLoading(false)
    }
  }

  const handleCancel = () => {
    reset()
    setIsEditing(false)
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Profile Settings</h1>
          <p className="text-gray-600 mt-2">
            Manage your account information and preferences.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Profile Card */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-lg shadow p-6">
              <div className="text-center">
                <div className="w-24 h-24 bg-primary-100 rounded-full flex items-center justify-center mx-auto mb-4">
                  <User className="w-12 h-12 text-primary-600" />
                </div>
                <h2 className="text-xl font-semibold text-gray-900">
                  {user?.firstName && user?.lastName 
                    ? `${user.firstName} ${user.lastName}`
                    : user?.username
                  }
                </h2>
                <p className="text-gray-600">{user?.email}</p>
                <div className="mt-4 flex items-center justify-center space-x-2">
                  <Shield className="w-4 h-4 text-green-500" />
                  <span className="text-sm text-green-600">Verified Account</span>
                </div>
              </div>
              
              <div className="mt-6 space-y-4">
                <div className="flex items-center space-x-3 text-sm text-gray-600">
                  <Mail className="w-4 h-4" />
                  <span>Email verified</span>
                </div>
                <div className="flex items-center space-x-3 text-sm text-gray-600">
                  <Calendar className="w-4 h-4" />
                  <span>
                    Member since {user?.createdAt 
                      ? new Date(user.createdAt).toLocaleDateString()
                      : 'Recently'
                    }
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Profile Form */}
          <div className="lg:col-span-2">
            <div className="bg-white rounded-lg shadow">
              <div className="p-6 border-b border-gray-200">
                <div className="flex items-center justify-between">
                  <h3 className="text-lg font-semibold text-gray-900">
                    Personal Information
                  </h3>
                  {!isEditing ? (
                    <button
                      onClick={() => setIsEditing(true)}
                      className="btn-outline btn-sm inline-flex items-center space-x-2"
                    >
                      <Edit3 size={16} />
                      <span>Edit</span>
                    </button>
                  ) : (
                    <div className="flex space-x-2">
                      <button
                        onClick={handleCancel}
                        className="btn-ghost btn-sm inline-flex items-center space-x-2"
                      >
                        <X size={16} />
                        <span>Cancel</span>
                      </button>
                      <button
                        onClick={handleSubmit(onSubmit)}
                        disabled={!isDirty || isLoading}
                        className="btn-primary btn-sm inline-flex items-center space-x-2"
                      >
                        {isLoading ? (
                          <LoadingSpinner size="sm" />
                        ) : (
                          <>
                            <Save size={16} />
                            <span>Save</span>
                          </>
                        )}
                      </button>
                    </div>
                  )}
                </div>
              </div>

              <form onSubmit={handleSubmit(onSubmit)} className="p-6">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div>
                    <label htmlFor="firstName" className="block text-sm font-medium text-gray-700">
                      First Name
                    </label>
                    <div className="mt-1">
                      <input
                        {...register('firstName')}
                        type="text"
                        disabled={!isEditing}
                        className="input disabled:bg-gray-50 disabled:text-gray-500"
                        placeholder="Enter your first name"
                      />
                    </div>
                  </div>

                  <div>
                    <label htmlFor="lastName" className="block text-sm font-medium text-gray-700">
                      Last Name
                    </label>
                    <div className="mt-1">
                      <input
                        {...register('lastName')}
                        type="text"
                        disabled={!isEditing}
                        className="input disabled:bg-gray-50 disabled:text-gray-500"
                        placeholder="Enter your last name"
                      />
                    </div>
                  </div>

                  <div>
                    <label htmlFor="username" className="block text-sm font-medium text-gray-700">
                      Username
                    </label>
                    <div className="mt-1">
                      <input
                        {...register('username', {
                          required: 'Username is required',
                          minLength: {
                            value: 3,
                            message: 'Username must be at least 3 characters',
                          },
                        })}
                        type="text"
                        disabled={!isEditing}
                        className="input disabled:bg-gray-50 disabled:text-gray-500"
                      />
                      {errors.username && (
                        <p className="mt-1 text-sm text-red-600">{errors.username.message}</p>
                      )}
                    </div>
                  </div>

                  <div>
                    <label htmlFor="email" className="block text-sm font-medium text-gray-700">
                      Email Address
                    </label>
                    <div className="mt-1">
                      <input
                        {...register('email', {
                          required: 'Email is required',
                          pattern: {
                            value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
                            message: 'Invalid email address',
                          },
                        })}
                        type="email"
                        disabled={!isEditing}
                        className="input disabled:bg-gray-50 disabled:text-gray-500"
                      />
                      {errors.email && (
                        <p className="mt-1 text-sm text-red-600">{errors.email.message}</p>
                      )}
                    </div>
                  </div>
                </div>
              </form>
            </div>

            {/* Security Section */}
            <div className="bg-white rounded-lg shadow mt-6">
              <div className="p-6 border-b border-gray-200">
                <h3 className="text-lg font-semibold text-gray-900">Security</h3>
              </div>
              <div className="p-6">
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <h4 className="text-sm font-medium text-gray-900">Password</h4>
                      <p className="text-sm text-gray-600">Last changed 30 days ago</p>
                    </div>
                    <button className="btn-outline btn-sm">
                      Change Password
                    </button>
                  </div>
                  
                  <div className="flex items-center justify-between">
                    <div>
                      <h4 className="text-sm font-medium text-gray-900">Two-Factor Authentication</h4>
                      <p className="text-sm text-gray-600">Add an extra layer of security</p>
                    </div>
                    <button className="btn-outline btn-sm">
                      Enable 2FA
                    </button>
                  </div>
                  
                  <div className="flex items-center justify-between">
                    <div>
                      <h4 className="text-sm font-medium text-gray-900">Active Sessions</h4>
                      <p className="text-sm text-gray-600">Manage your active sessions</p>
                    </div>
                    <button className="btn-outline btn-sm">
                      View Sessions
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default ProfilePage
