#!/usr/bin/env node
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')
const args = process.argv.slice(2)
const check = args.includes('--check')
const openapiPath = args.find((arg) => !arg.startsWith('--'))
const schemasOutputPath = path.join(root, 'frontend/src/contracts/openapi/schemas.ts')
const operationsOutputPath = path.join(root, 'frontend/src/contracts/openapi/operations.ts')

function fail(message) {
  console.error(`generate-openapi-frontend-types: ${message}`)
  process.exit(1)
}

function readJson(file) {
  if (!file || !fs.existsSync(file)) {
    fail('usage: node scripts/generate-openapi-frontend-types.js <openapi-json-file> [--check]')
  }

  try {
    return JSON.parse(fs.readFileSync(file, 'utf8'))
  } catch (error) {
    fail(`invalid OpenAPI JSON: ${error.message}`)
  }
}

function isIdentifier(value) {
  return /^[A-Za-z_$][A-Za-z0-9_$]*$/.test(value)
}

function propertyName(value) {
  return isIdentifier(value) ? value : JSON.stringify(value)
}

function typeName(value) {
  const normalized = String(value)
    .replace(/[^A-Za-z0-9_$]/g, '_')
    .replace(/^[^A-Za-z_$]+/, '')
  return normalized || 'UnnamedSchema'
}

function refName(ref) {
  const name = String(ref || '').split('/').pop()
  return typeName(name)
}

function literal(value) {
  return JSON.stringify(value)
}

function union(values) {
  return values.map(literal).join(' | ')
}

function sortedEntries(value) {
  return Object.entries(value || {}).sort(([left], [right]) => left.localeCompare(right))
}

function schemaType(schema, options = {}) {
  const refPrefix = options.refPrefix || ''

  if (!schema || Object.keys(schema).length === 0) {
    return 'unknown'
  }

  if (schema.$ref) {
    return `${refPrefix}${refName(schema.$ref)}`
  }

  if (Array.isArray(schema.oneOf) && schema.oneOf.length > 0) {
    return schema.oneOf.map((item) => schemaType(item, options)).join(' | ')
  }

  if (Array.isArray(schema.anyOf) && schema.anyOf.length > 0) {
    return schema.anyOf.map((item) => schemaType(item, options)).join(' | ')
  }

  if (Array.isArray(schema.allOf) && schema.allOf.length > 0) {
    return schema.allOf.map((item) => schemaType(item, options)).join(' & ')
  }

  if (Array.isArray(schema.enum) && schema.enum.length > 0) {
    return union(schema.enum)
  }

  let result
  switch (schema.type) {
    case 'string':
      result = schema.format === 'binary' ? 'File' : 'string'
      break
    case 'integer':
    case 'number':
      result = 'number'
      break
    case 'boolean':
      result = 'boolean'
      break
    case 'array':
      result = `${schemaType(schema.items, options)}[]`
      break
    case 'object':
      if (schema.additionalProperties) {
        result = `Record<string, ${schemaType(schema.additionalProperties, options)}>`
      } else if (schema.properties) {
        result = inlineObjectType(schema, options)
      } else {
        result = 'Record<string, unknown>'
      }
      break
    default:
      result = 'unknown'
  }

  return schema.nullable ? `${result} | null` : result
}

function inlineObjectType(schema, options = {}) {
  const required = new Set(schema.required || [])
  const properties = sortedEntries(schema.properties)
    .map(([name, child]) => `${propertyName(name)}${required.has(name) ? '' : '?'}: ${schemaType(child, options)}`)

  if (schema.additionalProperties) {
    properties.push(`[key: string]: ${schemaType(schema.additionalProperties, options)}`)
  }

  return properties.length ? `{ ${properties.join('; ')} }` : 'Record<string, unknown>'
}

function jsDoc(description) {
  if (!description) return []
  return [
    '/**',
    ` * ${String(description).replace(/\*\//g, '* /')}`,
    ' */'
  ]
}

function renderSchema(name, schema) {
  const lines = []
  const exportedName = typeName(name)
  lines.push(...jsDoc(schema.description))

  if (schema.type === 'object' && schema.properties) {
    const required = new Set(schema.required || [])
    lines.push(`export interface ${exportedName} {`)
    for (const [property, child] of sortedEntries(schema.properties)) {
      for (const comment of jsDoc(child.description)) {
        lines.push(`  ${comment}`)
      }
      lines.push(`  ${propertyName(property)}${required.has(property) ? '' : '?'}: ${schemaType(child)}`)
    }
    if (schema.additionalProperties) {
      lines.push(`  [key: string]: ${schemaType(schema.additionalProperties)}`)
    }
    lines.push('}')
    return lines.join('\n')
  }

  lines.push(`export type ${exportedName} = ${schemaType(schema)}`)
  return lines.join('\n')
}

