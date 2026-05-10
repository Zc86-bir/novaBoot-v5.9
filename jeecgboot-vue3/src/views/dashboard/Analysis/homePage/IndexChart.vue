<template>
  <div class="p-4">
    <ChartGroupCard class="enter-y" :loading="loading" type="chart" />

    <!-- 数据大屏入口 -->
    <a-row class="!my-4 enter-y" :gutter="16">
      <a-col :span="12">
        <a-card class="bigscreen-card" @click="goToCryptoScreen" :hoverable="true">
          <div class="bigscreen-content crypto-bg">
            <div class="bigscreen-icon">
              <LineChartOutlined />
            </div>
            <div class="bigscreen-info">
              <h3>加密货币实时看板</h3>
              <p>实时追踪BTC、ETH等主流加密货币行情</p>
              <a-tag color="orange">实时数据</a-tag>
            </div>
            <RightOutlined class="arrow-icon" />
          </div>
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card class="bigscreen-card" @click="goToStockScreen" :hoverable="true">
          <div class="bigscreen-content stock-bg">
            <div class="bigscreen-icon">
              <BarChartOutlined />
            </div>
            <div class="bigscreen-info">
              <h3>美股实时看板</h3>
              <p>纳斯达克、标普500等美股实时行情</p>
              <a-tag color="blue">实时数据</a-tag>
            </div>
            <RightOutlined class="arrow-icon" />
          </div>
        </a-card>
      </a-col>
    </a-row>

    <SaleTabCard class="!my-4 enter-y" :loading="loading" />
    <a-row>
      <a-col :span="24">
        <a-card :loading="loading" :bordered="false" title="最近一周访问量统计">
          <div class="infoArea">
            <HeadInfo title="今日IP" :iconColor="ipColor" :content="loginfo.todayIp" icon="environment"></HeadInfo>
            <HeadInfo title="今日访问" :iconColor="visitColor" :content="loginfo.todayVisitCount" icon="team"></HeadInfo>
            <HeadInfo title="总访问量" :iconColor="seriesColor" :content="loginfo.totalVisitCount" icon="rise"></HeadInfo>
          </div>
          <LineMulti :chartData="lineMultiData" height="33vh" type="line" :option="{ legend: { top: 'bottom' } }"></LineMulti>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>
<script lang="ts" setup>
  import { ref, watch } from 'vue';
  import { useRouter } from 'vue-router';
  import { LineChartOutlined, BarChartOutlined, RightOutlined } from '@ant-design/icons-vue';
  import ChartGroupCard from '../components/ChartGroupCard.vue';
  import SaleTabCard from '../components/SaleTabCard.vue';
  import LineMulti from '/@/components/chart/LineMulti.vue';
  import HeadInfo from '/@/components/chart/HeadInfo.vue';
  import { getLoginfo, getVisitInfo } from '../api.ts';
  import { useRootSetting } from '/@/hooks/setting/useRootSetting';

  const loading = ref(true);
  const router = useRouter();
  const { getThemeColor } = useRootSetting();

  setTimeout(() => {
    loading.value = false;
  }, 500);

  const loginfo = ref({});
  const lineMultiData = ref([]);

  function initLogInfo() {
    getLoginfo(null).then((res) => {
      if (res.success) {
        Object.keys(res.result).forEach((key) => {
          res.result[key] = res.result[key] + '';
        });
        loginfo.value = res.result;
      }
    });
    getVisitInfo(null).then((res) => {
      if (res.success) {
        lineMultiData.value = [];
        res.result.forEach((item) => {
          lineMultiData.value.push({ name: item.type, type: 'ip', value: item.ip, color: ipColor.value });
          lineMultiData.value.push({ name: item.type, type: 'visit', value: item.visit, color: visitColor.value });
        });
      }
    });
  }

  const ipColor = ref();
  const visitColor = ref();
  const seriesColor = ref();
  watch(
    () => getThemeColor.value,
    () => {
      seriesColor.value = getThemeColor.value;
      visitColor.value = '#67B962';
      ipColor.value = getThemeColor.value;
      initLogInfo();
    },
    { immediate: true }
  );

  function getRandomColor() {
    var letters = '0123456789ABCDEF';
    var color = '#';
    for (var i = 0; i < 6; i++) {
      color += letters[Math.floor(Math.random() * 16)];
    }
    return color;
  }

  // 跳转到加密货币大屏
  function goToCryptoScreen() {
    router.push('/report/crypto');
  }

  // 跳转到美股大屏
  function goToStockScreen() {
    router.push('/report/usstock/dashboard');
  }
</script>

<style lang="less" scoped>
   .infoArea {
    display: flex;
    justify-content: space-between;
    padding: 0 10%;
    .head-info.center {
      padding: 0;
    }
    .head-info {
      min-width: 0;
    }
  }
  .circle-cust {
    position: relative;
    top: 28px;
    left: -100%;
  }

  .extra-wrapper {
    line-height: 55px;
    padding-right: 24px;

    .extra-item {
      display: inline-block;
      margin-right: 24px;

      a {
        margin-left: 24px;
      }
    }
  }

  /* 首页访问量统计 */
  .head-info {
    position: relative;
    text-align: left;
    padding: 0 32px 0 0;
    min-width: 125px;

    &.center {
      text-align: center;
      padding: 0 32px;
    }

    span {
      color: rgba(0, 0, 0, 0.45);
      display: inline-block;
      font-size: 0.95rem;
      line-height: 42px;
      margin-bottom: 4px;
    }

    p {
      line-height: 42px;
      margin: 0;

      a {
        font-weight: 600;
        font-size: 1rem;
      }
    }
  }
  .ant-card {
    ::v-deep(.ant-card-head-title) {
      font-weight: normal;
    }
  }

  /* 大屏入口卡片样式 */
  .bigscreen-card {
    cursor: pointer;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
    }

    .bigscreen-content {
      display: flex;
      align-items: center;
      padding: 8px 4px;

      &.crypto-bg {
        .bigscreen-icon {
          background: linear-gradient(135deg, #f7931a 0%, #ffd700 100%);
        }
      }

      &.stock-bg {
        .bigscreen-icon {
          background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);
        }
      }
    }

    .bigscreen-icon {
      width: 64px;
      height: 64px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 32px;
      color: #fff;
      margin-right: 16px;
      flex-shrink: 0;
    }

    .bigscreen-info {
      flex: 1;
      min-width: 0;

      h3 {
        margin: 0 0 8px 0;
        font-size: 18px;
        font-weight: 600;
        color: rgba(0, 0, 0, 0.85);
      }

      p {
        margin: 0 0 8px 0;
        font-size: 14px;
        color: rgba(0, 0, 0, 0.45);
      }
    }

    .arrow-icon {
      font-size: 20px;
      color: rgba(0, 0, 0, 0.25);
      transition: transform 0.3s ease;
    }

    &:hover .arrow-icon {
      transform: translateX(4px);
      color: rgba(0, 0, 0, 0.45);
    }
  }
</style>
