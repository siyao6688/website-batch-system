import { useEffect, useState } from 'react';
import { Table, Button, Space, Tag, message, Popconfirm, Modal } from 'antd';
import { PlusOutlined, DeleteOutlined, EyeOutlined, EditOutlined, PlayCircleOutlined, PauseCircleOutlined, EyeInvisibleOutlined } from '@ant-design/icons';
import { companyApi } from '../services/api';

const CompanyList = ({ embedded = false }) => {
    const [companies, setCompanies] = useState([]);
    const [loading, setLoading] = useState(false);
    const [modalVisible, setModalVisible] = useState(false);

    const fetchCompanies = async () => {
        setLoading(true);
        try {
            const response = await companyApi.getAllCompanies();
            setCompanies(response.data || []);
        } catch (error) {
            message.error('获取公司列表失败');
            console.error(error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchCompanies();
    }, []);

    const handlePublish = async (record) => {
        try {
            await companyApi.publishCompany(record.id);
            message.success(`公司 ${record.companyName} 发布成功，可通过 http://${record.domain} 访问`);
            fetchCompanies();
        } catch (error) {
            message.error('发布失败');
            console.error(error);
        }
    };

    const handleUnpublish = async (record) => {
        try {
            await companyApi.unpublishCompany(record.id);
            message.success(`公司 ${record.companyName} 已取消发布`);
            fetchCompanies();
        } catch (error) {
            message.error('取消发布失败');
            console.error(error);
        }
    };

    const handleToggleStatus = async (record) => {
        try {
            await companyApi.toggleCompanyStatus(record.id);
            message.success(`公司 ${record.companyName} 状态已更新`);
            fetchCompanies();
        } catch (error) {
            message.error('状态更新失败');
            console.error(error);
        }
    };

    const handleDelete = async (record) => {
        try {
            await companyApi.deleteCompany(record.id);
            message.success('删除成功');
            fetchCompanies();
        } catch (error) {
            message.error('删除失败');
            console.error(error);
        }
    };

    const columns = [
        {
            title: '序号',
            key: 'serialNumber',
            width: 60,
            render: (_, __, index) => index + 1,
        },
        {
            title: '公司名称',
            dataIndex: 'companyName',
            key: 'companyName',
            render: (text) => <a>{text}</a>,
        },
        {
            title: '域名',
            dataIndex: 'domain',
            key: 'domain',
            render: (text) => {
                if (!text) return '-';
                const url = `http://${text}`;
                return (
                    <a href={url} target="_blank" rel="noopener noreferrer">
                        {text}
                    </a>
                );
            },
        },
        {
            title: '网站类型',
            key: 'websiteType',
            render: () => '企业官网', // 暂时静态显示
        },
        {
            title: '主标题',
            dataIndex: 'mainTitle',
            key: 'mainTitle',
            ellipsis: true,
        },
        {
            title: '备案号',
            dataIndex: 'icpNumber',
            key: 'icpNumber',
            width: 150,
        },
        {
            title: '操作',
            key: 'action',
            render: (_, record) => (
                <Space size="small">
                    {!record.isPublished && (
                        <Button
                            type="primary"
                            size="small"
                            icon={<PlayCircleOutlined />}
                            onClick={() => handlePublish(record)}
                        >
                            发布
                        </Button>
                    )}
                    {record.isPublished && (
                        <Button
                            size="small"
                            icon={<PauseCircleOutlined />}
                            onClick={() => handleUnpublish(record)}
                        >
                            取消
                        </Button>
                    )}
                    <Button
                        size="small"
                        icon={<EyeOutlined />}
                        onClick={() => handleView(record)}
                    >
                        查看
                    </Button>
                    <Button
                        size="small"
                        icon={<EyeInvisibleOutlined />}
                        onClick={() => handlePreview(record)}
                    >
                        预览
                    </Button>
                    <Button
                        size="small"
                        icon={<EditOutlined />}
                        onClick={() => handleEdit(record)}
                    >
                        编辑
                    </Button>
                    <Popconfirm
                        title="确定要删除这个公司吗？"
                        onConfirm={() => handleDelete(record)}
                        okText="确定"
                        cancelText="取消"
                    >
                        <Button
                            size="small"
                            danger
                            icon={<DeleteOutlined />}
                        >
                            删除
                        </Button>
                    </Popconfirm>
                </Space>
            ),
        },
    ];

    const handleView = (record) => {
        window.open(`/#/companies/${record.id}`, '_blank');
    };

    const handleEdit = (record) => {
        window.open(`/#/companies/${record.id}/edit`, '_blank');
    };

    const handlePreview = async (record) => {
        try {
            const response = await companyApi.previewWebsite(record.id);
            const previewUrl = response.data.previewUrl;
            // 构建完整URL
            const fullUrl = `${window.location.protocol}//${window.location.host}/api${previewUrl}`;
            window.open(fullUrl, '_blank');
        } catch (error) {
            message.error('预览失败：' + (error.response?.data || error.message));
            console.error(error);
        }
    };

    if (embedded) {
        return (
            <div style={{ padding: '16px' }}>
                <Table
                    columns={columns}
                    dataSource={companies}
                    rowKey="id"
                    loading={loading}
                    pagination={{
                        pageSize: 10,
                        showSizeChanger: true,
                        showTotal: (total) => `共 ${total} 条`,
                        showQuickJumper: true,
                    }}
                    rowClassName={() => 'ant-table-row'}
                />
            </div>
        );
    }

    return (
        <div style={{ padding: '24px', background: '#f0f2f5', minHeight: '100vh' }}>
            <Card
                title={
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span style={{ fontSize: '18px', fontWeight: 600 }}>网站列表</span>
                        <Space>
                            <Button icon={<PlusOutlined />} type="primary" onClick={() => window.open('/#/companies/new', '_blank')}>
                                新增公司
                            </Button>
                            <Button onClick={() => window.open('/#/excel', '_blank')}>
                                导入Excel
                            </Button>
                        </Space>
                    </div>
                }
                bordered={false}
                style={{ boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.03), 0 1px 6px -1px rgba(0, 0, 0, 0.02), 0 2px 4px 0 rgba(0, 0, 0, 0.02)' }}
            >
                <Table
                    columns={columns}
                    dataSource={companies}
                    rowKey="id"
                    loading={loading}
                    pagination={{
                        pageSize: 10,
                        showSizeChanger: true,
                        showTotal: (total) => `共 ${total} 条`,
                        showQuickJumper: true,
                    }}
                    rowClassName={() => 'ant-table-row'}
                />
            </Card>
        </div>
    );
};

export default CompanyList;
