import { Routes, Route, Navigate } from 'react-router-dom';
import MainLayout from './layouts/MainLayout';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import KnowledgeListPage from './pages/knowledge/KnowledgeListPage';
import KnowledgeDetailPage from './pages/knowledge/KnowledgeDetailPage';
import AgentListPage from './pages/agent/AgentListPage';
import AgentEditPage from './pages/agent/AgentEditPage';
import UserManagePage from './pages/user/UserManagePage';
import { useAuthStore } from './stores/authStore';

function App() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/"
        element={
          isAuthenticated ? <MainLayout /> : <Navigate to="/login" replace />
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="knowledge" element={<KnowledgeListPage />} />
        <Route path="knowledge/:id" element={<KnowledgeDetailPage />} />
        <Route path="agent" element={<AgentListPage />} />
        <Route path="agent/new" element={<AgentEditPage />} />
        <Route path="agent/:id/edit" element={<AgentEditPage />} />
        <Route path="users" element={<UserManagePage />} />
      </Route>
    </Routes>
  );
}

export default App;
