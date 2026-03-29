import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import 'dayjs/locale/zh-cn';

import CompanyList from './pages/CompanyList';
import CompanyDetail from './pages/CompanyDetail';
import CompanyEdit from './pages/CompanyEdit';
import TemplateList from './pages/TemplateList';
import ExcelUpload from './pages/ExcelUpload';
import AdminDashboard from './pages/AdminDashboard';
import LoginPage from './pages/LoginPage';

function App() {
    return (
        <ConfigProvider locale={zhCN}>
            <BrowserRouter>
                <Routes>
                    <Route path="/" element={<Navigate to="/login" replace />} />
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="/admin" element={<AdminDashboard />} />
                    <Route path="/companies" element={<CompanyList />} />
                    <Route path="/companies/:id" element={<CompanyDetail />} />
                    <Route path="/companies/:id/edit" element={<CompanyEdit />} />
                    <Route path="/templates" element={<TemplateList />} />
                    <Route path="/excel" element={<ExcelUpload />} />
                </Routes>
            </BrowserRouter>
        </ConfigProvider>
    );
}

export default App
