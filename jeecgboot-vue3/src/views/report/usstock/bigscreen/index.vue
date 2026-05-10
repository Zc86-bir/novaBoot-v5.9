<template>
  <div class="usstock-bigscreen">
    <!-- 顶部标题栏 -->
    <div class="screen-header">
      <div class="header-left">
        <div class="logo">
          <BarChartOutlined class="logo-icon" />
        </div>
        <div class="title-area">
          <h1 class="main-title">美股行情实时看板</h1>
          <p class="sub-title">{{ currentDate }} 数据实时更新 | 共{{ dataSource.length }}支股票</p>
        </div>
      </div>
      <div class="header-right">
        <!-- 市场状态 -->
        <div class="market-status" :style="{ borderColor: marketStatusText.color }">
          <component :is="marketStatusText.icon" class="status-icon" :style="{ color: marketStatusText.color }" />
          <div class="status-info">
            <div class="status-text" :style="{ color: marketStatusText.color }">{{ marketStatusText.text }}</div>
            <div class="countdown-text">{{ countdownText }}</div>
          </div>
        </div>

        <div class="market-indices">
          <div class="index-item" v-for="(index, i) in marketIndices" :key="i">
            <span class="index-name">{{ index.name }}</span>
            <span class="index-value" :class="index.change >= 0 ? 'up' : 'down'">
              {{ index.value }}
              <small>({{ index.change >= 0 ? '+' : '' }}{{ index.change }}%)</small>
            </span>
          </div>
        </div>

        <div class="update-time">
          <ClockCircleOutlined />
          <span>{{ updateTime }}</span>
        </div>

        <!-- 自动更新开关 -->
        <a-switch
          v-model:checked="isAutoUpdate"
          class="auto-update-switch"
          @change="toggleAutoUpdate"
        >
          <template #checkedChildren><SyncOutlined spin /></template>
          <template #unCheckedChildren><SyncOutlined /></template>
        </a-switch>

        <!-- 手动刷新 -->
        <a-button type="default" class="refresh-btn" @click="manualRefresh" :loading="loading">
          <ReloadOutlined />
          刷新
        </a-button>

        <a-button type="primary" class="fullscreen-btn" @click="toggleFullscreen">
          <FullscreenOutlined />
          {{ isFullscreen ? '退出全屏' : '全屏' }}
        </a-button>
      </div>
    </div>

    <!-- 主体内容 -->
    <div class="screen-body">
      <!-- 第一行：核心统计指标 -->
      <div class="stats-row">
        <div class="stat-card" v-for="(stat, index) in coreStats" :key="index">
          <div class="stat-bg" :style="{ background: stat.gradient }"></div>
          <div class="stat-content">
            <div class="stat-header">
              <component :is="stat.icon" class="stat-icon" />
              <span class="stat-title">{{ stat.title }}</span>
            </div>
            <div class="stat-body">
              <div class="stat-value" :style="{ color: stat.color }">{{ stat.value }}</div>
              <div class="stat-trend" :class="stat.change >= 0 ? 'up' : 'down'">
                <ArrowUpOutlined v-if="stat.change >= 0" />
                <ArrowDownOutlined v-else />
                {{ Math.abs(stat.change) }}%
              </div>
            </div>
            <div class="stat-detail" v-if="stat.detail">
              {{ stat.detail }}
            </div>
          </div>
        </div>
      </div>

      <!-- 第二行：详细统计数据 -->
      <div class="detail-stats-row">
        <div class="detail-stat-item" v-for="(item, index) in detailStats" :key="index">
          <div class="detail-label">{{ item.label }}</div>
          <div class="detail-value" :class="item.isPositive ? 'up' : item.isNegative ? 'down' : ''">
            {{ item.value }}
          </div>
          <div class="detail-desc">{{ item.desc }}</div>
        </div>
      </div>

      <!-- 中间图表区域 -->
      <div class="main-charts">
        <!-- 左侧图表列 -->
        <div class="chart-column left">
          <!-- 行业分布 -->
          <div class="chart-box">
            <div class="chart-header">
              <div class="header-title">
                <span class="title-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)"></span>
                <span class="title-text">行业分布</span>
              </div>
              <div class="header-tag">按数量</div>
            </div>
            <div class="chart-content">
              <Pie :chart-data="sectorPieData" height="220px" :option="sectorPieOption" />
            </div>
            <!-- 行业统计详情 -->
            <div class="sector-detail">
              <div class="sector-row" v-for="(sector, idx) in sectorStats.slice(0, 4)" :key="idx">
                <span class="sector-name">{{ sector.name }}</span>
                <div class="sector-bar">
                  <div class="sector-fill" :style="{ width: sector.percent + '%', background: sector.color }"></div>
                </div>
                <span class="sector-count">{{ sector.count }}家</span>
              </div>
            </div>
          </div>

          <!-- 涨跌分布 -->
          <div class="chart-box">
            <div class="chart-header">
              <div class="header-title">
                <span class="title-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)"></span>
                <span class="title-text">涨跌分布</span>
              </div>
              <div class="header-tag active">实时</div>
            </div>
            <div class="chart-content">
              <Pie :chart-data="upDownPieData" height="200px" :option="upDownPieOption" />
            </div>
            <!-- 涨跌区间统计 -->
            <div class="change-buckets">
              <div class="bucket" v-for="(bucket, idx) in changeBuckets" :key="idx">
                <div class="bucket-range">{{ bucket.range }}</div>
                <div class="bucket-bar">
                  <div class="bucket-fill" :class="bucket.type" :style="{ width: bucket.percent + '%' }"></div>
                </div>
                <div class="bucket-count">{{ bucket.count }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 中间主图表 -->
        <div class="chart-column center">
          <div class="chart-box main-chart">
            <div class="chart-header">
              <div class="header-title">
                <span class="title-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)"></span>
                <span class="title-text">涨跌幅排行榜 TOP20</span>
              </div>
              <a-radio-group v-model:value="rankType" size="small" class="rank-tabs" @change="handleRankChange">
                <a-radio-button value="up">
                  <RiseOutlined /> 涨幅榜
                </a-radio-button>
                <a-radio-button value="down">
                  <FallOutlined /> 跌幅榜
                </a-radio-button>
                <a-radio-button value="volume">
                  <BarChartOutlined /> 成交量
                </a-radio-button>
                <a-radio-button value="amplitude">
                  <LineChartOutlined /> 振幅
                </a-radio-button>
              </a-radio-group>
            </div>
            <div class="chart-content">
              <Bar :chart-data="rankBarData" height="520px" :option="rankBarOption" />
            </div>
          </div>

          <!-- 中间底部：价格区间分布 -->
          <div class="chart-box price-chart">
            <div class="chart-header">
              <div class="header-title">
                <span class="title-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)"></span>
                <span class="title-text">股价区间分布</span>
              </div>
            </div>
            <div class="chart-content">
              <Bar :chart-data="priceRangeData" height="200px" :option="priceRangeOption" />
            </div>
          </div>
        </div>

        <!-- 右侧图表列 -->
        <div class="chart-column right">
          <!-- 成交量分布 -->
          <div class="chart-box">
            <div class="chart-header">
              <div class="header-title">
                <span class="title-icon" style="background: linear-gradient(135deg, #fa709a 0%, #fee140 100%)"></span>
                <span class="title-text">成交量分布</span>
              </div>
            </div>
            <div class="chart-content">
              <Bar :chart-data="volumeBarData" height="220px" :option="volumeBarOption" />
            </div>
            <!-- 成交量统计 -->
            <div class="volume-stats">
              <div class="vol-stat">
                <span class="vol-label">总成交量</span>
                <span class="vol-value">{{ totalVolume }}</span>
              </div>
              <div class="vol-stat">
                <span class="vol-label">平均成交</span>
                <span class="vol-value">{{ avgVolume }}</span>
              </div>
            </div>
          </div>

          <!-- 市值分布 -->
          <div class="chart-box">
            <div class="chart-header">
              <div class="header-title">
                <span class="title-icon" style="background: linear-gradient(135deg, #30cfd0 0%, #330867 100%)"></span>
                <span class="title-text">市值分布</span>
              </div>
            </div>
            <div class="chart-content">
              <Bar :chart-data="marketCapBarData" height="200px" :option="marketCapBarOption" />
            </div>
            <!-- 市值统计 -->
            <div class="cap-stats">
              <div class="cap-item" v-for="(cap, idx) in marketCapStats" :key="idx">
                <div class="cap-icon" :style="{ background: cap.color }"></div>
                <div class="cap-info">
                  <div class="cap-name">{{ cap.name }}</div>
                  <div class="cap-value">{{ cap.value }}家</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 日内走势统计 -->
          <div class="chart-box">
            <div class="chart-header">
              <div class="header-title">
                <span class="title-icon" style="background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)"></span>
                <span class="title-text">日内走势</span>
              </div>
            </div>
            <div class="intraday-stats">
              <div class="intra-item">
                <div class="intra-label">高开</div>
                <div class="intra-value up">{{ gapUpCount }} <small>({{ gapUpPercent }}%)</small></div>
              </div>
              <div class="intra-item">
                <div class="intra-label">低开</div>
                <div class="intra-value down">{{ gapDownCount }} <small>({{ gapDownPercent }}%)</small></div>
              </div>
              <div class="intra-item">
                <div class="intra-label">收阳</div>
                <div class="intra-value up">{{ bullishCount }} <small>({{ bullishPercent }}%)</small></div>
              </div>
              <div class="intra-item">
                <div class="intra-label">收阴</div>
                <div class="intra-value down">{{ bearishCount }} <small>({{ bearishPercent }}%)</small></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部数据表格 -->
      <div class="bottom-section">
        <div class="chart-box table-box">
          <div class="chart-header">
            <div class="header-title">
              <span class="title-icon" style="background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)"></span>
              <span class="title-text">实时行情数据明细</span>
              <span class="table-subtitle">显示涨跌幅绝对值前50名</span>
            </div>
            <div class="header-actions">
              <a-radio-group v-model:value="tableFilter" size="small" class="table-filter">
                <a-radio-button value="all">全部</a-radio-button>
                <a-radio-button value="up">上涨</a-radio-button>
                <a-radio-button value="down">下跌</a-radio-button>
              </a-radio-group>
              <a-input-search
                v-model:value="searchKeyword"
                placeholder="搜索股票代码/名称"
                style="width: 200px"
                size="small"
                @search="handleSearch"
                class="dark-search"
              />
            </div>
          </div>
          <div class="table-content">
            <a-table
              :columns="columns"
              :data-source="filteredTableData"
              :pagination="{ pageSize: 8, size: 'small' }"
              size="small"
              :scroll="{ x: 1300 }"
              row-key="symbol"
              class="dark-table"
            >
              <template #bodyCell="{ column, record, text }">
                <template v-if="column.dataIndex === 'rank'">
                  <div class="rank-cell" :class="getRankClass(text)">{{ text }}</div>
                </template>
                <template v-if="column.dataIndex === 'change'">
                  <span :class="text >= 0 ? 'text-up' : 'text-down'">
                    {{ text >= 0 ? '+' : '' }}{{ text }}
                  </span>
                </template>
                <template v-if="column.dataIndex === 'changePct'">
                  <span :class="text >= 0 ? 'text-up' : 'text-down'" class="change-pill">
                    {{ text >= 0 ? '+' : '' }}{{ text }}%
                  </span>
                </template>
                <template v-if="column.dataIndex === 'amplitude'">
                  <span class="amplitude-value">{{ text }}%</span>
                </template>
                <template v-if="column.dataIndex === 'volume'">
                  {{ formatVolume(text) }}
                </template>
                <template v-if="column.dataIndex === 'turnover'">
                  <span class="turnover-value">{{ text }}亿</span>
                </template>
                <template v-if="column.dataIndex === 'sector'">
                  <a-tag :color="getSectorColor(text)" class="sector-tag">{{ formatSector(text) }}</a-tag>
                </template>
                <template v-if="column.dataIndex === 'highLow'">
                  <div class="hl-range">
                    <span class="hl-low">{{ record.lowPrice }}</span>
                    <span class="hl-separator">~</span>
                    <span class="hl-high">{{ record.highPrice }}</span>
                  </div>
                </template>
              </template>
            </a-table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { ref, onMounted, computed, onUnmounted } from 'vue';
  import axios from 'axios';
  import { message } from 'ant-design-vue';
  import {
    FullscreenOutlined,
    ClockCircleOutlined,
    BarChartOutlined,
    RiseOutlined,
    FallOutlined,
    ArrowUpOutlined,
    ArrowDownOutlined,
    LineChartOutlined,
    DashboardOutlined,
    FundOutlined,
    ReloadOutlined,
    SyncOutlined,
    CalendarOutlined,
  } from '@ant-design/icons-vue';
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

  const loading = ref(false);
  const dataSource = ref<StockData[]>([]);
  const displayData = ref<StockData[]>([]); // 用于平滑显示的过渡数据
  const prevDataSource = ref<StockData[]>([]); // 上一次数据
  const priceChangeMap = ref<Record<string, 'up' | 'down' | 'none'>>({}); // 价格变化标记
  const animationFrame = ref<number | null>(null); // 动画帧
  const currentDate = ref('');
  const updateTime = ref('');
  const isFullscreen = ref(false);
  const rankType = ref<'up' | 'down' | 'volume' | 'amplitude'>('up');
  const searchKeyword = ref('');
  const tableFilter = ref<'all' | 'up' | 'down'>('all');
  const isAutoUpdate = ref(true);
  const autoUpdateInterval = ref<number | null>(null);
  const countdown = ref(60);
  const countdownInterval = ref<number | null>(null);
  const marketStatus = ref<'closed' | 'pre' | 'open' | 'after'>('closed');

  // 模拟市场指数数据
  const marketIndices = ref([
    { name: '标普500', value: '4,783.45', change: 0.85 },
    { name: '纳斯达克', value: '15,056.62', change: 1.23 },
    { name: '道琼斯', value: '37,545.33', change: 0.42 },
  ]);

  // 扩展表格列 - 使用百分比实现自适应列宽
  const columns = [
    { title: '排名', dataIndex: 'rank', width: '5%', minWidth: 50, align: 'center' },
    { title: '代码', dataIndex: 'symbol', width: '8%', minWidth: 70 },
    { title: '名称', dataIndex: 'name', width: '12%', minWidth: 100, ellipsis: true },
    { title: '行业', dataIndex: 'sector', width: '8%', minWidth: 70, align: 'center' },
    { title: '收盘价($)', dataIndex: 'closePrice', width: '9%', minWidth: 85, align: 'right' },
    { title: '涨跌额($)', dataIndex: 'change', width: '9%', minWidth: 85, align: 'right' },
    { title: '涨跌幅', dataIndex: 'changePct', width: '8%', minWidth: 80, align: 'right' },
    { title: '振幅', dataIndex: 'amplitude', width: '7%', minWidth: 70, align: 'right' },
    { title: '成交量', dataIndex: 'volume', width: '9%', minWidth: 90, align: 'right' },
    { title: '成交额(亿)', dataIndex: 'turnover', width: '10%', minWidth: 95, align: 'right' },
    { title: '最高/最低($)', dataIndex: 'highLow', width: '12%', minWidth: 110, align: 'center' },
    { title: '市值', dataIndex: 'marketCap', width: '8%', minWidth: 75, align: 'center' },
  ];

  // ===== 核心统计数据 =====
  const coreStats = computed(() => [
    {
      title: '总股票数',
      value: dataSource.value.length,
      change: 0,
      color: '#4facfe',
      gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      icon: DashboardOutlined,
      detail: '涵盖6大行业板块',
    },
    {
      title: '上涨家数',
      value: upCount.value,
      change: parseFloat(upPercent.value),
      color: '#ff6b6b',
      gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
      icon: RiseOutlined,
      detail: `占比${upPercent.value}%`,
    },
    {
      title: '下跌家数',
      value: downCount.value,
      change: -parseFloat(downPercent.value),
      color: '#51cf66',
      gradient: 'linear-gradient(135deg, #30cfd0 0%, #330867 100%)',
      icon: FallOutlined,
      detail: `占比${downPercent.value}%`,
    },
    {
      title: '平均涨幅',
      value: (parseFloat(avgChangePct.value) >= 0 ? '+' : '') + avgChangePct.value + '%',
      change: parseFloat(avgChangePct.value),
      color: parseFloat(avgChangePct.value) >= 0 ? '#ff6b6b' : '#51cf66',
      gradient: parseFloat(avgChangePct.value) >= 0
        ? 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)'
        : 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
      icon: FundOutlined,
      detail: `振幅均值${avgAmplitude.value}%`,
    },
  ]);

  // ===== 详细统计数据 =====
  const detailStats = computed(() => [
    { label: '最高涨幅', value: maxChange.value, desc: maxChangeStock.value, isPositive: true },
    { label: '最大跌幅', value: minChange.value, desc: minChangeStock.value, isNegative: true },
    { label: '最大振幅', value: maxAmplitude.value + '%', desc: maxAmplitudeStock.value },
    { label: '最高成交', value: maxVolumeStock.value, desc: formatVolume(maxVolume.value) },
    { label: '最大市值', value: maxCapStock.value, desc: maxCap.value },
    { label: '活跃板块', value: mostActiveSector.value, desc: activeSectorCount.value + '家上涨' },
  ]);

  // ===== 基础统计数据计算 =====
  const upCount = computed(() => displayData.value.filter((item) => item.changePct > 0).length);
  const downCount = computed(() => displayData.value.filter((item) => item.changePct < 0).length);
  const flatCount = computed(() => displayData.value.filter((item) => item.changePct === 0).length);
  const totalCount = computed(() => displayData.value.length);

  const upPercent = computed(() => totalCount.value ? ((upCount.value / totalCount.value) * 100).toFixed(1) : '0');
  const downPercent = computed(() => totalCount.value ? ((downCount.value / totalCount.value) * 100).toFixed(1) : '0');

  const avgChangePct = computed(() => {
    if (displayData.value.length === 0) return '0.00';
    const sum = displayData.value.reduce((acc, item) => acc + item.changePct, 0);
    return (sum / displayData.value.length).toFixed(2);
  });

  // ===== 扩展统计数据 =====
  const avgAmplitude = computed(() => {
    if (displayData.value.length === 0) return '0.00';
    const sum = displayData.value.reduce((acc, item) => {
      const amp = ((item.highPrice - item.lowPrice) / item.prevClose) * 100;
      return acc + amp;
    }, 0);
    return (sum / displayData.value.length).toFixed(2);
  });

  const maxChange = computed(() => {
    if (displayData.value.length === 0) return '0.00%';
    const max = Math.max(...displayData.value.map((item) => item.changePct));
    return '+' + max.toFixed(2) + '%';
  });

  const maxChangeStock = computed(() => {
    if (displayData.value.length === 0) return '-';
    const stock = displayData.value.reduce((max, item) => item.changePct > max.changePct ? item : max);
    return stock.symbol;
  });

  const minChange = computed(() => {
    if (displayData.value.length === 0) return '0.00%';
    const min = Math.min(...displayData.value.map((item) => item.changePct));
    return min.toFixed(2) + '%';
  });

  const minChangeStock = computed(() => {
    if (displayData.value.length === 0) return '-';
    const stock = displayData.value.reduce((min, item) => item.changePct < min.changePct ? item : min);
    return stock.symbol;
  });

  const maxAmplitude = computed(() => {
    if (displayData.value.length === 0) return 0;
    const max = Math.max(...displayData.value.map((item) =>
      ((item.highPrice - item.lowPrice) / item.prevClose) * 100
    ));
    return max.toFixed(2);
  });

  const maxAmplitudeStock = computed(() => {
    if (displayData.value.length === 0) return '-';
    const stock = displayData.value.reduce((max, item) => {
      const amp = ((item.highPrice - item.lowPrice) / item.prevClose);
      const maxAmp = ((max.highPrice - max.lowPrice) / max.prevClose);
      return amp > maxAmp ? item : max;
    });
    return stock.symbol;
  });

  const maxVolume = computed(() => {
    if (displayData.value.length === 0) return 0;
    return Math.max(...displayData.value.map((item) => item.volume));
  });

  const maxVolumeStock = computed(() => {
    if (displayData.value.length === 0) return '-';
    const stock = displayData.value.reduce((max, item) => item.volume > max.volume ? item : max);
    return stock.symbol;
  });

  const maxCap = computed(() => {
    if (displayData.value.length === 0) return '-';
    const stock = displayData.value.reduce((max, item) => parseMarketCap(item.marketCap) > parseMarketCap(max.marketCap) ? item : max);
    return stock.marketCap;
  });

  const maxCapStock = computed(() => {
    if (displayData.value.length === 0) return '-';
    const stock = displayData.value.reduce((max, item) => parseMarketCap(item.marketCap) > parseMarketCap(max.marketCap) ? item : max);
    return stock.symbol;
  });

  const mostActiveSector = computed(() => {
    const sectorUpCount: Record<string, number> = {};
    displayData.value.filter((item) => item.changePct > 0).forEach((item) => {
      sectorUpCount[item.sector] = (sectorUpCount[item.sector] || 0) + 1;
    });
    const entries = Object.entries(sectorUpCount);
    if (entries.length === 0) return '-';
    const maxSector = entries.reduce((max, [sector, count]) => count > max[1] ? [sector, count] : max);
    return formatSector(maxSector[0]);
  });

  const activeSectorCount = computed(() => {
    const sectorUpCount: Record<string, number> = {};
    displayData.value.filter((item) => item.changePct > 0).forEach((item) => {
      sectorUpCount[item.sector] = (sectorUpCount[item.sector] || 0) + 1;
    });
    const entries = Object.entries(sectorUpCount);
    if (entries.length === 0) return 0;
    return entries.reduce((max, [_, count]) => count > max ? count : max, 0);
  });

  // ===== 日内走势统计 =====
  const gapUpCount = computed(() => displayData.value.filter((item) => item.openPrice > item.prevClose).length);
  const gapDownCount = computed(() => displayData.value.filter((item) => item.openPrice < item.prevClose).length);
  const gapUpPercent = computed(() => totalCount.value ? ((gapUpCount.value / totalCount.value) * 100).toFixed(1) : '0');
  const gapDownPercent = computed(() => totalCount.value ? ((gapDownCount.value / totalCount.value) * 100).toFixed(1) : '0');

  const bullishCount = computed(() => displayData.value.filter((item) => item.closePrice > item.openPrice).length);
  const bearishCount = computed(() => displayData.value.filter((item) => item.closePrice < item.openPrice).length);
  const bullishPercent = computed(() => totalCount.value ? ((bullishCount.value / totalCount.value) * 100).toFixed(1) : '0');
  const bearishPercent = computed(() => totalCount.value ? ((bearishCount.value / totalCount.value) * 100).toFixed(1) : '0');

  // ===== 成交量统计 =====
  const totalVolume = computed(() => {
    const sum = displayData.value.reduce((acc, item) => acc + item.volume, 0);
    return formatVolume(sum);
  });

  const avgVolume = computed(() => {
    if (displayData.value.length === 0) return '0';
    const avg = displayData.value.reduce((acc, item) => acc + item.volume, 0) / displayData.value.length;
    return formatVolume(avg);
  });

  // ===== 行业统计 =====
  const sectorStats = computed(() => {
    const sectorData: Record<string, { count: number; up: number; color: string }> = {};
    const colors = ['#4facfe', '#00f2fe', '#43e97b', '#fa709a', '#feca57', '#ff9ff3'];
    displayData.value.forEach((item, index) => {
      if (!sectorData[item.sector]) {
        sectorData[item.sector] = { count: 0, up: 0, color: colors[index % colors.length] };
      }
      sectorData[item.sector].count++;
      if (item.changePct > 0) sectorData[item.sector].up++;
    });
    const total = displayData.value.length;
    return Object.entries(sectorData).map(([name, data]) => ({
      name: formatSector(name),
      count: data.count,
      up: data.up,
      percent: total ? ((data.count / total) * 100).toFixed(1) : 0,
      color: data.color,
    })).sort((a, b) => b.count - a.count);
  });

  // ===== 涨跌区间统计 =====
  const changeBuckets = computed(() => {
    const buckets = [
      { range: '>5%', count: 0, type: 'up-strong' },
      { range: '3%~5%', count: 0, type: 'up-mid' },
      { range: '0~3%', count: 0, type: 'up-weak' },
      { range: '-3%~0', count: 0, type: 'down-weak' },
      { range: '-5%~-3%', count: 0, type: 'down-mid' },
      { range: '<-5%', count: 0, type: 'down-strong' },
    ];
    displayData.value.forEach((item) => {
      const pct = item.changePct;
      if (pct > 5) buckets[0].count++;
      else if (pct > 3) buckets[1].count++;
      else if (pct > 0) buckets[2].count++;
      else if (pct > -3) buckets[3].count++;
      else if (pct > -5) buckets[4].count++;
      else buckets[5].count++;
    });
    const maxCount = Math.max(...buckets.map((b) => b.count));
    return buckets.map((b) => ({ ...b, percent: maxCount ? (b.count / maxCount) * 100 : 0 }));
  });

  // ===== 市值统计 =====
  const marketCapStats = computed(() => {
    const stats = [
      { name: '超大市值', range: '>2000亿', color: '#ff6b6b' },
      { name: '大市值', range: '100-2000亿', color: '#feca57' },
      { name: '中市值', range: '20-100亿', color: '#48dbfb' },
      { name: '小市值', range: '<20亿', color: '#1dd1a1' },
    ];
    return stats.map((s) => ({
      ...s,
      value: displayData.value.filter((item) => {
        const cap = parseMarketCap(item.marketCap);
        if (s.range === '>2000亿') return cap >= 2000;
        if (s.range === '100-2000亿') return cap >= 100 && cap < 2000;
        if (s.range === '20-100亿') return cap >= 20 && cap < 100;
        return cap < 20;
      }).length,
    }));
  });

  // ===== 涨跌分布饼图 =====
  const upDownPieData = computed(() => [
    { name: '上涨', value: upCount.value, itemStyle: { color: '#ff6b6b' } },
    { name: '下跌', value: downCount.value, itemStyle: { color: '#51cf66' } },
    { name: '平盘', value: flatCount.value, itemStyle: { color: '#868e96' } },
  ]);

  const upDownPieOption = {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 5, left: 'center', textStyle: { color: '#fff', fontSize: 10 } },
    series: [{
      type: 'pie',
      radius: ['35%', '55%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: false,
      label: { show: true, formatter: '{b}\n{d}%', color: '#fff', fontSize: 10 },
      data: [],
    }],
  };

  // ===== 行业分布饼图 =====
  const sectorPieData = computed(() => sectorStats.value.map((s) => ({
    name: s.name,
    value: s.count,
    itemStyle: { color: s.color },
  })));

  const sectorPieOption = {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', right: 5, top: 'center', textStyle: { color: '#fff', fontSize: 10 } },
    series: [{
      type: 'pie',
      radius: '55%',
      center: ['35%', '50%'],
      data: [],
      label: { color: '#fff', fontSize: 9 },
    }],
  };

  // ===== 排行榜数据 =====
  const rankBarData = computed(() => {
    let sorted: StockData[] = [];
    if (rankType.value === 'up') {
      sorted = [...displayData.value].sort((a, b) => b.changePct - a.changePct).slice(0, 20);
    } else if (rankType.value === 'down') {
      sorted = [...displayData.value].sort((a, b) => a.changePct - b.changePct).slice(0, 20);
    } else if (rankType.value === 'volume') {
      sorted = [...displayData.value].sort((a, b) => b.volume - a.volume).slice(0, 20);
    } else {
      sorted = [...displayData.value].sort((a, b) => {
        const ampA = ((a.highPrice - a.lowPrice) / a.prevClose);
        const ampB = ((b.highPrice - b.lowPrice) / b.prevClose);
        return ampB - ampA;
      }).slice(0, 20);
    }
    return sorted.map((item) => {
      let value = item.changePct;
      if (rankType.value === 'volume') value = item.volume;
      else if (rankType.value === 'amplitude') value = ((item.highPrice - item.lowPrice) / item.prevClose) * 100;
      return {
        name: `${item.symbol} ${item.name}`,
        value,
        itemStyle: { color: item.changePct >= 0 ? '#ff6b6b' : '#51cf66' },
      };
    }).reverse();
  });

  const rankBarOption = computed(() => ({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      borderColor: 'rgba(255, 255, 255, 0.2)',
      textStyle: { color: '#fff' },
      formatter: (params: any[]) => {
        const item = params[0];
        const parts = item.name.split(' ');
        const symbol = parts[0];
        const name = parts.slice(1).join(' ');
        const stock = displayData.value.find((s) => s.symbol === symbol);
        if (rankType.value === 'volume') return `${symbol} ${name}<br/>成交量: ${formatVolume(item.value)}`;
        if (rankType.value === 'amplitude') return `${symbol} ${name}<br/>振幅: ${item.value.toFixed(2)}%`;
        return `${symbol} ${name}<br/>涨跌幅: ${stock ? (stock.changePct >= 0 ? '+' : '') + stock.changePct + '%' : ''}`;
      },
    },
    grid: { left: 140, right: 50, top: 15, bottom: 15 },
    xAxis: {
      type: 'value',
      axisLabel: {
        color: 'rgba(255, 255, 255, 0.7)',
        fontSize: 9,
        formatter: rankType.value === 'volume' ? (val: number) => formatVolume(val) : '{value}%',
      },
      splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.1)' } },
    },
    yAxis: {
      type: 'category',
      data: [],
      axisLabel: { color: 'rgba(255, 255, 255, 0.9)', fontSize: 10 },
      axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.2)' } },
    },
    series: [{
      type: 'bar',
      data: [],
      barWidth: 14,
      label: {
        show: true,
        position: 'right',
        formatter: (params: any) => {
          if (rankType.value === 'volume') return formatVolume(params.value);
          if (rankType.value === 'amplitude') return params.value.toFixed(2) + '%';
          return params.value + '%';
        },
        color: '#fff',
        fontSize: 9,
      },
    }],
  }));

  // ===== 成交量分布 =====
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
      value: displayData.value.filter((item) => item.volume >= range.min && item.volume < range.max).length,
    }));
  });

  const volumeBarOption = {
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(0, 0, 0, 0.8)', borderColor: 'rgba(255, 255, 255, 0.2)', textStyle: { color: '#fff' } },
    grid: { left: 70, right: 20, top: 15, bottom: 35 },
    xAxis: {
      type: 'category',
      data: [],
      axisLabel: { color: 'rgba(255, 255, 255, 0.8)', fontSize: 9, rotate: 25 },
      axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.2)' } },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: 'rgba(255, 255, 255, 0.7)', fontSize: 9 },
      splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.1)' } },
    },
    series: [{
      type: 'bar',
      data: [],
      barWidth: 20,
      itemStyle: { color: '#fa709a', borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top', color: '#fff', fontSize: 10 },
    }],
  };

  // ===== 市值分布 =====
  const marketCapBarData = computed(() => marketCapStats.value.map((s) => ({ name: s.name, value: s.value })));

  const marketCapBarOption = {
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(0, 0, 0, 0.8)', borderColor: 'rgba(255, 255, 255, 0.2)', textStyle: { color: '#fff' } },
    grid: { left: 80, right: 30, top: 15, bottom: 15 },
    xAxis: {
      type: 'value',
      axisLabel: { color: 'rgba(255, 255, 255, 0.7)', fontSize: 9 },
      splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.1)' } },
    },
    yAxis: {
      type: 'category',
      data: [],
      axisLabel: { color: 'rgba(255, 255, 255, 0.9)', fontSize: 10 },
      axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.2)' } },
    },
    series: [{
      type: 'bar',
      data: [],
      barWidth: 15,
      itemStyle: { color: '#30cfd0', borderRadius: [0, 4, 4, 0] },
      label: { show: true, position: 'right', color: '#fff', fontSize: 9 },
    }],
  };

  // ===== 价格区间分布 =====
  const priceRangeData = computed(() => {
    const ranges = [
      { name: '<$50', min: 0, max: 50 },
      { name: '$50-100', min: 50, max: 100 },
      { name: '$100-200', min: 100, max: 200 },
      { name: '$200-500', min: 200, max: 500 },
      { name: '>$500', min: 500, max: Infinity },
    ];
    return ranges.map((range) => ({
      name: range.name,
      value: displayData.value.filter((item) => item.closePrice >= range.min && item.closePrice < range.max).length,
    }));
  });

  const priceRangeOption = {
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(0, 0, 0, 0.8)', borderColor: 'rgba(255, 255, 255, 0.2)', textStyle: { color: '#fff' } },
    grid: { left: 60, right: 30, top: 15, bottom: 30 },
    xAxis: {
      type: 'category',
      data: [],
      axisLabel: { color: 'rgba(255, 255, 255, 0.8)', fontSize: 10 },
      axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.2)' } },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: 'rgba(255, 255, 255, 0.7)', fontSize: 9 },
      splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.1)' } },
    },
    series: [{
      type: 'bar',
      data: [],
      barWidth: 24,
      itemStyle: { color: '#43e97b', borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top', color: '#fff', fontSize: 10 },
    }],
  };

  // ===== 表格数据 =====
  const filteredTableData = computed(() => {
    let data = [...displayData.value];
    if (tableFilter.value === 'up') data = data.filter((item) => item.changePct > 0);
    if (tableFilter.value === 'down') data = data.filter((item) => item.changePct < 0);
    if (searchKeyword.value) {
      const keyword = searchKeyword.value.toLowerCase();
      data = data.filter((item) => item.symbol.toLowerCase().includes(keyword) || item.name.toLowerCase().includes(keyword));
    }
    return data
      .sort((a, b) => Math.abs(b.changePct) - Math.abs(a.changePct))
      .slice(0, 50)
      .map((item, index) => ({
        ...item,
        rank: index + 1,
        amplitude: (((item.highPrice - item.lowPrice) / item.prevClose) * 100).toFixed(2),
        turnover: ((item.volume * item.closePrice) / 100000000).toFixed(2),
      }));
  });

  // ===== 工具函数 =====
  function formatVolume(vol: number): string {
    if (vol >= 100000000) return (vol / 100000000).toFixed(2) + '亿';
    if (vol >= 10000) return (vol / 10000).toFixed(0) + '万';
    return vol.toLocaleString();
  }

  function formatSector(sector: string): string {
    // API返回已经是中文，直接返回或映射
    const sectorMap: Record<string, string> = {
      'Technology': '科技',
      'Consumer': '消费',
      'Finance': '金融',
      'Healthcare': '医疗',
      'Energy': '能源',
      'Industrial': '工业',
      'Auto': '汽车',
      '汽车': '汽车',
    };
    return sectorMap[sector] || sector;
  }

  function getSectorColor(sector: string): string {
    const colorMap: Record<string, string> = {
      'Technology': 'blue',
      '科技': 'blue',
      'Consumer': 'green',
      '消费': 'green',
      'Finance': 'gold',
      '金融': 'gold',
      'Healthcare': 'cyan',
      '医疗': 'cyan',
      'Energy': 'orange',
      '能源': 'orange',
      'Industrial': 'purple',
      '工业': 'purple',
      'Auto': 'red',
      '汽车': 'red',
    };
    return colorMap[sector] || 'default';
  }

  function parseMarketCap(cap: string): number {
    // 处理格式如: "910B", "3.50T", "236B"
    if (!cap) return 0;
    const trimmed = cap.trim();
    if (trimmed.endsWith('T')) {
      return parseFloat(trimmed.slice(0, -1)) * 10000; // 万亿转亿
    }
    if (trimmed.endsWith('B')) {
      return parseFloat(trimmed.slice(0, -1)); // 十亿转亿 (B=10亿)
    }
    if (trimmed.includes('万亿')) return parseFloat(trimmed) * 10000;
    if (trimmed.includes('亿')) return parseFloat(trimmed);
    return parseFloat(trimmed) || 0;
  }

  function getRankClass(rank: number) {
    if (rank === 1) return 'rank-1';
    if (rank === 2) return 'rank-2';
    if (rank === 3) return 'rank-3';
    return '';
  }

  // ===== 交易日自动更新功能 =====
  // 获取美东时间
  function getESTTime(): Date {
    const now = new Date();
    // 美东时间 = UTC-5 (冬令时) 或 UTC-4 (夏令时)
    // 简化为固定偏移5小时
    return new Date(now.getTime() - 5 * 60 * 60 * 1000);
  }

  // 判断是否是交易日（周一到周五）
  function isTradingDay(): boolean {
    const estTime = getESTTime();
    const day = estTime.getDay();
    return day >= 1 && day <= 5; // 周一到周五
  }

  // 判断市场状态
  function getMarketStatus(): 'closed' | 'pre' | 'open' | 'after' {
    const estTime = getESTTime();
    const hours = estTime.getHours();
    const minutes = estTime.getMinutes();
    const timeValue = hours * 60 + minutes;

    // 美股时间 (EST/EDT)
    // 盘前: 4:00-9:30
    // 盘中: 9:30-16:00
    // 盘后: 16:00-20:00
    // 休市: 其他时间
    if (timeValue >= 570 && timeValue < 960) {
      return 'open'; // 9:30-16:00
    } else if (timeValue >= 240 && timeValue < 570) {
      return 'pre'; // 4:00-9:30
    } else if (timeValue >= 960 && timeValue < 1200) {
      return 'after'; // 16:00-20:00
    }
    return 'closed';
  }

  // 市场状态文本
  const marketStatusText = computed(() => {
    const statusMap = {
      'closed': { text: '市场休市', color: '#868e96', icon: CalendarOutlined },
      'pre': { text: '盘前交易', color: '#feca57', icon: ClockCircleOutlined },
      'open': { text: '交易中', color: '#51cf66', icon: SyncOutlined },
      'after': { text: '盘后交易', color: '#48dbfb', icon: ClockCircleOutlined },
    };
    return statusMap[marketStatus.value];
  });

  // 倒计时显示
  const countdownText = computed(() => {
    if (!isAutoUpdate.value) return '自动更新已暂停';
    if (marketStatus.value === 'closed') return '市场休市中';
    return `${countdown.value}秒后刷新`;
  });

  // 启动倒计时
  function startCountdown() {
    if (countdownInterval.value) {
      clearInterval(countdownInterval.value);
    }
    countdown.value = 60;
    countdownInterval.value = window.setInterval(() => {
      countdown.value--;
      if (countdown.value <= 0) {
        countdown.value = 60;
      }
    }, 1000);
  }

  // 停止倒计时
  function stopCountdown() {
    if (countdownInterval.value) {
      clearInterval(countdownInterval.value);
      countdownInterval.value = null;
    }
  }

  // 检查是否应该自动更新
  function shouldAutoUpdate(): boolean {
    if (!isAutoUpdate.value) return false;
    if (!isTradingDay()) return false;
    const status = getMarketStatus();
    marketStatus.value = status;
    // 只在盘前、盘中、盘后自动更新
    return status !== 'closed';
  }

  // 获取数据 - 支持实时API和数据库
  async function fetchData() {
    loading.value = true;
    try {
      // 优先尝试从实时API获取
      const res = await axios.get('/jeecgboot/test/usstock/realtime');
      if (res.data?.success && res.data.result && res.data.result.length > 0) {
        const newData = res.data.result.map((item: any) => ({
          symbol: item.symbol,
          name: item.name,
          sector: item.sector,
          closePrice: Number(item.closePrice),
          prevClose: Number(item.prevClose),
          change: Number(item.change),
          changePct: Number(item.changePct),
          openPrice: Number(item.openPrice),
          highPrice: Number(item.highPrice),
          lowPrice: Number(item.lowPrice),
          volume: Number(item.volume),
          marketCap: item.marketCap,
          tradeDate: item.tradeDate || new Date().toISOString().split('T')[0],
          // 计算振幅和成交额
          amplitude: ((Number(item.highPrice) - Number(item.lowPrice)) / Number(item.prevClose) * 100).toFixed(2),
          turnover: (Number(item.volume) * Number(item.closePrice) / 1e8).toFixed(2),
        })) as StockData[];

        handleNewData(newData);
      } else {
        // 回退到数据库
        const dbRes = await axios.get('/jeecgboot/test/usstock/list');
        if (dbRes.data?.success && dbRes.data.result) {
          handleNewData(dbRes.data.result as StockData[]);
        }
      }
    } catch (e) {
      console.error('获取美股数据失败', e);
      message.error('获取美股数据失败');
    } finally {
      loading.value = false;
    }
  }

  // 处理新数据 - 添加平滑过渡
  function handleNewData(newData: StockData[]) {
    // 比较新旧数据，标记价格变化
    if (dataSource.value.length > 0) {
      const changes: Record<string, 'up' | 'down' | 'none'> = {};
      newData.forEach((newItem) => {
        const oldItem = dataSource.value.find((old) => old.symbol === newItem.symbol);
        if (oldItem) {
          if (newItem.closePrice > oldItem.closePrice) {
            changes[newItem.symbol] = 'up';
          } else if (newItem.closePrice < oldItem.closePrice) {
            changes[newItem.symbol] = 'down';
          } else {
            changes[newItem.symbol] = 'none';
          }
        }
      });
      priceChangeMap.value = changes;

      // 1秒后清除变化标记
      setTimeout(() => {
        priceChangeMap.value = {};
      }, 1000);
    }

    prevDataSource.value = [...dataSource.value];
    dataSource.value = newData;

    // 平滑过渡到新数据
    smoothUpdateDisplayData(newData);

    if (dataSource.value.length > 0) {
      currentDate.value = dataSource.value[0].tradeDate;
    }
    updateTime.value = new Date().toLocaleTimeString();
    marketStatus.value = getMarketStatus();
  }

  // 平滑更新显示数据
  function smoothUpdateDisplayData(targetData: StockData[]) {
    if (displayData.value.length === 0) {
      displayData.value = JSON.parse(JSON.stringify(targetData));
      return;
    }

    // 取消之前的动画
    if (animationFrame.value) {
      cancelAnimationFrame(animationFrame.value);
    }

    const startData = JSON.parse(JSON.stringify(displayData.value));
    const duration = 800; // 800ms 过渡时间
    const startTime = performance.now();

    function animate(currentTime: number) {
      const elapsed = currentTime - startTime;
      const progress = Math.min(elapsed / duration, 1);

      // 使用 easeOutCubic 缓动函数
      const easeProgress = 1 - Math.pow(1 - progress, 3);

      displayData.value = targetData.map((target, index) => {
        const start = startData[index] || target;
        return {
          ...target,
          closePrice: lerp(start.closePrice, target.closePrice, easeProgress),
          change: lerp(start.change, target.change, easeProgress),
          changePct: lerp(start.changePct, target.changePct, easeProgress),
          volume: Math.round(lerp(start.volume, target.volume, easeProgress)),
        };
      });

      if (progress < 1) {
        animationFrame.value = requestAnimationFrame(animate);
      } else {
        displayData.value = JSON.parse(JSON.stringify(targetData));
        animationFrame.value = null;
      }
    }

    animationFrame.value = requestAnimationFrame(animate);
  }

  // 线性插值
  function lerp(start: number, end: number, t: number): number {
    return start + (end - start) * t;
  }

  // 实时价格模拟 - 每3秒微调价格
  function startRealtimeSimulation() {
    if (realtimeInterval.value) clearInterval(realtimeInterval.value);
    realtimeInterval.value = window.setInterval(() => {
      simulatePriceChanges();
    }, 3000);
  }

  // 模拟价格波动
  function simulatePriceChanges() {
    if (displayData.value.length === 0) return;

    const changes: Record<string, 'up' | 'down' | 'none'> = {};
    const updatedData = displayData.value.map((stock) => {
      // 随机波动 -0.3% 到 +0.3%
      const fluctuation = (Math.random() - 0.5) * 0.006;
      const newPrice = stock.closePrice * (1 + fluctuation);
      const priceChange = fluctuation > 0.0001 ? 'up' : fluctuation < -0.0001 ? 'down' : 'none';

      if (priceChange !== 'none') {
        changes[stock.symbol] = priceChange;
      }

      // 更新价格和涨跌幅
      const priceDiff = newPrice - stock.prevClose;
      const newChangePct = (priceDiff / stock.prevClose) * 100;

      return {
        ...stock,
        closePrice: newPrice,
        change: priceDiff,
        changePct: Number(newChangePct.toFixed(2)),
      };
    });

    if (Object.keys(changes).length > 0) {
      priceChangeMap.value = changes;
      dataSource.value = updatedData;
      displayData.value = updatedData;
      updateTime.value = new Date().toLocaleTimeString();

      // 1秒后清除变化标记
      setTimeout(() => {
        priceChangeMap.value = {};
      }, 1000);
    }
  }

  const realtimeInterval = ref<number | null>(null);

  // 执行自动更新
  function executeAutoUpdate() {
    if (shouldAutoUpdate()) {
      fetchData();
      message.success('数据已自动更新');
    }
  }

  // 启动自动更新 - 30秒轮询
  function startAutoUpdate() {
    if (autoUpdateInterval.value) {
      clearInterval(autoUpdateInterval.value);
    }
    // 每30秒刷新一次
    autoUpdateInterval.value = window.setInterval(() => {
      executeAutoUpdate();
    }, 30000);
    startCountdown();
    startRealtimeSimulation();
  }

  // 停止自动更新
  function stopAutoUpdate() {
    if (autoUpdateInterval.value) {
      clearInterval(autoUpdateInterval.value);
      autoUpdateInterval.value = null;
    }
    if (realtimeInterval.value) {
      clearInterval(realtimeInterval.value);
      realtimeInterval.value = null;
    }
    stopCountdown();
  }

  // 切换自动更新
  function toggleAutoUpdate() {
    isAutoUpdate.value = !isAutoUpdate.value;
    if (isAutoUpdate.value) {
      startAutoUpdate();
      message.success('已开启交易日自动更新');
    } else {
      stopAutoUpdate();
      message.info('已暂停自动更新');
    }
  }

  // 手动刷新
  async function manualRefresh() {
    await fetchData();
    countdown.value = 60;
    message.success('数据已刷新');
  }

  function handleRankChange() {}
  function handleSearch() {}

  function toggleFullscreen() {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen();
      isFullscreen.value = true;
    } else {
      document.exitFullscreen();
      isFullscreen.value = false;
    }
  }

  onMounted(() => {
    fetchData();
    marketStatus.value = getMarketStatus();
    if (isAutoUpdate.value) {
      startAutoUpdate();
    }
  });

  onUnmounted(() => {
    stopAutoUpdate();
  });
