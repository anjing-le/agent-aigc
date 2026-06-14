<template>
  <div class="aigc-models">
    <div class="aigc-models__header">
      <div>
        <h2 class="aigc-models__title">模型配置</h2>
        <p class="aigc-models__subtitle">Provider 状态来自后端配置和已注册 Bean</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadModels">刷新</el-button>
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
              <el-tag
                size="small"
                :type="probeResults[model.id].routable ? 'success' : 'warning'"
                effect="plain"
              >
                {{ probeResults[model.id].message }}
              </el-tag>
              <span>{{ formatProbeTime(probeResults[model.id].checkedAt) }}</span>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { Connection, Refresh } from '@element-plus/icons-vue'
  import { ElMessage } from 'element-plus'
  import { fetchGetModelList, fetchProbeProvider, fetchUpdateActiveProvider } from '@/api/aigc'
  import type {
    ContentType,
    ModelInfo,
    ModelListResponse,
    ProviderProbeResponse
  } from '@/api/model/aigcModel'
  import { formatDateTime } from '@/utils/time'

  defineOptions({ name: 'AIGCModels' })

  const loading = ref(false)
  const models = ref<ModelListResponse>({
    imageModels: [],
    videoModels: [],
    audioModels: []
  })
  const probingKey = ref('')
  const switchingKey = ref('')
  const probeResults = ref<Record<string, ProviderProbeResponse>>({})

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

  const formatDefaultParams = (model: ModelInfo) => {
    const params = model.defaultParams || {}
    return Object.entries(params).map(([key, value]) => `${key}: ${value}`)
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
      await loadModels()
    } catch (error) {
      console.error('Provider 路由切换失败:', error)
      ElMessage.error('Provider 路由切换失败')
    } finally {
      switchingKey.value = ''
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

  onMounted(() => {
    loadModels()
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
      align-items: center;
      gap: 8px;
      width: 100%;
      padding-top: 10px;
      border-top: 1px dashed var(--el-border-color-lighter);

      span {
        font-size: 12px;
        color: var(--el-text-color-secondary);
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

    @media (max-width: 1180px) {
      &__groups {
        grid-template-columns: 1fr;
      }
    }
  }
</style>
