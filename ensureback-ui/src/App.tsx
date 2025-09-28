import type { ReactElement } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';

import NavBar from '@/components/NavBar';
import { useAuth } from '@/hooks/useAuth';
import BuyerOrders from '@/pages/BuyerOrders';
import Dashboard from '@/pages/Dashboard';
import DeveloperCenter from '@/pages/DeveloperCenter';
import Landing from '@/pages/Landing';
import Login from '@/pages/Login';

const ProtectedRoute = ({ children }: { children: ReactElement }) => {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return children;
};

const App = () => {
  return (
    <div className="min-h-screen bg-background text-foreground">
      <NavBar />
      <Routes>
        <Route path="/" element={<Landing />} />
        <Route path="/developer" element={<DeveloperCenter />} />
        <Route path="/login" element={<Login />} />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <Dashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/buyer/orders"
          element={
            <ProtectedRoute>
              <BuyerOrders />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  );
};

export default App;
