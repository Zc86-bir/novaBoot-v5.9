import type { AppRouteRecordRaw } from '/@/router/types';
import { LAYOUT } from '/@/router/constant';

export const AI_ROUTE: AppRouteRecordRaw = {
  path: '',
  name: 'ai-parent',
  component: LAYOUT,
  meta: {
    title: 'ai',
  },
  children: [
    {
      path: '/ai',
      name: 'ai',
      component: () => import('/@/views/dashboard/ai/index.vue'),
      meta: {
        title: 'AI助手',
      },
    },
  ],
};

export const REPORT_ROUTE: AppRouteRecordRaw = {
  path: '',
  name: 'report-parent',
  component: LAYOUT,
  meta: {
    title: '报表中心',
  },
  children: [
    {
      path: '/report/usstock',
      name: 'UsStockReport',
      component: () => import('/@/views/report/usstock/index.vue'),
      meta: {
        title: '美股涨跌行情',
      },
    },
    {
      path: '/report/usstock/dashboard',
      name: 'UsStockDashboard',
      component: () => import('/@/views/report/usstock/bigscreen/index.vue'),
      meta: {
        title: '美股行情大屏',
        keepAlive: false,
        ignoreAuth: true, // 免登录访问
      },
    },
  ],
};

export const CRYPTO_ROUTE: AppRouteRecordRaw = {
  path: '',
  name: 'crypto-parent',
  component: LAYOUT,
  meta: {
    title: '加密货币',
  },
  children: [
    {
      path: '/report/crypto',
      name: 'CryptoReport',
      component: () => import('/@/views/report/crypto/bigscreen/index.vue'),
      meta: {
        title: '加密货币大屏',
        keepAlive: false,
        ignoreAuth: true, // 免登录访问
      },
    },
  ],
};

export const staticRoutesList = [AI_ROUTE, REPORT_ROUTE, CRYPTO_ROUTE];
