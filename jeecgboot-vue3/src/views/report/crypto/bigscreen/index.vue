<template>
  <div class="crypto-bigscreen">
    <!-- 顶部标题栏 -->
    <div class="screen-header">
      <div class="header-left">
        <div class="logo">
          <LineChartOutlined class="logo-icon" />
        </div>
        <div class="title-area">
          <h1 class="main-title">加密货币实时看板</h1>
          <p class="sub-title">{{ currentDate }} 数据实时更新 | 共{{ dataSource.length }}种加密货币</p>
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

        <!-- BTC/ETH 快速信息 -->
        <div class="quick-info">
          <div class="quick-item btc" :class="{ 'price-up': btcChange > 0, 'price-down': btcChange < 0 }">
            <span class="quick-label">BTC</span>
            <span class="quick-value">${{ formatPrice(btcPrice) }}</span>
            <span class="quick-change" :class="btcChange >= 0 ? 'up' : 'down'">
              {{ btcChange >= 0 ? '+' : '' }}{{ btcChange }}%
            </span>
          </div>
          <div class="quick-item eth" :class="{ 'price-up': ethChange > 0, 'price-down': ethChange < 0 }">
            <span class="quick-label">ETH</span>
            <span class="quick-value">${{ formatPrice(ethPrice) }}</span>
            <span class="quick-change" :class="ethChange >= 0 ? 'up' : 'down'">
              {{ ethChange >= 0 ? '+' : '' }}{{ ethChange }}%
            </span>
          </div>
        </div>

        <div class="update-time">
          <ClockCircleOutlined />
          <span>{{ updateTime }}</span>
        </div>

        <!-- 自动更新开关 -->
        <a-switch v-model:checked="isAutoUpdate" class="auto-update-switch">
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
            <div class="stat-detail" v-if="stat.detail">{{ stat.detail }}</div>
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
          <!-- 市值排名分布 -->
          <div class="chart-box">
            <div class="chart-header">
              <div class="header-title">
                <span class="title-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)"></span>
                <span class="title-text">类别分布</span>
              </div>
              <div class="header-tag">按数量</div>
            </div>
            <div class="chart-content">
              <Pie :chart-data="categoryPieData" height="220px" :option="categoryPieOption" />
            </div>
            <!-- 类别统计详情 -->
            <div class="sector-detail">
              <div class="sector-row" v-for="(cat, idx) in categoryStats.slice(0, 4)" :key="idx">
                <span class="sector-name">{{ cat.name }}</span>
                <div class="sector-bar">
                  <div class="sector-fill" :style="{ width: cat.percent + '%', background: cat.color }"></div>
                </div>
                <span class="sector-count">{{ cat.count }}种</span>
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
              <div class="header-tag active">24H</div>
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
                <a-radio-button value="marketCap">
                  <DollarOutlined /> 市值
                </a-radio-button>
              </a-radio-group>
            </div>
            <div class="chart-content">
              <Bar :chart-data="rankBarData" height="520px" :option="rankBarOption" />
            </div>
          </div>

          <!-- 中间底部：市值分布 -->
          <div class="chart-box price-chart">
            <div class="chart-header">
              <div class="header-title">
                <span class="title-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)"></span>
                <span class="title-text">市值区间分布</span>
              </div>
            </div>
            <div class="chart-content">
              <Bar :chart-data="marketCapRangeData" height="200px" :option="marketCapRangeOption" />
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
                <span class="title-text">成交量 TOP10</span>
              </div>
              <div class="header-tag">24H</div>
            </div>
            <div class="chart-content">
              <Bar :chart-data="volumeBarData" height="220px" :option="volumeBarOption" />
            </div>
          </div>

          <!-- 7日涨跌分布 -->
          <div class="chart-box">
            <div class="chart-header">
              <div class="header-title">
                <span class="title-icon" style="background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)"></span>
                <span class="title-text">7日涨跌分布</span>
              </div>
              <div class="header-tag">7D</div>
            </div>
            <div class="chart-content">
              <Pie :chart-data="change7dPieData" height="200px" :option="change7dPieOption" />
            </div>
            <!-- 7日涨跌区间 -->
            <div class="change-buckets">
              <div class="bucket" v-for="(bucket, idx) in change7dBuckets" :key="idx">
                <div class="bucket-range">{{ bucket.range }}</div>
                <div class="bucket-bar">
                  <div class="bucket-fill" :class="bucket.type" :style="{ width: bucket.percent + '%' }"></div>
                </div>
                <div class="bucket-count">{{ bucket.count }}</div>
              </div>
            </div>
          </div>

          <!-- BTC实时走势 -->
          <div class="chart-box">
            <div class="chart-header">
              <div class="header-title">
                <span class="title-icon" style="background: linear-gradient(135deg, #f7931a 0%, #ac6600 100%)"></span>
                <span class="title-text">BTC 实时走势</span>
              </div>
              <div class="header-tag live">LIVE</div>
            </div>
            <div class="chart-content">
              <SingleLine :chart-data="btcTrendData" height="180px" :option="btcTrendOption" seriesColor="#f7931a" />
            </div>
          </div>

          <!-- ETH实时走势 -->
          <div class="chart-box">
            <div class="chart-header">
              <div class="header-title">
                <span class="title-icon" style="background: linear-gradient(135deg, #627eea 0%, #3c5fc0 100%)"></span>
                <span class="title-text">ETH 实时走势</span>
              </div>
              <div class="header-tag live">LIVE</div>
            </div>
            <div class="chart-content">
              <SingleLine :chart-data="ethTrendData" height="180px" :option="ethTrendOption" seriesColor="#627eea" />
            </div>
          </div>
        </div>
      </div>

      <!-- 底部：加密货币列表 -->
      <div class="bottom-table">
        <div class="table-header">
          <div class="header-title">
            <span class="title-icon" style="background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)"></span>
            <span class="title-text">加密货币行情列表</span>
          </div>
          <div class="table-tabs">
            <a-radio-group v-model:value="tableFilter" size="small" @change="handleFilterChange">
              <a-radio-button value="all">全部</a-radio-button>
              <a-radio-button value="layer1">Layer1</a-radio-button>
              <a-radio-button value="defi">DeFi</a-radio-button>
              <a-radio-button value="meme">Meme</a-radio-button>
              <a-radio-button value="layer2">Layer2</a-radio-button>
              <a-radio-button value="ai">AI</a-radio-button>
            </a-radio-group>
          </div>
        </div>
        <div class="table-content">
          <div class="crypto-row header-row">
            <div class="crypto-rank">排名</div>
            <div class="crypto-symbol">币种</div>
            <div class="crypto-category">分类</div>
            <div class="crypto-price">价格</div>
            <div class="crypto-change">24H涨跌</div>
            <div class="crypto-7d">7日涨跌</div>
            <div class="crypto-volume">成交量</div>
            <div class="crypto-marketcap">市值</div>
            <div class="crypto-range">24H范围</div>
          </div>
          <div class="crypto-row" v-for="(crypto, index) in filteredDataSource" :key="crypto.symbol">
            <div class="crypto-rank">{{ crypto.marketCapRank }}</div>
            <div class="crypto-symbol">
              <span class="symbol-name">{{ crypto.symbol }}</span>
              <span class="symbol-full">{{ crypto.name }}</span>
            </div>
            <div class="crypto-category">
              <span class="category-tag" :class="getCategoryClass(crypto.symbol)">{{ getCategory(crypto.symbol) }}</span>
            </div>
            <div class="crypto-price" :class="priceChangeMap[crypto.symbol]">
              <span class="price-value">${{ formatPrice(crypto.price) }}</span>
              <span v-if="priceChangeMap[crypto.symbol] === 'up'" class="price-arrow">▲</span>
              <span v-if="priceChangeMap[crypto.symbol] === 'down'" class="price-arrow">▼</span>
            </div>
            <div class="crypto-change" :class="crypto.changePct24h >= 0 ? 'up' : 'down'">
              {{ crypto.changePct24h >= 0 ? '+' : '' }}{{ crypto.changePct24h }}%
            </div>
            <div class="crypto-7d" :class="crypto.change7d >= 0 ? 'up' : 'down'">
              {{ crypto.change7d >= 0 ? '+' : '' }}{{ crypto.change7d }}%
            </div>
            <div class="crypto-volume">{{ formatVolume(crypto.volume24h) }}</div>
            <div class="crypto-marketcap">{{ formatMarketCap(crypto.marketCap) }}</div>
            <div class="crypto-range">
              <div class="range-bar">
                <div class="range-indicator" :style="{ left: getRangePosition(crypto) + '%' }"></div>
              </div>
              <span class="range-values">${{ formatPrice(crypto.low24h) }} - ${{ formatPrice(crypto.high24h) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
  import { message } from 'ant-design-vue';
  import axios from 'axios';
  import {
    LineChartOutlined,
    ClockCircleOutlined,
    SyncOutlined,
    ReloadOutlined,
    FullscreenOutlined,
    RiseOutlined,
    FallOutlined,
    ArrowUpOutlined,
    ArrowDownOutlined,
    BarChartOutlined,
    DollarOutlined,
    CheckCircleFilled,
    CloseCircleFilled,
    WarningFilled,
    ClockCircleFilled,
  } from '@ant-design/icons-vue';
  import Pie from '/@/components/chart/Pie.vue';
  import Bar from '/@/components/chart/Bar.vue';
  import SingleLine from '/@/components/chart/SingleLine.vue';
  // 加密货币大屏 - 实时数据展示

  // 数据类型定义
  interface CryptoData {
    symbol: string;
    name: string;
    price: number;
    priceBtc: number;
    change24h: number;
    changePct24h: number;
    change7d: number;
    high24h: number;
    low24h: number;
    volume24h: number;
    marketCap: number;
    marketCapRank: number;
    circulatingSupply: number;
    totalSupply: number;
    maxSupply: number;
    tradeDate: string;
  }

  // 状态变量
  const dataSource = ref<CryptoData[]>([]);
  const displayData = ref<CryptoData[]>([]); // 用于显示的平滑过渡数据
  const prevDataSource = ref<CryptoData[]>([]);
  const priceChangeMap = ref<Record<string, 'up' | 'down' | 'none'>>({});
  const currentDate = ref('2026-05-10');
  const updateTime = ref('');
  const loading = ref(false);
  const isFullscreen = ref(false);
  const isAutoUpdate = ref(true);
  const autoUpdateInterval = ref<number | null>(null);
  const countdown = ref(30);
  const countdownInterval = ref<number | null>(null);
  const marketStatus = ref<'open' | 'closed'>('open');
  const rankType = ref('up');
  const tableFilter = ref('all');
  const realtimeInterval = ref<number | null>(null);
  const animationFrame = ref<number | null>(null);

  // 价格历史数据 - 用于走势图
  const priceHistory = ref<{ time: string; btc: number; eth: number; totalCap: number }[]>([]);
  const maxHistoryPoints = 50;

  // 加密货币市场始终开放
  const marketStatusText = computed(() => {
    return {
      text: '实时更新',
      icon: CheckCircleFilled,
      color: '#52c41a',
    };
  });

  // BTC/ETH 价格
  const btcPrice = computed(() => displayData.value.find((c) => c.symbol === 'BTC')?.price || 0);
  const btcChange = computed(() => displayData.value.find((c) => c.symbol === 'BTC')?.changePct24h || 0);
  const ethPrice = computed(() => displayData.value.find((c) => c.symbol === 'ETH')?.price || 0);
  const ethChange = computed(() => displayData.value.find((c) => c.symbol === 'ETH')?.changePct24h || 0);

  const countdownText = computed(() => {
    const seconds = countdown.value;
    return `${seconds}s`;
  });

  // 核心统计
  const coreStats = computed(() => {
    const total = totalCount.value || 1;
    return [
      {
        title: '总市值',
        value: `$${formatLargeNumber(totalMarketCap.value)}`,
        change: avgChange.value,
        icon: DollarOutlined,
        color: '#1890ff',
        gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        detail: `${displayData.value.length}种加密货币`,
      },
      {
        title: '平均涨幅',
        value: `${avgChange.value >= 0 ? '+' : ''}${avgChange.value}%`,
        change: avgChange.value,
        icon: RiseOutlined,
        color: avgChange.value >= 0 ? '#52c41a' : '#ff4d4f',
        gradient: avgChange.value >= 0 ? 'linear-gradient(135deg, #11998e 0%, #38ef7d 100%)' : 'linear-gradient(135deg, #eb3349 0%, #f45c43 100%)',
      },
      {
        title: '上涨数量',
        value: upCount.value,
        change: (upCount.value / total) * 100,
        icon: ArrowUpOutlined,
        color: '#52c41a',
        gradient: 'linear-gradient(135deg, #11998e 0%, #38ef7d 100%)',
        detail: `占比${((upCount.value / total) * 100).toFixed(1)}%`,
      },
      {
        title: '下跌数量',
        value: downCount.value,
        change: -(downCount.value / total) * 100,
        icon: ArrowDownOutlined,
        color: '#ff4d4f',
        gradient: 'linear-gradient(135deg, #eb3349 0%, #f45c43 100%)',
        detail: `占比${((downCount.value / total) * 100).toFixed(1)}%`,
      },
      {
        title: '总成交量',
        value: `$${formatLargeNumber(totalVolume.value)}`,
        change: 0,
        icon: BarChartOutlined,
        color: '#722ed1',
        gradient: 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
        detail: '24小时',
      },
      {
        title: '最大涨幅',
        value: maxGain.value,
        change: maxGainPct.value,
        icon: RiseOutlined,
        color: '#52c41a',
        gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
        detail: maxGainSymbol.value,
      },
    ];
  });

  // 详细统计
  const detailStats = computed(() => {
    const total = totalCount.value || 1;
    return [
      { label: '最强币种', value: maxGainSymbol.value, desc: `+${maxGainPct.value}%`, isPositive: true },
      { label: '最弱币种', value: maxLossSymbol.value, desc: `${maxLossPct.value}%`, isNegative: true },
      { label: '成交量最高', value: maxVolumeSymbol.value, desc: formatLargeNumber(maxVolume.value), isPositive: false },
      { label: '市值最高', value: 'BTC', desc: formatLargeNumber(btcMarketCap.value), isPositive: false },
      { label: '平均7日涨幅', value: `${avgChange7d.value >= 0 ? '+' : ''}${avgChange7d.value}%`, desc: '7天平均', isPositive: avgChange7d.value >= 0 },
      { label: '上涨占比', value: `${((upCount.value / total) * 100).toFixed(1)}%`, desc: '24小时', isPositive: true },
    ];
  });

  // 统计计算
  const totalMarketCap = computed(() => displayData.value.reduce((acc, item) => acc + item.marketCap, 0));
  const totalVolume = computed(() => displayData.value.reduce((acc, item) => acc + item.volume24h, 0));
  const upCount = computed(() => displayData.value.filter((item) => item.changePct24h > 0).length);
  const downCount = computed(() => displayData.value.filter((item) => item.changePct24h < 0).length);
  const flatCount = computed(() => displayData.value.filter((item) => item.changePct24h === 0).length);
  const totalCount = computed(() => displayData.value.length);
  const avgChange = computed(() => {
    if (displayData.value.length === 0) return 0;
    return Number((displayData.value.reduce((acc, item) => acc + item.changePct24h, 0) / displayData.value.length).toFixed(2));
  });
  const avgChange7d = computed(() => {
    if (displayData.value.length === 0) return 0;
    return Number((displayData.value.reduce((acc, item) => acc + item.change7d, 0) / displayData.value.length).toFixed(2));
  });
  const maxGainPct = computed(() => displayData.value.length > 0 ? Math.max(...displayData.value.map((item) => item.changePct24h)) : 0);
  const maxGainSymbol = computed(() => {
    if (displayData.value.length === 0) return '-';
    const maxItem = displayData.value.reduce((max, item) => item.changePct24h > max.changePct24h ? item : max, displayData.value[0]);
    return maxItem?.symbol || '-';
  });
  const maxGain = computed(() => maxGainSymbol.value);
  const maxLossPct = computed(() => displayData.value.length > 0 ? Math.min(...displayData.value.map((item) => item.changePct24h)) : 0);
  const maxLossSymbol = computed(() => {
    if (displayData.value.length === 0) return '-';
    const minItem = displayData.value.reduce((min, item) => item.changePct24h < min.changePct24h ? item : min, displayData.value[0]);
    return minItem?.symbol || '-';
  });
  const maxVolume = computed(() => displayData.value.length > 0 ? Math.max(...displayData.value.map((item) => item.volume24h)) : 0);
  const maxVolumeSymbol = computed(() => {
    if (displayData.value.length === 0) return '-';
    const maxItem = displayData.value.reduce((max, item) => item.volume24h > max.volume24h ? item : max, displayData.value[0]);
    return maxItem?.symbol || '-';
  });
  const btcMarketCap = computed(() => displayData.value.find((c) => c.symbol === 'BTC')?.marketCap || 0);

  // 类别分类
  const getCategory = (symbol: string) => {
    const layer1 = ['BTC', 'ETH', 'SOL', 'BNB', 'XRP', 'ADA', 'AVAX', 'DOT', 'NEAR', 'ATOM', 'FTM', 'ALGO', 'ICP', 'FIL', 'TRX', 'LTC', 'BCH', 'EOS', 'XTZ', 'FLOW'];
    const defi = ['LINK', 'UNI', 'AAVE', 'MKR', 'CRV', 'COMP', 'SUSHI', 'LDO', 'SNX', 'YFI'];
    const meme = ['DOGE', 'SHIB', 'PEPE', 'FLOKI', 'BONK', 'WIF', 'MEME', 'BABYDOGE'];
    const layer2 = ['MATIC', 'ARB', 'OP', 'IMX', 'LRC', 'STRK', 'CRO', 'FTT', 'KCS', 'BTT', 'CELO', 'METIS'];
    const ai = ['FET', 'RNDR', 'TAO', 'AGIX', 'OCEAN'];
    if (layer1.includes(symbol)) return 'Layer1';
    if (defi.includes(symbol)) return 'DeFi';
    if (meme.includes(symbol)) return 'Meme';
    if (layer2.includes(symbol)) return 'Layer2';
    if (ai.includes(symbol)) return 'AI';
    return 'Other';
  };

  // 获取分类样式类名
  const getCategoryClass = (symbol: string) => {
    const category = getCategory(symbol);
    const classMap: Record<string, string> = {
      'Layer1': 'layer1',
      'DeFi': 'defi',
      'Meme': 'meme',
      'Layer2': 'layer2',
      'AI': 'ai',
      'Other': 'other',
    };
    return classMap[category] || 'other';
  };

  // 类别统计
  const categoryStats = computed(() => {
    const categories: Record<string, { count: number; color: string }> = {
      Layer1: { count: 0, color: '#1890ff' },
      DeFi: { count: 0, color: '#722ed1' },
      Meme: { count: 0, color: '#fa8c16' },
      Layer2: { count: 0, color: '#13c2c2' },
      AI: { count: 0, color: '#eb2f96' },
    };
    displayData.value.forEach((item) => {
      const cat = getCategory(item.symbol);
      if (categories[cat]) categories[cat].count++;
    });
    const total = Object.values(categories).reduce((acc, c) => acc + c.count, 0);
    return Object.entries(categories).map(([name, data]) => ({
      name,
      count: data.count,
      percent: total > 0 ? (data.count / total) * 100 : 0,
      color: data.color,
    }));
  });

  // 类别饼图数据
  const categoryPieData = computed(() => {
    const colors = ['#1890ff', '#722ed1', '#fa8c16', '#13c2c2', '#eb2f96'];
    return categoryStats.value.map((cat, index) => ({
      name: cat.name,
      value: cat.count,
      itemStyle: { color: colors[index % colors.length] },
    }));
  });
  const categoryPieOption = {
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

  // 涨跌饼图
  const upDownPieData = computed(() => [
    { name: '上涨', value: upCount.value, itemStyle: { color: '#52c41a' } },
    { name: '下跌', value: downCount.value, itemStyle: { color: '#ff4d4f' } },
    { name: '持平', value: flatCount.value, itemStyle: { color: '#d9d9d9' } },
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

  // 7日涨跌饼图
  const up7dCount = computed(() => displayData.value.filter((item) => item.change7d > 0).length);
  const down7dCount = computed(() => displayData.value.filter((item) => item.change7d < 0).length);
  const change7dPieData = computed(() => [
    { name: '7日上涨', value: up7dCount.value, itemStyle: { color: '#52c41a' } },
    { name: '7日下跌', value: down7dCount.value, itemStyle: { color: '#ff4d4f' } },
  ]);
  const change7dPieOption = {
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

  // 涨跌区间
  const changeBuckets = computed(() => {
    const buckets = [
      { range: '>10%', min: 10, max: 100, count: 0, type: 'strong-up' },
      { range: '5-10%', min: 5, max: 10, count: 0, type: 'up' },
      { range: '0-5%', min: 0, max: 5, count: 0, type: 'light-up' },
      { range: '-5-0%', min: -5, max: 0, count: 0, type: 'light-down' },
      { range: '-10--5%', min: -10, max: -5, count: 0, type: 'down' },
      { range: '<-10%', min: -100, max: -10, count: 0, type: 'strong-down' },
    ];
    displayData.value.forEach((item) => {
      const pct = item.changePct24h;
      buckets.forEach((b) => {
        if (pct >= b.min && pct < b.max) b.count++;
      });
    });
    const total = displayData.value.length;
    return buckets.map((b) => ({ ...b, percent: total > 0 ? (b.count / total) * 100 : 0 }));
  });

  // 7日涨跌区间
  const change7dBuckets = computed(() => {
    const buckets = [
      { range: '>20%', min: 20, max: 100, count: 0, type: 'strong-up' },
      { range: '10-20%', min: 10, max: 20, count: 0, type: 'up' },
      { range: '0-10%', min: 0, max: 10, count: 0, type: 'light-up' },
      { range: '-10-0%', min: -10, max: 0, count: 0, type: 'light-down' },
      { range: '<-10%', min: -100, max: -10, count: 0, type: 'strong-down' },
    ];
    displayData.value.forEach((item) => {
      const pct = item.change7d;
      buckets.forEach((b) => {
        if (pct >= b.min && pct < b.max) b.count++;
      });
    });
    const total = displayData.value.length;
    return buckets.map((b) => ({ ...b, percent: total > 0 ? (b.count / total) * 100 : 0 }));
  });

  // 排行榜数据
  const rankBarData = computed(() => {
    let sorted: CryptoData[] = [];
    if (rankType.value === 'up') {
      sorted = [...displayData.value].sort((a, b) => b.changePct24h - a.changePct24h).slice(0, 20);
    } else if (rankType.value === 'down') {
      sorted = [...displayData.value].sort((a, b) => a.changePct24h - b.changePct24h).slice(0, 20);
    } else if (rankType.value === 'volume') {
      sorted = [...displayData.value].sort((a, b) => b.volume24h - a.volume24h).slice(0, 20);
    } else {
      sorted = [...displayData.value].sort((a, b) => b.marketCap - a.marketCap).slice(0, 20);
    }
    return sorted.map((item) => ({
      name: item.symbol,
      value: rankType.value === 'up' || rankType.value === 'down' ? item.changePct24h : rankType.value === 'volume' ? item.volume24h / 1e9 : item.marketCap / 1e9,
      itemStyle: { color: rankType.value === 'up' ? '#52c41a' : rankType.value === 'down' ? '#ff4d4f' : '#1890ff' },
    })).reverse();
  });
  const rankBarOption = computed(() => ({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      borderColor: 'rgba(255, 255, 255, 0.2)',
      textStyle: { color: '#fff' },
      formatter: (params: any[]) => {
        const item = params[0];
        const stock = displayData.value.find((s) => s.symbol === item.name);
        if (rankType.value === 'volume') return `${item.name}<br/>成交量: $${item.value.toFixed(2)}B`;
        if (rankType.value === 'marketCap') return `${item.name}<br/>市值: $${item.value.toFixed(1)}B`;
        return `${item.name}<br/>${stock ? (stock.changePct24h >= 0 ? '+' : '') + stock.changePct24h + '%' : ''}`;
      },
    },
    grid: { left: 80, right: 80, top: 15, bottom: 15 },
    xAxis: {
      type: 'value',
      axisLabel: {
        color: 'rgba(255, 255, 255, 0.7)',
        fontSize: 9,
        formatter: rankType.value === 'up' || rankType.value === 'down' ? '{value}%' : '${value}B',
      },
      splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.1)' } },
    },
    yAxis: {
      type: 'category',
      axisLabel: { color: 'rgba(255, 255, 255, 0.9)', fontSize: 10 },
      axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.2)' } },
      data: [],
    },
    series: [{
      type: 'bar',
      barWidth: 14,
      label: {
        show: true,
        position: 'right',
        formatter: (params: any) => {
          if (rankType.value === 'up' || rankType.value === 'down') return `${params.value >= 0 ? '+' : ''}${params.value}%`;
          return `$${params.value.toFixed(1)}B`;
        },
        color: '#fff',
        fontSize: 9,
      },
      data: [],
    }],
  }));

  // 成交量TOP10
  const volumeBarData = computed(() =>
    [...displayData.value]
      .sort((a, b) => b.volume24h - a.volume24h)
      .slice(0, 10)
      .map((item) => ({
        name: item.symbol,
        value: item.volume24h / 1e9,
        itemStyle: { color: '#722ed1' },
      }))
      .reverse()
  );
  const volumeBarOption = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      borderColor: 'rgba(255, 255, 255, 0.2)',
      textStyle: { color: '#fff' },
      formatter: (params: any[]) => `${params[0].name}<br/>成交量: $${params[0].value.toFixed(2)}B`,
    },
    grid: { left: 60, right: 50, top: 15, bottom: 15 },
    xAxis: {
      type: 'value',
      axisLabel: { color: 'rgba(255, 255, 255, 0.7)', fontSize: 9, formatter: '{value}B' },
      splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.1)' } },
    },
    yAxis: {
      type: 'category',
      axisLabel: { color: 'rgba(255, 255, 255, 0.9)', fontSize: 10 },
      axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.2)' } },
      data: [],
    },
    series: [{
      type: 'bar',
      barWidth: 14,
      label: { show: true, position: 'right', formatter: '${c}B', color: '#fff', fontSize: 9 },
      data: [],
    }],
  };

  // 市值区间分布
  const marketCapRangeData = computed(() => {
    const ranges = [
      { name: '> $100B', min: 100e9, max: Infinity, color: '#1890ff' },
      { name: '$10B-100B', min: 10e9, max: 100e9, color: '#52c41a' },
      { name: '$1B-10B', min: 1e9, max: 10e9, color: '#faad14' },
      { name: '< $1B', min: 0, max: 1e9, color: '#ff4d4f' },
    ];
    return ranges.map((r) => ({
      name: r.name,
      value: displayData.value.filter((item) => item.marketCap >= r.min && item.marketCap < r.max).length,
      itemStyle: { color: r.color },
    }));
  });
  const marketCapRangeOption = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '10%', right: '10%', top: '15%', bottom: '15%' },
    xAxis: { type: 'category', axisLabel: { color: '#fff', fontSize: 10 }, axisLine: { lineStyle: { color: 'rgba(255,255,255,0.2)' } } },
    yAxis: { type: 'value', axisLabel: { color: '#fff', fontSize: 10 }, splitLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } } },
    series: [{
      type: 'bar',
      barWidth: 30,
      label: { show: true, position: 'top', color: '#fff' },
      data: [],
    }],
  };

  // BTC价格走势图数据
  const btcTrendData = computed(() => {
    return priceHistory.value.map((item) => ({
      name: item.time,
      value: item.btc,
    }));
  });

  const btcTrendOption = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      borderColor: 'rgba(255, 255, 255, 0.2)',
      textStyle: { color: '#fff' },
      formatter: (params: any[]) => {
        const item = params[0];
        return `${item.name}<br/>BTC: $${Number(item.value).toLocaleString()}`;
      },
    },
    grid: { left: 50, right: 20, top: 15, bottom: 30 },
    xAxis: {
      type: 'category',
      axisLabel: { color: 'rgba(255, 255, 255, 0.7)', fontSize: 9, interval: 9 },
      axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.2)' } },
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: 'rgba(255, 255, 255, 0.7)',
        fontSize: 9,
        formatter: (val: number) => `$${(val / 1000).toFixed(0)}k`,
      },
      splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.1)' } },
    },
    series: [{
      type: 'line',
      smooth: true,
      symbol: 'none',
      lineStyle: { width: 2, color: '#f7931a' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(247, 147, 26, 0.3)' },
            { offset: 1, color: 'rgba(247, 147, 26, 0.05)' },
          ],
        },
      },
      data: [],
    }],
  };

  // ETH价格走势图数据
  const ethTrendData = computed(() => {
    return priceHistory.value.map((item) => ({
      name: item.time,
      value: item.eth,
    }));
  });

  const ethTrendOption = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      borderColor: 'rgba(255, 255, 255, 0.2)',
      textStyle: { color: '#fff' },
      formatter: (params: any[]) => {
        const item = params[0];
        return `${item.name}<br/>ETH: $${Number(item.value).toLocaleString()}`;
      },
    },
    grid: { left: 50, right: 20, top: 15, bottom: 30 },
    xAxis: {
      type: 'category',
      axisLabel: { color: 'rgba(255, 255, 255, 0.7)', fontSize: 9, interval: 9 },
      axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.2)' } },
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: 'rgba(255, 255, 255, 0.7)',
        fontSize: 9,
        formatter: (val: number) => `$${val.toFixed(0)}`,
      },
      splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.1)' } },
    },
    series: [{
      type: 'line',
      smooth: true,
      symbol: 'none',
      lineStyle: { width: 2, color: '#627eea' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(98, 126, 234, 0.3)' },
            { offset: 1, color: 'rgba(98, 126, 234, 0.05)' },
          ],
        },
      },
      data: [],
    }],
  };

  // 过滤数据
  const filteredDataSource = computed(() => {
    if (tableFilter.value === 'all') return displayData.value.slice(0, 30);
    return displayData.value.filter((item) => getCategory(item.symbol) === tableFilter.value).slice(0, 20);
  });

  // 格式化函数
  function formatPrice(price: number): string {
    if (price >= 1000) return price.toFixed(0);
    if (price >= 100) return price.toFixed(2);
    if (price >= 1) return price.toFixed(2);
    if (price >= 0.01) return price.toFixed(4);
    return price.toFixed(8);
  }
  function formatLargeNumber(num: number): string {
    if (num >= 1e12) return `${(num / 1e12).toFixed(1)}T`;
    if (num >= 1e9) return `${(num / 1e9).toFixed(1)}B`;
    if (num >= 1e6) return `${(num / 1e6).toFixed(1)}M`;
    return `${num.toFixed(0)}`;
  }
  function formatVolume(num: number): string {
    return `$${formatLargeNumber(num)}`;
  }
  function formatMarketCap(num: number): string {
    return `$${formatLargeNumber(num)}`;
  }
  function getRangePosition(crypto: CryptoData): number {
    if (!crypto.high24h || !crypto.low24h) return 50;
    return ((crypto.price - crypto.low24h) / (crypto.high24h - crypto.low24h)) * 100;
  }

  // 获取数据 - 支持实时API和数据库
  async function fetchData() {
    loading.value = true;
    try {
      // 优先尝试从CoinGecko API直接获取（通过后端代理）
      const res = await axios.get('/jeecgboot/test/crypto/realtime');
      if (res.data?.success && res.data.result && res.data.result.length > 0) {
        const newData = res.data.result.map((item: any) => ({
          symbol: item.symbol,
          name: item.name,
          price: Number(item.price),
          priceBtc: Number(item.priceBtc),
          change24h: Number(item.change24h),
          changePct24h: Number(item.changePct24h),
          change7d: Number(item.change7d),
          high24h: Number(item.high24h),
          low24h: Number(item.low24h),
          volume24h: Number(item.volume24h),
          marketCap: Number(item.marketCap),
          marketCapRank: Number(item.marketCapRank),
          circulatingSupply: Number(item.circulatingSupply),
          totalSupply: Number(item.totalSupply),
          maxSupply: item.maxSupply ? Number(item.maxSupply) : null,
          tradeDate: item.tradeDate || new Date().toISOString().split('T')[0],
        })) as CryptoData[];

        handleNewData(newData);
      } else {
        // 回退到数据库
        const dbRes = await axios.get('/jeecgboot/test/crypto/list');
        if (dbRes.data?.success && dbRes.data.result) {
          handleNewData(dbRes.data.result as CryptoData[]);
        }
      }
    } catch (e) {
      console.error('获取加密货币数据失败', e);
      message.error('获取加密货币数据失败');
    } finally {
      loading.value = false;
    }
  }

  // 处理新数据
  function handleNewData(newData: CryptoData[]) {
    // 比较新旧数据，标记价格变化
    if (dataSource.value.length > 0) {
      const changes: Record<string, 'up' | 'down' | 'none'> = {};
      newData.forEach((newItem) => {
        const oldItem = dataSource.value.find((old) => old.symbol === newItem.symbol);
        if (oldItem) {
          if (newItem.price > oldItem.price) {
            changes[newItem.symbol] = 'up';
          } else if (newItem.price < oldItem.price) {
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

    // 更新价格历史
    updatePriceHistory();

    if (dataSource.value.length > 0) {
      currentDate.value = dataSource.value[0].tradeDate;
    }
    updateTime.value = new Date().toLocaleTimeString();
  }

  // 平滑更新显示数据
  function smoothUpdateDisplayData(targetData: CryptoData[]) {
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
          price: lerp(start.price, target.price, easeProgress),
          changePct24h: lerp(start.changePct24h, target.changePct24h, easeProgress),
          change7d: lerp(start.change7d, target.change7d, easeProgress),
          volume24h: lerp(start.volume24h, target.volume24h, easeProgress),
          marketCap: lerp(start.marketCap, target.marketCap, easeProgress),
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

  // 自动更新逻辑 - 5分钟轮询
  function startAutoUpdate() {
    if (autoUpdateInterval.value) clearInterval(autoUpdateInterval.value);
    autoUpdateInterval.value = window.setInterval(() => {
      fetchData();
    }, 300000);
    startCountdown();
    startRealtimeSimulation();
  }
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

  // 实时价格模拟 - 每15秒微调价格
  function startRealtimeSimulation() {
    if (realtimeInterval.value) clearInterval(realtimeInterval.value);
    realtimeInterval.value = window.setInterval(() => {
      simulatePriceChanges();
    }, 15000);
  }

  // 模拟价格波动
  function simulatePriceChanges() {
    if (displayData.value.length === 0) return;

    const changes: Record<string, 'up' | 'down' | 'none'> = {};
    const updatedData = displayData.value.map((crypto) => {
      // 随机波动 -0.5% 到 +0.5%
      const fluctuation = (Math.random() - 0.5) * 0.01;
      const newPrice = crypto.price * (1 + fluctuation);
      const priceChange = fluctuation > 0.0001 ? 'up' : fluctuation < -0.0001 ? 'down' : 'none';

      if (priceChange !== 'none') {
        changes[crypto.symbol] = priceChange;
      }

      // 更新价格和涨跌幅
      const priceDiff = newPrice - (crypto.price / (1 + crypto.changePct24h / 100));
      const newChangePct24h = ((priceDiff / newPrice) * 100);

      return {
        ...crypto,
        price: newPrice,
        changePct24h: Number(newChangePct24h.toFixed(2)),
      };
    });

    if (Object.keys(changes).length > 0) {
      priceChangeMap.value = changes;
      dataSource.value = updatedData; // 同步更新数据源
      displayData.value = updatedData;
      updateTime.value = new Date().toLocaleTimeString();

      // 更新价格历史
      updatePriceHistory();

      // 1秒后清除变化标记
      setTimeout(() => {
        priceChangeMap.value = {};
      }, 1000);
    }
  }

  // 更新价格历史
  function updatePriceHistory() {
    const now = new Date();
    const timeStr = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`;
    const btc = displayData.value.find((c) => c.symbol === 'BTC')?.price || 0;
    const eth = displayData.value.find((c) => c.symbol === 'ETH')?.price || 0;
    const totalCap = displayData.value.reduce((sum, c) => sum + c.marketCap, 0);

    priceHistory.value.push({ time: timeStr, btc, eth, totalCap });

    // 保持最多50个数据点
    if (priceHistory.value.length > maxHistoryPoints) {
      priceHistory.value.shift();
    }
  }

  // 监听自动更新开关变化
  watch(isAutoUpdate, (newVal) => {
    if (newVal) {
      startAutoUpdate();
      message.success('已开启自动更新(5分钟)');
    } else {
      stopAutoUpdate();
      message.info('已暂停自动更新');
    }
  });

  function startCountdown() {
    countdown.value = 30;
    if (countdownInterval.value) clearInterval(countdownInterval.value);
    countdownInterval.value = window.setInterval(() => {
      countdown.value--;
      if (countdown.value <= 0) countdown.value = 30;
    }, 1000);
  }
  function stopCountdown() {
    if (countdownInterval.value) {
      clearInterval(countdownInterval.value);
      countdownInterval.value = null;
    }
  }
  async function manualRefresh() {
    await fetchData();
    // 重置倒计时
    countdown.value = 30;
    message.success('数据已刷新');
  }
  function toggleFullscreen() {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen();
      isFullscreen.value = true;
    } else {
      document.exitFullscreen();
      isFullscreen.value = false;
    }
  }
  function handleRankChange() {}
  function handleFilterChange() {}

  onMounted(() => {
    fetchData();
    // 如果自动更新开启，启动定时器
    if (isAutoUpdate.value) {
      startAutoUpdate();
    }
  });
  onUnmounted(() => {
    stopAutoUpdate();
  });
</script>

<style lang="less" scoped>
  .crypto-bigscreen {
    width: 100%;
    height: 100vh;
    background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
    overflow: hidden;
    color: #fff;
    font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif;

    .screen-header {
      height: 80px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0 30px;
      background: rgba(0, 0, 0, 0.3);
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);

      .header-left {
        display: flex;
        align-items: center;

        .logo {
          width: 50px;
          height: 50px;
          background: linear-gradient(135deg, #f7931a 0%, #ac6600 100%);
          border-radius: 12px;
          display: flex;
          justify-content: center;
          align-items: center;
          margin-right: 15px;

          .logo-icon {
            font-size: 28px;
            color: #fff;
          }
        }

        .title-area {
          .main-title {
            font-size: 28px;
            font-weight: bold;
            margin: 0;
            color: #fff;
          }
          .sub-title {
            font-size: 14px;
            color: rgba(255, 255, 255, 0.7);
            margin: 5px 0 0 0;
          }
        }
      }

      .header-right {
        display: flex;
        align-items: center;
        gap: 20px;

        .market-status {
          display: flex;
          align-items: center;
          padding: 8px 15px;
          border: 2px solid;
          border-radius: 8px;
          background: rgba(0, 0, 0, 0.2);

          .status-icon {
            font-size: 20px;
            margin-right: 8px;
          }
          .status-info {
            .status-text {
              font-size: 14px;
              font-weight: bold;
            }
            .countdown-text {
              font-size: 12px;
              color: rgba(255, 255, 255, 0.6);
            }
          }
        }

        .quick-info {
          display: flex;
          gap: 15px;

          .quick-item {
            padding: 8px 12px;
            border-radius: 8px;
            background: rgba(0, 0, 0, 0.3);
            transition: all 0.3s ease;

            &.price-up {
              animation: pulseGreen 0.5s ease;
            }

            &.price-down {
              animation: pulseRed 0.5s ease;
            }

            .quick-label {
              font-size: 12px;
              color: rgba(255, 255, 255, 0.7);
            }
            .quick-value {
              font-size: 16px;
              font-weight: bold;
              margin-left: 5px;
            }
            .quick-change {
              font-size: 12px;
              margin-left: 5px;
            }
          }
          .btc {
            border: 1px solid #f7931a;
          }
          .eth {
            border: 1px solid #627eea;
          }
        }

        @keyframes pulseGreen {
          0% {
            box-shadow: 0 0 0 0 rgba(82, 196, 26, 0.7);
            border-color: #52c41a;
          }
          70% {
            box-shadow: 0 0 0 10px rgba(82, 196, 26, 0);
            border-color: #52c41a;
          }
          100% {
            box-shadow: 0 0 0 0 rgba(82, 196, 26, 0);
          }
        }

        @keyframes pulseRed {
          0% {
            box-shadow: 0 0 0 0 rgba(255, 77, 79, 0.7);
            border-color: #ff4d4f;
          }
          70% {
            box-shadow: 0 0 0 10px rgba(255, 77, 79, 0);
            border-color: #ff4d4f;
          }
          100% {
            box-shadow: 0 0 0 0 rgba(255, 77, 79, 0);
          }
        }

        .update-time {
          font-size: 14px;
          color: rgba(255, 255, 255, 0.7);
        }

        .auto-update-switch {
          background: rgba(255, 255, 255, 0.2);
        }

        .refresh-btn {
          background: rgba(255, 255, 255, 0.1);
          border-color: rgba(255, 255, 255, 0.3);
          color: #fff;
        }

        .fullscreen-btn {
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          border: none;
        }
      }
    }

    .screen-body {
      height: calc(100vh - 80px);
      padding: 20px;
      overflow-y: auto;

      .stats-row {
        display: flex;
        gap: 20px;
        margin-bottom: 20px;

        .stat-card {
          width: calc(100% / 6 - 17px);
          height: 100px;
          border-radius: 12px;
          overflow: hidden;
          position: relative;

          .stat-bg {
            position: absolute;
            inset: 0;
            opacity: 0.3;
          }
          .stat-content {
            position: relative;
            padding: 15px;
            z-index: 1;

            .stat-header {
              display: flex;
              align-items: center;
              margin-bottom: 10px;
              .stat-icon {
                font-size: 20px;
                margin-right: 8px;
                color: rgba(255, 255, 255, 0.8);
              }
              .stat-title {
                font-size: 14px;
                color: rgba(255, 255, 255, 0.8);
              }
            }
            .stat-body {
              display: flex;
              align-items: center;
              .stat-value {
                font-size: 22px;
                font-weight: bold;
              }
              .stat-trend {
                font-size: 14px;
                margin-left: 10px;
                display: flex;
                align-items: center;
                &.up {
                  color: #52c41a;
                }
                &.down {
                  color: #ff4d4f;
                }
              }
            }
            .stat-detail {
              font-size: 12px;
              color: rgba(255, 255, 255, 0.6);
              margin-top: 5px;
            }
          }
        }
      }

      .detail-stats-row {
        display: flex;
        justify-content: space-between;
        padding: 15px 20px;
        background: rgba(0, 0, 0, 0.2);
        border-radius: 12px;
        margin-bottom: 20px;

        .detail-stat-item {
          text-align: center;
          .detail-label {
            font-size: 12px;
            color: rgba(255, 255, 255, 0.6);
          }
          .detail-value {
            font-size: 18px;
            font-weight: bold;
            margin: 5px 0;
            &.up {
              color: #52c41a;
            }
            &.down {
              color: #ff4d4f;
            }
          }
          .detail-desc {
            font-size: 12px;
            color: rgba(255, 255, 255, 0.5);
          }
        }
      }

      .main-charts {
        display: flex;
        gap: 20px;
        margin-bottom: 20px;

        .chart-column {
          &.left {
            width: 25%;
          }
          &.center {
            width: 50%;
          }
          &.right {
            width: 25%;
          }
        }

        .chart-box {
          background: rgba(0, 0, 0, 0.3);
          border-radius: 12px;
          padding: 15px;
          margin-bottom: 20px;

          .chart-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 10px;

            .header-title {
              display: flex;
              align-items: center;
              .title-icon {
                width: 24px;
                height: 24px;
                border-radius: 6px;
                margin-right: 8px;
              }
              .title-text {
                font-size: 16px;
                font-weight: bold;
              }
            }
            .header-tag {
              padding: 4px 8px;
              background: rgba(255, 255, 255, 0.1);
              border-radius: 4px;
              font-size: 12px;
              &.active {
                background: #52c41a;
              }
              &.live {
                background: #ff4d4f;
                animation: livePulse 1.5s infinite;
              }
            }

            @keyframes livePulse {
              0% {
                opacity: 1;
                box-shadow: 0 0 0 0 rgba(255, 77, 79, 0.7);
              }
              50% {
                opacity: 0.8;
                box-shadow: 0 0 0 6px rgba(255, 77, 79, 0);
              }
              100% {
                opacity: 1;
                box-shadow: 0 0 0 0 rgba(255, 77, 79, 0);
              }
            }
            .rank-tabs {
              background: rgba(0, 0, 0, 0.2);
            }
          }

          .chart-content {
            min-height: 200px;
          }

          .sector-detail {
            .sector-row {
              display: flex;
              align-items: center;
              margin: 8px 0;
              .sector-name {
                width: 60px;
                font-size: 12px;
              }
              .sector-bar {
                width: 100px;
                height: 8px;
                background: rgba(255, 255, 255, 0.1);
                border-radius: 4px;
                margin-right: 10px;
                .sector-fill {
                  height: 100%;
                  border-radius: 4px;
                }
              }
              .sector-count {
                font-size: 12px;
                color: rgba(255, 255, 255, 0.7);
              }
            }
          }

          .change-buckets {
            .bucket {
              display: flex;
              align-items: center;
              margin: 6px 0;
              .bucket-range {
                width: 50px;
                font-size: 12px;
              }
              .bucket-bar {
                width: 80px;
                height: 8px;
                background: rgba(255, 255, 255, 0.1);
                border-radius: 4px;
                margin-right: 8px;
                .bucket-fill {
                  height: 100%;
                  border-radius: 4px;
                  &.strong-up {
                    background: #52c41a;
                  }
                  &.up {
                    background: #73d13d;
                  }
                  &.light-up {
                    background: #95de64;
                  }
                  &.light-down {
                    background: #ff7875;
                  }
                  &.down {
                    background: #ff4d4f;
                  }
                  &.strong-down {
                    background: #f5222d;
                  }
                }
              }
              .bucket-count {
                font-size: 12px;
              }
            }
          }
        }

        .main-chart {
          height: 600px;
        }

        .price-chart {
          height: 280px;
        }
      }

      .bottom-table {
        background: rgba(0, 0, 0, 0.3);
        border-radius: 12px;
        padding: 15px;
        overflow-x: auto;

        // 确保表格容器有最小宽度
        min-width: 100%;

        .table-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 15px;
          min-width: 800px;

          .header-title {
            display: flex;
            align-items: center;
            .title-icon {
              width: 24px;
              height: 24px;
              border-radius: 6px;
              margin-right: 8px;
            }
            .title-text {
              font-size: 16px;
              font-weight: bold;
            }
          }

          .table-tabs {
            background: rgba(0, 0, 0, 0.2);
          }
        }

        .table-content {
          min-width: 800px;

          .crypto-row {
            display: flex;
            align-items: center;
            padding: 10px 15px;
            background: rgba(255, 255, 255, 0.05);
            border-radius: 8px;
            margin-bottom: 8px;
            transition: all 0.3s;
            gap: 8px;

            &:hover {
              background: rgba(255, 255, 255, 0.1);
            }

            // 通用列样式
            .crypto-rank,
            .crypto-symbol,
            .crypto-category,
            .crypto-price,
            .crypto-change,
            .crypto-7d,
            .crypto-volume,
            .crypto-marketcap,
            .crypto-range {
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }

            &.header-row {
              background: rgba(0, 0, 0, 0.3);
              font-weight: bold;
              font-size: 12px;
              color: rgba(255, 255, 255, 0.7);
              padding: 8px 15px;
              margin-bottom: 12px;
              text-align: center;

              &:hover {
                background: rgba(0, 0, 0, 0.3);
              }

              // 表头各列保持一致的 flex 布局
              .crypto-rank { flex: 0 0 50px; min-width: 50px; text-align: center; }
              .crypto-symbol { flex: 1 1 100px; min-width: 100px; max-width: 150px; text-align: left; }
              .crypto-category { flex: 0 0 80px; min-width: 70px; max-width: 100px; text-align: center; }
              .crypto-price { flex: 1 1 100px; min-width: 100px; max-width: 140px; text-align: right; }
              .crypto-change { flex: 0 0 90px; min-width: 80px; max-width: 100px; text-align: right; }
              .crypto-7d { flex: 0 0 90px; min-width: 80px; max-width: 100px; text-align: right; }
              .crypto-volume { flex: 1 1 90px; min-width: 80px; max-width: 120px; text-align: right; }
              .crypto-marketcap { flex: 1 1 90px; min-width: 80px; max-width: 120px; text-align: right; }
              .crypto-range { flex: 2 1 200px; min-width: 150px; text-align: right; justify-content: flex-end; }
            }

            .crypto-rank {
              flex: 0 0 50px;
              min-width: 50px;
              font-size: 14px;
              color: rgba(255, 255, 255, 0.6);
              text-align: center;
            }

            .crypto-symbol {
              flex: 1 1 100px;
              min-width: 100px;
              max-width: 150px;
              .symbol-name {
                font-size: 14px;
                font-weight: bold;
              }
              .symbol-full {
                font-size: 12px;
                color: rgba(255, 255, 255, 0.6);
                margin-left: 5px;
              }
            }

            .crypto-category {
              flex: 0 0 80px;
              min-width: 70px;
              max-width: 100px;
              .category-tag {
                display: inline-block;
                padding: 2px 8px;
                border-radius: 4px;
                font-size: 11px;
                font-weight: 500;
                text-align: center;
                white-space: nowrap;
                &.layer1 {
                  background: rgba(24, 144, 255, 0.2);
                  color: #1890ff;
                  border: 1px solid rgba(24, 144, 255, 0.3);
                }
                &.defi {
                  background: rgba(114, 46, 209, 0.2);
                  color: #722ed1;
                  border: 1px solid rgba(114, 46, 209, 0.3);
                }
                &.meme {
                  background: rgba(250, 140, 22, 0.2);
                  color: #fa8c16;
                  border: 1px solid rgba(250, 140, 22, 0.3);
                }
                &.layer2 {
                  background: rgba(19, 194, 194, 0.2);
                  color: #13c2c2;
                  border: 1px solid rgba(19, 194, 194, 0.3);
                }
                &.ai {
                  background: rgba(235, 47, 150, 0.2);
                  color: #eb2f96;
                  border: 1px solid rgba(235, 47, 150, 0.3);
                }
                &.other {
                  background: rgba(255, 255, 255, 0.1);
                  color: rgba(255, 255, 255, 0.7);
                  border: 1px solid rgba(255, 255, 255, 0.2);
                }
              }
            }

            .crypto-price {
              flex: 1 1 100px;
              min-width: 100px;
              max-width: 140px;
              font-size: 14px;
              transition: all 0.3s ease;

              .price-value {
                font-weight: 600;
              }

              .price-arrow {
                font-size: 10px;
                margin-left: 4px;
              }

              &.up {
                color: #52c41a;
                animation: priceFlashGreen 0.5s ease;
                .price-arrow {
                  color: #52c41a;
                }
              }

              &.down {
                color: #ff4d4f;
                animation: priceFlashRed 0.5s ease;
                .price-arrow {
                  color: #ff4d4f;
                }
              }
            }

            @keyframes priceFlashGreen {
              0% {
                background-color: rgba(82, 196, 26, 0.3);
                transform: scale(1.05);
              }
              100% {
                background-color: transparent;
                transform: scale(1);
              }
            }

            @keyframes priceFlashRed {
              0% {
                background-color: rgba(255, 77, 79, 0.3);
                transform: scale(1.05);
              }
              100% {
                background-color: transparent;
                transform: scale(1);
              }
            }

            .crypto-change {
              flex: 0 0 90px;
              min-width: 80px;
              max-width: 100px;
              font-size: 14px;
              &.up {
                color: #52c41a;
              }
              &.down {
                color: #ff4d4f;
              }
            }

            .crypto-7d {
              flex: 0 0 90px;
              min-width: 80px;
              max-width: 100px;
              font-size: 14px;
              &.up {
                color: #52c41a;
              }
              &.down {
                color: #ff4d4f;
              }
            }

            .crypto-volume {
              flex: 1 1 90px;
              min-width: 80px;
              max-width: 120px;
              font-size: 14px;
              color: rgba(255, 255, 255, 0.8);
            }

            .crypto-marketcap {
              flex: 1 1 90px;
              min-width: 80px;
              max-width: 120px;
              font-size: 14px;
              color: rgba(255, 255, 255, 0.8);
            }

            .crypto-range {
              flex: 2 1 200px;
              min-width: 150px;
              display: flex;
              align-items: center;
              justify-content: flex-end;
              .range-bar {
                flex: 1;
                min-width: 60px;
                max-width: 100px;
                height: 6px;
                background: rgba(255, 255, 255, 0.1);
                border-radius: 3px;
                margin-right: 10px;
                .range-indicator {
                  width: 8px;
                  height: 8px;
                  background: #1890ff;
                  border-radius: 50%;
                  position: relative;
                  top: -1px;
                }
              }
              .range-values {
                font-size: 12px;
                color: rgba(255, 255, 255, 0.6);
                white-space: nowrap;
              }
            }
          }
        }
      }
    }

    // 涨跌颜色
    .up {
      color: #52c41a;
    }
    .down {
      color: #ff4d4f;
      transition: color 0.3s ease;
    }

    // 数字变化平滑过渡
    .stat-value, .detail-value, .price-value, .quick-value {
      transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
    }

    // 表格行平滑过渡
    .crypto-row {
      transition: background-color 0.3s ease;
    }
  }
</style>