function render(openapi) {
  const schemas = openapi.components?.schemas || {}
  const schemaNames = Object.keys(schemas).sort((left, right) => left.localeCompare(right))
  if (schemaNames.length === 0) {
    fail('OpenAPI JSON does not contain components.schemas')
  }

  const sections = [
    '/* eslint-disable */',
    '// Generated from OpenAPI JSON. Do not edit manually.',
    '// Run: node scripts/generate-openapi-frontend-types.js <openapi-json-file>',
    '',
    'export type JsonObject = Record<string, unknown>',
    ''
  ]

  for (const name of schemaNames) {
    sections.push(renderSchema(name, schemas[name]))
    sections.push('')
  }

  sections.push('export interface OpenApiSchemas {')
  for (const name of schemaNames) {
    const exportedName = typeName(name)
    sections.push(`  ${propertyName(name)}: ${exportedName}`)
  }
  sections.push('}')
  sections.push('')

  return sections.join('\n')
}

function operationResponseSchema(operation) {
  const responses = operation.responses || {}
  const successResponse = responses['200'] || responses['201'] || responses['204'] || responses.default
  const content = successResponse?.content || {}
  return content['application/json']?.schema || content['*/*']?.schema || {}
}

function operationRequestSchema(operation) {
  const content = operation.requestBody?.content || {}
  return content['application/json']?.schema ||
    content['multipart/form-data']?.schema ||
    content['application/x-www-form-urlencoded']?.schema ||
    content['*/*']?.schema ||
    null
}

function componentRefName(ref, prefix) {
  const value = String(ref || '')
  return value.startsWith(prefix) ? value.slice(prefix.length) : ''
}

function resolveParameter(openapi, parameter) {
  if (!parameter?.$ref) return parameter
  const name = componentRefName(parameter.$ref, '#/components/parameters/')
  return name ? openapi.components?.parameters?.[name] || parameter : parameter
}

function operationParameters(openapi, pathItem, operation, location) {
  const parameterMap = new Map()
  const parameters = [
    ...(Array.isArray(pathItem.parameters) ? pathItem.parameters : []),
    ...(Array.isArray(operation.parameters) ? operation.parameters : [])
  ]

  for (const item of parameters) {
    const parameter = resolveParameter(openapi, item)
    if (!parameter || parameter.in !== location || !parameter.name) continue
    parameterMap.set(`${parameter.in}:${parameter.name}`, parameter)
  }

  return Array.from(parameterMap.values()).sort((left, right) => {
    return left.name.localeCompare(right.name)
  })
}

function parameterSchema(parameter) {
  const content = parameter.content || {}
  return parameter.schema || content['application/json']?.schema || content['*/*']?.schema || null
}

function parameterObjectType(parameters, location) {
  if (parameters.length === 0) return 'undefined'

  const properties = parameters.map((parameter) => {
    const required = location === 'path' || parameter.required
    return `${propertyName(parameter.name)}${required ? '' : '?'}: ${schemaType(parameterSchema(parameter), { refPrefix: 'Schemas.' })}`
  })

  return `{ ${properties.join('; ')} }`
}

function operationId(pathKey, method, operation, usedIds) {
  const preferred = typeName(operation.operationId || `${method}_${pathKey}`)
  let candidate = preferred
  let index = 2
  while (usedIds.has(candidate)) {
    candidate = `${preferred}${index}`
    index += 1
  }
  usedIds.add(candidate)
  return candidate
}

function collectOperations(openapi) {
  const operations = []
  const usedIds = new Set()
  const methods = new Set(['get', 'post', 'put', 'delete', 'patch'])

  for (const [pathKey, pathItem] of sortedEntries(openapi.paths || {})) {
    for (const [method, operation] of sortedEntries(pathItem)) {
      if (!methods.has(method) || !operation || typeof operation !== 'object') {
        continue
      }

      operations.push({
        id: operationId(pathKey, method, operation, usedIds),
        method: method.toUpperCase(),
        path: pathKey,
        pathParamsType: parameterObjectType(operationParameters(openapi, pathItem, operation, 'path'), 'path'),
        queryType: parameterObjectType(operationParameters(openapi, pathItem, operation, 'query'), 'query'),
        requestType: operationRequestSchema(operation)
          ? schemaType(operationRequestSchema(operation), { refPrefix: 'Schemas.' })
          : 'undefined',
        responseType: schemaType(operationResponseSchema(operation), { refPrefix: 'Schemas.' })
      })
    }
  }

  if (operations.length === 0) {
    fail('OpenAPI JSON does not contain runtime operations')
  }

  return operations.sort((left, right) => left.id.localeCompare(right.id))
}

