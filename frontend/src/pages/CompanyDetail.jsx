import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, Descriptions, Button, Space, Typography, Tag, message, Divider } from 'antd';
import { ArrowLeftOutlined, EditOutlined, PlayCircleOutlined, PauseCircleOutlined } from '@ant-design/icons';
import { companyApi } from '../services/api';

const { Title, Paragraph } = Typography;

const CompanyDetail = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [company, setCompany] = useState(null);
    const [loading, setLoading] = useState(false);

    const fetchCompany = async () => {
        setLoading(true);
        try {
            const response = await companyApi.getCompanyById(id);
            setCompany(response.data);
        } catch (error) {
            message.error('获取公司详情失败');
            console.error(error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchCompany();
    }, [id]);

    const handlePublish = async () => {
        try {
            await companyApi.publishCompany(id);
            message.success('发布成功');
            fetchCompany();
        } catch (error) {
            message.error('发布失败');
            console.error(error);
        }
    };

    const handleUnpublish = async () => {
        try {
            await companyApi.unpublishCompany(id);
            message.success('取消发布成功');
            fetchCompany();
        } catch (error) {
            message.error('取消发布失败');
            console.error(error);
        }
    };

    if (loading) {
        return <div>加载中...</div>;
    }

    if (!company) {
        return <div>未找到公司信息</div>;
    }

    return (
        <div style={{ padding: '24px' }}>
            <Button
                icon={<ArrowLeftOutlined />}
                onClick={() => navigate(-1)}
                style={{ marginBottom: '16px' }}
            >
                返回
            </Button>

            <div style={{ marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Title level={2}>
                    公司详情
                    {company.isPublished ? <Tag color="green" style={{ marginLeft: '12px' }}>已发布</Tag> : <Tag color="default" style={{ marginLeft: '12px' }}>未发布</Tag>}
                </Title>
                <Space>
                    {!company.isPublished && (
                        <Button type="primary" icon={<PlayCircleOutlined />} onClick={handlePublish}>
                            发布
                        </Button>
                    )}
                    {company.isPublished && (
                        <Button icon={<PauseCircleOutlined />} onClick={handleUnpublish}>
                            取消发布
                        </Button>
                    )}
                    <Button icon={<EditOutlined />} onClick={() => navigate(`/companies/${id}/edit`)}>
                        编辑
                    </Button>
                </Space>
            </div>

            <Card>
                <Descriptions column={2} bordered>
                    <Descriptions.Item label="公司名称" span={2}>{company.companyName}</Descriptions.Item>
                    <Descriptions.Item label="域名" span={1}>{company.domain}</Descriptions.Item>
                    <Descriptions.Item label="备案号" span={1}>{company.icpNumber}</Descriptions.Item>
                    <Descriptions.Item label="法人">{company.legalRepresentative}</Descriptions.Item>
                    <Descriptions.Item label="域名负责人">{company.domainContact}</Descriptions.Item>
                    <Descriptions.Item label="腾讯云ID">{company.tencentCloudId}</Descriptions.Item>
                    <Descriptions.Item label="腾讯云邮箱">{company.tencentEmail}</Descriptions.Item>
                    <Descriptions.Item label="腾讯云手机">{company.tencentPhone}</Descriptions.Item>
                    <Descriptions.Item label="主标题">{company.mainTitle}</Descriptions.Item>
                    <Descriptions.Item label="副标题">{company.subtitle}</Descriptions.Item>
                    {company.hasWebsite && (
                        <Descriptions.Item label="是否搭建官网" span={2}>
                            <Tag color="green">是</Tag>
                        </Descriptions.Item>
                    )}
                    {company.submissionTime && (
                        <Descriptions.Item label="提交备案时间">{company.submissionTime}</Descriptions.Item>
                    )}
                    {company.smsVerifyTime && (
                        <Descriptions.Item label="短信核验时间">{company.smsVerifyTime}</Descriptions.Item>
                    )}
                    {company.approvalTime && (
                        <Descriptions.Item label="备案通过时间">{company.approvalTime}</Descriptions.Item>
                    )}
                    {company.resolutionTime && (
                        <Descriptions.Item label="解析时间">{company.resolutionTime}</Descriptions.Item>
                    )}
                    {company.publishDate && (
                        <Descriptions.Item label="发布时间" span={2}>{company.publishDate}</Descriptions.Item>
                    )}
                </Descriptions>
            </Card>

            <Divider />

            <Title level={3}>公司地址</Title>
            <Card>
                <Paragraph>{company.companyAddress || '未填写'}</Paragraph>
            </Card>

            <Divider />

            <Title level={3}>联系电话</Title>
            <Card>
                <Paragraph>{company.phone || '未填写'}</Paragraph>
            </Card>

            <Divider />

            <Title level={3}>电子邮箱</Title>
            <Card>
                <Paragraph>{company.email || '未填写'}</Paragraph>
            </Card>

            <Divider />

            <Title level={3}>微信</Title>
            <Card>
                <Paragraph>{company.wechat || '未填写'}</Paragraph>
            </Card>
        </div>
    );
};

export default CompanyDetail;
