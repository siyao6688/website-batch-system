import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Layout, Card, Table, Button, Space, Tag, Badge, Statistic, Row, Col,
  Drawer, Form, Input, Select, Modal, message, Popconfirm, Tooltip, Tabs, Dropdown, Menu,
  Upload, Divider, Typography, Switch, Radio, ColorPicker
} from 'antd';
import {
  DashboardOutlined, FileTextOutlined,
  SettingOutlined, PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined,
  ExportOutlined, SearchOutlined, CheckCircleOutlined, ClockCircleOutlined,
  DownloadOutlined, ReloadOutlined, PlusSquareOutlined, BankOutlined, UploadOutlined,
  UserOutlined, MailOutlined, GlobalOutlined, IdcardOutlined, PhoneOutlined,
  ContactsOutlined, HomeOutlined, InfoCircleOutlined, CloseCircleOutlined
} from '@ant-design/icons';
import { companyApi, templateApi, excelApi } from '../services/api';
import './AdminDashboard.css';
import CompanyList from './CompanyList';
import TemplatePreviewModal from '../components/TemplatePreviewModal';

const { Header, Sider, Content } = Layout;
const { Title, Text } = Typography;

const AdminDashboard = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [companies, setCompanies] = useState([]);
  const [filteredCompanies, setFilteredCompanies] = useState([]);
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [isDraftVisible, setIsDraftVisible] = useState(false);
  const [editingCompany, setEditingCompany] = useState(null);
  const [selectedTemplate, setSelectedTemplate] = useState(null);
  const [selectedCompany, setSelectedCompany] = useState(null);

  // 统计数据
  const [stats, setStats] = useState({
    total: 0,
    published: 0,
    active: 0,
    hasWebsite: 0
  });

  // 模板列表
  const [templates, setTemplates] = useState([]);
  const [previewTemplate, setPreviewTemplate] = useState(null);
  const [previewVisible, setPreviewVisible] = useState(false);
  const [username, setUsername] = useState('');

  // 当前选中的菜单项
  const [selectedMenuKey, setSelectedMenuKey] = useState('overview');

  // Excel上传模态框
  const [excelModalVisible, setExcelModalVisible] = useState(false);
  const [excelLoading, setExcelLoading] = useState(false);
  const [importedCompanies, setImportedCompanies] = useState([]);

  // 批量操作状态
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  const [batchLoading, setBatchLoading] = useState(false);

  // 表格列配置
  const columns = [
    {
      title: '序号',
      dataIndex: 'serialNumber',
      key: 'serialNumber',
      width: 80,
      render: (text) => text || '-'
    },
    {
      title: '公司名称',
      dataIndex: 'companyName',
      key: 'companyName',
      width: 200,
      render: (text) => <a onClick={() => handleViewCompany(text)}>{text}</a>
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
      width: 180,
    },
    {
      title: '域名',
      dataIndex: 'domain',
      key: 'domain',
      width: 180,
      render: (text) => {
        if (!text) return '-';
        const url = `http://${text}`;
        return (
          <a href={url} target="_blank" rel="noopener noreferrer">
            {text}
          </a>
        );
      }
    },
    {
      title: '备案号',
      dataIndex: 'icpNumber',
      key: 'icpNumber',
      width: 150,
      render: (text) => text ? <Tag color="green">{text}</Tag> : '-'
    },
    {
      title: '发布时间',
      dataIndex: 'publishDate',
      key: 'publishDate',
      width: 160,
      render: (text) => text ? new Date(text).toLocaleString() : '-'
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (text) => new Date(text).toLocaleString()
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 200,
      render: (_, record) => (
        <Space size="small">
          <Tooltip title="编辑">
            <Button
              type="text"
              icon={<EditOutlined />}
              onClick={() => handleEdit(record)}
            />
          </Tooltip>
          <Tooltip title="预览网站">
            <Button
              type="text"
              icon={<EyeOutlined />}
              onClick={() => handlePreview(record.id)}
              disabled={!record.hasWebsite}
            />
          </Tooltip>
          <Tooltip title="生成网站">
            <Button
              type="text"
              icon={<PlusSquareOutlined />}
              onClick={() => handleGenerateWebsite(record)}
            />
          </Tooltip>
          {record.isPublished ? (
            <Tooltip title="已发布">
              <Tag color="green">已发布</Tag>
            </Tooltip>
          ) : (
            <Popconfirm
              title="确认发布？（会自动生成网站并部署）"
              onConfirm={() => handlePublish(record.id)}
              okText="确认"
              cancelText="取消"
            >
              <Tooltip title="发布">
                <Button
                  type="text"
                  icon={<CheckCircleOutlined />}
                />
              </Tooltip>
            </Popconfirm>
          )}
          <Popconfirm
            title="确认删除？"
            description="删除后无法恢复"
            onConfirm={() => handleDelete(record.id)}
            okText="确认"
            cancelText="取消"
          >
            <Tooltip title="删除">
              <Button
                type="text"
                danger
                icon={<DeleteOutlined />}
              />
            </Tooltip>
          </Popconfirm>
        </Space>
      )
    }
  ];

  // 模板表格列配置
  const templateColumns = [
    {
      title: '模板名称',
      dataIndex: 'templateName',
      key: 'templateName',
    },
    {
      title: '模板代码',
      dataIndex: 'templateCode',
      key: 'templateCode',
      width: 150,
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
    },
    {
      title: '预览图',
      dataIndex: 'previewImage',
      key: 'previewImage',
      render: (text) => text ? <span>有预览图</span> : <Tag color="default">无</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'isActive',
      key: 'isActive',
      render: (status) => (
        <Tag color={status ? 'green' : 'red'}>
          {status ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 220,
      render: (_, record) => (
        <Space size="small">
          <Tooltip title="预览">
            <Button
              size="small"
              icon={<EyeOutlined />}
              onClick={() => handleTemplateView(record)}
            />
          </Tooltip>
          <Tooltip title="编辑">
            <Button
              size="small"
              icon={<EditOutlined />}
              onClick={() => handleTemplateEdit(record)}
            />
          </Tooltip>
          <Tooltip title="删除">
            <Button
              size="small"
              danger
              icon={<DeleteOutlined />}
              onClick={() => handleTemplateDelete(record)}
            />
          </Tooltip>
        </Space>
      ),
    },
  ];

  // Excel上传相关函数
  const downloadExcelTemplate = () => {
    const template = [
      ['序号', '公司名称', '邮箱', '域名', '备案号']
    ];

    const csvContent = "data:text/csv;charset=utf-8,\uFEFF" +
      template.map(e => e.join(",")).join("\n");

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", "公司信息模板.csv");
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const loadImportedCompanies = async () => {
    setExcelLoading(true);
    try {
      const response = await excelApi.getImportedCompanies();
      if (Array.isArray(response.data)) {
        setImportedCompanies(response.data);
      } else {
        console.error('获取到的数据格式不正确:', response.data);
        message.error('获取公司列表失败：数据格式不正确');
      }
    } catch (error) {
      console.error('获取导入的公司列表失败:', error);
      const errorMsg = error.response?.data?.message || error.response?.data || error.message || '网络错误';
      message.error('获取公司列表失败：' + errorMsg);
    } finally {
      setExcelLoading(false);
    }
  };

  const handleExcelUpload = async (options) => {
    console.log('handleExcelUpload called', options);
    const { file, onSuccess, onError } = options;
    setExcelLoading(true);

    try {
      const fileToUpload = file.originFileObj || file;
      console.log('file object:', fileToUpload);
      const response = await excelApi.uploadExcel(fileToUpload);
      console.log('upload response:', response);
      if (response.status === 207) {
          message.warning(response.data);
      } else {
          message.success('Excel导入成功');
          setExcelModalVisible(false); // 关闭弹窗
      }
      // 调用 Ant Design 的 onSuccess 回调
      if (onSuccess) {
        onSuccess(response.data, file, response);
      }
      loadData(); // 刷新主数据
    } catch (error) {
      console.error('导入错误:', error);
      const errorDetail = error.response?.data?.message || error.response?.data || error.message || '网络错误';
      let errorMessage = 'Excel导入失败：' + errorDetail;
      message.error(errorMessage);
      // 调用 Ant Design 的 onError 回调
      if (onError) {
        onError(error, file, error.response);
      }
    } finally {
      setExcelLoading(false);
    }
  };

  const handleExcelImportClick = () => {
    setExcelModalVisible(true);
  };

  // 认证检查
  useEffect(() => {
    const isAuthenticated = localStorage.getItem('isAuthenticated') === 'true';
    if (!isAuthenticated) {
      navigate('/login');
      message.warning('请先登录系统');
    } else {
      const savedUsername = localStorage.getItem('username') || '管理员';
      setUsername(savedUsername);
    }
  }, [navigate]);

  // 加载数据
  useEffect(() => {
    loadData();
    loadTemplates();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const [companiesResponse, templatesResponse] = await Promise.all([
        companyApi.getCompanies(),
        templateApi.getAllTemplates()
      ]);

      if (Array.isArray(companiesResponse.data)) {
        setCompanies(companiesResponse.data);
        setFilteredCompanies(companiesResponse.data);
        updateStats(companiesResponse.data);
      }

      if (Array.isArray(templatesResponse.data)) {
        setTemplates(templatesResponse.data);
      }
    } catch (error) {
      const errorMsg = error.response?.data?.message || error.response?.data || error.message || '未知错误';
      message.error('加载数据失败：' + errorMsg);
    } finally {
      setLoading(false);
    }
  };

  const loadTemplates = async () => {
    try {
      const response = await templateApi.getAllTemplates();
      if (Array.isArray(response.data)) {
        setTemplates(response.data);
      }
    } catch (error) {
      console.error('加载模板失败:', error);
    }
  };

  const updateStats = (data) => {
    setStats({
      total: data.length,
      published: data.filter(c => c.isPublished).length,
      active: data.filter(c => c.isActive).length,
      hasWebsite: data.filter(c => c.hasWebsite).length
    });
  };

  // 处理菜单点击
  const handleMenuClick = (key) => {
    setSelectedMenuKey(key);
    switch (key) {
      case 'overview':
        loadData();
        break;
      case 'companies':
        loadData();
        break;
      case 'templates':
        loadTemplates();
        break;
      // 其他菜单项可以添加对应的数据加载逻辑
      default:
        break;
    }
  };

  // 模板操作函数
  const handleTemplateView = (record) => {
    setPreviewTemplate(record);
    setPreviewVisible(true);
  };

  const handleTemplateEdit = (record) => {
    message.info(`编辑模板: ${record.templateName}`);
  };

  const handleTemplateDelete = (record) => {
    Modal.confirm({
      title: '确认删除模板',
      content: `确定要删除模板 "${record.templateName}" 吗？删除后不可恢复。`,
      okText: '确认删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await templateApi.deleteTemplate(record.id);
          message.success('模板删除成功');
          loadTemplates(); // 刷新模板列表
        } catch (error) {
          const errorMsg = error.response?.data?.message || error.response?.data || error.message || '未知错误';
          message.error('删除失败: ' + errorMsg);
        }
      }
    });
  };

  // 筛选
  const handleSearch = (value) => {
    if (!value) {
      setFilteredCompanies(companies);
      return;
    }

    const filtered = companies.filter(company =>
      company.companyName?.toLowerCase().includes(value.toLowerCase()) ||
      company.domain?.toLowerCase().includes(value.toLowerCase()) ||
      company.icpNumber?.toLowerCase().includes(value.toLowerCase())
    );

    setFilteredCompanies(filtered);
  };

  // 编辑
  const handleEdit = (company) => {
    setEditingCompany(company);
    setIsModalVisible(true);
  };

  // 生成网站
  const handleGenerateWebsite = (company) => {
    setSelectedCompany(company);
    setSelectedTemplate(templates[0]); // 默认选择第一个模板
    setIsDraftVisible(true);
  };

  // 查看公司
  const handleViewCompany = (companyName) => {
    const company = companies.find(c => c.companyName === companyName);
    if (company) {
      handleEdit(company);
    }
  };

  // 发布
  const handlePublish = async (id) => {
    try {
      await companyApi.publishCompany(id);
      message.success('发布成功');
      loadData();
    } catch (error) {
      const errorMsg = error.response?.data?.message || error.response?.data || error.message || '未知错误';
      message.error('发布失败：' + errorMsg);
    }
  };

  // 删除
  const handleDelete = async (id) => {
    try {
      await companyApi.deleteCompany(id);
      message.success('删除成功');
      loadData();
    } catch (error) {
      const errorMsg = error.response?.data?.message || error.response?.data || error.message || '未知错误';
      message.error('删除失败：' + errorMsg);
    }
  };

  // 预览网站
  const handlePreview = async (id) => {
    try {
      const response = await companyApi.previewWebsite(id);
      const previewUrl = response.data.previewUrl;
      // 构建完整URL
      const fullUrl = `${window.location.protocol}//${window.location.host}/api${previewUrl}`;
      window.open(fullUrl, '_blank');
    } catch (error) {
      message.error('预览失败：' + (error.response?.data || error.message));
      console.error(error);
    }
  };

  // 退出登录
  const handleLogout = () => {
    Modal.confirm({
      title: '确认退出',
      content: '确定要退出登录吗？',
      okText: '确认退出',
      cancelText: '取消',
      onOk: () => {
        localStorage.removeItem('isAuthenticated');
        localStorage.removeItem('username');
        message.success('已退出登录');
        navigate('/login');
      }
    });
  };

  // 表格行选择
  const rowSelection = {
    selectedRowKeys,
    onChange: (newSelectedRowKeys) => setSelectedRowKeys(newSelectedRowKeys),
    getCheckboxProps: (record) => ({
      disabled: record.isPublished, // 已发布的不允许选择
    }),
  };

  // 批量发布
  const handleBatchPublish = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请先选择要发布的公司');
      return;
    }
    setBatchLoading(true);
    try {
      const response = await companyApi.batchPublish(selectedRowKeys);
      const result = response.data;
      if (result.failCount > 0) {
        message.warning(result.message);
        if (result.failures && result.failures.length > 0) {
          Modal.info({
            title: '发布失败详情',
            content: (
              <div>
                {result.failures.map((f, idx) => (
                  <p key={idx}>{f.companyName} ({f.domain}): {f.reason}</p>
                ))}
              </div>
            ),
          });
        }
      } else {
        message.success(result.message);
      }
      setSelectedRowKeys([]);
      loadData();
    } catch (error) {
      const errorMsg = error.response?.data?.message || error.response?.data || error.message || '未知错误';
      message.error('批量发布失败：' + errorMsg);
    } finally {
      setBatchLoading(false);
    }
  };

  // 批量生成网站
  const handleBatchGenerate = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请先选择要生成网站的公司');
      return;
    }
    setBatchLoading(true);
    try {
      const response = await companyApi.batchGenerate(selectedRowKeys);
      const result = response.data;
      if (result.failCount > 0) {
        message.warning(result.message);
        if (result.failures && result.failures.length > 0) {
          Modal.info({
            title: '生成失败详情',
            content: (
              <div>
                {result.failures.map((f, idx) => (
                  <p key={idx}>{f.companyName} ({f.domain}): {f.reason}</p>
                ))}
              </div>
            ),
          });
        }
      } else {
        message.success(result.message);
      }
      setSelectedRowKeys([]);
      loadData();
    } catch (error) {
      const errorMsg = error.response?.data?.message || error.response?.data || error.message || '未知错误';
      message.error('批量生成失败：' + errorMsg);
    } finally {
      setBatchLoading(false);
    }
  };

  return (
    <Layout className="admin-layout">
      <Sider width={260} className="admin-sider">
        <div className="logo-container">
          <DashboardOutlined className="logo-icon" />
          <span className="logo-text">网站批量生成系统</span>
        </div>

        <div className="menu-container">
          <Menu
            selectedKeys={[selectedMenuKey]}
            items={[
              {
                key: 'overview',
                icon: <DashboardOutlined />,
                label: '数据概览',
                onClick: () => handleMenuClick('overview')
              },
              {
                key: 'companies',
                icon: <BankOutlined />,
                label: '公司管理',
                onClick: () => handleMenuClick('companies')
              },
              {
                key: 'templates',
                icon: <FileTextOutlined />,
                label: '模板管理',
                onClick: () => handleMenuClick('templates')
              },
              {
                key: 'generate',
                icon: <SettingOutlined />,
                label: '网站生成',
                onClick: () => message.info('网站生成功能开发中')
              },
              {
                key: 'settings',
                icon: <SettingOutlined />,
                label: '系统设置',
                onClick: () => message.info('系统设置功能开发中')
              }
            ]}
          />
        </div>
      </Sider>

      <Layout>
        <Header className="admin-header">
          <div className="header-content">
            <Input
              placeholder="搜索公司名称、域名、备案号..."
              prefix={<SearchOutlined />}
              onChange={(e) => handleSearch(e.target.value)}
              allowClear
              style={{ width: 400 }}
            />
            <Space size="large" className="header-actions">
              <Button icon={<ReloadOutlined />} onClick={loadData}>
                刷新
              </Button>
              <Button type="primary" icon={<ExportOutlined />} onClick={() => message.info('导出数据功能开发中')}>
                导出数据
              </Button>
              <Dropdown menu={{
                items: [
                  {
                    key: 'import',
                    icon: <UploadOutlined />,
                    label: '导入Excel',
                    onClick: handleExcelImportClick
                  },
                  {
                    key: 'export',
                    icon: <ExportOutlined />,
                    label: '导出数据',
                    onClick: () => message.info('导出数据功能开发中')
                  },
                  {
                    key: 'backup',
                    icon: <DownloadOutlined />,
                    label: '备份数据',
                    onClick: () => message.info('备份数据功能开发中')
                  }
                ]
              }}>
                <Button icon={<PlusOutlined />}>
                  新增
                </Button>
              </Dropdown>
              <Dropdown menu={{
                items: [
                  {
                    key: 'profile',
                    icon: <UserOutlined />,
                    label: '个人资料',
                    onClick: () => message.info('个人资料功能开发中')
                  },
                  {
                    type: 'divider'
                  },
                  {
                    key: 'logout',
                    icon: <ExportOutlined />,
                    label: '退出登录',
                    onClick: handleLogout
                  }
                ]
              }}>
                <Button type="text" icon={<UserOutlined />} style={{ color: '#1890ff' }}>
                  {username}
                </Button>
              </Dropdown>
            </Space>
          </div>
        </Header>

        <Content className="admin-content">
          {selectedMenuKey === 'overview' && (<>
          <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
            <Col xs={24} sm={12} md={6}>
              <Card hoverable>
                <Statistic
                  title="公司总数"
                  value={stats.total}
                  prefix={<BankOutlined />}
                  styles={{ content: { color: '#3f8600' } }}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12} md={6}>
              <Card hoverable>
                <Statistic
                  title="已发布"
                  value={stats.published}
                  prefix={<CheckCircleOutlined />}
                  styles={{ content: { color: '#1890ff' } }}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12} md={6}>
              <Card hoverable>
                <Statistic
                  title="正常状态"
                  value={stats.active}
                  prefix={<CheckCircleOutlined />}
                  styles={{ content: { color: '#52c41a' } }}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12} md={6}>
              <Card hoverable>
                <Statistic
                  title="已生成网站"
                  value={stats.hasWebsite}
                  prefix={<DownloadOutlined />}
                  styles={{ content: { color: '#722ed1' } }}
                />
              </Card>
            </Col>
          </Row>

          <Card className="main-card">
            <div className="card-header">
              <Space>
                <span className="card-title">公司列表</span>
                <Badge count={filteredCompanies.length} />
              </Space>
              <Space>
                {selectedRowKeys.length > 0 && (
                  <>
                    <Button
                      type="primary"
                      icon={<CheckCircleOutlined />}
                      loading={batchLoading}
                      onClick={handleBatchPublish}
                    >
                      批量发布 ({selectedRowKeys.length})
                    </Button>
                    <Button
                      icon={<PlusSquareOutlined />}
                      loading={batchLoading}
                      onClick={handleBatchGenerate}
                    >
                      批量生成网站 ({selectedRowKeys.length})
                    </Button>
                    <Button onClick={() => setSelectedRowKeys([])}>
                      取消选择
                    </Button>
                  </>
                )}
                <Button icon={<PlusOutlined />} type="primary" onClick={() => {
                  setEditingCompany(null);
                  setIsModalVisible(true);
                }}>
                  新增公司
                </Button>
                <Button icon={<ExportOutlined />} onClick={() => message.info('导出Excel功能开发中')}>
                  导出Excel
                </Button>
                <Button icon={<UploadOutlined />} onClick={handleExcelImportClick}>
                  导入Excel
                </Button>
              </Space>
            </div>

            <Table
              columns={columns}
              dataSource={filteredCompanies}
              rowKey="id"
              loading={loading}
              rowSelection={rowSelection}
              scroll={{ x: 1500 }}
              pagination={{
                pageSize: 20,
                showSizeChanger: true,
                showTotal: (total) => `共 ${total} 条`,
                pageSizeOptions: ['10', '20', '50', '100']
              }}
            />
          </Card>
          </>)}
          {selectedMenuKey === 'templates' && (
            <Card className="main-card">
              <div className="card-header">
                <Space>
                  <span className="card-title">模板管理</span>
                </Space>
                <Space>
                  <Button icon={<PlusOutlined />} onClick={() => message.info('新增模板功能暂未实现')}>
                    新增模板
                  </Button>
                </Space>
              </div>
              <Table
                columns={templateColumns}
                dataSource={templates}
                rowKey="id"
                loading={loading}
                scroll={{ x: 800 }}
                pagination={{
                  pageSize: 10,
                  showSizeChanger: true,
                  showTotal: (total) => `共 ${total} 条`,
                }}
              />
            </Card>
          )}
          {selectedMenuKey === 'companies' && (
            <CompanyList embedded={true} />
          )}
        </Content>
      </Layout>

      {/* 编辑/新增模态框 */}
      <Modal
        title={
          <Space>
            <BankOutlined style={{ color: '#1890ff' }} />
            <span style={{ fontSize: '16px', fontWeight: 500 }}>
              {editingCompany ? '编辑公司信息' : '新增公司'}
            </span>
          </Space>
        }
        open={isModalVisible}
        onCancel={() => setIsModalVisible(false)}
        footer={null}
        width={700}
        destroyOnClose
      >
        <Form
          key={editingCompany ? `edit-${editingCompany.id}` : 'new'}
          layout="vertical"
          initialValues={editingCompany ? editingCompany : {
            isActive: true,
            hasWebsite: false
          }}
          onFinish={async (values) => {
            try {
              // 确保域名以.cn结尾
              if (values.domain && !values.domain.toLowerCase().endsWith('.cn')) {
                values.domain = values.domain + '.cn';
              }

              if (editingCompany) {
                await companyApi.updateCompany(editingCompany.id, values);
                message.success('更新成功');
              } else {
                await companyApi.createCompany(values);
                message.success('创建成功');
              }
              setIsModalVisible(false);
              loadData();
            } catch (error) {
              const errorMsg = error.response?.data?.message || error.response?.data || error.message || '未知错误';
              message.error('操作失败：' + errorMsg);
            }
          }}
        >
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="公司名称"
                name="companyName"
                rules={[{ required: true, message: '请输入公司名称' }]}
              >
                <Input
                  placeholder="请输入公司名称"
                  prefix={<UserOutlined style={{ color: 'rgba(0,0,0,.25)' }} />}
                  size="large"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="邮箱"
                name="email"
                rules={[
                  { required: false },
                  { type: 'email', message: '请输入有效的邮箱地址' }
                ]}
              >
                <Input
                  placeholder="请输入邮箱"
                  prefix={<MailOutlined style={{ color: 'rgba(0,0,0,.25)' }} />}
                  size="large"
                />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="域名"
                name="domain"
                getValueFromEvent={(e) => {
                  // 从输入事件获取值，如果是Input组件，e.target.value是输入值
                  const value = e && e.target ? e.target.value : e;
                  if (!value) return value;
                  // 如果用户输入了.cn后缀，去除它（不区分大小写）
                  const lowerValue = value.toLowerCase();
                  if (lowerValue.endsWith('.cn')) {
                    return value.substring(0, value.length - 3);
                  }
                  return value;
                }}
                getValueProps={(value) => {
                  // 从数据库值转换为显示值
                  if (!value) return { value: '' };
                  // 如果数据库值以.cn结尾，去除.cn后缀显示
                  const lowerValue = value.toLowerCase();
                  if (lowerValue.endsWith('.cn')) {
                    return { value: value.substring(0, value.length - 3) };
                  }
                  return { value };
                }}
                rules={[
                  { required: true, message: '请输入域名' },
                  {
                    pattern: /^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?(\.[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?)*$/,
                    message: '请输入有效的域名格式（字母、数字、连字符和点号，不能以点号或连字符开头结尾）'
                  }
                ]}
              >
                <Input
                  placeholder="输入域名主体（如：example）"
                  prefix={<GlobalOutlined style={{ color: 'rgba(0,0,0,.25)' }} />}
                  addonAfter=".cn"
                  size="large"
                  allowClear
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="备案号"
                name="icpNumber"
              >
                <Input
                  placeholder="请输入备案号"
                  prefix={<IdcardOutlined style={{ color: 'rgba(0,0,0,.25)' }} />}
                  size="large"
                />
              </Form.Item>
            </Col>
          </Row>

          <Divider orientation="left" style={{ margin: '20px 0' }}>网站设置</Divider>

          <Form.Item
            label="是否搭建官网"
            name="hasWebsite"
            valuePropName="checked"
          >
            <Switch
              checkedChildren="是"
              unCheckedChildren="否"
              defaultChecked={false}
              style={{ minWidth: 80 }}
            />
          </Form.Item>

          <Divider orientation="left" style={{ margin: '20px 0' }}>网站生成选项</Divider>

          <Form.Item
            label="生成方式"
            name="websiteGenerationType"
            initialValue="defaultTemplate"
          >
            <Radio.Group>
              <Radio.Button value="defaultTemplate">使用默认模板</Radio.Button>
              <Radio.Button value="custom">自定义网站内容</Radio.Button>
            </Radio.Group>
          </Form.Item>

          <Form.Item
            noStyle
            shouldUpdate={(prevValues, currentValues) => prevValues.websiteGenerationType !== currentValues.websiteGenerationType}
          >
            {({ getFieldValue }) => {
              const generationType = getFieldValue('websiteGenerationType');
              if (generationType !== 'custom') {
                return null;
              }
              return (
                <>
                  <Row gutter={16}>
                    <Col span={12}>
                      <Form.Item
                        label="主标题"
                        name="mainTitle"
                      >
                        <Input placeholder="请输入网站主标题" />
                      </Form.Item>
                    </Col>
                    <Col span={12}>
                      <Form.Item
                        label="副标题"
                        name="subtitle"
                      >
                        <Input placeholder="请输入网站副标题" />
                      </Form.Item>
                    </Col>
                  </Row>
                  <Form.Item
                    label="网站背景色"
                    name="backgroundColor"
                    initialValue="#1890ff"
                  >
                    <ColorPicker showText format="hex" />
                  </Form.Item>
                </>
              );
            }}
          </Form.Item>

          <Form.Item>
            <Space style={{ float: 'right' }}>
              <Button
                onClick={() => setIsModalVisible(false)}
                icon={<CloseCircleOutlined />}
                size="large"
              >
                取消
              </Button>
              <Button
                type="primary"
                htmlType="submit"
                icon={<CheckCircleOutlined />}
                size="large"
              >
                {editingCompany ? '更新' : '创建'}
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* 生成网站模态框 */}
      <Modal
        title="生成网站"
        open={isDraftVisible}
        onCancel={() => setIsDraftVisible(false)}
        footer={null}
        width={600}
      >
        <Form
          layout="vertical"
          initialValues={{
            hasWebsite: true,
            publishNow: true
          }}
          onFinish={async (values) => {
            try {
              if (selectedCompany) {
                // 先更新公司信息：设置模板ID和hasWebsite为true
                const updateData = {
                  templateId: values.templateId,
                  hasWebsite: true
                };
                await companyApi.updateCompany(selectedCompany.id, updateData);

                if (values.publishNow === true || values.publishNow === 'true') {
                  // 立即发布
                  await companyApi.publishCompany(selectedCompany.id);
                  message.success('网站生成并发布成功！');
                } else {
                  // 仅保存为草稿
                  message.success('网站已保存为草稿！');
                }
              }
              setIsDraftVisible(false);
              loadData();
            } catch (error) {
              const errorMsg = error.response?.data?.message || error.response?.data || error.message || '未知错误';
              message.error('生成失败：' + errorMsg);
              console.error('生成网站失败:', error);
            }
          }}
        >
          <Form.Item label="选择公司">
            <Input value={selectedCompany?.companyName} disabled />
          </Form.Item>

          <Form.Item
            label="选择模板"
            name="templateId"
            rules={[{ required: true, message: '请选择模板' }]}
          >
            <Select placeholder="请选择模板">
              {templates.map(template => (
                <Select.Option key={template.id} value={template.id}>
                  {template.templateName}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            label="是否立即发布"
            name="publishNow"
          >
            <Select>
              <Select.Option value={true}>是 - 生成后立即发布</Select.Option>
              <Select.Option value={false}>否 - 保存为草稿</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item>
            <Space style={{ float: 'right' }}>
              <Button onClick={() => setIsDraftVisible(false)}>
                取消
              </Button>
              <Button type="primary" htmlType="submit">
                生成并发布
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* Excel上传模态框 */}
      <Modal
        title={
          <Space>
            <UploadOutlined style={{ color: '#1890ff' }} />
            <span style={{ fontSize: '16px', fontWeight: 500 }}>Excel导入</span>
          </Space>
        }
        open={excelModalVisible}
        onCancel={() => setExcelModalVisible(false)}
        footer={null}
        width={600}
      >
        <Card style={{ marginBottom: '16px' }}>
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <div>
              <Text strong>步骤1: 下载模板</Text>
              <p>请先下载Excel模板，按照模板格式填写公司信息</p>
              <Button
                icon={<DownloadOutlined />}
                onClick={downloadExcelTemplate}
              >
                下载模板
              </Button>
            </div>

            <Divider />

            <div>
              <Text strong>步骤2: 上传Excel文件</Text>
              <p>上传已填写好的Excel文件进行导入</p>
              <Upload
                action="/api/excel/upload"
                customRequest={handleExcelUpload}
                showUploadList={false}
                accept=".xlsx,.xls,.csv"
              >
                <Button
                  type="primary"
                  icon={<UploadOutlined />}
                  loading={excelLoading}
                >
                  上传Excel文件
                </Button>
              </Upload>
            </div>

          </Space>
        </Card>

      </Modal>

      {/* 模板预览模态框 */}
      <TemplatePreviewModal
        open={previewVisible}
        onCancel={() => setPreviewVisible(false)}
        template={previewTemplate}
      />
    </Layout>
  );
};

export default AdminDashboard;
