import React from 'react'
import { useAuth } from '../contexts/AuthContext'
import Navbar from '../components/Navbar'
import { User, Shield, Activity, Settings, Bell, Calendar } from 'lucide-react'

const DashboardPage: React.FC = () => {
  const { user } = useAuth()

  const stats = [
    {
      name: 'Profile Completion',
      value: '85%',
      icon: <User className="w-6 h-6" />,
      color: 'bg-blue-500',
    },
    {
      name: 'Security Score',
      value: '9.2/10',
      icon: <Shield className="w-6 h-6" />,
      color: 'bg-green-500',
    },
    {
      name: 'Active Sessions',
      value: '3',
      icon: <Activity className="w-6 h-6" />,
      color: 'bg-yellow-500',
    },
    {
      name: 'Last Login',
      value: 'Today',
      icon: <Calendar className="w-6 h-6" />,
      color: 'bg-purple-500',
    },
  ]

  const recentActivity = [
    {
      action: 'Profile updated',
      time: '2 hours ago',
      icon: <User className="w-4 h-4" />,
    },
    {
      action: 'Password changed',
      time: '1 day ago',
      icon: <Shield className="w-4 h-4" />,
    },
    {
      action: 'New login from Chrome',
      time: '2 days ago',
      icon: <Activity className="w-4 h-4" />,
    },
  ]

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">
            Welcome back, {user?.username}!
          </h1>
          <p className="text-gray-600 mt-2">
            Here's what's happening with your account today.
          </p>
        </div>

        {/* Stats Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          {stats.map((stat, index) => (
            <div key={index} className="bg-white rounded-lg shadow p-6">
              <div className="flex items-center">
                <div className={`${stat.color} text-white p-3 rounded-lg`}>
                  {stat.icon}
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">{stat.name}</p>
                  <p className="text-2xl font-bold text-gray-900">{stat.value}</p>
                </div>
              </div>
            </div>
          ))}
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Quick Actions */}
          <div className="lg:col-span-2">
            <div className="bg-white rounded-lg shadow">
              <div className="p-6 border-b border-gray-200">
                <h2 className="text-lg font-semibold text-gray-900">Quick Actions</h2>
              </div>
              <div className="p-6">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <button className="flex items-center space-x-3 p-4 border border-gray-200 rounded-lg hover:border-primary-300 hover:bg-primary-50 transition-colors">
                    <User className="w-5 h-5 text-primary-600" />
                    <span className="font-medium text-gray-900">Update Profile</span>
                  </button>
                  
                  <button className="flex items-center space-x-3 p-4 border border-gray-200 rounded-lg hover:border-primary-300 hover:bg-primary-50 transition-colors">
                    <Shield className="w-5 h-5 text-primary-600" />
                    <span className="font-medium text-gray-900">Security Settings</span>
                  </button>
                  
                  <button className="flex items-center space-x-3 p-4 border border-gray-200 rounded-lg hover:border-primary-300 hover:bg-primary-50 transition-colors">
                    <Bell className="w-5 h-5 text-primary-600" />
                    <span className="font-medium text-gray-900">Notifications</span>
                  </button>
                  
                  <button className="flex items-center space-x-3 p-4 border border-gray-200 rounded-lg hover:border-primary-300 hover:bg-primary-50 transition-colors">
                    <Settings className="w-5 h-5 text-primary-600" />
                    <span className="font-medium text-gray-900">Account Settings</span>
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* Recent Activity */}
          <div className="bg-white rounded-lg shadow">
            <div className="p-6 border-b border-gray-200">
              <h2 className="text-lg font-semibold text-gray-900">Recent Activity</h2>
            </div>
            <div className="p-6">
              <div className="space-y-4">
                {recentActivity.map((activity, index) => (
                  <div key={index} className="flex items-center space-x-3">
                    <div className="flex-shrink-0 w-8 h-8 bg-gray-100 rounded-full flex items-center justify-center">
                      {activity.icon}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-gray-900">
                        {activity.action}
                      </p>
                      <p className="text-sm text-gray-500">{activity.time}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Account Overview */}
        <div className="mt-8 bg-white rounded-lg shadow">
          <div className="p-6 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-900">Account Overview</h2>
          </div>
          <div className="p-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <h3 className="text-sm font-medium text-gray-900 mb-3">Account Information</h3>
                <dl className="space-y-2">
                  <div className="flex justify-between">
                    <dt className="text-sm text-gray-600">Username:</dt>
                    <dd className="text-sm font-medium text-gray-900">{user?.username}</dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-sm text-gray-600">Email:</dt>
                    <dd className="text-sm font-medium text-gray-900">{user?.email}</dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-sm text-gray-600">Account Type:</dt>
                    <dd className="text-sm font-medium text-gray-900">Standard</dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-sm text-gray-600">Member Since:</dt>
                    <dd className="text-sm font-medium text-gray-900">
                      {user?.createdAt ? new Date(user.createdAt).toLocaleDateString() : 'Recently'}
                    </dd>
                  </div>
                </dl>
              </div>
              
              <div>
                <h3 className="text-sm font-medium text-gray-900 mb-3">Security Status</h3>
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Two-Factor Authentication</span>
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800">
                      Not Enabled
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Email Verification</span>
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                      Verified
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Password Strength</span>
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                      Strong
                    </span>
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

export default DashboardPage
