import { Routes, Route, Navigate } from 'react-router-dom'
import { getAuth } from './store/auth'
import Layout from './components/Layout'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import DashboardPage from './pages/DashboardPage'
import AppointmentsPage from './pages/AppointmentsPage'
import NewAppointmentPage from './pages/NewAppointmentPage'
import SubscriptionsPage from './pages/SubscriptionsPage'
import ServicesPage from './pages/ServicesPage'
import EmployeesPage from './pages/EmployeesPage'
import AnalyticsPage from './pages/AnalyticsPage'
import SchedulePage from './pages/SchedulePage'
import SettingsPage from './pages/SettingsPage'
import ReviewsPage from './pages/ReviewsPage'
import BroadcastPage from './pages/BroadcastPage'
import UsersPage from './pages/UsersPage'

function RequireAuth({ children }: { children: React.ReactNode }) {
  const { token } = getAuth()
  return token ? <>{children}</> : <Navigate to="/login" replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/" element={<RequireAuth><Layout /></RequireAuth>}>
        <Route index element={<DashboardPage />} />
        <Route path="appointments" element={<AppointmentsPage />} />
        <Route path="appointments/new" element={<NewAppointmentPage />} />
        <Route path="subscriptions" element={<SubscriptionsPage />} />
        <Route path="services" element={<ServicesPage />} />
        <Route path="employees" element={<EmployeesPage />} />
        <Route path="analytics" element={<AnalyticsPage />} />
        <Route path="schedule" element={<SchedulePage />} />
        <Route path="settings" element={<SettingsPage />} />
        <Route path="reviews" element={<ReviewsPage />} />
        <Route path="broadcast" element={<BroadcastPage />} />
        <Route path="users" element={<UsersPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
