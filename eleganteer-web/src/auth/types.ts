export type UserType = Record<string, any> | null;

export type AuthState = {
  user: UserType;
  loading: boolean;
};

export type AuthContextValue = {
  user: UserType;
  loading: boolean;
  authenticated: boolean;
  unauthenticated: boolean;
  checkUserSession?: () => Promise<void>;
};

interface OAuth2ServerMetadata {
  /**
   * OAuth2 服务器的颁发者标识符
   */
  issuer: string;

  /**
   * 授权端点 URL
   */
  authorization_endpoint: string;

  /**
   * 令牌端点 URL
   */
  token_endpoint: string;

  /**
   * 令牌端点支持的客户端认证方法
   */
  token_endpoint_auth_methods_supported: AuthMethod[];

  /**
   * JSON Web Key Set URI
   */
  jwks_uri: string;

  /**
   * 支持的响应类型
   */
  response_types_supported: ResponseType[];

  /**
   * 支持的授权类型
   */
  grant_types_supported: GrantType[];

  /**
   * 令牌撤销端点 URL
   */
  revocation_endpoint: string;

  /**
   * 撤销端点支持的客户端认证方法
   */
  revocation_endpoint_auth_methods_supported: AuthMethod[];

  /**
   * 令牌内省端点 URL
   */
  introspection_endpoint: string;

  /**
   * 内省端点支持的客户端认证方法
   */
  introspection_endpoint_auth_methods_supported: AuthMethod[];

  /**
   * 支持的代码挑战方法（PKCE）
   */
  code_challenge_methods_supported: CodeChallengeMethod[];

  /**
   * 是否支持 TLS 客户端证书绑定的访问令牌
   */
  tls_client_certificate_bound_access_tokens: boolean;

  /**
   * DPoP 签名支持的算法值
   */
  dpop_signing_alg_values_supported: SigningAlgorithm[];
}

/**
 * 客户端认证方法
 */
type AuthMethod =
  | 'client_secret_basic'
  | 'client_secret_post'
  | 'client_secret_jwt'
  | 'private_key_jwt'
  | 'tls_client_auth'
  | 'self_signed_tls_client_auth';

/**
 * OAuth2 响应类型
 */
type ResponseType = 'code' | 'token' | 'id_token';

/**
 * OAuth2 授权类型
 */
type GrantType =
  | 'authorization_code'
  | 'client_credentials'
  | 'refresh_token'
  | 'password'
  | 'urn:ietf:params:oauth:grant-type:token-exchange'
  | 'urn:ietf:params:oauth:grant-type:jwt-bearer';

/**
 * PKCE 代码挑战方法
 */
type CodeChallengeMethod = 'plain' | 'S256';

/**
 * JWT 签名算法
 */
type SigningAlgorithm =
  | 'RS256'
  | 'RS384'
  | 'RS512'
  | 'PS256'
  | 'PS384'
  | 'PS512'
  | 'ES256'
  | 'ES384'
  | 'ES512'
  | 'HS256'
  | 'HS384'
  | 'HS512';

export type {
  GrantType,
  AuthMethod,
  ResponseType,
  SigningAlgorithm,
  CodeChallengeMethod,
  OAuth2ServerMetadata,
};
