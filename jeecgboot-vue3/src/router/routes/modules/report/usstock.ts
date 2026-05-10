import type { AppRouteModule } from '/@/router/types';

import { LAYOUT } from '/@/router/constant';

const usstock: AppRouteModule = {
  path: '/report',
  name: 'Report',
  component: LAYOUT,
  redirect: '/report/usstock',
  meta: {
    orderNo: 100,
    icon: 'ion:analytics-outline',
    title: '报表中心',
  },
  children: [
    {
      path: 'usstock',
      name: 'UsStockReport',
      component: () => import('/@/views/report/usstock/index.vue'),
      meta: {
        title: '美股涨跌行情',
        ignoreKeepAlive: true,
      },
    },
  ],
};

export default usstock;
