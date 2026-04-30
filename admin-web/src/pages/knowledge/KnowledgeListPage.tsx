import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Table, Button, Space, Tag, Modal, Form, Input, message, Popconfirm, Typography, Switch } from 'antd';
import { PlusOutlined, DeleteOutlined, FileTextOutlined } from '@ant-design/icons';
import { getKnowledgeBases, createKnowledgeBase, deleteKnowledgeBase, updateKnowledgeBase } from '../../services/api';
import type { KnowledgeBase } from '../../types';

const { Title } = Typography;

export default function KnowledgeListPage() {
  const [data, setData] = useState<KnowledgeBase[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [form] = Form.useForm();
  const navigate = useNavigate();

  const fetchData = async () => {
    setLoading(true);
    try {
      const res: any = await getKnowledgeBases();
      setData(res.data.list);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, []);

  const handleCreate = async (values: { name: string; description?: string }) => {
    try {
      await createKnowledgeBase(values);
      message.success('创建成功');
      setModalVisible(false);
      form.resetFields();
      fetchData();
    } catch (err: any) {
      message.error(err.response?.data?.message || '创建失败');
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await deleteKnowledgeBase(id);
      message.success('删除成功');
      fetchData();
    } catch (err: any) {
      message.error('删除失败');
    }
  };

  const handleToggle = async (id: string, enabled: boolean) => {
    try {
      await updateKnowledgeBase(id, { enabled });
      message.success(enabled ? '已启用' : '已禁用');
      fetchData();
    } catch {
      message.error('操作失败');
    }
  };

  const columns = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
    {
      title: '状态',
      dataIndex: 'enabled',
      key: 'enabled',
      render: (enabled: boolean, record: KnowledgeBase) => (
        <Switch
          checked={enabled}
          onChange={(checked) => handleToggle(record.id, checked)}
          checkedChildren="启用"
          unCheckedChildren="禁用"
        />
      ),
    },
    {
      title: '文档数',
      dataIndex: 'documentCount',
      key: 'documentCount',
      render: (count: number) => <Tag color="blue">{count}</Tag>,
    },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt' },
    {
      title: '操作',
      key: 'actions',
      render: (_: any, record: KnowledgeBase) => (
        <Space>
          <Button
            type="link"
            icon={<FileTextOutlined />}
            onClick={() => navigate(`/knowledge/${record.id}`)}
          >
            文档
          </Button>
          <Popconfirm title="确定删除？" onConfirm={() => handleDelete(record.id)}>
            <Button type="link" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 24 }}>
        <Title level={3} style={{ margin: 0 }}>知识库管理</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalVisible(true)}>
          新建知识库
        </Button>
      </div>

      <Table
        dataSource={data}
        columns={columns}
        rowKey="id"
        loading={loading}
      />

      <Modal
        title="新建知识库"
        open={modalVisible}
        onCancel={() => { setModalVisible(false); form.resetFields(); }}
        onOk={() => form.submit()}
      >
        <Form form={form} onFinish={handleCreate} layout="vertical">
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input placeholder="输入知识库名称" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea placeholder="输入描述（可选）" rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
