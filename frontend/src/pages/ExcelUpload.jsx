import { useEffect, useState } from 'react';
import { Button, Upload, Table, message, Space, Typography, Divider, Card } from 'antd';
import { UploadOutlined, DownloadOutlined } from '@ant-design/icons';
import { excelApi } from '../services/api';

const { Title, Text } = Typography;

// 下载Excel模板
const downloadTemplate = () => {
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

const ExcelUpload = () => {
    const [loading, setLoading] = useState(false);
    const [companies, setCompanies] = useState([]);
    const [uploading, setUploading] = useState(false);

    const fetchCompanies = async () => {
        setLoading(true);
        try {
            const response = await excelApi.getImportedCompanies();
            if (Array.isArray(response.data)) {
                setCompanies(response.data);
            } else {
                console.error('获取到的数据格式不正确:', response.data);
                message.error('获取公司列表失败：数据格式不正确');
            }
        } catch (error) {
            console.error('获取导入的公司列表失败:', error);
            if (error.response) {
                message.error('获取公司列表失败：' + (error.response.data || error.message));
            } else {
                message.error('获取公司列表失败：' + (error.message || '网络错误'));
            }
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchCompanies();
    }, []);

    const handleUpload = async (options) => {
        console.log('handleUpload called', options);
        const { file, onSuccess, onError } = options;
        setUploading(true);

        try {
            const fileToUpload = file.originFileObj || file;
            console.log('file object:', fileToUpload);
            const response = await excelApi.uploadExcel(fileToUpload);
            console.log('upload response:', response);
            message.success('Excel导入成功');
            // 调用 Ant Design 的 onSuccess 回调
            if (onSuccess) {
                onSuccess(response.data, file, response);
            }
            fetchCompanies();
        } catch (error) {
            console.error('导入错误:', error);
            let errorMessage = 'Excel导入失败';
            if (error.response) {
                errorMessage += `：${error.response.data || error.message}`;
            } else {
                errorMessage += `：${error.message || '网络错误'}`;
            }
            message.error(errorMessage);
            // 调用 Ant Design 的 onError 回调
            if (onError) {
                onError(error, file, error.response);
            }
        } finally {
            setUploading(false);
        }
    };

    const columns = [
        {
            title: '序号',
            dataIndex: 'serialNumber',
            key: 'serialNumber',
            width: 80,
        },
        {
            title: '公司名称',
            dataIndex: 'companyName',
            key: 'companyName',
            width: 200,
        },
        {
            title: '邮箱',
            dataIndex: 'email',
            key: 'email',
            width: 200,
        },
        {
            title: '域名',
            dataIndex: 'domain',
            key: 'domain',
            width: 200,
        },
        {
            title: '备案号',
            dataIndex: 'icpNumber',
            key: 'icpNumber',
            width: 150,
        },
        {
            title: '创建时间',
            dataIndex: 'createdAt',
            key: 'createdAt',
            render: (text) => text ? new Date(text).toLocaleString() : '-',
        },
    ];

    return (
        <div style={{ padding: '24px' }}>
            <div style={{ marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Title level={2}>Excel导入</Title>
            </div>

            <Card style={{ marginBottom: '24px' }}>
                <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                    <div>
                        <Text strong>步骤1: 下载模板</Text>
                        <p>请先下载Excel模板，按照模板格式填写公司信息</p>
                        <Button
                            icon={<DownloadOutlined />}
                            onClick={downloadTemplate}
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
                            customRequest={handleUpload}
                            showUploadList={false}
                            accept=".xlsx,.xls,.csv"
                        >
                            <Button
                                type="primary"
                                icon={<UploadOutlined />}
                                loading={uploading}
                            >
                                上传Excel文件
                            </Button>
                        </Upload>
                    </div>

                    <Divider />

                    <div>
                        <Text strong>步骤3: 查看导入结果</Text>
                        <p>上传成功后可查看已导入的公司列表</p>
                    </div>
                </Space>
            </Card>

            <Card title="已导入的公司列表">
                <Table
                    columns={columns}
                    dataSource={companies}
                    rowKey="id"
                    loading={loading}
                    pagination={{
                        pageSize: 10,
                        showSizeChanger: true,
                        showTotal: (total) => `共 ${total} 条`,
                    }}
                />
            </Card>
        </div>
    );
};

export default ExcelUpload;
