<template>
  <div class="usstock-report">
    <!-- 左侧区域：筛选表单 + 图表 -->
    <div class="left-panel">
      <!-- 统计卡片 -->
      <a-row :gutter="[0, 12]">
        <a-col :span="12">
          <a-card size="small" class="stat-card up">
            <div class="stat-title">上涨家数</div>
            <div class="stat-value up">{{ upCount }}</div>
            <div class="stat-sub">{{ upPercent }}%</div>
          </a-card>
        </a-col>
        <a-col :span="12">
          <a-card size="small" class="stat-card down">
            <div class="stat-title">下跌家数</div>
            <div class="stat-value down">{{ downCount }}</div>
            <div class="stat-sub">{{ downPercent }}%</div>
          </a-card>
        </a-col>
        <a-col :span="12">
          <a-card size="small" class="stat-card">
            <div class="stat-title">平盘家数</div>
            <div class="stat-value">{{ flatCount }}</div>
            <div class="stat-sub">{{ flatPercent }}%</div>
          </a-card>
        </a-col>
        <a-col :span="12">
          <a-card size="small" class="stat-card">
            <div class="stat-title">平均涨幅</div>
            <div class="stat-value" :class="avgChangePct >= 0 ? 'up' : 'down'">
              {{ avgChangePct >= 0 ? '+' : '' }}{{ avgChangePct }}%
            </div>
            <div class="stat-sub">{{ currentDate }}</div>
          </a-card>
        </a-col>
      </a-row>

      <!-- 涨跌分布饼图 -->
      <a-card title="涨跌分布" size="small" class="chart-card" :bordered="false">
        <Pie :chart-data="upDownPieData" height="180px" :option="upDownPieOption" />
      </a-card>

      <!-- 行业分布饼图 -->
      <a-card title="行业分布" size="small" class="chart-card" :bordered="false">
        <Pie :chart-data="sectorPieData" height="180px" :option="sectorPieOption" />
      </a-card>

      <!-- 涨跌幅排行榜 -->
      <a-card title="涨跌幅 TOP10" size="small" class="chart-card" :bordered="false">
        <Bar :chart-data="top10BarData" height="240px" :option="top10BarOption" />
      </a-card>

      <!-- 成交量分布 -->
      <a-card title="成交量分布" size="small" class="chart-card" :bordered="false">
        <Bar :chart-data="volumeBarData" height="180px" :option="volumeBarOption" />
      </a-card>

      <!-- 筛选表单 -->
      <a-card title="筛选条件" size="small" class="filter-card" :bordered="false">
        <a-form layout="vertical" :model="filterForm">
          <a-form-item label="行业板块">
            <a-select v-model:value="filterForm.sector" placeholder="请选择" allow-clear>
              <a-select-option value="Technology">科技</a-select-option>
              <a-select-option value="Consumer">消费</a-select-option>
              <a-select-option value="Finance">金融</a-select-option>
              <a-select-option value="Healthcare">医疗</a-select-option>
              <a-select-option value="Energy">能源</a-select-option>
              <a-select-option value="Industrial">工业</a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item label="涨跌幅范围">
            <a-slider v-model:value="filterForm.changePctRange" range :min="-10" :max="10" :step="0.5" />
          </a-form-item>

          <a-form-item label="涨跌状态">
            <a-radio-group v-model:value="filterForm.changeType">
              <a-radio-button value="all">全部</a-radio-button>
              <a-radio-button value="up">上涨</a-radio-button>
              <a-radio-button value="down">下跌</a-radio-button>
            </a-radio-group>
          </a-form-item>

          <a-form-item label="股票搜索">
            <a-input v-model:value="filterForm.keyword" placeholder="代码/名称" allow-clear>
              <template #prefix><SearchOutlined /></template>
            </a-input>
          </a-form-item>
        </a-form>

        <div class="filter-actions">
          <a-space direction="vertical" style="width: 100%">
            <a-button type="primary" block @click="handleFilter">
              <FilterOutlined />筛选
            </a-button>
            <a-button block @click="handleReset">
              <ReloadOutlined />重置
            </a-button>
          </a-space>
        </div>
      </a-card>
    </div>

    <!-- 右侧数据表格 -->
    <div class="table-content">
      <a-card :bordered="false" title="上周五美股主流股票涨跌行情">
        <template #extra>
          <a-space>
            <a-tag color="blue">{{ currentDate }}</a-tag>
            <a-button type="primary" size="small" @click="exportData">
              <DownloadOutlined />导出
            </a-button>
          </a-space>
        </template>
        <a-table
          :columns="columns"
          :data-source="filteredData"
          :loading="loading"
          :pagination="{ pageSize: 20 }"
          :scroll="{ x: 1400 }"
          row-key="symbol"
          size="small"
        >
          <template #bodyCell="{ column, record, text }">
            <template v-if="column.dataIndex === 'change'">
              <span :style="{ color: text >= 0 ? '#cf1322' : '#3f8600', fontWeight: 'bold' }">
                {{ text >= 0 ? '+' : '' }}{{ text }}
              </span>
            </template>
            <template v-if="column.dataIndex === 'changePct'">
              <a-tag :color="text >= 0 ? 'red' : 'green'">
                {{ text >= 0 ? '+' : '' }}{{ text }}%
              </a-tag>
            </template>
            <template v-if="column.dataIndex === 'volume'">
              {{ formatVolume(text) }}
            </template>
            <template v-if="column.dataIndex === 'sector'">
              <a-tag color="blue">{{ formatSector(text) }}</a-tag>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { ref, onMounted, computed, reactive } from 'vue';
  import axios from 'axios';
  import {
    SearchOutlined,
    FilterOutlined,
    ReloadOutlined,
    DownloadOutlined,
  } from '@ant-design/icons-vue';
  import { message } from 'ant-design-vue';
  import Pie from '/@/components/chart/Pie.vue';
  import Bar from '/@/components/chart/Bar.vue';

  interface StockData {
    symbol: string;
    name: string;
    sector: string;
    closePrice: number;
    prevClose: number;
    change: number;
    changePct: number;
    openPrice: number;
    highPrice: number;
    lowPrice: number;
    volume: number;
    marketCap: string;
    tradeDate: string;
  }

  interface FilterForm {
    sector: string | undefined;
    changePctRange: number[];
    changeType: 'all' | 'up' | 'down';
    keyword: string;
  }

  const loading = ref(false);
  const dataSource = ref<StockData[]>([]);
  const currentDate = ref('');

  const filterForm = reactive<FilterForm>({
    sector: undefined,
    changePctRange: [-10, 10],
    changeType: 'all',
    keyword: '',
  });

  const columns = [
    { title: '股票代码', dataIndex: 'symbol', width: 100, fixed: 'left' as const },
    { title: '股票名称', dataIndex: 'name', width: 150, fixed: 'left' as const },
    { title: '行业板块', dataIndex: 'sector', width: 100 },
    { title: '收盘价($)', dataIndex: 'closePrice', width: 100, align: 'right' as const },
    { title: '前日收盘($)', dataIndex: 'prevClose', width: 110, align: 'right' as const },
    { title: '涨跌额($)', dataIndex: 'change', width: 100, sorter: true, align: 'right' as const },
    { title: '涨跌幅(%)', dataIndex: 'changePct', width: 100, sorter: true, align: 'right' as const },
    { title: '开盘价($)', dataIndex: 'openPrice', width: 100, align: 'right' as const },
    { title: '最高价($)', dataIndex: 'highPrice', width: 100, align: 'right' as const },
    { title: '最低价($)', dataIndex: 'lowPrice', width: 100, align: 'right' as const },
    { title: '成交量', dataIndex: 'volume', width: 120, align: 'right' as const },
    { title: '市值', dataIndex: 'marketCap', width: 100 },
    { title: '交易日期', dataIndex: 'tradeDate', width: 110 },
  ];

  // 过滤后的数据
  const filteredData = computed(() => {
    return dataSource.value.filter((item) => {
      if (filterForm.sector && item.sector !== filterForm.sector) return false;
      if (item.changePct < filterForm.changePctRange[0] || item.changePct > filterForm.changePctRange[1]) return false;
      if (filterForm.changeType === 'up' && item.changePct <= 0) return false;
      if (filterForm.changeType === 'down' && item.changePct >= 0) return false;
      if (filterForm.keyword) {
        const keyword = filterForm.keyword.toLowerCase();
        return item.symbol.toLowerCase().includes(keyword) || item.name.toLowerCase().includes(keyword);
      }
      return true;
    });
  });

  // 统计数据
  const upCount = computed(() => filteredData.value.filter((item) => item.changePct > 0).length);
  const downCount = computed(() => filteredData.value.filter((item) => item.changePct < 0).length);
  const flatCount = computed(() => filteredData.value.filter((item) => item.changePct === 0).length);
  const totalCount = computed(() => filteredData.value.length);

  const upPercent = computed(() => totalCount.value ? ((upCount.value / totalCount.value) * 100).toFixed(1) : '0');
  const downPercent = computed(() => totalCount.value ? ((downCount.value / totalCount.value) * 100).toFixed(1) : '0');
  const flatPercent = computed(() => totalCount.value ? ((flatCount.value / totalCount.value) * 100).toFixed(1) : '0');

  const avgChangePct = computed(() => {
    if (filteredData.value.length === 0) return 0;
    const sum = filteredData.value.reduce((acc, item) => acc + item.changePct, 0);
    return (sum / filteredData.value.length).toFixed(2);
  });

  // ========== 图表数据 ==========

  // 涨跌分布饼图数据
  const upDownPieData = computed(() => [
    { name: '上涨', value: upCount.value, itemStyle: { color: '#cf1322' } },
    { name: '下跌', value: downCount.value, itemStyle: { color: '#3f8600' } },
    { name: '平盘', value: flatCount.value, itemStyle: { color: '#999' } },
  ]);

  const upDownPieOption = {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, left: 'center', textStyle: { fontSize: 11 } },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: false,
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 12, fontWeight: 'bold' } },
      data: [],
    }],
  };

  // 行业分布饼图数据
  const sectorPieData = computed(() => {
    const sectorCount: Record<string, number> = {};
    filteredData.value.forEach((item) => {
      sectorCount[item.sector] = (sectorCount[item.sector] || 0) + 1;
    });
    const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272'];
    return Object.entries(sectorCount).map(([name, value], index) => ({
      name: formatSector(name),
      value,
      itemStyle: { color: colors[index % colors.length] },
    }));
  });

  const sectorPieOption = {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { type: 'scroll', bottom: 0, left: 'center', textStyle: { fontSize: 10 } },
    series: [{
      type: 'pie',
      radius: '65%',
      center: ['50%', '45%'],
      data: [],
      label: { fontSize: 10 },
    }],
  };

  // 涨跌幅 TOP10 条形图数据
  const top10BarData = computed(() => {
    const sorted = [...filteredData.value].sort((a, b) => Math.abs(b.changePct) - Math.abs(a.changePct));
    return sorted.slice(0, 10).map((item) => ({
      name: item.symbol,
      value: item.changePct,
      itemStyle: { color: item.changePct >= 0 ? '#cf1322' : '#3f8600' },
    })).sort((a, b) => b.value - a.value);
  });

  const top10BarOption = {
    tooltip: { trigger: 'axis', formatter: '{b}: {c}%' },
    grid: { left: 60, right: 20, top: 10, bottom: 20 },
    xAxis: { type: 'value', axisLabel: { formatter: '{value}%' } },
    yAxis: { type: 'category', data: [], axisLabel: { fontSize: 10 } },
    series: [{
      type: 'bar',
      data: [],
      barWidth: 14,
      label: { show: true, position: 'right', formatter: '{c}%', fontSize: 9 },
    }],
  };

  // 成交量分布条形图
  const volumeBarData = computed(() => {
    const ranges = [
      { name: '<100万', min: 0, max: 1000000 },
      { name: '100-500万', min: 1000000, max: 5000000 },
      { name: '500-1000万', min: 5000000, max: 10000000 },
      { name: '1000-5000万', min: 10000000, max: 50000000 },
      { name: '>5000万', min: 50000000, max: Infinity },
    ];
    return ranges.map((range) => ({
      name: range.name,
      value: filteredData.value.filter((item) => item.volume >= range.min && item.volume < range.max).length,
    }));
  });

  const volumeBarOption = {
    tooltip: { trigger: 'axis' },
    grid: { left: 80, right: 20, top: 10, bottom: 20 },
    xAxis: { type: 'category', data: [], axisLabel: { fontSize: 10 } },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: [],
      barWidth: 20,
      itemStyle: { color: '#1890ff' },
      label: { show: true, position: 'top', fontSize: 10 },
    }],
  };

  function formatVolume(vol: number): string {
    if (vol >= 100000000) return (vol / 100000000).toFixed(2) + '亿';
    if (vol >= 10000) return (vol / 10000).toFixed(0) + '万';
    return vol.toLocaleString();
  }

  function formatSector(sector: string): string {
    const sectorMap: Record<string, string> = {
      Technology: '科技', Consumer: '消费', Finance: '金融',
      Healthcare: '医疗', Energy: '能源', Industrial: '工业',
    };
    return sectorMap[sector] || sector;
  }

  async function fetchData() {
    loading.value = true;
    try {
      const res = await axios.get('/jeecgboot/test/usstock/list');
      if (res.data?.success && res.data.result) {
        dataSource.value = res.data.result;
        if (dataSource.value.length > 0) {
          currentDate.value = dataSource.value[0].tradeDate;
        }
      }
    } catch (e) {
      console.error('获取美股数据失败', e);
      message.error('获取美股数据失败');
    } finally {
      loading.value = false;
    }
  }

  function handleFilter() {
    message.success(`筛选完成，共 ${filteredData.value.length} 条数据`);
  }

  function handleReset() {
    filterForm.sector = undefined;
    filterForm.changePctRange = [-10, 10];
    filterForm.changeType = 'all';
    filterForm.keyword = '';
    message.success('已重置筛选条件');
  }

  function exportData() {
    const headers = columns.map((col) => col.title).join(',');
    const rows = filteredData.value.map((item) =>
      columns.map((col) => {
        const val = item[col.dataIndex as keyof StockData];
        return typeof val === 'string' && val.includes(',') ? `"${val}"` : val;
      }).join(',')
    );
    const csv = [headers, ...rows].join('\n');
    const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `美股行情_${currentDate.value || 'export'}.csv`;
    link.click();
    message.success('导出成功');
  }

  onMounted(() => {
    fetchData();
  });
