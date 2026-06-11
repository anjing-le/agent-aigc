#!/usr/bin/env node
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')

const sharedKernelFiles = [
  'backend/src/main/java/com/anjing/model/constants/ApiConstants.java',
  'backend/src/main/java/com/anjing/model/constants/PlatformContractConstants.java',
  'backend/src/main/java/com/anjing/model/constants/RequestHeaderConstants.java',
  'backend/src/main/java/com/anjing/model/errorcode/ErrorCode.java',
  'backend/src/main/java/com/anjing/model/errorcode/AuthErrorCode.java',
  'backend/src/main/java/com/anjing/model/errorcode/CommonErrorCode.java',
  'backend/src/main/java/com/anjing/model/errorcode/LockErrorCode.java',
  'backend/src/main/java/com/anjing/model/errorcode/RemoteErrorCode.java',
  'backend/src/main/java/com/anjing/model/errorcode/StateMachineErrorCode.java',
  'backend/src/main/java/com/anjing/model/exception/BizException.java',
  'backend/src/main/java/com/anjing/model/exception/SystemException.java',
  'backend/src/main/java/com/anjing/model/request/BaseRequest.java',
  'backend/src/main/java/com/anjing/model/request/GlobalRequestContext.java',
  'backend/src/main/java/com/anjing/model/request/PageRequest.java',
  'backend/src/main/java/com/anjing/model/response/APIResponse.java',
  'backend/src/main/java/com/anjing/model/response/BaseResponse.java',
  'backend/src/main/java/com/anjing/model/response/MultiResponse.java',
  'backend/src/main/java/com/anjing/model/response/PageResult.java',
  'backend/src/main/java/com/anjing/context/GlobalRequestContextHolder.java',
  'backend/src/main/java/com/anjing/util/DateUtils.java',
  'backend/src/main/java/com/anjing/util/IdUtils.java',
  'backend/src/main/java/com/anjing/util/LocaleUtils.java',
  'backend/src/main/java/com/anjing/util/StringUtils.java',
  'backend/src/main/java/com/anjing/util/TimeZoneUtils.java',
  'backend/src/main/java/com/anjing/util/ValidationUtils.java'
]

const disallowedReferences = [
  [/org\.springframework\./, 'Spring framework dependency'],
  [/jakarta\.servlet\./, 'Servlet API dependency'],
  [/javax\.servlet\./, 'Servlet API dependency'],
  [/jakarta\.persistence\./, 'Persistence API dependency'],
  [/javax\.persistence\./, 'Persistence API dependency'],
  [/com\.anjing\.(Advice|aspect|client|config|controller|example|statemachine)\./, 'runtime/application layer dependency']
]

const disallowedAnnotations = [
  '@Autowired',
  '@Bean',
  '@Component',
  '@Configuration',
  '@Controller',
  '@Repository',
  '@RestController',
  '@Service'
]

function fail(message) {
  console.error(`check-shared-kernel: ${message}`)
  process.exit(1)
}

function read(relativeFile) {
  const file = path.join(root, relativeFile)
  if (!fs.existsSync(file)) {
    fail(`missing shared kernel file: ${relativeFile}`)
  }
  return fs.readFileSync(file, 'utf8')
}

for (const relativeFile of sharedKernelFiles) {
  const source = read(relativeFile)

  for (const [pattern, description] of disallowedReferences) {
    if (pattern.test(source)) {
      fail(`${relativeFile} contains ${description}: ${pattern}`)
    }
  }

  for (const annotation of disallowedAnnotations) {
    if (source.includes(annotation)) {
      fail(`${relativeFile} contains runtime annotation ${annotation}`)
    }
  }
}

console.log('check-shared-kernel: ok')
