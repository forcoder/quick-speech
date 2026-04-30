import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Button, Table, Upload, message, Typography, Card, Progress, Input, Space, Tag } from 'antd';
import { UploadOutlined, SearchOutlined, FileOutlined } from '@ant-design/icons';
import { getDocuments, uploadDocument, searchKnowledge } from '../../services/api';
import type { KnowledgeDocument } from '../../types';

const { Title } = Typography;

export default function KnowledgeDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [documents, setDocuments] = useState<KnowledgeDocument[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchResults, setSearchResults] = useState<any[]>([]);
  const [searchQuery, setSearchQuery] = useState('');

  const fetchDocuments = async () => {
    setLoading(true);
    try {
      const res: any = await getDocuments(id!);
      setDocuments(res.data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchDocuments(); }, [id]);

  const handleUpload = async (options: any) => {
    const { file, onSuccess, onError } = options;
    const formData = new FormData();
    formData.append('file', file);
    try {
      await uploadDocument(id!, formData);
      message.success('上传成功');
      fetchDocuments();
      onSuccess();
    } catch (err: any) {
      message.error('上传失败');
      onError(err);
    }
  };

  const handleSearch = async () => {
    if (!searchQuery.trim()) return;
    try {
      const res: any = await searchKnowledge(id!, searchQuery);
      setSearchResults(res.data);
    } catch {
      message.error('搜索失败');
    }
  };

  const statusMap: Record<string, { text: string; color: string }> = {
    processing: { text: '处理中', color: 'processing' },
    ready: { text: '就绪', color: 'success' },
    error: { text: '错误', color: 'error' },
  };

  const columns = [
    { title: '文件名', dataIndex: 'name', key: 'name' },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      render: (type: string) => <Tag>{type.toUpperCase()}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        const s = statusMap[status] || { text: status, color: 'default' };
        return <Tag color={s.color}>{s.text}</Tag>;
      },
    },
    { title: '分块数', dataIndex: 'chunkCount', key: 'chunkCount' },
    { title: '上传时间', dataIndex: 'createdAt', key: 'createdAt' },
  ];

  return (
    <div>
      <Title level={3} style={{ marginBottom: 24 }}>知识库文档</Title>

      <Card style={{ marginBottom: 16 }}>
        <Space>
          <Upload customRequest={handleUpload} showUploadList={false}>
            <Button type="primary" icon={<UploadOutlined />}>上传文档</Button>
          </Upload>
          <span style={{ color: '#888' }}>支持 PDF、Word、Excel、TXT、Markdown</span>
        </Space>
      </Card>

      <Card title="文档列表" style={{ marginBottom: 16 }}>
        <Table
          dataSource={documents}
          columns={columns}
          rowKey="id"
          loading={loading}
        />
      </Card>

      <Card title="检索测试">
        <Space.Compact style={{ width: '100%', marginBottom: 16 }}>
          <Input
            placeholder="输入搜索关键词..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onPressEnter={handleSearch}
          />
          <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
            搜索
          </Button>
        </Space.Compact>
        {searchResults.map((result, index) => (
          <Card key={index} size="small" style={{ marginBottom: 8 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
              <Tag color="blue">相关度: {((result.score || 0) * 100).toFixed(1)}%</Tag>
            </div>
            <p>{result.content}</p>
          </Card>
        ))}
      </Card>
    </div>
  );
}
