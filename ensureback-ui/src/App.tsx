import type { ReactElement } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';

import NavBar from '@/components/NavBar';
import { useAuth } from '@/hooks/useAuth';
import BuyerDashboard from '@/pages/dashboard/buyer';
import AdminDashboard from '@/pages/dashboard/admin';
import MerchantDashboard from '@/pages/dashboard/merchant';
import IntegrationWizard from '@/pages/IntegrationWizard';
import Landing from '@/pages/Landing';
import StripeConnectCallback from '@/pages/StripeConnectCallback';
import { IntegrationWizardGuard, MerchantGate, RequireMerchantIntegration } from '@/routes/MerchantStatusGates';

const ProtectedRoute = ({ children }: { children: ReactElement }) => {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/" replace state={{ from: location }} />;
  }

  return children;
};

const App = () => {
  return (
    <div className="min-h-screen bg-background text-foreground">
      <NavBar />
      <Routes>
        <Route path="/" element={<Landing />} />
        <Route
          path="/developer"
          element={
            <ProtectedRoute><IntegrationWizardGuard><IntegrationWizard /></IntegrationWizardGuard></ProtectedRoute>
          }
        />
        <Route path="/integration-wizard" element={<ProtectedRoute><IntegrationWizardGuard><IntegrationWizard /></IntegrationWizardGuard></ProtectedRoute>} />
        <Route path="/auth/callback" element={<StripeConnectCallback />} />
        <Route path="/login" element={<StripeConnectCallback />} />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <MerchantGate />
            </ProtectedRoute>
          }
        />
        <Route
          path="/merchant/dashboard/*"
          element={
            <ProtectedRoute>
              <RequireMerchantIntegration>
                <MerchantDashboard />
              </RequireMerchantIntegration>
            </ProtectedRoute>
          }
        />
        <Route
          path="/dashboard/merchant/*"
          element={<Navigate to="/merchant/dashboard" replace />}
        />
        <Route
          path="/dashboard/buyer/*"
          element={
            <ProtectedRoute>
              <BuyerDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/dashboard/admin/*"
          element={
            <ProtectedRoute>
              <AdminDashboard />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  );
};

export default App;

