<template>
  <div class="aigc-models">
    <div class="aigc-models__header">
      <div>
        <h2 class="aigc-models__title">模型配置</h2>
        <p class="aigc-models__subtitle">Provider 状态来自后端配置和已注册 Bean</p>
      </div>
      <el-button
        :icon="Refresh"
        :loading="loading || auditLoading || executionReportLoading"
        @click="loadModelConsole"
      >
        刷新
      </el-button>
    </div>

    <div class="aigc-models__groups">
      <section v-for="group in modelGroups" :key="group.type" class="aigc-models__group">
        <div class="aigc-models__group-header">
          <div>
            <h3 class="aigc-models__group-title">{{ group.title }}</h3>
            <p class="aigc-models__group-desc">{{ group.description }}</p>
          </div>
          <div class="aigc-models__group-tags">
            <el-tag size="small" type="primary" effect="plain"
              >active: {{ group.activeProvider }}</el-tag
            >
            <el-tag size="small" effect="plain">{{ group.models.length }} 个</el-tag>
          </div>
        </div>

        <el-empty
          v-if="!loading && group.models.length === 0"
          description="暂无可用 Provider"
          :image-size="80"
        />

        <div v-else class="aigc-models__list">
          <div v-for="model in group.models" :key="model.id" class="aigc-models__item">
            <div class="aigc-models__item-main">
              <div class="aigc-models__item-title">
                <div class="aigc-models__item-name">{{ model.name }}</div>
                <el-tag v-if="model.active" size="small" type="primary" effect="plain"
                  >当前路由</el-tag
                >
              </div>
              <div class="aigc-models__item-desc">{{ model.description }}</div>
              <div class="aigc-models__item-config">
                <div>
                  <span>模型</span>
                  <strong>{{ model.configuredModel || '-' }}</strong>
                </div>
                <div>
                  <span>状态</span>
                  <strong>{{ model.statusReason || '-' }}</strong>
                </div>
                <div>
                  <span>来源</span>
                  <strong>{{ formatRouteConfigSource(model.routeConfigSource) }}</strong>
                </div>
                <div>
                  <span>凭证</span>
                  <strong>{{ formatCredentialSource(model.credentialSource) }}</strong>
                </div>
                <div>
                  <span>安全</span>
                  <strong>{{ formatCredentialStorageMode(model.credentialStorageMode) }}</strong>
                </div>
                <div v-if="model.credentialUpdatedAt">
                  <span>更新</span>
                  <strong>{{ formatProbeTime(model.credentialUpdatedAt) }}</strong>
                </div>
                <div>
                  <span>参数</span>
                  <strong>{{ formatConfigSource(model.paramConfigSource) }}</strong>
                </div>
                <div v-if="model.paramConfigUpdatedAt">
                  <span>调参</span>
                  <strong>{{ formatProbeTime(model.paramConfigUpdatedAt) }}</strong>
                </div>
                <div>
                  <span>成本</span>
                  <strong>{{ formatCostReadiness(model) }}</strong>
                </div>
              </div>
              <div v-if="model.missingConfig" class="aigc-models__missing">
                {{ model.missingConfig }}
              </div>
              <div v-if="formatDefaultParams(model).length" class="aigc-models__params">
                <el-tag
                  v-for="item in formatDefaultParams(model)"
                  :key="item"
                  size="small"
                  effect="plain"
                >
                  {{ item }}
                </el-tag>
              </div>
            </div>
            <div class="aigc-models__item-meta">
              <el-tag size="small" type="info" effect="plain">{{ model.provider }}</el-tag>
              <el-tag size="small" :type="model.available ? 'success' : 'danger'" effect="plain">
                {{ model.available ? '可用' : '不可用' }}
              </el-tag>
              <el-button
                size="small"
                :icon="Connection"
                :loading="probingKey === model.id"
                @click="handleProbe(model)"
              >
                探测
              </el-button>
              <el-button
                v-if="supportsCredentialUpdate(model)"
                size="small"
                :icon="Key"
                plain
                @click="openCredentialDialog(model)"
              >
                凭证
              </el-button>
              <el-button
                v-if="supportsParamUpdate(model)"
                size="small"
                :icon="Setting"
                plain
                @click="openParamDialog(model)"
              >
                参数
              </el-button>
              <el-button
                v-if="supportsSmokeTest(model)"
                size="small"
                :icon="Connection"
                plain
                :loading="smokeTestingKey === model.id"
                @click="handleSmokeTest(model)"
              >
                测试
              </el-button>
              <el-button
                v-if="!model.active"
                size="small"
                type="primary"
                plain
                :loading="switchingKey === model.id"
                @click="handleSwitchProvider(model)"
              >
                设为路由
              </el-button>
            </div>
            <div v-if="probeResults[model.id]" class="aigc-models__probe">
              <div class="aigc-models__probe-summary">
                <el-tag
                  size="small"
                  :type="probeResults[model.id].routable ? 'success' : 'warning'"
                  effect="plain"
                >
                  {{ probeResults[model.id].message }}
                </el-tag>
                <span>{{ formatProbeTime(probeResults[model.id].checkedAt) }}</span>
              </div>
              <div class="aigc-models__checks">
                <el-tag
                  v-for="check in diagnosticChecks(probeResults[model.id])"
                  :key="check.id"
                  size="small"
                  :type="diagnosticTagType(check.status)"
                  effect="plain"
                >
                  {{ check.label }}: {{ check.message }}
                </el-tag>
              </div>
            </div>
            <div v-if="smokeResults[model.id]" class="aigc-models__smoke">
              <div class="aigc-models__probe-summary">
                <el-tag
                  size="small"
                  :type="smokeResults[model.id].success ? 'success' : 'warning'"
                  effect="plain"
                >
                  {{ smokeResults[model.id].message || smokeResults[model.id].status }}
                </el-tag>
                <span>{{ formatProbeTime(smokeResults[model.id].checkedAt) }}</span>
              </div>
              <div class="aigc-models__smoke-grid">
                <div>
                  <span>任务</span>
                  <strong>{{ smokeResults[model.id].taskId || '-' }}</strong>
                </div>
                <div>
                  <span>资产</span>
                  <strong>{{ smokeResults[model.id].assetId || '-' }}</strong>
                </div>
                <div>
                  <span>耗时</span>
                  <strong>{{ formatDuration(smokeResults[model.id].durationMs) }}</strong>
                </div>
                <div>
                  <span>成本</span>
                  <strong>{{ formatProviderExecutionCost(smokeResults[model.id]) }}</strong>
                </div>
              </div>
              <div v-if="smokeResults[model.id].errorMessage" class="aigc-models__missing">
                {{ smokeResults[model.id].errorMessage }}
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>

    <section class="aigc-models__execution">
      <div class="aigc-models__execution-header">
        <div>
          <h3 class="aigc-models__execution-title">调用报表</h3>
          <p class="aigc-models__execution-desc">Provider、模型和内容类型的任务执行表现</p>
        </div>
        <div class="aigc-models__execution-actions">
          <el-radio-group v-model="executionReportContentType" size="small" @change="loadExecutionReport">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="IMAGE">图片</el-radio-button>
            <el-radio-button label="VIDEO">视频</el-radio-button>
            <el-radio-button label="AUDIO">音频</el-radio-button>
          </el-radio-group>
          <el-select
            v-model="executionReportDays"
            size="small"
            class="aigc-models__execution-days"
            @change="loadExecutionReport"
          >
            <el-option label="7 天" :value="7" />
            <el-option label="30 天" :value="30" />
            <el-option label="90 天" :value="90" />
          </el-select>
          <el-button
            size="small"
            :icon="DataAnalysis"
            :loading="executionReportLoading"
            @click="loadExecutionReport"
          >
            刷新报表
          </el-button>
        </div>
      </div>
      <div class="aigc-models__execution-stats">
        <div v-for="item in executionReportStats" :key="item.label">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </div>
      <div class="aigc-models__execution-meta">
        <el-tag size="small" effect="plain">{{ formatReportWindow }}</el-tag>
        <span>{{ formatProbeTime(executionReport?.generatedAt) || '等待生成' }}</span>
        <span>{{ executionReport?.costStatusSummary || '-' }}</span>
      </div>
      <div class="aigc-models__execution-tables">
        <section>
          <div class="aigc-models__execution-table-title">Provider 表现</div>
          <el-table
            v-loading="executionReportLoading"
            :data="providerExecutionMetrics"
            size="small"
            empty-text="暂无 Provider 调用记录"
          >
            <el-table-column label="Provider" min-width="150" show-overflow-tooltip>
              <template #default="{ row }">{{ formatExecutionMetricLabel(row) }}</template>
            </el-table-column>
            <el-table-column label="任务" width="78">
              <template #default="{ row }">{{ formatExecutionCount(row.totalTasks) }}</template>
            </el-table-column>
            <el-table-column label="成功率" width="86">
              <template #default="{ row }">{{ formatExecutionPercent(row.successRate) }}</template>
            </el-table-column>
            <el-table-column label="耗时" width="88">
              <template #default="{ row }">{{ formatDuration(row.averageDurationMs) }}</template>
            </el-table-column>
            <el-table-column label="成本" min-width="118" show-overflow-tooltip>
              <template #default="{ row }">{{ formatExecutionCost(row) }}</template>
            </el-table-column>
          </el-table>
        </section>
        <section>
          <div class="aigc-models__execution-table-title">模型表现</div>
          <el-table
            v-loading="executionReportLoading"
            :data="modelExecutionMetrics"
            size="small"
            empty-text="暂无模型调用记录"
          >
            <el-table-column label="模型" min-width="150" show-overflow-tooltip>
              <template #default="{ row }">{{ formatExecutionMetricLabel(row) }}</template>
            </el-table-column>
            <el-table-column label="类型" width="72">
              <template #default="{ row }">{{ formatContentType(row.contentType) }}</template>
            </el-table-column>
            <el-table-column label="任务" width="78">
              <template #default="{ row }">{{ formatExecutionCount(row.totalTasks) }}</template>
            </el-table-column>
            <el-table-column label="成功率" width="86">
              <template #default="{ row }">{{ formatExecutionPercent(row.successRate) }}</template>
            </el-table-column>
            <el-table-column label="耗时" width="88">
              <template #default="{ row }">{{ formatDuration(row.averageDurationMs) }}</template>
            </el-table-column>
          </el-table>
        </section>
      </div>
    </section>

    <section class="aigc-models__governance">
      <div class="aigc-models__governance-header">
        <div>
          <h3 class="aigc-models__governance-title">数据治理</h3>
          <p class="aigc-models__governance-desc">历史空归属数据回填到当前请求上下文</p>
        </div>
        <div class="aigc-models__governance-actions">
          <el-button
            :icon="DataAnalysis"
            :loading="ownershipLoading"
            @click="handleOwnershipDryRun"
          >
            检查归属
          </el-button>
          <el-button
            type="primary"
            plain
            :icon="Check"
            :loading="ownershipApplying"
            :disabled="!ownershipBackfillResult || totalOwnershipCandidates === 0"
            @click="handleOwnershipApply"
          >
            执行回填
          </el-button>
        </div>
      </div>
      <div class="aigc-models__governance-grid">
        <div>
          <span>资产候选</span>
          <strong>{{ formatGovernanceCount(ownershipBackfillResult?.assetCandidates) }}</strong>
        </div>
        <div>
          <span>素材候选</span>
          <strong>{{ formatGovernanceCount(ownershipBackfillResult?.materialCandidates) }}</strong>
        </div>
        <div>
          <span>任务候选</span>
          <strong>{{ formatGovernanceCount(ownershipBackfillResult?.taskCandidates) }}</strong>
        </div>
        <div>
          <span>写入结果</span>
          <strong>{{ formatOwnershipUpdated(ownershipBackfillResult) }}</strong>
        </div>
      </div>
      <div v-if="ownershipBackfillResult" class="aigc-models__governance-result">
        <el-tag
          size="small"
          :type="ownershipBackfillResult.dryRun ? 'info' : 'success'"
          effect="plain"
        >
          {{ ownershipBackfillResult.dryRun ? '预演' : '已执行' }}
        </el-tag>
        <span>{{ formatOwnershipMessage(ownershipBackfillResult) }}</span>
        <small>{{ formatProbeTime(ownershipBackfillResult.checkedAt) }}</small>
      </div>
    </section>

    <section class="aigc-models__audit">
      <div class="aigc-models__audit-header">
        <div>
          <h3 class="aigc-models__audit-title">最近变更</h3>
          <p class="aigc-models__audit-desc">Provider 路由、凭证和参数模板的运行时变更记录</p>
        </div>
        <el-tag size="small" effect="plain">{{ auditLogs.length }} 条</el-tag>
      </div>
      <el-table
        v-loading="auditLoading"
        :data="auditLogs"
        size="small"
        class="aigc-models__audit-table"
        empty-text="暂无变更记录"
      >
        <el-table-column label="动作" min-width="108">
          <template #default="{ row }">
            <el-tag size="small" :type="auditActionTag(row.action)" effect="plain">
              {{ formatAuditAction(row.action) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" min-width="84">
          <template #default="{ row }">{{ formatContentType(row.contentType) }}</template>
        </el-table-column>
        <el-table-column
          prop="providerName"
          label="Provider"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column label="变更摘要" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">{{ formatAuditSummary(row) }}</template>
        </el-table-column>
        <el-table-column label="操作人" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ row.operatorName || row.operatorId || '-' }}</template>
        </el-table-column>
        <el-table-column label="时间" min-width="150">
          <template #default="{ row }">{{ formatProbeTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="credentialDialogVisible" title="Provider 凭证" width="440px">
      <el-form label-position="top">
        <el-form-item label="Provider">
          <el-input :model-value="credentialForm.providerName" disabled />
        </el-form-item>
        <el-form-item label="新凭证">
          <el-input
            v-model="credentialForm.credential"
            type="password"
            show-password
            clearable
            autocomplete="new-password"
            placeholder="粘贴新的运行时凭证"
            @keyup.enter="handleSaveCredential"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeCredentialDialog">取消</el-button>
        <el-button
          type="primary"
          :loading="savingCredential"
          :disabled="!credentialForm.credential.trim()"
          @click="handleSaveCredential"
        >
          保存
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="paramDialogVisible" title="Provider 参数模板" width="520px">
      <el-form label-position="top">
        <el-form-item label="Provider">
          <el-input :model-value="paramForm.providerName" disabled />
        </el-form-item>
        <el-form-item
          v-for="entry in paramForm.entries"
          :key="entry.key"
          :label="formatParamLabel(entry.key)"
        >
          <el-input-number
            v-if="entry.valueType === 'number'"
            :model-value="numberParamValue(entry)"
            :min="0"
            :precision="entry.key === 'temperature' ? 1 : 0"
            controls-position="right"
            class="aigc-models__param-input"
            @update:model-value="setParamEntryValue(entry, $event || 0)"
          />
          <el-switch
            v-else-if="entry.valueType === 'boolean'"
            :model-value="Boolean(entry.value)"
            @update:model-value="setParamEntryValue(entry, $event)"
          />
          <el-select
            v-else-if="paramOptions(entry).length"
            :model-value="stringParamValue(entry)"
            filterable
            @update:model-value="setParamEntryValue(entry, $event)"
          >
            <el-option
              v-for="option in paramOptions(entry)"
              :key="option"
              :label="option"
              :value="option"
            />
          </el-select>
          <el-input
            v-else
            :model-value="stringParamValue(entry)"
            @update:model-value="setParamEntryValue(entry, $event)"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeParamDialog">取消</el-button>
        <el-button type="primary" :loading="savingParams" @click="handleSaveParams">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
  import { Check, Connection, DataAnalysis, Key, Refresh, Setting } from '@element-plus/icons-vue'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import {
    fetchBackfillOwnership,
    fetchGetProviderAuditLogs,
    fetchGetProviderExecutionReport,
    fetchGetModelList,
    fetchProbeProvider,
    fetchSmokeTestProvider,
    fetchUpdateActiveProvider,
    fetchUpdateProviderCredential,
    fetchUpdateProviderParams
  } from '@/api/aigc'
  import type {
    ContentType,
    GenerationParams,
    ModelInfo,
    ModelListResponse,
    OwnershipBackfillResponse,
    ProviderAuditLogItem,
    ProviderExecutionMetric,
    ProviderExecutionReportResponse,
    ProviderProbeResponse,
    ProviderSmokeTestResponse
  } from '@/api/model/aigcModel'
  import { formatDateTime } from '@/utils/time'

  defineOptions({ name: 'AIGCModels' })

  type ProviderDiagnosticCheck = NonNullable<ProviderProbeResponse['checks']>[number]

  type ParamEntry = {
    key: string
    value: string | number | boolean
    valueType: 'string' | 'number' | 'boolean'
  }

  const loading = ref(false)
  const auditLoading = ref(false)
  const executionReportLoading = ref(false)
  const models = ref<ModelListResponse>({
    imageModels: [],
    videoModels: [],
    audioModels: []
  })
  const auditLogs = ref<ProviderAuditLogItem[]>([])
  const executionReport = ref<ProviderExecutionReportResponse | null>(null)
  const executionReportDays = ref(30)
  const executionReportContentType = ref<ContentType | ''>('')
  const probingKey = ref('')
  const switchingKey = ref('')
  const savingCredential = ref(false)
  const savingParams = ref(false)
  const credentialDialogVisible = ref(false)
  const paramDialogVisible = ref(false)
  const credentialForm = ref({
    contentType: 'IMAGE' as ContentType,
    provider: '',
    providerName: '',
    credential: ''
  })
  const paramForm = ref<{
    contentType: ContentType
    provider: string
    providerName: string
    entries: ParamEntry[]
  }>({
    contentType: 'IMAGE',
    provider: '',
    providerName: '',
    entries: []
  })
  const probeResults = ref<Record<string, ProviderProbeResponse>>({})
  const smokeTestingKey = ref('')
  const smokeResults = ref<Record<string, ProviderSmokeTestResponse>>({})
  const ownershipLoading = ref(false)
  const ownershipApplying = ref(false)
  const ownershipBackfillResult = ref<OwnershipBackfillResponse | null>(null)

  const modelGroups = computed<
    Array<{
      type: ContentType
      title: string
      description: string
      activeProvider: string
      models: ModelInfo[]
    }>
  >(() => [
    {
      type: 'IMAGE',
      title: '图片生成',
      description: '文生图、图生图、风格迁移等创作能力',
      activeProvider: models.value.imageModels[0]?.activeProvider || '-',
      models: models.value.imageModels
    },
    {
      type: 'VIDEO',
      title: '视频生成',
      description: '文生视频、图生视频和动态化能力',
      activeProvider: models.value.videoModels[0]?.activeProvider || '-',
      models: models.value.videoModels
    },
    {
      type: 'AUDIO',
      title: '音频生成',
      description: '配音、朗读、音乐和声音创作能力',
      activeProvider: models.value.audioModels[0]?.activeProvider || '-',
      models: models.value.audioModels
    }
  ])

  const providerExecutionMetrics = computed<ProviderExecutionMetric[]>(
    () => executionReport.value?.providerMetrics || []
  )

  const modelExecutionMetrics = computed<ProviderExecutionMetric[]>(
    () => executionReport.value?.modelMetrics || []
  )

  const executionReportStats = computed(() => [
    { label: '总任务', value: formatExecutionCount(executionReport.value?.totalTasks) },
    { label: '完成', value: formatExecutionCount(executionReport.value?.completedTasks) },
    { label: '失败', value: formatExecutionCount(executionReport.value?.failedTasks) },
    { label: '运行中', value: formatExecutionCount(executionReport.value?.pendingTasks) },
    { label: '成功率', value: formatExecutionPercent(executionReport.value?.successRate) },
    { label: '平均耗时', value: formatDuration(executionReport.value?.averageDurationMs) },
    { label: '估算成本', value: formatExecutionCost(executionReport.value) }
  ])

  const formatReportWindow = computed(() => {
    const type = executionReportContentType.value
      ? formatContentType(executionReportContentType.value)
      : '全部类型'
    return `${type} · ${executionReportDays.value} 天`
  })

  const formatDefaultParams = (model: ModelInfo) => {
    const params = model.defaultParams || {}
    return Object.entries(params).map(([key, value]) => `${key}: ${value}`)
  }

  const formatRouteConfigSource = (source?: string) => {
    return formatConfigSource(source)
  }

  const formatCredentialSource = (source?: string) => {
    return formatConfigSource(source)
  }

  const formatCredentialStorageMode = (mode?: string) => {
    if (mode === 'encrypted-database') return '加密托管'
    if (mode === 'legacy-database') return '旧明文'
    if (mode === 'configuration') return '环境配置'
    if (mode === 'not-required') return '无需配置'
    if (mode === 'missing') return '未配置'
    return '-'
  }

  const formatConfigSource = (source?: string) => {
    if (source === 'database') return '页面保存'
    if (source === 'configuration') return '环境配置'
    if (source === 'not-required') return '无需配置'
    if (source === 'missing') return '未配置'
    return '-'
  }

  const formatContentType = (contentType?: string) => {
    if (contentType === 'IMAGE') return '图片'
    if (contentType === 'VIDEO') return '视频'
    if (contentType === 'AUDIO') return '音频'
    return '-'
  }

  const formatAuditAction = (action?: string) => {
    if (action === 'active-provider') return '路由'
    if (action === 'credential') return '凭证'
    if (action === 'params') return '参数'
    if (action === 'smoke-test') return '测试'
    if (action === 'ownership-backfill') return '归属'
    return action || '-'
  }

  const auditActionTag = (action?: string) => {
    if (action === 'active-provider') return 'primary'
    if (action === 'credential') return 'warning'
    if (action === 'params') return 'success'
    if (action === 'smoke-test') return 'info'
    if (action === 'ownership-backfill') return 'primary'
    return 'info'
  }

  const supportsCredentialUpdate = (model: ModelInfo) => model.provider === 'GOOGLE'
  const supportsParamUpdate = (model: ModelInfo) => model.provider === 'GOOGLE'
  const supportsSmokeTest = (model: ModelInfo) => model.contentType === 'IMAGE'

  const formatCostReadiness = (model: ModelInfo) => {
    if (model.costEstimateConfigured) return formatCostStatus(model.costStatus)
    return `${formatCostStatus(model.costStatus)}，待配置`
  }

  const formatCostStatus = (status?: string) => {
    const map: Record<string, string> = {
      MOCK_FREE: '模拟免费',
      ESTIMATED: '已估算',
      ESTIMATE_NOT_CONFIGURED: '待配置',
      UNTRACKED: '未接入',
      PENDING: '统计中'
    }
    return status ? map[status] || status : '-'
  }

  const diagnosticChecks = (probe?: ProviderProbeResponse) => {
    return (probe?.checks || []).filter(Boolean) as ProviderDiagnosticCheck[]
  }

  const diagnosticTagType = (status?: string) => {
    if (status === 'PASS') return 'success'
    if (status === 'FAIL') return 'danger'
    return 'warning'
  }

  const formatDuration = (durationMs?: number) => {
    if (durationMs === undefined || durationMs === null) return '-'
    if (durationMs < 1000) return `${durationMs}ms`
    return `${(durationMs / 1000).toFixed(1)}s`
  }

  const formatProviderExecutionCost = (result: ProviderSmokeTestResponse) => {
    const execution = result.providerExecution
    if (!execution) return '-'
    if (execution.estimatedCostAmount !== undefined && execution.estimatedCostAmount !== null) {
      const currency = execution.estimatedCostCurrency || 'USD'
      const unit = execution.costUnit ? `/${execution.costUnit}` : ''
      return `${currency} ${Number(execution.estimatedCostAmount).toFixed(6)}${unit}`
    }
    return formatCostStatus(execution.costStatus)
  }

  const totalOwnershipCandidates = computed(() => {
    const result = ownershipBackfillResult.value
    return (
      Number(result?.assetCandidates || 0) +
      Number(result?.materialCandidates || 0) +
      Number(result?.taskCandidates || 0)
    )
  })

  const formatGovernanceCount = (value?: number) => {
    if (value === undefined || value === null) return '-'
    return Number(value).toLocaleString()
  }

  const formatExecutionCount = (value?: number | null) => {
    if (value === undefined || value === null) return '-'
    return Number(value).toLocaleString()
  }

  const formatExecutionPercent = (value?: number | null) => {
    if (value === undefined || value === null) return '-'
    return `${Number(value).toFixed(1)}%`
  }

  const formatExecutionCost = (
    item?: Pick<ProviderExecutionMetric, 'estimatedCostAmount' | 'estimatedCostCurrency'> | null
  ) => {
    if (item?.estimatedCostAmount === undefined || item.estimatedCostAmount === null) return '-'
    const currency = item.estimatedCostCurrency || 'USD'
    return `${currency} ${Number(item.estimatedCostAmount).toFixed(6)}`
  }

  const formatExecutionMetricLabel = (row: ProviderExecutionMetric) => {
    return row.label || row.providerName || row.model || row.providerType || '-'
  }

  const formatOwnershipUpdated = (result?: OwnershipBackfillResponse | null) => {
    if (!result) return '-'
    const totalUpdated =
      Number(result.assetUpdated || 0) +
      Number(result.materialUpdated || 0) +
      Number(result.taskUpdated || 0)
    if (result.dryRun) return '未写入'
    return `${totalUpdated.toLocaleString()} 条`
  }

  const formatOwnershipMessage = (result: OwnershipBackfillResponse) => {
    if (result.dryRun) return '仅预演，未写入数据'
    return '归属回填已执行'
  }

  const openCredentialDialog = (model: ModelInfo) => {
    credentialForm.value = {
      contentType: model.contentType,
      provider: model.provider,
      providerName: model.name,
      credential: ''
    }
    credentialDialogVisible.value = true
  }

  const closeCredentialDialog = () => {
    credentialDialogVisible.value = false
    credentialForm.value.credential = ''
  }

  const openParamDialog = (model: ModelInfo) => {
    paramForm.value = {
      contentType: model.contentType,
      provider: model.provider,
      providerName: model.name,
      entries: Object.entries(model.defaultParams || {}).map(([key, value]) => ({
        key,
        value,
        valueType: typeof value as ParamEntry['valueType']
      }))
    }
    paramDialogVisible.value = true
  }

  const closeParamDialog = () => {
    paramDialogVisible.value = false
    paramForm.value.entries = []
  }

  const formatParamLabel = (key: string) => {
    const labels: Record<string, string> = {
      aspectRatio: '宽高比',
      imageSize: '图片尺寸',
      timeoutMs: '超时毫秒',
      resolution: '分辨率',
      duration: '视频时长',
      voice: '音色',
      bpm: 'BPM',
      temperature: '温度'
    }
    return labels[key] || key
  }

  const paramOptions = (entry: { key: string }) => {
    const optionMap: Record<string, string[]> = {
      aspectRatio:
        paramForm.value.contentType === 'VIDEO'
          ? ['16:9', '9:16']
          : ['1:1', '2:3', '3:2', '3:4', '4:3', '4:5', '5:4', '9:16', '16:9', '21:9'],
      imageSize: ['1K', '2K', '4K'],
      resolution: ['720p', '1080p'],
      voice: ['Kore', 'Aoede', 'Fenrir', 'Puck', 'Charon']
    }
    return optionMap[entry.key] || []
  }

  const numberParamValue = (entry: ParamEntry) => {
    return typeof entry.value === 'number' ? entry.value : Number(entry.value || 0)
  }

  const stringParamValue = (entry: ParamEntry) => {
    return String(entry.value ?? '')
  }

  const setParamEntryValue = (entry: ParamEntry, value: string | number | boolean) => {
    entry.value = value
  }

  const buildParamPayload = () => {
    return paramForm.value.entries.reduce<GenerationParams>((params, entry) => {
      params[entry.key] = entry.value
      return params
    }, {})
  }

  const formatAuditValue = (value: unknown) => {
    if (value == null || value === '') return '-'
    if (typeof value === 'object') {
      return Object.entries(value as Record<string, unknown>)
        .map(([key, item]) => `${key}: ${item}`)
        .join(', ')
    }
    return String(value)
  }

  const formatAuditSummary = (row: ProviderAuditLogItem) => {
    const before = row.beforeSummary || {}
    const after = row.afterSummary || {}
    const changedKeys = Array.from(new Set([...Object.keys(before), ...Object.keys(after)]))
    return changedKeys
      .slice(0, 4)
      .map((key) => `${key}: ${formatAuditValue(before[key])} -> ${formatAuditValue(after[key])}`)
      .join('；')
  }

  const handleProbe = async (model: ModelInfo) => {
    try {
      probingKey.value = model.id
      const result = await fetchProbeProvider({
        contentType: model.contentType,
        provider: model.provider,
        providerName: model.name
      })
      probeResults.value = {
        ...probeResults.value,
        [model.id]: result
      }
      if (result.routable) {
        ElMessage.success(result.message || '探测通过')
      } else {
        ElMessage.warning(result.message || '探测完成')
      }
    } catch (error) {
      console.error('Provider 探测失败:', error)
      ElMessage.error('Provider 探测失败')
    } finally {
      probingKey.value = ''
    }
  }

  const handleSmokeTest = async (model: ModelInfo) => {
    const confirmExternalCall = model.provider === 'GOOGLE'
    if (confirmExternalCall) {
      try {
        await ElMessageBox.confirm(
          '这会立即调用外部 Google 图片 Provider，并可能产生模型费用。',
          '确认运行 smoke test',
          {
            confirmButtonText: '确认测试',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
      } catch {
        return
      }
    }

    try {
      smokeTestingKey.value = model.id
      const result = await fetchSmokeTestProvider({
        contentType: model.contentType,
        provider: model.provider,
        providerName: model.name,
        confirmExternalCall,
        prompt: 'A tiny clean smoke test image for agent-aigc provider validation'
      })
      smokeResults.value = {
        ...smokeResults.value,
        [model.id]: result
      }
      if (result.success) {
        ElMessage.success(result.message || 'Smoke test 通过')
      } else {
        ElMessage.warning(result.message || result.errorMessage || 'Smoke test 未通过')
      }
      await loadModelConsole()
    } catch (error) {
      console.error('Provider smoke test 失败:', error)
      ElMessage.error('Provider smoke test 失败')
    } finally {
      smokeTestingKey.value = ''
    }
  }

  const handleSaveCredential = async () => {
    const credential = credentialForm.value.credential.trim()
    if (!credential) {
      ElMessage.warning('请输入 Provider 凭证')
      return
    }

    try {
      savingCredential.value = true
      const result = await fetchUpdateProviderCredential({
        contentType: credentialForm.value.contentType,
        provider: credentialForm.value.provider,
        providerName: credentialForm.value.providerName,
        credential
      })
      if (result.configurationComplete) {
        ElMessage.success(result.message || 'Provider 凭证已保存')
      } else {
        ElMessage.warning(result.message || 'Provider 凭证已保存，请继续检查配置')
      }
      closeCredentialDialog()
      await loadModelConsole()
    } catch (error) {
      console.error('Provider 凭证保存失败:', error)
      ElMessage.error('Provider 凭证保存失败')
    } finally {
      savingCredential.value = false
    }
  }

  const handleSwitchProvider = async (model: ModelInfo) => {
    try {
      switchingKey.value = model.id
      const result = await fetchUpdateActiveProvider({
        contentType: model.contentType,
        provider: model.provider,
        providerName: model.name
      })
      if (result.routable) {
        ElMessage.success(result.message || 'Provider 路由已切换')
      } else {
        ElMessage.warning(result.message || 'Provider 已切换，请检查配置')
      }
      await loadModelConsole()
    } catch (error) {
      console.error('Provider 路由切换失败:', error)
      ElMessage.error('Provider 路由切换失败')
    } finally {
      switchingKey.value = ''
    }
  }

  const handleSaveParams = async () => {
    try {
      savingParams.value = true
      const result = await fetchUpdateProviderParams({
        contentType: paramForm.value.contentType,
        provider: paramForm.value.provider,
        providerName: paramForm.value.providerName,
        defaultParams: buildParamPayload()
      })
      ElMessage.success(result.message || 'Provider 参数模板已保存')
      closeParamDialog()
      await loadModelConsole()
    } catch (error) {
      console.error('Provider 参数模板保存失败:', error)
      ElMessage.error('Provider 参数模板保存失败')
    } finally {
      savingParams.value = false
    }
  }

  const handleOwnershipDryRun = async () => {
    try {
      ownershipLoading.value = true
      ownershipBackfillResult.value = await fetchBackfillOwnership({
        dryRun: true,
        confirmBackfill: false
      })
      if (totalOwnershipCandidates.value > 0) {
        ElMessage.warning(`发现 ${totalOwnershipCandidates.value.toLocaleString()} 条空归属记录`)
      } else {
        ElMessage.success('未发现空归属记录')
      }
    } catch (error) {
      console.error('归属检查失败:', error)
      ElMessage.error('归属检查失败')
    } finally {
      ownershipLoading.value = false
    }
  }

  const handleOwnershipApply = async () => {
    if (!ownershipBackfillResult.value || totalOwnershipCandidates.value === 0) return
    try {
      await ElMessageBox.confirm(
        `将把 ${totalOwnershipCandidates.value.toLocaleString()} 条历史空归属记录回填到当前用户上下文。`,
        '确认执行归属回填',
        {
          confirmButtonText: '确认回填',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
    } catch {
      return
    }

    try {
      ownershipApplying.value = true
      ownershipBackfillResult.value = await fetchBackfillOwnership({
        dryRun: false,
        confirmBackfill: true
      })
      ElMessage.success('归属回填已执行')
      await loadAuditLogs()
    } catch (error) {
      console.error('归属回填失败:', error)
      ElMessage.error('归属回填失败')
    } finally {
      ownershipApplying.value = false
    }
  }

  const formatProbeTime = (time?: string) => (time ? formatDateTime(time) : '')

  const loadModels = async () => {
    try {
      loading.value = true
      models.value = await fetchGetModelList()
    } catch (error) {
      console.error('加载模型配置失败:', error)
    } finally {
      loading.value = false
    }
  }

  const loadAuditLogs = async () => {
    try {
      auditLoading.value = true
      const result = await fetchGetProviderAuditLogs({ current: 1, size: 8 })
      auditLogs.value = result.records || []
    } catch (error) {
      console.error('加载 Provider 审计日志失败:', error)
    } finally {
      auditLoading.value = false
    }
  }

  const loadExecutionReport = async () => {
    try {
      executionReportLoading.value = true
      executionReport.value = await fetchGetProviderExecutionReport({
        days: executionReportDays.value,
        contentType: executionReportContentType.value || undefined
      })
    } catch (error) {
      console.error('加载 Provider 调用报表失败:', error)
      executionReport.value = null
    } finally {
      executionReportLoading.value = false
    }
  }

  const loadModelConsole = async () => {
    await Promise.all([loadModels(), loadAuditLogs(), loadExecutionReport()])
  }

  onMounted(() => {
    loadModelConsole()
  })
</script>

<style lang="scss" scoped>
  .aigc-models {
    min-height: 100%;
    padding: 20px;
    background: var(--el-bg-color-page);

    &__header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      margin-bottom: 20px;
    }

    &__title {
      margin: 0;
      font-size: 22px;
      font-weight: 600;
      color: var(--el-text-color-primary);
    }

    &__subtitle {
      margin: 6px 0 0;
      font-size: 13px;
      color: var(--el-text-color-secondary);
    }

    &__groups {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 16px;
    }

    &__audit {
      margin-top: 16px;
      padding: 16px;
      border: 1px solid var(--el-border-color-light);
      border-radius: 8px;
      background: var(--el-bg-color);
    }

    &__governance {
      margin-top: 16px;
      padding: 16px;
      border: 1px solid var(--el-border-color-light);
      border-radius: 8px;
      background: var(--el-bg-color);
    }

    &__execution {
      margin-top: 16px;
      padding: 16px;
      border: 1px solid var(--el-border-color-light);
      border-radius: 8px;
      background: var(--el-bg-color);
    }

    &__execution-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 12px;
      margin-bottom: 12px;
    }

    &__execution-title {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
      color: var(--el-text-color-primary);
    }

    &__execution-desc {
      margin: 6px 0 0;
      font-size: 12px;
      color: var(--el-text-color-secondary);
      line-height: 1.5;
    }

    &__execution-actions {
      display: flex;
      flex-wrap: wrap;
      justify-content: flex-end;
      gap: 8px;
    }

    &__execution-days {
      width: 96px;
    }

    &__execution-stats {
      display: grid;
      grid-template-columns: repeat(7, minmax(0, 1fr));
      gap: 10px;

      div {
        min-width: 0;
        padding: 12px;
        border: 1px solid var(--el-border-color-lighter);
        border-radius: 8px;
        background: var(--el-fill-color-blank);
      }

      span {
        display: block;
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }

      strong {
        display: block;
        min-width: 0;
        margin-top: 6px;
        overflow: hidden;
        color: var(--el-text-color-primary);
        font-size: 18px;
        font-weight: 600;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    &__execution-meta {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 8px;
      margin-top: 12px;
      color: var(--el-text-color-secondary);
      font-size: 12px;
    }

    &__execution-tables {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 12px;
      margin-top: 12px;

      section {
        min-width: 0;
      }
    }

    &__execution-table-title {
      margin-bottom: 8px;
      color: var(--el-text-color-primary);
      font-size: 13px;
      font-weight: 600;
    }

    &__governance-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 12px;
      margin-bottom: 12px;
    }

    &__governance-title {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
      color: var(--el-text-color-primary);
    }

    &__governance-desc {
      margin: 6px 0 0;
      font-size: 12px;
      color: var(--el-text-color-secondary);
      line-height: 1.5;
    }

    &__governance-actions {
      display: flex;
      flex-wrap: wrap;
      justify-content: flex-end;
      gap: 8px;
    }

    &__governance-grid {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 10px;

      div {
        min-width: 0;
        padding: 12px;
        border: 1px solid var(--el-border-color-lighter);
        border-radius: 8px;
        background: var(--el-fill-color-blank);
      }

      span {
        display: block;
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }

      strong {
        display: block;
        min-width: 0;
        margin-top: 6px;
        overflow: hidden;
        color: var(--el-text-color-primary);
        font-size: 18px;
        font-weight: 600;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    &__governance-result {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 8px;
      margin-top: 12px;
      font-size: 12px;
      color: var(--el-text-color-secondary);
    }

    &__audit-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 12px;
      margin-bottom: 12px;
    }

    &__audit-title {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
      color: var(--el-text-color-primary);
    }

    &__audit-desc {
      margin: 6px 0 0;
      font-size: 12px;
      color: var(--el-text-color-secondary);
      line-height: 1.5;
    }

    &__audit-table {
      width: 100%;
    }

    &__group {
      min-width: 0;
      padding: 16px;
      border: 1px solid var(--el-border-color-light);
      border-radius: 8px;
      background: var(--el-bg-color);
    }

    &__group-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 12px;
      margin-bottom: 14px;
    }

    &__group-tags {
      display: flex;
      flex-wrap: wrap;
      justify-content: flex-end;
      gap: 6px;
    }

    &__group-title {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
      color: var(--el-text-color-primary);
    }

    &__group-desc {
      margin: 6px 0 0;
      font-size: 12px;
      color: var(--el-text-color-secondary);
      line-height: 1.5;
    }

    &__list {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }

    &__item {
      display: flex;
      flex-wrap: wrap;
      align-items: flex-start;
      justify-content: space-between;
      gap: 12px;
      padding: 12px;
      border: 1px solid var(--el-border-color-lighter);
      border-radius: 8px;
      background: var(--el-fill-color-blank);
    }

    &__item-main {
      min-width: 0;
      flex: 1;
    }

    &__item-title {
      display: flex;
      align-items: center;
      gap: 8px;
      min-width: 0;
    }

    &__item-name {
      min-width: 0;
      overflow: hidden;
      font-size: 14px;
      font-weight: 600;
      color: var(--el-text-color-primary);
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    &__item-desc {
      margin-top: 6px;
      font-size: 12px;
      line-height: 1.5;
      color: var(--el-text-color-secondary);
    }

    &__item-meta {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 6px;
      flex-shrink: 0;
    }

    &__probe {
      display: flex;
      flex-direction: column;
      align-items: stretch;
      gap: 8px;
      width: 100%;
      padding-top: 10px;
      border-top: 1px dashed var(--el-border-color-lighter);

      span {
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }

    &__probe-summary {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    &__checks {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      min-width: 0;

      :deep(.el-tag) {
        max-width: 100%;
        height: auto;
        min-height: 24px;
        white-space: normal;
      }
    }

    &__smoke {
      display: flex;
      flex-direction: column;
      gap: 8px;
      width: 100%;
      padding-top: 10px;
      border-top: 1px dashed var(--el-border-color-lighter);
    }

    &__smoke-grid {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 8px;
      font-size: 12px;

      div {
        min-width: 0;
        padding: 8px;
        border: 1px solid var(--el-border-color-lighter);
        border-radius: 6px;
        background: var(--el-fill-color-light);
      }

      span {
        display: block;
        color: var(--el-text-color-secondary);
      }

      strong {
        display: block;
        min-width: 0;
        margin-top: 4px;
        overflow: hidden;
        color: var(--el-text-color-primary);
        font-weight: 500;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    &__item-config {
      display: grid;
      grid-template-columns: minmax(0, 1fr);
      gap: 6px;
      margin-top: 10px;
      font-size: 12px;

      div {
        display: grid;
        grid-template-columns: 42px minmax(0, 1fr);
        gap: 8px;
        min-width: 0;
      }

      span {
        color: var(--el-text-color-secondary);
      }

      strong {
        min-width: 0;
        overflow: hidden;
        color: var(--el-text-color-primary);
        font-weight: 500;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    &__missing {
      margin-top: 10px;
      padding: 8px;
      border: 1px solid var(--el-color-warning-light-5);
      border-radius: 6px;
      background: var(--el-color-warning-light-9);
      color: var(--el-color-warning-dark-2);
      font-size: 12px;
      line-height: 1.5;
    }

    &__params {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      margin-top: 10px;
    }

    &__param-input {
      width: 100%;
    }

    @media (max-width: 1180px) {
      &__groups {
        grid-template-columns: 1fr;
      }

      &__execution-stats {
        grid-template-columns: repeat(4, minmax(0, 1fr));
      }

      &__execution-tables {
        grid-template-columns: 1fr;
      }

      &__governance-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }
    }

    @media (max-width: 720px) {
      &__header,
      &__audit-header,
      &__execution-header,
      &__governance-header {
        flex-direction: column;
      }

      &__execution-actions {
        justify-content: flex-start;
        width: 100%;
      }

      &__execution-stats {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }

      &__governance-actions {
        justify-content: flex-start;
        width: 100%;
      }

      &__governance-grid {
        grid-template-columns: 1fr;
      }
    }
  }
</style>
