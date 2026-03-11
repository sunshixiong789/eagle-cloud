import { paths } from 'src/routes/paths';

import packageJson from '../package.json';

// ----------------------------------------------------------------------

export type ConfigValue = {
  appName: string;
  appVersion: string;
  serverUrl: string;
  assetsDir: string;
  auth: {
    method: 'jwt' | 'auth0';
    skip: boolean;
    redirectPath: string;
  };
  auth0: { clientId: string; domain: string; scope: string };
};

// ----------------------------------------------------------------------

export const CONFIG: ConfigValue = {
  appName: 'ELEGANTEER WEB',
  appVersion: packageJson.version,
  serverUrl: import.meta.env.VITE_SERVER_URL ?? '',
  assetsDir: import.meta.env.VITE_ASSETS_DIR ?? '',
  /**
   * Auth
   * @method jwt | auth0
   */
  auth: {
    method: 'auth0',
    skip: false,
    redirectPath: paths.dashboard.root,
  },
  /**
   * Auth0
   */
  auth0: {
    clientId: import.meta.env.VITE_AUTH0_CLIENT_ID ?? '',
    domain: import.meta.env.VITE_AUTH0_DOMAIN ?? '',
    scope: import.meta.env.VITE_AUTH0_SCOPE ?? '',
  },
};
