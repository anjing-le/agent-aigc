#!/usr/bin/env node
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')

const files = {
  application: 'backend/src/main/java/com/anjing/Application.java',
  holder: 'backend/src/main/java/com/anjing/context/GlobalRequestContextHolder.java',
  asyncConfig: 'backend/src/main/java/com/anjing/config/async/AsyncConfig.java',
  taskDecorator: 'backend/src/main/java/com/anjing/config/async/RequestContextTaskDecorator.java',
  taskDecoratorTest: 'backend/src/test/java/com/anjing/config/async/RequestContextTaskDecoratorTest.java',
  properties: 'backend/src/main/java/com/anjing/config/properties/AsyncExecutorProperties.java',
  applicationYaml: 'backend/src/main/resources/application.yml',
  environmentGuide: 'project_document/ENVIRONMENT_PROFILE_GUIDE.md',
  sharedKernelGuide: 'project_document/SHARED_KERNEL_GUIDE.md',
  status: 'project_document/STATUS.md',
  roadmap: 'project_document/ROADMAP.md',
  architecture: 'project_document/ARCHITECTURE_EVOLUTION.md'
}

function fail(message) {
  console.error(`check-async-context-contract: ${message}`)
  process.exit(1)
}

function read(relativeFile) {
  const file = path.join(root, relativeFile)
  if (!fs.existsSync(file)) {
    fail(`missing required file: ${relativeFile}`)
  }
  return fs.readFileSync(file, 'utf8')
}

function requireToken(relativeFile, token) {
  const source = read(relativeFile)
  if (!source.includes(token)) {
    fail(`${relativeFile} is missing token: ${token}`)
  }
}

requireToken(files.application, '@EnableAsync')

for (const token of [
  'public static Optional<GlobalRequestContext> capture()',
  'public static GlobalRequestContext copyOf(GlobalRequestContext context)',
  'public static void setOrClear(GlobalRequestContext context)',
  'public static void runWith(GlobalRequestContext context, Runnable runnable)',
  'public static <T> T callWith(GlobalRequestContext context, Callable<T> callable)'
]) {
  requireToken(files.holder, token)
}

for (const token of [
  'implements TaskDecorator',
  'GlobalRequestContextHolder.capture()',
  'GlobalRequestContextHolder.setOrClear(contextSnapshot)',
  'MDC.getCopyOfContextMap()',
  'MDC.setContextMap(contextMap)',
  'MDC.clear()'
]) {
  requireToken(files.taskDecorator, token)
}

for (const token of [
  'decorateShouldPropagateRequestContextAndMdcThenRestorePreviousValues',
  'GlobalRequestContextHolder.requestIdOrEmpty()',
  'GlobalRequestContextHolder.traceIdOrEmpty()',
  'MDC.get("requestId")',
  'assertEquals("rid-worker"'
]) {
  requireToken(files.taskDecoratorTest, token)
}

for (const token of [
  'implements AsyncConfigurer',
  'TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME',
  'ThreadPoolTaskExecutor',
  'setTaskDecorator(requestContextTaskDecorator)',
  'getAsyncUncaughtExceptionHandler',
  'ASYNC_TASK_ERROR',
  'GlobalRequestContextHolder.traceIdOrEmpty()'
]) {
  requireToken(files.asyncConfig, token)
}

for (const token of [
  '@ConfigurationProperties(prefix = "app.async")',
  'private int corePoolSize',
  'private int maxPoolSize',
  'private int queueCapacity',
  'private String threadNamePrefix',
  'private boolean waitForTasksToCompleteOnShutdown'
]) {
  requireToken(files.properties, token)
}

for (const token of [
  'async:',
  'APP_ASYNC_CORE_POOL_SIZE',
  'APP_ASYNC_MAX_POOL_SIZE',
  'APP_ASYNC_QUEUE_CAPACITY',
  'APP_ASYNC_THREAD_NAME_PREFIX'
]) {
  requireToken(files.applicationYaml, token)
}

requireToken(files.sharedKernelGuide, 'capture/restore')
requireToken(files.environmentGuide, 'APP_ASYNC_CORE_POOL_SIZE')
requireToken(files.status, 'node scripts/check-async-context-contract.js')
requireToken(files.status, 'mvn -q -Dtest=RequestContextTaskDecoratorTest test')
requireToken(files.roadmap, 'scripts/check-async-context-contract.js')
requireToken(files.architecture, 'scripts/check-async-context-contract.js')

console.log('check-async-context-contract: ok')
