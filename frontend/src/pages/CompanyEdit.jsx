import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Form, Input, DatePicker, Button, Space, message, Card, Divider, Switch, Row, Col, Tag } from 'antd';
import { ArrowLeftOutlined, SaveOutlined, EyeTwoTone, EyeInvisibleOutlined } from '@ant-design/icons';
import { companyApi } from '../services/api';

const { TextArea } = Input;
const { RangePicker } = DatePicker;

const CompanyEdit = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [form] = Form.useForm();
    const [loading, setLoading] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const isEdit = id && id !== 'new';

    useEffect(() => {
        if (isEdit) {
            fetchCompany();
        }
    }, [id]);

    const fetchCompany = async () => {
        setLoading(true);
        try {
            const response = await companyApi.getCompanyById(id);
            form.setFieldsValue(response.data);
        } catch (error) {
            message.error('获取公司信息失败');
            console.error(error);
        } finally {
            setLoading(false);
        }
    };

    const handleSubmit = async (values) => {
        setSubmitting(true);
        try {
            // 确保域名以.cn结尾
            if (values.domain && !values.domain.toLowerCase().endsWith('.cn')) {
                values.domain = values.domain + '.cn';
            }

            if (isEdit) {
                await companyApi.updateCompany(id, values);
                message.success('更新成功');
            } else {
                await companyApi.createCompany(values);
                message.success('创建成功');
            }
            navigate(-1);
        } catch (error) {
            message.error(isEdit ? '更新失败' : '创建失败');
            console.error(error);
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div style={{ padding: '24px' }}>
            <Button
                icon={<ArrowLeftOutlined />}
                onClick={() => navigate(-1)}
                style={{ marginBottom: '16px' }}
            >
                返回
            </Button>

            <Form
                form={form}
                layout="vertical"
                onFinish={handleSubmit}
                initialValues={{
                    hasWebsite: false,
                    isActive: true,
                    isPublished: false
                }}
            >
                <Card title="基本信息" style={{ marginBottom: '16px' }}>
                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item
                                name="companyName"
                                label="公司名称"
                                rules={[{ required: true, message: '请输入公司名称' }]}
                            >
                                <Input placeholder="请输入公司名称" />
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item
                                name="domain"
                                label="域名"
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
                                  addonAfter=".cn"
                                />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item
                                name="mainTitle"
                                label="主标题"
                            >
                                <Input placeholder="请输入主标题" />
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item
                                name="subtitle"
                                label="副标题"
                            >
                                <Input placeholder="请输入副标题" />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item
                                name="legalRepresentative"
                                label="法人"
                            >
                                <Input placeholder="请输入法人姓名" />
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item
                                name="domainContact"
                                label="域名负责人"
                            >
                                <Input placeholder="请输入域名负责人" />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item
                                name="icpNumber"
                                label="备案号"
                            >
                                <Input placeholder="请输入备案号" />
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item
                                name="tencentCloudId"
                                label="腾讯云ID"
                            >
                                <Input placeholder="请输入腾讯云ID" />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item
                                name="tencentEmail"
                                label="腾讯云登录邮箱"
                            >
                                <Input placeholder="请输入腾讯云登录邮箱" />
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item
                                name="tencentPhone"
                                label="腾讯云手机号"
                            >
                                <Input placeholder="请输入腾讯云手机号" />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item
                                name="tencentPassword"
                                label="腾讯云登录密码"
                            >
                                <Input.Password placeholder="请输入腾讯云登录密码" />
                            </Form.Item>
                        </Col>
                    </Row>
                </Card>

                <Card title="备案信息" style={{ marginBottom: '16px' }}>
                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item
                                name="hasWebsite"
                                label="是否搭建官网"
                                valuePropName="checked"
                            >
                                <Switch checkedChildren="是" unCheckedChildren="否" />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item label="提交备案时间">
                                <DatePicker style={{ width: '100%' }} />
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item label="短信核验时间">
                                <DatePicker style={{ width: '100%' }} />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item label="备案通过时间">
                                <DatePicker style={{ width: '100%' }} />
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item label="解析时间">
                                <DatePicker style={{ width: '100%' }} />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={24}>
                            <Form.Item
                                name="companyAddress"
                                label="公司地址"
                            >
                                <TextArea rows={3} placeholder="请输入公司地址" />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item
                                name="phone"
                                label="联系电话"
                            >
                                <Input placeholder="请输入联系电话" />
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item
                                name="email"
                                label="电子邮箱"
                            >
                                <Input placeholder="请输入电子邮箱" />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={24}>
                            <Form.Item
                                name="wechat"
                                label="微信"
                            >
                                <Input placeholder="请输入微信" />
                            </Form.Item>
                        </Col>
                    </Row>
                </Card>

                <Card title="状态设置" style={{ marginBottom: '16px' }}>
                    <Row gutter={16}>
                        <Col span={12}>
                            <Form.Item
                                name="isActive"
                                label="激活状态"
                                valuePropName="checked"
                            >
                                <Switch checkedChildren="激活" unCheckedChildren="禁用" />
                            </Form.Item>
                        </Col>
                        <Col span={12}>
                            <Form.Item
                                name="isPublished"
                                label="发布状态"
                                valuePropName="checked"
                            >
                                <Switch checkedChildren="已发布" unCheckedChildren="未发布" />
                            </Form.Item>
                        </Col>
                    </Row>
                </Card>

                <Form.Item>
                    <Space>
                        <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={submitting}>
                            {isEdit ? '保存' : '创建'}
                        </Button>
                        <Button onClick={() => navigate(-1)}>取消</Button>
                    </Space>
                </Form.Item>
            </Form>
        </div>
    );
};

export default CompanyEdit;
