import 'src/global.css';

import React, { useEffect } from 'react';
import { LocalizationProvider } from '@mui/x-date-pickers';

import { CONFIG } from 'src/global-config';
import { usePathname } from 'src/routes/hooks';
import { themeConfig, ThemeProvider } from 'src/theme';
import { ProgressBar } from 'src/components/progress-bar';
import { MotionLazy } from 'src/components/animate/motion-lazy';
import { AuthProvider as JwtAuthProvider } from 'src/auth/context/jwt';
import { AuthProvider as Auth0AuthProvider } from 'src/auth/context/auth0';
import { SettingsDrawer, defaultSettings, SettingsProvider } from 'src/components/settings';

import { I18nProvider } from './locales';

const AuthProvider = (CONFIG.auth.method === 'auth0' && Auth0AuthProvider) || JwtAuthProvider;

type AppProps = {
  children: React.ReactNode;
};

export default function App({ children }: AppProps) {
  useScrollToTop();

  return (
    <I18nProvider>
      <AuthProvider>
        <SettingsProvider defaultSettings={defaultSettings}>
          <LocalizationProvider>
            <ThemeProvider
              modeStorageKey={themeConfig.modeStorageKey}
              defaultMode={themeConfig.defaultMode}
            >
              <MotionLazy>
                <ProgressBar />
                <SettingsDrawer defaultSettings={defaultSettings} />
                {children}
              </MotionLazy>
            </ThemeProvider>
          </LocalizationProvider>
        </SettingsProvider>
      </AuthProvider>
    </I18nProvider>
  );
}

// ----------------------------------------------------------------------

function useScrollToTop() {
  const pathname = usePathname();

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);

  return null;
}
