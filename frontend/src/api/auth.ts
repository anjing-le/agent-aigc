import { openApiRequest } from './openapiClient'
import type { LoginParams, LoginResponse, RefreshTokenParams, UserInfo } from './model/authModel'

const normalizeLoginResponse = (data: Omit<LoginResponse, 'token'> & { token?: string }): LoginResponse => ({
  ...data,
  token: data.token || data.accessToken
})

const normalizeUserInfo = (data: Omit<UserInfo, 'buttons'> & { buttons?: string[] }): UserInfo => ({
  ...data,
  roles: data.roles || [],
  permissions: data.permissions || [],
  buttons: data.buttons || data.permissions || []
})

/**
 * 登录
 * @param params 登录参数
 * @returns 登录响应
 */
export async function fetchLogin(params: LoginParams): Promise<LoginResponse> {
  const data = await openApiRequest('login', {
    body: params
  })
  return normalizeLoginResponse(data)
}

/**
 * 获取用户信息
 * @returns 用户信息
 */
export async function fetchGetUserInfo(): Promise<UserInfo> {
  const data = await openApiRequest('getCurrentUser')
  return normalizeUserInfo(data)
}

/**
 * 刷新 Token
 * @param params refresh token 参数
 * @returns 登录响应
 */
export async function fetchRefreshToken(params: RefreshTokenParams): Promise<LoginResponse> {
  const data = await openApiRequest('refreshToken', {
    body: params
  })
  return normalizeLoginResponse(data)
}
