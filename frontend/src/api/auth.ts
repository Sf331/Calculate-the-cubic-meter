import { get, post } from './request'

export interface CurrentUser {
  id: number
  username: string
  realName: string
  /** PRINCIPAL / ACADEMIC / TEACHER / STUDENT / PARENT */
  role: string
}

export const login = (username: string, password: string) =>
  post<CurrentUser>('/auth/login', { username, password })

export const logout = () => post<void>('/auth/logout')

export const fetchMe = () => get<CurrentUser>('/auth/me')
