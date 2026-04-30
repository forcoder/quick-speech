import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Table, Button, Space, Tag, Popconfirm, Typography, message } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { getAgents, deleteAgent } from '../../services/api';
import type { AiAgent } from '../../types';

const { Title } = Typography;

export default function AgentListPage() {
  const [data, setData] = useState<AiAgent[]>([]);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const fetchData = async () => {
    setLoading(true);
    try {
      const res: any = await getAgents();
      setData(res.data.list);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, []);

  const handleDelete = async (id: string) => {
    try {
      await deleteAgent(id);
      message.success('删除成功');
      fetchData();
    } catch {
      message.error('删除失败');
    }
  };

  const columns = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
    {
      title: '模型',
      dataIndex: 'model',
      key: 'model',
      render: (model: string) => <Tag color="purple">{model}</Tag>,
    },
    {
      title: '场景绑定',
      dataIndex: 'sceneBindings',
      key: 'sceneBindings',
      render: (bindings: any[]) => (
        <Space>
          {bindings?.map((b, i) => (
            <Tag key={i} color="cyan">{b.appType}</Tag>
          ))}
        </Space>
      ),
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      key: 'enabled',
      render: (enabled: boolean) => (
        <Tag color={enabled ? 'green' : 'red'}>{enabled ? '启用' : '禁用'}</Tag>
      ),
    },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt' },
    {
      title: '操作',
      key: 'actions',
      render: (_: any, record: AiAgent) => (
        <Space>
          <Button type="link" icon={<EditOutlined />} onClick={() => navigate(`/agent/${record.id}/edit`)}>
            编辑
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
        <Title level={3} style={{ margin: 0 }}>AI智能体管理</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/agent/new')}>
          新建智能体
        </Button>
      </div>
      <Table dataSource={data} columns={columns} rowKey="id" loading={loading} />
    </div>
  );
}
