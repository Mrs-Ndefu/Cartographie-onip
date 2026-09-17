const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

async function request<T>(path: string, options: RequestInit, token?: string | null): Promise<T> {
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(options.headers ?? {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  }

  const response = await fetch(`${API_URL}${path}`, { ...options, headers })

  if (!response.ok) {
    throw new ApiError(response.status, `Erreur ${response.status} sur ${path}`)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

async function requestForm<T>(path: string, formData: FormData, token?: string | null): Promise<T> {
  const headers: HeadersInit = {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  }
  const response = await fetch(`${API_URL}${path}`, { method: 'POST', body: formData, headers })

  if (!response.ok) {
    throw new ApiError(response.status, `Erreur ${response.status} sur ${path}`)
  }
  return (await response.json()) as T
}

export const apiClient = {
  post: <T>(path: string, body: unknown, token?: string | null) =>
    request<T>(path, { method: 'POST', body: JSON.stringify(body) }, token),
  put: <T>(path: string, body: unknown, token?: string | null) =>
    request<T>(path, { method: 'PUT', body: JSON.stringify(body) }, token),
  get: <T>(path: string, token?: string | null) => request<T>(path, { method: 'GET' }, token),
  postForm: <T>(path: string, formData: FormData, token?: string | null) => requestForm<T>(path, formData, token),
}
