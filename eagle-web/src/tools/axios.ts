import axios from 'axios';
import type { AxiosRequestConfig } from 'axios';

import { CONFIG } from 'src/global-config';

import { paths } from '../routes/paths';
import { JWT_STORAGE_KEY } from '../auth/context/jwt';

// ----------------------------------------------------------------------

const axiosInstance = axios.create({
  baseURL: CONFIG.serverUrl,
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * Optional: Add token (if using auth)
 */
axiosInstance.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    // 1、处理 401 未授权错误
    if (error?.response?.status === 401) {
      // 1、1 刷新token

      // 清除本地存储的 token
      sessionStorage.removeItem(JWT_STORAGE_KEY);
      // 清除 axios 默认请求头
      delete axiosInstance.defaults.headers.common.Authorization;

      // 获取当前路径用于登录后返回
      const queryString = new URLSearchParams({ returnTo: window.location.pathname }).toString();
      // 重定向到登录页
      window.location.href = `${paths.auth.jwt.signIn}?${queryString}`;
      return Promise.reject(new Error('Unauthorized: Token expired or invalid'));
    }
    const message = error?.response?.data?.message || error?.message || 'Something went wrong!';
    console.error('Axios error:', message);
    return Promise.reject(new Error(message));
  }
);

export default axiosInstance;

// ----------------------------------------------------------------------

export const fetcher = async <T = unknown>(
  args: string | [string, AxiosRequestConfig]
): Promise<T> => {
  try {
    const [url, config] = Array.isArray(args) ? args : [args, {}];

    const res = await axiosInstance.get<T>(url, config);

    return res.data;
  } catch (error) {
    console.error('Fetcher failed:', error);
    throw error;
  }
};

// ----------------------------------------------------------------------

export const endpoints = {
  chat: '/api/chat',
  kanban: '/api/kanban',
  calendar: '/api/calendar',
  auth: {
    me: '/api/auth/me',
    signIn: '/api/auth/sign-in',
    signUp: '/api/auth/sign-up',
  },
  mail: {
    list: '/api/mail/list',
    details: '/api/mail/details',
    labels: '/api/mail/labels',
  },
  post: {
    list: '/api/post/list',
    details: '/api/post/details',
    latest: '/api/post/latest',
    search: '/api/post/search',
  },
  product: {
    list: '/api/product/list',
    details: '/api/product/details',
    search: '/api/product/search',
  },
} as const;
