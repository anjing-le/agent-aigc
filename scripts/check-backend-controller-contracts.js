#!/usr/bin/env node
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')
const controllerDir = path.join(root, 'backend/src/main/java/com/anjing/controller')

function fail(message) {
  console.error(`check-backend-controller-contracts: ${message}`)
  process.exit(1)
}

function relative(file) {
  return path.relative(root, file).replace(/\\/g, '/')
}

function controllerFiles() {
  if (!fs.existsSync(controllerDir)) {
    fail('missing backend controller directory')
  }
  return fs.readdirSync(controllerDir)
    .filter((name) => name.endsWith('Controller.java'))
    .map((name) => path.join(controllerDir, name))
}

function findMappingMethodSignature(lines, mappingLineIndex) {
  for (let index = mappingLineIndex + 1; index < Math.min(lines.length, mappingLineIndex + 12); index += 1) {
    const line = lines[index].trim()
    if (line.startsWith('@')) continue
    if (line.includes(' public ') || line.startsWith('public ')) {
      return line
    }
  }
  return ''
}

for (const file of controllerFiles()) {
  const source = fs.readFileSync(file, 'utf8')
  const relativeFile = relative(file)
  if (source.includes('@ScaffoldSample')) {
    continue
  }

  if (/APIResponse\s*<\s*Map\s*</.test(source)) {
    fail(`${relativeFile} returns Map payload from a non-sample controller`)
  }

  if (/@RequestBody\s+(?:@Validated\s+)?Map\s*</.test(source)) {
    fail(`${relativeFile} accepts Map request body from a non-sample controller`)
  }

  if (/\bResponseEntity\s*</.test(source)) {
    fail(`${relativeFile} uses ResponseEntity directly instead of APIResponse envelope`)
  }

  const lines = source.split(/\r?\n/)
  lines.forEach((line, index) => {
    if (!/@(?:GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\b/.test(line)) {
      return
    }
    const signature = findMappingMethodSignature(lines, index)
    if (!signature || !/\bAPIResponse\s*</.test(signature)) {
      fail(`${relativeFile} mapping near line ${index + 1} must return APIResponse<T>`)
    }
  })
}

console.log('check-backend-controller-contracts: ok')
