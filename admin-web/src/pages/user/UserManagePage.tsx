import { useEffect, useState } from 'react';
import { Table, Tag, Select, Typography, message } from 'antd';
import { getUsers, updateUserRole } from '../../services/api';
import type { User } from '../../types';

const { Title } = Typography;

export default function UserManagePage() {
  const [data, setData] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchData = async () => {
    setLoading(true);
    try {
      const res: any = await getUsers();
      setData(res.data.list);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, []);

  const handleRoleChange = async (userId: string, role: string) => {
    try {
      await updateUserRole(userId, role);
      message.success('角色更新成功');
      fetchData();
    } catch {
      message.error('更新失败');
    }
  };

  const columns = [
    { title: '用户名', dataIndex: 'username', key: 'username' },
    { title: '邮箱', dataIndex: 'email', key: 'email' },
    { title: '手机号', dataIndex: 'phone', key: 'phone' },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      render: (role: string, record: User) => (
        <Select
          value={role}
          onChange={(value) => handleRoleChange(record.id, value)}
          style={{ width: 120 }}
          options={[
            { value: 'admin', label: '管理员' },
            { value: 'user', label: '普通用户' },
          ]}
        />
      ),
    },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt' },
  ];

  return (
    <div>
      <Title level={3} style={{ marginBottom: 24 }}>用户管理</Title>
      <Table dataSource={data} columns={columns} rowKey="id" loading={loading} />
    </div>
  );
}