</script>

<style scoped>
  .usstock-bigscreen {
    width: 100%;
    min-height: 100vh;
    background: linear-gradient(135deg, #0c0c1a 0%, #1a1a2e 50%, #16213e 100%);
    color: #fff;
    overflow-x: hidden;
  }

  /* 顶部标题栏 */
  .screen-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 28px;
    background: linear-gradient(180deg, rgba(255,255,255,0.05) 0%, transparent 100%);
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }

  .header-left {
    display: flex;
    align-items: center;
    gap: 14px;
  }

  .logo {
    width: 44px;
    height: 44px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
  }

  .logo-icon {
    font-size: 26px;
    color: #fff;
  }

  .title-area {
    display: flex;
    flex-direction: column;
  }

  .main-title {
    font-size: 24px;
    font-weight: 700;
    margin: 0;
    background: linear-gradient(90deg, #fff 0%, #a8edea 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .sub-title {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.5);
    margin: 2px 0 0 0;
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .market-indices {
    display: flex;
    gap: 12px;
  }

  .index-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 6px 14px;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 8px;
    border: 1px solid rgba(255, 255, 255, 0.1);
  }

  .index-name {
    font-size: 11px;
    color: rgba(255, 255, 255, 0.6);
  }

  .index-value {
    font-size: 13px;
    font-weight: 600;
  }

  .index-value.up { color: #ff6b6b; }
  .index-value.down { color: #51cf66; }
  .index-value small { font-size: 10px; opacity: 0.8; }

  /* 市场状态 */
  .market-status {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 14px;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 10px;
    border: 1px solid;
    border-color: rgba(255, 255, 255, 0.1);
    transition: all 0.3s;
  }

  .status-icon {
    font-size: 20px;
  }

  .status-info {
    display: flex;
    flex-direction: column;
    min-width: 90px;
  }

  .status-text {
    font-size: 13px;
    font-weight: 600;
    line-height: 1.2;
  }

  .countdown-text {
    font-size: 10px;
    color: rgba(255, 255, 255, 0.5);
  }

  .update-time {
    display: flex;
    align-items: center;
    gap: 6px;
    color: rgba(255, 255, 255, 0.7);
    font-size: 12px;
    padding: 8px 14px;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 8px;
    border: 1px solid rgba(255, 255, 255, 0.1);
  }

  /* 自动更新开关 */
  .auto-update-switch {
    :deep(.ant-switch-inner) {
      background: rgba(255, 255, 255, 0.2);
    }

    :deep(.ant-switch-checked .ant-switch-inner) {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }
  }

  /* 刷新按钮 */
  .refresh-btn {
    background: rgba(255, 255, 255, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.2);
    color: #fff;
    border-radius: 8px;
    font-size: 12px;
    height: 34px;
    padding: 0 14px;

    &:hover {
      background: rgba(255, 255, 255, 0.15);
      border-color: rgba(255, 255, 255, 0.3);
    }
  }

  .fullscreen-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    color: rgba(255, 255, 255, 0.6);
    font-size: 12px;
    padding: 8px 14px;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 8px;
    border: 1px solid rgba(255, 255, 255, 0.1);
  }

  .fullscreen-btn {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border: none;
    border-radius: 8px;
    font-size: 12px;
    height: 34px;
    padding: 0 16px;
  }

  /* 主体内容 */
  .screen-body {
    padding: 16px 28px;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  /* 核心统计卡片 */
  .stats-row {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
  }

  .stat-card {
    position: relative;
    padding: 16px 18px;
    background: rgba(255, 255, 255, 0.03);
    border-radius: 14px;
    border: 1px solid rgba(255, 255, 255, 0.1);
    overflow: hidden;
    transition: transform 0.3s, box-shadow 0.3s;
  }

  .stat-card:hover {
    transform: translateY(-3px);
    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
  }

  .stat-bg {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    opacity: 0.8;
  }

  .stat-content {
    position: relative;
    z-index: 1;
  }

  .stat-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 10px;
  }

  .stat-icon {
    font-size: 18px;
    color: rgba(255, 255, 255, 0.8);
  }

  .stat-title {
    font-size: 13px;
    color: rgba(255, 255, 255, 0.7);
  }

  .stat-body {
    display: flex;
    align-items: baseline;
    gap: 10px;
  }

  .stat-value {
    font-size: 28px;
    font-weight: 700;
  }

  .stat-trend {
    display: flex;
    align-items: center;
    gap: 3px;
    font-size: 12px;
    padding: 3px 8px;
    border-radius: 16px;
  }

  .stat-trend.up {
    color: #ff6b6b;
    background: rgba(255, 107, 107, 0.15);
  }

  .stat-trend.down {
    color: #51cf66;
    background: rgba(81, 207, 102, 0.15);
  }

  .stat-detail {
    margin-top: 8px;
    font-size: 11px;
    color: rgba(255, 255, 255, 0.5);
  }

  /* 详细统计行 */
  .detail-stats-row {
    display: grid;
    grid-template-columns: repeat(6, 1fr);
    gap: 12px;
    padding: 14px 18px;
    background: rgba(255, 255, 255, 0.03);
    border-radius: 12px;
    border: 1px solid rgba(255, 255, 255, 0.08);
  }

  .detail-stat-item {
    text-align: center;
  }

  .detail-label {
    font-size: 11px;
    color: rgba(255, 255, 255, 0.5);
    margin-bottom: 4px;
  }

  .detail-value {
    font-size: 16px;
    font-weight: 600;
    color: #fff;
  }

  .detail-value.up { color: #ff6b6b; }
  .detail-value.down { color: #51cf66; }

  .detail-desc {
    font-size: 10px;
    color: rgba(255, 255, 255, 0.4);
    margin-top: 2px;
  }

  /* 中间图表区域 */
  .main-charts {
    display: grid;
    grid-template-columns: 1fr 1.8fr 1fr;
    gap: 16px;
    min-height: 580px;
  }

  .chart-column {
    display: flex;
    flex-direction: column;
    gap: 14px;
  }

  .chart-column.left,
  .chart-column.right {
    min-width: 280px;
  }

  .chart-column.center {
    min-width: 480px;
  }

  .chart-box {
    flex: 1;
    background: rgba(255, 255, 255, 0.03);
    border-radius: 14px;
    border: 1px solid rgba(255, 255, 255, 0.1);
    overflow: hidden;
    transition: transform 0.3s;
  }

  .chart-box:hover {
    transform: translateY(-2px);
  }

  .chart-box.main-chart {
    flex: 1.2;
  }

  .chart-box.price-chart {
    flex: 0.8;
  }

  .chart-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    background: rgba(255, 255, 255, 0.02);
  }

  .header-title {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .title-icon {
    width: 3px;
    height: 16px;
    border-radius: 2px;
  }

  .title-text {
    font-size: 14px;
    font-weight: 600;
    color: #fff;
  }

  .header-tag {
    padding: 3px 10px;
    background: rgba(255, 255, 255, 0.1);
    border-radius: 10px;
    font-size: 10px;
    color: rgba(255, 255, 255, 0.8);
  }

  .header-tag.active {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
  }

  .chart-content {
    padding: 12px;
  }

  .rank-tabs {
    :deep(.ant-radio-button-wrapper) {
      background: rgba(255, 255, 255, 0.05);
      border-color: rgba(255, 255, 255, 0.2);
      color: rgba(255, 255, 255, 0.7);
      font-size: 11px;
      padding: 0 10px;
    }

    :deep(.ant-radio-button-wrapper-checked) {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-color: transparent;
      color: #fff;
    }
  }

  /* 行业详情 */
  .sector-detail {
    padding: 0 16px 12px;
  }

  .sector-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
  }

  .sector-name {
    width: 45px;
    font-size: 11px;
    color: rgba(255, 255, 255, 0.7);
  }

  .sector-bar {
    flex: 1;
    height: 6px;
    background: rgba(255, 255, 255, 0.1);
    border-radius: 3px;
    overflow: hidden;
  }

  .sector-fill {
    height: 100%;
    border-radius: 3px;
    transition: width 0.5s;
  }

  .sector-count {
    width: 35px;
    font-size: 10px;
    color: rgba(255, 255, 255, 0.6);
    text-align: right;
  }

  /* 涨跌区间统计 */
  .change-buckets {
    padding: 0 16px 12px;
  }

  .bucket {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 6px;
  }

  .bucket-range {
    width: 55px;
    font-size: 10px;
    color: rgba(255, 255, 255, 0.6);
  }

  .bucket-bar {
    flex: 1;
    height: 14px;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 7px;
    overflow: hidden;
  }

  .bucket-fill {
    height: 100%;
    border-radius: 7px;
    transition: width 0.5s;
  }

  .bucket-fill.up-strong { background: linear-gradient(90deg, #ff6b6b, #ff8787); }
  .bucket-fill.up-mid { background: linear-gradient(90deg, #ffa8a8, #ff6b6b); }
  .bucket-fill.up-weak { background: linear-gradient(90deg, #ffd4d4, #ffa8a8); }
  .bucket-fill.down-weak { background: linear-gradient(90deg, #a8e6cf, #7fcdcd); }
  .bucket-fill.down-mid { background: linear-gradient(90deg, #7fcdcd, #51cf66); }
  .bucket-fill.down-strong { background: linear-gradient(90deg, #51cf66, #2ecc71); }

  .bucket-count {
    width: 30px;
    font-size: 10px;
    color: rgba(255, 255, 255, 0.7);
    text-align: right;
  }

  /* 成交量统计 */
  .volume-stats {
    display: flex;
    padding: 0 16px 12px;
    gap: 20px;
  }

  .vol-stat {
    display: flex;
    flex-direction: column;
  }

  .vol-label {
    font-size: 10px;
    color: rgba(255, 255, 255, 0.5);
  }

  .vol-value {
    font-size: 14px;
    font-weight: 600;
    color: #fa709a;
  }

  /* 市值统计 */
  .cap-stats {
    padding: 0 16px 12px;
  }

  .cap-item {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 8px;
  }

  .cap-icon {
    width: 10px;
    height: 10px;
    border-radius: 50%;
  }

  .cap-info {
    display: flex;
    flex: 1;
    justify-content: space-between;
  }

  .cap-name {
    font-size: 11px;
    color: rgba(255, 255, 255, 0.7);
  }

  .cap-value {
    font-size: 11px;
    color: rgba(255, 255, 255, 0.9);
  }

  /* 日内走势统计 */
  .intraday-stats {
    padding: 16px;
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .intra-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 10px;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 10px;
  }

  .intra-label {
    font-size: 11px;
    color: rgba(255, 255, 255, 0.6);
    margin-bottom: 4px;
  }

  .intra-value {
    font-size: 18px;
    font-weight: 600;
  }

  .intra-value small {
    font-size: 11px;
    opacity: 0.8;
  }

  .intra-value.up { color: #ff6b6b; }
  .intra-value.down { color: #51cf66; }

  /* 底部表格 */
  .bottom-section {
    margin-top: 6px;
  }

  .table-box {
    padding: 0;
    overflow-x: auto;

    // 深色表格样式增强
    :deep(.ant-table) {
      background: transparent;
      color: #fff;
    }
  }

  .table-subtitle {
    font-size: 11px;
    color: rgba(255, 255, 255, 0.5);
    margin-left: 12px;
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .table-filter {
    :deep(.ant-radio-button-wrapper) {
      background: rgba(255, 255, 255, 0.05);
      border-color: rgba(255, 255, 255, 0.2);
      color: rgba(255, 255, 255, 0.7);
      font-size: 11px;
    }

    :deep(.ant-radio-button-wrapper-checked) {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-color: transparent;
      color: #fff;
    }
  }

  .dark-search :deep(.ant-input) {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.2);
    color: #fff;
    font-size: 12px;
  }

  .table-content {
    padding: 12px 16px;
    min-width: 100%;
    overflow-x: auto;
  }

  /* 深色表格样式 */
  .dark-table :deep(.ant-table) {
    background: transparent;
  }

  .dark-table :deep(.ant-table-thead > tr > th) {
    background: rgba(255, 255, 255, 0.05) !important;
    color: rgba(255, 255, 255, 0.85) !important;
    font-weight: 600;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
    padding: 10px 8px;
    font-size: 12px;
  }

  .dark-table :deep(.ant-table-tbody > tr > td) {
    background: transparent;
    color: rgba(255, 255, 255, 0.8);
    border-bottom: 1px solid rgba(255, 255, 255, 0.04);
    padding: 10px 8px;
    font-size: 12px;
  }

  .dark-table :deep(.ant-table-tbody > tr:hover > td) {
    background: rgba(255, 255, 255, 0.04);
  }

  /* 排名样式 */
  .rank-cell {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    border-radius: 50%;
    font-weight: 600;
    font-size: 11px;
  }

  .rank-cell.rank-1 {
    background: linear-gradient(135deg, #ffd700 0%, #ffed4a 100%);
    color: #333;
    box-shadow: 0 2px 6px rgba(255, 215, 0, 0.4);
  }

  .rank-cell.rank-2 {
    background: linear-gradient(135deg, #c0c0c0 0%, #e8e8e8 100%);
    color: #333;
    box-shadow: 0 2px 6px rgba(192, 192, 192, 0.4);
  }

  .rank-cell.rank-3 {
    background: linear-gradient(135deg, #cd7f32 0%, #daa520 100%);
    color: #fff;
    box-shadow: 0 2px 6px rgba(205, 127, 50, 0.4);
  }

  /* 涨跌颜色 */
  .text-up { color: #ff6b6b; font-weight: 600; }
  .text-down { color: #51cf66; font-weight: 600; }

  .change-pill {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 3px 10px;
    border-radius: 10px;
    font-size: 11px;
    font-weight: 600;
    min-width: 50px;
  }

  .change-pill.text-up { background: rgba(255, 107, 107, 0.15); }
  .change-pill.text-down { background: rgba(81, 207, 102, 0.15); }

  .amplitude-value {
    color: #feca57;
    font-weight: 500;
  }

  .turnover-value {
    color: #48dbfb;
    font-weight: 500;
  }

  .hl-range {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 3px;
    font-size: 11px;
  }

  .hl-low { color: rgba(255, 255, 255, 0.5); }
  .hl-high { color: rgba(255, 255, 255, 0.9); }
  .hl-separator { color: rgba(255, 255, 255, 0.3); }

  .sector-tag {
    font-size: 10px;
    padding: 1px 6px;
  }

  /* 响应式 */
  @media screen and (max-width: 1600px) {
    .main-charts {
      grid-template-columns: 1fr 1.5fr 1fr;
    }

    .detail-stats-row {
      grid-template-columns: repeat(3, 1fr);
    }
  }

  @media screen and (max-width: 1400px) {
    .main-charts {
      grid-template-columns: 1fr;
    }

    .chart-column.left,
    .chart-column.right,
    .chart-column.center {
      min-width: auto;
    }

    .stats-row {
      grid-template-columns: repeat(2, 1fr);
    }

    .detail-stats-row {
      grid-template-columns: repeat(2, 1fr);
    }

    .market-indices {
      display: none;
    }
  }
</style>