function dataType(responseType) {
  if (responseType.startsWith('Schemas.APIResponse')) {
    return `NonNullable<${responseType}['data']>`
  }
  return 'unknown'
}

function renderOperations(openapi) {
  const operations = collectOperations(openapi)
  const sections = [
    '/* eslint-disable */',
    '// Generated from OpenAPI JSON. Do not edit manually.',
    '// Run: node scripts/generate-openapi-frontend-types.js <openapi-json-file>',
    '',
    "import type * as Schemas from './schemas'",
    '',
    "export type OpenApiHttpMethod = 'DELETE' | 'GET' | 'PATCH' | 'POST' | 'PUT'",
    '',
    'export interface OpenApiOperationMeta {',
    '  method: OpenApiHttpMethod',
    '  path: string',
    '  operationId: string',
    '}',
    '',
    'export const OPENAPI_OPERATIONS = {'
  ]

  for (const operation of operations) {
    sections.push(`  ${propertyName(operation.id)}: {`)
    sections.push(`    method: ${literal(operation.method)},`)
    sections.push(`    path: ${literal(operation.path)},`)
    sections.push(`    operationId: ${literal(operation.id)}`)
    sections.push('  },')
  }
  sections.push('} as const satisfies Record<string, OpenApiOperationMeta>')
  sections.push('')
  sections.push('export type OpenApiOperationId = keyof typeof OPENAPI_OPERATIONS')
  sections.push('')
  sections.push('export interface OpenApiOperationTypes {')
  for (const operation of operations) {
    sections.push(`  ${propertyName(operation.id)}: {`)
    sections.push(`    pathParams: ${operation.pathParamsType}`)
    sections.push(`    query: ${operation.queryType}`)
    sections.push(`    request: ${operation.requestType}`)
    sections.push(`    response: ${operation.responseType}`)
    sections.push(`    data: ${dataType(operation.responseType)}`)
    sections.push('  }')
  }
  sections.push('}')
  sections.push('')
  sections.push('export type OpenApiOperationPathParams<T extends OpenApiOperationId> = OpenApiOperationTypes[T][\'pathParams\']')
  sections.push('export type OpenApiOperationQuery<T extends OpenApiOperationId> = OpenApiOperationTypes[T][\'query\']')
  sections.push('export type OpenApiOperationRequest<T extends OpenApiOperationId> = OpenApiOperationTypes[T][\'request\']')
  sections.push('export type OpenApiOperationResponse<T extends OpenApiOperationId> = OpenApiOperationTypes[T][\'response\']')
  sections.push('export type OpenApiOperationData<T extends OpenApiOperationId> = OpenApiOperationTypes[T][\'data\']')
  sections.push('')

  return sections.join('\n')
}

function ensureFile(file, expected) {
  if (!fs.existsSync(file)) {
    fail(`missing generated file: ${path.relative(root, file)}`)
  }

  const current = fs.readFileSync(file, 'utf8')
  if (current !== expected) {
    fail(`generated frontend OpenAPI contract is stale: ${path.relative(root, file)}`)
  }
}

const openapi = readJson(openapiPath)
const schemasOutput = render(openapi)
const operationsOutput = renderOperations(openapi)

if (check) {
  ensureFile(schemasOutputPath, schemasOutput)
  ensureFile(operationsOutputPath, operationsOutput)

  console.log('generate-openapi-frontend-types: ok')
  process.exit(0)
}

fs.mkdirSync(path.dirname(schemasOutputPath), { recursive: true })
fs.writeFileSync(schemasOutputPath, schemasOutput)
fs.writeFileSync(operationsOutputPath, operationsOutput)
console.log(`generate-openapi-frontend-types: wrote ${path.relative(root, schemasOutputPath)}`)
console.log(`generate-openapi-frontend-types: wrote ${path.relative(root, operationsOutputPath)}`)
