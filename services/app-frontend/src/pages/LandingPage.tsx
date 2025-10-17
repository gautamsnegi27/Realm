import React from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import Navbar from '../components/Navbar'
import { ArrowRight, Shield, Zap, Globe, Users, Code, Database } from 'lucide-react'

const LandingPage: React.FC = () => {
  const { isAuthenticated } = useAuth()

  const features = [
    {
      icon: <Shield className="w-6 h-6" />,
      title: 'Secure Authentication',
      description: 'JWT-based authentication with OAuth2 social login support for Google and GitHub.'
    },
    {
      icon: <Zap className="w-6 h-6" />,
      title: 'High Performance',
      description: 'Built with Spring Boot microservices architecture for scalability and performance.'
    },
    {
      icon: <Globe className="w-6 h-6" />,
      title: 'API Gateway',
      description: 'Centralized API gateway with rate limiting, load balancing, and security.'
    },
    {
      icon: <Users className="w-6 h-6" />,
      title: 'User Management',
      description: 'Complete user profile management with role-based access control.'
    },
    {
      icon: <Code className="w-6 h-6" />,
      title: 'Modern Stack',
      description: 'React frontend with TypeScript, Spring Boot backend, and MongoDB database.'
    },
    {
      icon: <Database className="w-6 h-6" />,
      title: 'Service Discovery',
      description: 'Eureka service discovery with configuration management and monitoring.'
    }
  ]

  return (
    <div className="min-h-screen bg-white">
      <Navbar />
      
      {/* Hero Section */}
      <section className="relative overflow-hidden bg-gradient-to-br from-primary-50 to-secondary-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-24">
          <div className="text-center">
            <h1 className="text-4xl md:text-6xl font-bold text-gray-900 mb-6">
              Welcome to{' '}
              <span className="text-primary-600">Realm</span>
            </h1>
            <p className="text-xl text-gray-600 mb-8 max-w-3xl mx-auto">
              A modern microservices platform with secure authentication, 
              API gateway, and scalable architecture. Built for developers, by developers.
            </p>
            
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              {isAuthenticated ? (
                <Link
                  to="/dashboard"
                  className="btn-primary btn-lg inline-flex items-center space-x-2"
                >
                  <span>Go to Dashboard</span>
                  <ArrowRight size={20} />
                </Link>
              ) : (
                <>
                  <Link
                    to="/signup"
                    className="btn-primary btn-lg inline-flex items-center space-x-2"
                  >
                    <span>Get Started</span>
                    <ArrowRight size={20} />
                  </Link>
                  <Link
                    to="/login"
                    className="btn-outline btn-lg"
                  >
                    Sign In
                  </Link>
                </>
              )}
            </div>
          </div>
        </div>
        
        {/* Background decoration */}
        <div className="absolute inset-0 -z-10">
          <div className="absolute top-0 left-1/2 transform -translate-x-1/2 w-96 h-96 bg-primary-200 rounded-full opacity-20 blur-3xl"></div>
          <div className="absolute bottom-0 right-1/4 w-64 h-64 bg-secondary-200 rounded-full opacity-20 blur-3xl"></div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-24 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="text-3xl md:text-4xl font-bold text-gray-900 mb-4">
              Powerful Features
            </h2>
            <p className="text-lg text-gray-600 max-w-2xl mx-auto">
              Everything you need to build modern, scalable applications with enterprise-grade security.
            </p>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {features.map((feature, index) => (
              <div
                key={index}
                className="p-6 rounded-lg border border-gray-200 hover:border-primary-300 hover:shadow-lg transition-all duration-300"
              >
                <div className="w-12 h-12 bg-primary-100 rounded-lg flex items-center justify-center text-primary-600 mb-4">
                  {feature.icon}
                </div>
                <h3 className="text-xl font-semibold text-gray-900 mb-2">
                  {feature.title}
                </h3>
                <p className="text-gray-600">
                  {feature.description}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-24 bg-primary-600">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <h2 className="text-3xl md:text-4xl font-bold text-white mb-4">
            Ready to get started?
          </h2>
          <p className="text-xl text-primary-100 mb-8 max-w-2xl mx-auto">
            Join thousands of developers building amazing applications with Realm.
          </p>
          
          {!isAuthenticated && (
            <Link
              to="/signup"
              className="inline-flex items-center space-x-2 bg-white text-primary-600 hover:bg-gray-50 px-8 py-3 rounded-lg font-semibold transition-colors"
            >
              <span>Create your account</span>
              <ArrowRight size={20} />
            </Link>
          )}
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-gray-900 text-white py-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center">
            <div className="flex items-center justify-center space-x-2 mb-4">
              <div className="w-8 h-8 bg-primary-600 rounded-lg flex items-center justify-center">
                <span className="text-white font-bold text-lg">R</span>
              </div>
              <span className="text-xl font-bold">Realm</span>
            </div>
            <p className="text-gray-400">
              © 2024 Realm. Built with Spring Boot, React, and modern microservices architecture.
            </p>
          </div>
        </div>
      </footer>
    </div>
  )
}

export default LandingPage
