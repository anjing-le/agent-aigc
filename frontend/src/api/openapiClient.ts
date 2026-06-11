import request from '@/utils/http'
import {
  OPENAPI_OPERATIONS,
  type OpenApiHttpMethod,
  type OpenApiOperationData,
  type OpenApiOperationId,
  type OpenApiOperationPathParams,
  type OpenApiOperationQuery,
  type OpenApiOperationRequest
} from '@/contracts/openapi/operations'
import { resolveApiPath } from './paths'

export type OpenApiPathParamValue = string | number | boolean
export type OpenApiPathParams = Record<string, OpenApiPathParamValue>
export type OpenApiQueryParams = Record<string, unknown>

type OpenApiPathParamsOption<T extends OpenApiOperationId> =
  OpenApiOperationPathParams<T> extends undefined
    ? { pathParams?: never }
    : { pathParams: OpenApiOperationPathParams<T> }

type OpenApiQueryOption<T extends OpenApiOperationId> =
  OpenApiOperationQuery<T> extends undefined
    ? { query?: never }
    : { query?: OpenApiOperationQuery<T> }

type OpenApiBodyOption<T extends OpenApiOperationId> =
  OpenApiOperationRequest<T> extends undefined
    ? { body?: never }
    : { body: OpenApiOperationRequest<T> }

export type OpenApiRequestOptions<T extends OpenApiOperationId> = OpenApiPathParamsOption<T> &
  OpenApiQueryOption<T> &
  OpenApiBodyOption<T> & {
    showErrorMessage?: boolean
    showSuccessMessage?: boolean
  }

type OpenApiPathParamsArg<T extends OpenApiOperationId> =
  OpenApiOperationPathParams<T> extends undefined
    ? [pathParams?: undefined]
    : [pathParams: OpenApiOperationPathParams<T>]

type OpenApiOperationsWithOptionalOptions = {
  [K in OpenApiOperationId]: Record<string, never> extends OpenApiRequestOptions<K> ? K : never
}[OpenApiOperationId]

type OpenApiOperationsWithRequiredOptions = Exclude<
  OpenApiOperationId,
  OpenApiOperationsWithOptionalOptions
>

type OpenApiRuntimeOptions<T extends OpenApiOperationId> = {
  pathParams?: OpenApiPathParams
  query?: OpenApiQueryParams
  body?: OpenApiOperationRequest<T>
  showErrorMessage?: boolean
  showSuccessMessage?: boolean
}

interface OpenApiRuntimeRequestConfig {
  url: string
  method: OpenApiHttpMethod
  params?: OpenApiQueryParams
  data?: unknown
  showErrorMessage?: boolean
  showSuccessMessage?: boolean
}

const BODY_METHODS = new Set<OpenApiHttpMethod>(['POST', 'PUT', 'PATCH'])
const PATH_PARAM_PATTERN = /\{([^}]+)\}/g

export const bindOpenApiPathParams = (
  apiPath: string,
  params: OpenApiPathParams = {}
): string => {
  return apiPath.replace(PATH_PARAM_PATTERN, (token, name: string) => {
    const value = params[name]
    if (value === undefined) {
      throw new Error(`Missing OpenAPI path param: ${name}`)
    }
    return encodeURIComponent(String(value))
  })
}

export const openApiPath = <T extends OpenApiOperationId>(
  operationId: T,
  ...[pathParams]: OpenApiPathParamsArg<T>
): string => {
  return bindOpenApiPathParams(
    OPENAPI_OPERATIONS[operationId].path,
    pathParams as OpenApiPathParams | undefined
  )
}

export const resolveOpenApiPath = <T extends OpenApiOperationId>(
  operationId: T,
  ...[pathParams]: OpenApiPathParamsArg<T>
): string => {
  return resolveApiPath(
    bindOpenApiPathParams(
      OPENAPI_OPERATIONS[operationId].path,
      pathParams as OpenApiPathParams | undefined
    )
  )
}

export function openApiRequest<T extends OpenApiOperationsWithOptionalOptions>(
  operationId: T,
  options?: OpenApiRequestOptions<T>
): Promise<OpenApiOperationData<T>>

export function openApiRequest<T extends OpenApiOperationsWithRequiredOptions>(
  operationId: T,
  options: OpenApiRequestOptions<T>
): Promise<OpenApiOperationData<T>>

export function openApiRequest<T extends OpenApiOperationId>(
  operationId: T,
  options?: OpenApiRequestOptions<T>
): Promise<OpenApiOperationData<T>> {
  const requestOptions = (options || {}) as OpenApiRuntimeOptions<T>
  const operation = OPENAPI_OPERATIONS[operationId]
  const config: OpenApiRuntimeRequestConfig = {
    url: bindOpenApiPathParams(operation.path, requestOptions.pathParams),
    method: operation.method,
    showErrorMessage: requestOptions.showErrorMessage,
    showSuccessMessage: requestOptions.showSuccessMessage
  }

  if (requestOptions.query) {
    config.params = requestOptions.query
  }

  if (BODY_METHODS.has(operation.method) && requestOptions.body !== undefined) {
    config.data = requestOptions.body
  }

  return request.request<OpenApiOperationData<T>>(config)
}
