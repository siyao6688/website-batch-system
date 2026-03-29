import { useEffect, useState } from 'react';
import { Table, Button, Space, Tag, message } from 'antd';
import { PlusOutlined, EditOutlined, EyeOutlined, DeleteOutlined } from '@ant-design/icons';
import { templateApi } from '../services/api';

const TemplateList = () => {
    const [templates, setTemplates] = useState([]);
    const [loading, setLoading] = useState(false);

    const fetchTemplates = async () => {
        setLoading(true);
        try {
            const response = await templateApi.getAllTemplates();
            setTemplates(response.data || []);
        } catch (error) {
            message.error('获取模板列表失败');
            console.error(error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchTemplates();
    }, []);

    const handleDelete = async (record) => {
        try {
            await templateApi.deleteTemplate(record.id);
            message.success('删除成功');
            fetchTemplates();
        } catch (error) {
            message.error('删除失败');
            console.error(error);
        }
    };

    const columns = [
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
            render: (_, record) => (
                <Space size="small">
                    <Button
                        size="small"
                        icon={<EyeOutlined />}
                        onClick={() => handleView(record)}
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
                    <Button
                        size="small"
                        danger
                        icon={<DeleteOutlined />}
                        onClick={() => handleDelete(record)}
                    >
                        删除
                    </Button>
                </Space>
            ),
        },
    ];

    const handleView = (record) => {
        // TODO: 实现模板预览功能
        message.info(`预览模板: ${record.templateName}`);
    };

    const handleEdit = (record) => {
        // TODO: 实现模板编辑功能
        message.info(`编辑模板: ${record.templateName}`);
    };

    return (
        <div style={{ padding: '24px' }}>
            <div style={{ marginBottom: '16px', display: 'flex', justifyContent: 'space-between' }}>
                <h2>模板管理</h2>
                <Space>
                    <Button icon={<PlusOutlined />} onClick={() => message.info('新增模板功能暂未实现')}>
                        新增模板
                    </Button>
                </Space>
            </div>
            <Table
                columns={columns}
                dataSource={templates}
                rowKey="id"
                loading={loading}
                pagination={{
                    pageSize: 10,
                    showSizeChanger: true,
                    showTotal: (total) => `共 ${total} 条`,
                }}
            />
        </div>
    );
};

export default TemplateList;
