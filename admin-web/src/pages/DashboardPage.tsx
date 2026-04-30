import { useEffect, useState } from 'react';
import { Row, Col, Card, Statistic, Table, Typography } from 'antd';
import {
  UserOutlined,
  DatabaseOutlined,
  RobotOutlined,
  RiseOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import { getDashboardStats, getUsageData } from '../services/api';
import type { DashboardStats, UsageData } from '../types';

const { Title } = Typography;

export default function DashboardPage() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [usageData, setUsageData] = useState<UsageData[]>([]);

  useEffect(() => {
    getDashboardStats().then((res: any) => setStats(res.data));
    getUsageData(30).then((res: any) => setUsageData(res.data));
  }, []);

  const chartOption = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['请求数', '采纳数'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: usageData.map((d) => d.date),
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '请求数',
        type: 'line',
        smooth: true,
        data: usageData.map((d) => d.requests),
        itemStyle: { color: '#1677ff' },
      },
      {
        name: '采纳数',
        type: 'line',
        smooth: true,
        data: usageData.map((d) => d.adopted),
        itemStyle: { color: '#52c41a' },
      },
    ],
  };

  const columns = [
    { title: '日期', dataIndex: 'date', key: 'date' },
    { title: '请求数', dataIndex: 'requests', key: 'requests' },
    { title: '采纳数', dataIndex: 'adopted', key: 'adopted' },
    {
      title: '采纳率',
      key: 'rate',
      render: (_: any, record: UsageData) =>
        record.requests > 0
          ? `${((record.adopted / record.requests) * 100).toFixed(1)}%`
          : '0%',
    },
  ];

  return (
    <div>
      <Title level={3} style={{ marginBottom: 24 }}>数据看板</Title>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="总用户数"
              value={stats?.totalUsers ?? 0}
              prefix={<UserOutlined />}
              valueStyle={{ color: '#1677ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="活跃用户"
              value={stats?.activeUsers ?? 0}
              prefix={<RiseOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="知识库数"
              value={stats?.totalKnowledgeBases ?? 0}
              prefix={<DatabaseOutlined />}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="AI智能体"
              value={stats?.totalAgents ?? 0}
              prefix={<RobotOutlined />}
              valueStyle={{ color: '#fa8c16' }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col span={24}>
          <Card title="近30天使用趋势">
            <ReactECharts option={chartOption} style={{ height: 300 }} />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col span={24}>
          <Card title="每日使用详情">
            <Table
              dataSource={usageData}
              columns={columns}
              rowKey="date"
              pagination={{ pageSize: 10 }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col span={12}>
          <Card>
            <Statistic
              title="日均请求"
              value={stats?.dailyRequests ?? 0}
              prefix={<RiseOutlined />}
            />
          </Card>
        </Col>
        <Col span={12}>
          <Card>
            <Statistic
              title="整体采纳率"
              value={stats?.adoptionRate ?? 0}
              precision={1}
              suffix="%"
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
}
