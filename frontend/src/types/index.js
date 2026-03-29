// 公司实体
export const Company = {
    id: null,
    legalRepresentative: '',
    companyName: '',
    domain: '',
    domainContact: '',
    tencentCloudId: '',
    tencentEmail: '',
    tencentPassword: '',
    tencentPhone: '',
    submissionTime: null,
    smsVerifyTime: null,
    approvalTime: null,
    resolutionTime: null,
    icpNumber: '',
    hasWebsite: false,
    mainTitle: '',
    subtitle: '',
    templateId: null,
    isPublished: false,
    isActive: true,
    publishDate: null,
    createdAt: null,
    updatedAt: null
};

// 模板实体
export const WebsiteTemplate = {
    id: null,
    templateName: '',
    templateCode: '',
    previewImage: '',
    description: '',
    isActive: true,
    createdAt: null,
    updatedAt: null
};

// 网站内容实体
export const WebsiteContent = {
    id: null,
    companyId: null,
    companyName: '',
    contentTitle: '',
    contentType: '',
    title: '',
    description: '',
    contentDetail: '',
    imageUrl: '',
    sortOrder: 0,
    isActive: true,
    createdAt: null,
    updatedAt: null
};

// 公司列表项
export const CompanyListItem = {
    id: null,
    companyName: '',
    domain: '',
    templateId: null,
    isPublished: false,
    isActive: true,
    createdAt: null
};

// 内容类型枚举
export const ContentType = {
    HEADER: 'header',
    ABOUT: 'about',
    PRODUCTS: 'products',
    SERVICES: 'services',
    TEAM: 'team',
    NEWS: 'news',
    FOOTER: 'footer'
};
