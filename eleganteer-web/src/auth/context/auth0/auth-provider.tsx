import type { AppState } from '@auth0/auth0-react';
import { useAuth0, Auth0Provider } from '@auth0/auth0-react';
import React, { useMemo, useEffect, useCallback } from 'react';

import { CONFIG } from 'src/global-config';

import { AuthContext } from '../auth-context';
import { paths } from '../../../routes/paths';

// ----------------------------------------------------------------------

type Props = {
  children: React.ReactNode;
};

export function AuthProvider({ children }: Props) {
  const { domain, clientId, scope } = CONFIG.auth0;

  const onRedirectCallback = useCallback((appState?: AppState) => {
    window.location.replace(appState?.returnTo || window.location.pathname);
  }, []);

  if (!(domain && clientId)) {
    return null;
  }

  return (
    <Auth0Provider
      domain={domain}
      clientId={clientId}
      authorizationParams={{
        redirect_uri: `${window.location.origin}${paths.auth.auth0.signIn}`,
        scope,
      }}
      onRedirectCallback={onRedirectCallback}
      cacheLocation="localstorage"
    >
      <AuthProviderContainer>{children}</AuthProviderContainer>
    </Auth0Provider>
  );
}

// ----------------------------------------------------------------------

function AuthProviderContainer({ children }: Props) {
  const { user, isLoading, isAuthenticated, getAccessTokenSilently } = useAuth0();

  /*const getAccessToken   =  () => {
    try {
      if (isAuthenticated) {
        const token = await getAccessTokenSilently();
        axios.defaults.headers.common.Authorization = `Bearer ${token}`;
        return token;
      } else {
        delete axios.defaults.headers.common.Authorization;
        return null;
      }
    } catch (error) {
      console.error(error);
    }
  };

  useEffect(() => {
    getAccessToken()
  }, [getAccessToken]);*/
  useEffect(() => {
    getAccessTokenSilently().then((token) => {
      console.log(token);
    });
  });

  // ----------------------------------------------------------------------

  const checkAuthenticated = isAuthenticated ? 'authenticated' : 'unauthenticated';

  const status = isLoading ? 'loading' : checkAuthenticated;

  const memoizedValue = useMemo(
    () => ({
      user: user
        ? {
            ...user,
            id: user?.sub,
            displayName: user?.name,
            photoURL: user?.picture,
            role: user?.role ?? 'admin',
          }
        : null,
      loading: status === 'loading',
      authenticated: status === 'authenticated',
      unauthenticated: status === 'unauthenticated',
    }),
    [status, user]
  );

  return <AuthContext value={memoizedValue}>{children}</AuthContext>;
}
