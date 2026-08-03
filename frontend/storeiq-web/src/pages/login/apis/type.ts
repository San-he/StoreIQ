export interface LoginRequestData {
  /** 用户名 */
  username: string
  /** 密码 */
  password: string
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  role: string
  avatar: string | null
  permissions: string[]
}

export type LoginResponseData = ApiResponseData<{
  token: string
  user: UserInfo
}>

export type UserInfoResponseData = ApiResponseData<UserInfo>
