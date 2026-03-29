import { Modal, Typography, Row, Col, Tag, Space, Divider, Card } from 'antd';
import {
  EyeOutlined,
  FileTextOutlined,
  GlobalOutlined,
  IdcardOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined
} from '@ant-design/icons';

const { Title, Text, Paragraph } = Typography;

const TemplatePreviewModal = ({ open, onCancel, template }) => {
  if (!template) return null;

  // 根据模板代码获取颜色方案
  const getColorScheme = (templateCode) => {
    const schemes = {
      standard: {
        primary: '#4361ee',
        primaryLight: '#4895ef',
        primaryDark: '#3a0ca3',
        secondary: '#7209b7',
        accent: '#f72585',
        name: '标准蓝色调'
      },
      modern: {
        primary: '#2196F3',
        primaryLight: '#64B5F6',
        primaryDark: '#1976D2',
        secondary: '#4CAF50',
        accent: '#FF9800',
        name: '现代蓝绿色调'
      },
      classic: {
        primary: '#B71C1C',
        primaryLight: '#E53935',
        primaryDark: '#8B0000',
        secondary: '#D4AF37',
        accent: '#5D4037',
        name: '经典红金色调'
      }
    };
    return schemes[templateCode] || schemes.standard;
  };

  const colorScheme = getColorScheme(template.templateCode);

  return (
    <Modal
      title={
        <Space>
          <EyeOutlined />
          <span>模板预览 - {template.templateName}</span>
        </Space>
      }
      open={open}
      onCancel={onCancel}
      width={800}
      footer={null}
    >
      <div style={{ padding: '20px 0' }}>
        {/* 模板基本信息 */}
        <Card style={{ marginBottom: 16 }}>
          <Title level={4} style={{ marginBottom: 16 }}>
            <FileTextOutlined style={{ marginRight: 8 }} />
            模板信息
          </Title>
          <Row gutter={[16, 16]}>
            <Col span={12}>
              <Text strong>模板名称:</Text>
              <Paragraph style={{ marginTop: 4 }}>{template.templateName}</Paragraph>
            </Col>
            <Col span={12}>
              <Text strong>模板代码:</Text>
              <Paragraph style={{ marginTop: 4 }}>
                <Tag color="blue">{template.templateCode}</Tag>
              </Paragraph>
            </Col>
            <Col span={24}>
              <Text strong>描述:</Text>
              <Paragraph style={{ marginTop: 4 }}>{template.description || '暂无描述'}</Paragraph>
            </Col>
            <Col span={12}>
              <Text strong>状态:</Text>
              <Paragraph style={{ marginTop: 4 }}>
                {template.isActive ? (
                  <Tag icon={<CheckCircleOutlined />} color="success">启用</Tag>
                ) : (
                  <Tag icon={<ClockCircleOutlined />} color="error">禁用</Tag>
                )}
              </Paragraph>
            </Col>
            <Col span={12}>
              <Text strong>标准模板:</Text>
              <Paragraph style={{ marginTop: 4 }}>
                {template.isStandard ? (
                  <Tag color="green">是</Tag>
                ) : (
                  <Tag color="default">否</Tag>
                )}
              </Paragraph>
            </Col>
          </Row>
        </Card>

        {/* 颜色方案预览 */}
        <Card style={{ marginBottom: 16 }}>
          <Title level={4} style={{ marginBottom: 16 }}>
            <GlobalOutlined style={{ marginRight: 8 }} />
            颜色方案 - {colorScheme.name}
          </Title>
          <Row gutter={[16, 16]}>
            <Col span={4}>
              <div style={{ textAlign: 'center' }}>
                <div
                  style={{
                    width: '60px',
                    height: '60px',
                    backgroundColor: colorScheme.primary,
                    borderRadius: '8px',
                    margin: '0 auto 8px',
                    boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
                  }}
                />
                <Text strong>主色</Text>
                <div style={{ fontSize: '12px', color: '#666' }}>{colorScheme.primary}</div>
              </div>
            </Col>
            <Col span={4}>
              <div style={{ textAlign: 'center' }}>
                <div
                  style={{
                    width: '60px',
                    height: '60px',
                    backgroundColor: colorScheme.primaryLight,
                    borderRadius: '8px',
                    margin: '0 auto 8px',
                    boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
                  }}
                />
                <Text strong>主色浅</Text>
                <div style={{ fontSize: '12px', color: '#666' }}>{colorScheme.primaryLight}</div>
              </div>
            </Col>
            <Col span={4}>
              <div style={{ textAlign: 'center' }}>
                <div
                  style={{
                    width: '60px',
                    height: '60px',
                    backgroundColor: colorScheme.primaryDark,
                    borderRadius: '8px',
                    margin: '0 auto 8px',
                    boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
                  }}
                />
                <Text strong>主色深</Text>
                <div style={{ fontSize: '12px', color: '#666' }}>{colorScheme.primaryDark}</div>
              </div>
            </Col>
            <Col span={4}>
              <div style={{ textAlign: 'center' }}>
                <div
                  style={{
                    width: '60px',
                    height: '60px',
                    backgroundColor: colorScheme.secondary,
                    borderRadius: '8px',
                    margin: '0 auto 8px',
                    boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
                  }}
                />
                <Text strong>辅色</Text>
                <div style={{ fontSize: '12px', color: '#666' }}>{colorScheme.secondary}</div>
              </div>
            </Col>
            <Col span={4}>
              <div style={{ textAlign: 'center' }}>
                <div
                  style={{
                    width: '60px',
                    height: '60px',
                    backgroundColor: colorScheme.accent,
                    borderRadius: '8px',
                    margin: '0 auto 8px',
                    boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
                  }}
                />
                <Text strong>强调色</Text>
                <div style={{ fontSize: '12px', color: '#666' }}>{colorScheme.accent}</div>
              </div>
            </Col>
          </Row>
        </Card>

        {/* 样式预览 */}
        <Card>
          <Title level={4} style={{ marginBottom: 16 }}>
            <IdcardOutlined style={{ marginRight: 8 }} />
            样式预览
          </Title>
          <div style={{
            padding: '24px',
            backgroundColor: '#f8f9fa',
            borderRadius: '8px',
            border: '1px solid #e9ecef'
          }}>
            {/* 导航栏预览 */}
            <div style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              padding: '12px 20px',
              backgroundColor: 'rgba(255, 255, 255, 0.95)',
              borderRadius: '8px',
              marginBottom: '20px',
              boxShadow: '0 2px 8px rgba(0,0,0,0.08)'
            }}>
              <div style={{
                display: 'flex',
                alignItems: 'center',
                color: colorScheme.primary,
                fontWeight: 'bold'
              }}>
                <div style={{
                  width: '24px',
                  height: '24px',
                  backgroundColor: colorScheme.primary,
                  borderRadius: '4px',
                  marginRight: '8px'
                }} />
                {template.templateName}
              </div>
              <div style={{ display: 'flex', gap: '16px' }}>
                <span style={{ color: '#666', cursor: 'pointer' }}>首页</span>
                <span style={{ color: '#666', cursor: 'pointer' }}>产品</span>
                <span style={{ color: '#666', cursor: 'pointer' }}>服务</span>
                <span style={{ color: '#666', cursor: 'pointer' }}>关于</span>
              </div>
            </div>

            {/* 按钮预览 */}
            <div style={{ display: 'flex', gap: '16px', marginBottom: '20px' }}>
              <button style={{
                padding: '8px 16px',
                backgroundColor: colorScheme.primary,
                color: 'white',
                border: 'none',
                borderRadius: '6px',
                cursor: 'pointer',
                fontWeight: '500'
              }}>
                主要按钮
              </button>
              <button style={{
                padding: '8px 16px',
                backgroundColor: 'transparent',
                color: colorScheme.primary,
                border: `2px solid ${colorScheme.primary}`,
                borderRadius: '6px',
                cursor: 'pointer',
                fontWeight: '500'
              }}>
                次要按钮
              </button>
              <button style={{
                padding: '8px 16px',
                backgroundColor: colorScheme.accent,
                color: 'white',
                border: 'none',
                borderRadius: '6px',
                cursor: 'pointer',
                fontWeight: '500'
              }}>
                强调按钮
              </button>
            </div>

            {/* 卡片预览 */}
            <div style={{
              display: 'flex',
              gap: '16px',
              marginBottom: '20px'
            }}>
              <div style={{
                flex: 1,
                padding: '16px',
                backgroundColor: 'white',
                borderRadius: '8px',
                boxShadow: '0 4px 12px rgba(0,0,0,0.08)'
              }}>
                <div style={{
                  width: '40px',
                  height: '40px',
                  backgroundColor: colorScheme.primary,
                  borderRadius: '20px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'white',
                  marginBottom: '12px'
                }}>
                  ✓
                </div>
                <div style={{ fontWeight: '600', marginBottom: '8px' }}>功能卡片</div>
                <div style={{ fontSize: '14px', color: '#666' }}>
                  这是一个示例卡片，展示模板的卡片样式
                </div>
              </div>
              <div style={{
                flex: 1,
                padding: '16px',
                backgroundColor: 'white',
                borderRadius: '8px',
                boxShadow: '0 4px 12px rgba(0,0,0,0.08)'
              }}>
                <div style={{
                  width: '40px',
                  height: '40px',
                  backgroundColor: colorScheme.secondary,
                  borderRadius: '20px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'white',
                  marginBottom: '12px'
                }}>
                  ✨
                </div>
                <div style={{ fontWeight: '600', marginBottom: '8px' }}>特色功能</div>
                <div style={{ fontSize: '14px', color: '#666' }}>
                  展示模板的配色和布局效果
                </div>
              </div>
            </div>

            {/* 标签预览 */}
            <div style={{ display: 'flex', gap: '8px' }}>
              <span style={{
                padding: '4px 12px',
                backgroundColor: colorScheme.primaryLight,
                color: 'white',
                borderRadius: '16px',
                fontSize: '14px'
              }}>
                标签一
              </span>
              <span style={{
                padding: '4px 12px',
                backgroundColor: colorScheme.secondary,
                color: 'white',
                borderRadius: '16px',
                fontSize: '14px'
              }}>
                标签二
              </span>
              <span style={{
                padding: '4px 12px',
                backgroundColor: colorScheme.accent,
                color: 'white',
                borderRadius: '16px',
                fontSize: '14px'
              }}>
                标签三
              </span>
            </div>
          </div>
        </Card>

        <Divider />

        <div style={{ textAlign: 'center', color: '#999', fontSize: '14px' }}>
          此预览展示了模板的基本样式和颜色方案，实际生成网站时将使用完整的HTML模板。
        </div>
      </div>
    </Modal>
  );
};

export default TemplatePreviewModal;