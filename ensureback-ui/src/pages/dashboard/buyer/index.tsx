import { Navigate, Route, Routes } from 'react-router-dom';

import Overview from './Overview';
import CaseDetail from './CaseDetail';

const BuyerDashboard = () => (
  <div className="mx-auto flex w-full max-w-5xl flex-1 flex-col gap-6 px-4 py-10">
    <Routes>
      <Route index element={<Overview />} />
      <Route path="cases/:caseId" element={<CaseDetail />} />
      <Route path="*" element={<Navigate to="." replace />} />
    </Routes>
  </div>
);

export default BuyerDashboard;
