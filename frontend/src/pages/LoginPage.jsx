import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Card,
  Form,
  Input,
  Button,
  Typography,
  Space,
  Divider,
  message,
  Checkbox,
  Row,
  Col
} from 'antd';
import {
  UserOutlined,
  LockOutlined,
  EyeOutlined,
  EyeInvisibleOutlined,
  EyeTwoTone,
  DashboardOutlined,
  SafetyOutlined
} from '@ant-design/icons';
import { authApi } from '../services/api';
import './LoginPage.css';

const { Title, Text, Paragraph } = Typography;

const LoginPage = () => {
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [rememberMe, setRememberMe] = useState(true);

  // 如果已经登录，重定向到管理后台
  useEffect(() => {
    const isAuthenticated = localStorage.getItem('isAuthenticated') === 'true';
    if (isAuthenticated) {
      navigate('/admin');
    }
  }, [navigate]);

  const handleLogin = async (values) => {
    setLoading(true);
    try {
      console.log('Login values:', values);

      // 调用后端登录API
      const response = await authApi.login(values);
      const result = response.data;

      if (result.success) {
        message.success('登录成功！');

        // 保存登录状态
        localStorage.setItem('isAuthenticated', 'true');
        localStorage.setItem('username', result.username);
        localStorage.setItem('role', result.role);
        localStorage.setItem('token', result.token);

        if (rememberMe) {
          localStorage.setItem('rememberedUsername', values.username);
        }

        // 跳转到管理后台
        navigate('/admin');
      } else {
        message.error(result.message || '用户名或密码错误');
      }
    } catch (error) {
      console.error('Login error:', error);
      if (error.response && error.response.data && error.response.data.message) {
        message.error('登录失败：' + error.response.data.message);
      } else {
        message.error('登录失败：网络错误或服务器异常');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleForgotPassword = () => {
    message.info('请联系系统管理员重置密码');
  };

  return (
    <div className="login-container">
      {/* 背景装饰元素 */}
      <div className="bg-shape shape-1"></div>
      <div className="bg-shape shape-2"></div>
      <div className="bg-shape shape-3"></div>
      <div className="bg-shape shape-4"></div>

      <div className="login-wrapper">
        {/* 左侧品牌区域 */}
        <div className="login-brand-section">
          <div className="brand-content">
            <div className="brand-logo">
              <DashboardOutlined className="logo-icon" />
              <span className="logo-text">网站批量生成系统</span>
            </div>
            <Title level={2} className="brand-title">
              企业级网站批量<br />生成与管理平台
            </Title>
            <Paragraph className="brand-description">
              专业的网站批量生成解决方案，助力企业快速构建标准化网站
              提供模板管理、内容编辑、一键发布等全方位功能
            </Paragraph>

            <div className="brand-features">
              <Space direction="vertical" size="middle">
                <div className="feature-item">
                  <SafetyOutlined className="feature-icon" />
                  <Text strong>安全可靠</Text>
                  <Text type="secondary">银行级数据加密保护</Text>
                </div>
                <div className="feature-item">
                  <DashboardOutlined className="feature-icon" />
                  <Text strong>高效管理</Text>
                  <Text type="secondary">批量操作，一键部署</Text>
                </div>
                <div className="feature-item">
                  <EyeOutlined className="feature-icon" />
                  <Text strong>实时监控</Text>
                  <Text type="secondary">网站状态实时跟踪</Text>
                </div>
              </Space>
            </div>
          </div>
        </div>

        {/* 右侧登录表单区域 */}
        <Card className="login-card" hoverable>
          <div className="card-header">
            <Title level={3}>欢迎回来</Title>
            <Text type="secondary">请输入您的账号密码登录系统</Text>
          </div>

          <Form
            form={form}
            layout="vertical"
            onFinish={handleLogin}
            initialValues={{
              username: '',
              password: '',
              remember: true
            }}
            size="large"
          >
            <Form.Item
              name="username"
              rules={[
                { required: true, message: '请输入用户名' }
              ]}
            >
              <Input
                prefix={<UserOutlined />}
                placeholder="用户名"
                className="login-input"
              />
            </Form.Item>

            <Form.Item
              name="password"
              rules={[
                { required: true, message: '请输入密码' },
                { min: 6, message: '密码至少6个字符' }
              ]}
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder="密码"
                iconRender={(visible) =>
                  visible ? <EyeOutlined /> : <EyeInvisibleOutlined />
                }
                className="login-input"
              />
            </Form.Item>

            <Row justify="space-between" align="middle" className="login-options">
              <Col>
                <Form.Item name="remember" valuePropName="checked" noStyle>
                  <Checkbox
                    checked={rememberMe}
                    onChange={(e) => setRememberMe(e.target.checked)}
                  >
                    记住我
                  </Checkbox>
                </Form.Item>
              </Col>
              <Col>
                <Button type="link" onClick={handleForgotPassword} className="forgot-link">
                  忘记密码？
                </Button>
              </Col>
            </Row>

            <Form.Item>
              <Button
                type="primary"
                htmlType="submit"
                loading={loading}
                block
                size="large"
                className="login-button"
              >
                登录系统
              </Button>
            </Form.Item>


            <div className="login-footer">
              <Text type="secondary" className="version-info">
                v1.0.0 · © 2026 网站批量生成系统
              </Text>
            </div>
          </Form>
        </Card>
      </div>
    </div>
  );
};

export default LoginPage;