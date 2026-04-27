import { Navigate, Route, Routes } from 'react-router-dom'
import { AnimatePresence, motion } from 'framer-motion'
import { useLocation } from 'react-router-dom'
import AuthLayout from './components/layout/AuthLayout.jsx'
import ProtectedRoute from './components/routing/ProtectedRoute.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import BookDetailPage from './pages/BookDetailPage.jsx'
import AddBookPage from './pages/AddBookPage.jsx'
import WriteReviewPage from './pages/WriteReviewPage.jsx'
import UserProfilePage from './pages/UserProfilePage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'
import SearchResultsPage from './pages/SearchResultsPage.jsx'
import ActivityFeedPage from './pages/ActivityFeedPage.jsx'
import { useAuth } from './hooks/useAuth.js'

function App() {
  const { isAuthenticated } = useAuth()
  const location = useLocation()
  const MotionDiv = motion.div

  return (
    <AnimatePresence mode="wait">
      <MotionDiv
        key={location.pathname + location.search}
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: -8 }}
        transition={{ duration: 0.3, ease: 'easeOut' }}
      >
        <Routes location={location}>
          <Route
            path="/"
            element={<Navigate to={isAuthenticated ? '/dashboard' : '/auth/login'} replace />}
          />
          <Route path="/auth" element={<AuthLayout />}>
            <Route path="login" element={<LoginPage />} />
            <Route path="register" element={<RegisterPage />} />
          </Route>
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <DashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/search"
            element={
              <ProtectedRoute>
                <SearchResultsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/feed"
            element={
              <ProtectedRoute>
                <ActivityFeedPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/books/:id"
            element={
              <ProtectedRoute>
                <BookDetailPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/books/new"
            element={
              <ProtectedRoute>
                <AddBookPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/books/:id/review/new"
            element={
              <ProtectedRoute>
                <WriteReviewPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/profile"
            element={
              <ProtectedRoute>
                <UserProfilePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/users/:id"
            element={
              <ProtectedRoute>
                <UserProfilePage />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </MotionDiv>
    </AnimatePresence>
  )
}

export default App
