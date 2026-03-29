import axios from 'axios';

const API_BASE_URL = '/api';

const api = axios.create({
    baseURL: API_BASE_URL,
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json'
    }
});

// 请求拦截器
api.interceptors.request.use(
    config => {
        // 可以在这里添加token
        return config;
    },
    error => {
        return Promise.reject(error);
    }
);

// 响应拦截器
api.interceptors.response.use(
    response => response,
    error => {
        console.error('API Error:', error);
        return Promise.reject(error);
    }
);

// 公司相关API
export const companyApi = {
    // 获取所有公司
    getAllCompanies: (params) => api.get('/companies', { params }),

    // 获取公司详情
    getCompanyById: (id) => api.get(`/companies/${id}`),

    // 根据域名获取公司
    getCompanyByDomain: (domain) => api.get(`/companies/domain/${domain}`),

    // 创建公司
    createCompany: (data) => api.post('/companies', data),

    // 更新公司
    updateCompany: (id, data) => api.put(`/companies/${id}`, data),

    // 发布公司
    publishCompany: (id) => api.post(`/companies/${id}/publish`),

    // 取消发布
    unpublishCompany: (id) => api.post(`/companies/${id}/unpublish`),

    // 切换状态
    toggleCompanyStatus: (id) => api.post(`/companies/${id}/toggle-status`),

    // 删除公司
    deleteCompany: (id) => api.delete(`/companies/${id}`),

    // 预览网站
    previewWebsite: (id) => api.post(`/companies/${id}/preview`),

    // 获取公司列表（包含状态过滤）
    getCompanies: (params) => api.get('/companies', { params })
};

// 模板相关API
export const templateApi = {
    // 获取所有模板
    getAllTemplates: (params) => api.get('/templates', { params }),

    // 获取模板详情
    getTemplateById: (id) => api.get(`/templates/${id}`),

    // 获取激活的模板
    getActiveTemplates: () => api.get('/templates', { params: { isActive: true } }),

    // 删除模板
    deleteTemplate: (id) => api.delete(`/templates/${id}`)
};

// 内容相关API
export const contentApi = {
    // 获取公司内容
    getCompanyContents: (companyId) => api.get(`/contents/company/${companyId}`),

    // 添加内容
    addContent: (companyId, data) => api.post(`/contents/company/${companyId}`, data),

    // 更新内容
    updateContent: (id, data) => api.put(`/contents/${id}`, data),

    // 删除内容
    deleteContent: (id) => api.delete(`/contents/${id}`)
};

// Excel相关API
export const excelApi = {
    // 上传Excel
    uploadExcel: (file) => {
        console.log('uploadExcel called with file:', file);
        const formData = new FormData();
        formData.append('file', file);
        console.log('FormData entries:', Array.from(formData.entries()));
        return api.post('/excel/upload', formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        });
    },

    // 获取导入的公司列表
    getImportedCompanies: (params) => api.get('/excel/companies', { params })
};

export default api;
