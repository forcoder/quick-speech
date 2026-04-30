import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Form, Input, InputNumber, Select, Switch, Button, Card, Space, message, Typography } from 'antd';
import { SendOutlined } from '@ant-design/icons';
import { createAgent, updateAgent, testAgent } from '../../services/api';

const { Title } = Typography;
const { TextArea } = Input;
const { Option } = Select;

const MODEL_OPTIONS = [
  'gpt-4o',
  'gpt-4o-mini',
  'claude-3-5-sonnet',
  'claude-3-haiku',
  'qwen-turbo',
  'qwen-plus',
  'qwen-max',
];

const SCENE_OPTIONS = [
  { value: 'email', label: '邮件' },
  { value: 'im', label: '即时通讯' },
  { value: 'document', label: '文档编辑' },
  { value: 'other', label: '其他' },
];

export default function AgentEditPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEdit = !!id && id !== 'new';
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [testMessage, setTestMessage] = useState('');
  const [testReply, setTestReply] = useState('');
  const [testLoading, setTestLoading] = useState(false);

  const handleSubmit = async (values: any) => {
    setLoading(true);
    try {
      if (isEdit) {
        await updateAgent(id!, values);
        message.success('更新成功');
      } else {
        await createAgent(values);
        message.success('创建成功');
      }
      navigate('/agent');
    } catch (err: any) {
      message.error(err.response?.data?.message || '保存失败');
    } finally {
      setLoading(false);
    }
  };

  const handleTest = async () => {
    if (!testMessage.trim()) {
      message.warning('请输入测试消息');
      return;
    }
    setTestLoading(true);
    try {
      const res: any = await testAgent(id!, testMessage);
      setTestReply(res.data.reply);
    } catch {
      message.error('测试失败');
    } finally {
      setTestLoading(false);
    }
  };

  return (
    <div>
      <Title level={3} style={{ marginBottom: 24 }}>
        {isEdit ? '编辑智能体' : '新建智能体'}
      </Title>

      <Card style={{ marginBottom: 16 }}>
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          initialValues={{
            temperature: 0.7,
            maxTokens: 2048,
            enabled: true,
            model: 'gpt-4o',
          }}
        >
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input placeholder="输入智能体名称" />
          </Form.Item>

          <Form.Item name="description" label="描述">
            <Input placeholder="输入描述（可选）" />
          </Form.Item>

          <Form.Item name="promptTemplate" label="Prompt模板" rules={[{ required: true, message: '请输入Prompt模板' }]}>
            <TextArea
              placeholder="输入系统Prompt，可用变量：{{context}}、{{knowledge}}、{{userStyle}}"
              rows={8}
            />
          </Form.Item>

          <Form.Item name="model" label="模型" rules={[{ required: true }]}>
            <Select>
              {MODEL_OPTIONS.map((m) => (
                <Option key={m} value={m}>{m}</Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item name="temperature" label="Temperature">
            <InputNumber min={0} max={2} step={0.1} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item name="maxTokens" label="最大Token数">
            <InputNumber min={256} max={8192} step={256} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item name="sceneBindings" label="场景绑定">
            <Select mode="multiple" placeholder="选择绑定的场景" options={SCENE_OPTIONS} />
          </Form.Item>

          <Form.Item name="enabled" label="启用" valuePropName="checked">
            <Switch />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" loading={loading}>
                保存
              </Button>
              <Button onClick={() => navigate('/agent')}>取消</Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      {isEdit && (
        <Card title="测试对话">
          <Space.Compact style={{ width: '100%', marginBottom: 16 }}>
            <Input
              placeholder="输入测试消息..."
              value={testMessage}
              onChange={(e) => setTestMessage(e.target.value)}
            />
            <Button type="primary" icon={<SendOutlined />} onClick={handleTest} loading={testLoading}>
              发送
            </Button>
          </Space.Compact>
          {testReply && (
            <Card size="small" style={{ background: '#f5f5f5' }}>
              <p style={{ whiteSpace: 'pre-wrap' }}>{testReply}</p>
            </Card>
          )}
        </Card>
      )}
    </div>
  );
}