</script>

<style scoped>
  .usstock-report {
    display: flex;
    gap: 16px;
    padding: 16px;
    min-height: calc(100vh - 120px);
  }

  .left-panel {
    width: 320px;
    flex-shrink: 0;
  }

  .table-content {
    flex: 1;
    min-width: 0;
  }

  /* 统计卡片 */
  .stat-card {
    margin-bottom: 8px;
  }

  .stat-card.up {
    border-left: 3px solid #cf1322;
  }

  .stat-card.down {
    border-left: 3px solid #3f8600;
  }

  .stat-title {
    font-size: 12px;
    color: #666;
    margin-bottom: 4px;
  }

  .stat-value {
    font-size: 20px;
    font-weight: bold;
    color: #333;
  }

  .stat-value.up {
    color: #cf1322;
  }

  .stat-value.down {
    color: #3f8600;
  }

  .stat-sub {
    font-size: 11px;
    color: #999;
    margin-top: 2px;
  }

  /* 图表卡片 */
  .chart-card {
    margin-bottom: 12px;
  }

  .chart-card :deep(.ant-card-head) {
    background-color: #fafafa;
    padding: 8px 12px;
    min-height: 36px;
  }

  .chart-card :deep(.ant-card-head-title) {
    font-size: 13px;
    font-weight: 600;
  }

  .chart-card :deep(.ant-card-body) {
    padding: 8px;
  }

  /* 筛选表单 */
  .filter-card {
    margin-top: 8px;
  }

  .filter-card :deep(.ant-card-head) {
    background-color: #fafafa;
  }

  .filter-actions {
    margin-top: 16px;
  }

  /* 表格样式 */
  :deep(.ant-table-thead > tr > th) {
    background-color: #4472c4 !important;
    color: #fff !important;
    font-weight: bold;
  }

  /* 响应式 */
  @media screen and (max-width: 1200px) {
    .usstock-report {
      flex-direction: column;
    }

    .left-panel {
      width: 100%;
    }
  }
</style>
