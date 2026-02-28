<!-- 创作输入组件 - 极简设计 -->
<!-- 用户只需：输入描述 + 可选上传素材 -->
<template>
  <div class="creation-input">
    <!-- 上传的素材预览 -->
    <div v-if="localFiles.length > 0" class="creation-input__files">
      <div
        v-for="(file, index) in filesPreviews"
        :key="index"
        class="creation-input__file-item"
      >
        <img :src="file.preview" :alt="file.name" />
        <el-icon class="creation-input__file-remove" @click="removeFile(index)">
          <Close />
        </el-icon>
      </div>
      <div class="creation-input__file-tip">
        已添加 {{ localFiles.length }} 个素材，AI将基于这些素材创作
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="creation-input__main">
      <!-- 上传按钮 -->
      <el-upload
        ref="uploadRef"
        :show-file-list="false"
        :auto-upload="false"
        accept="image/*,video/*"
        multiple
        :limit="4"
        :on-change="handleFileChange"
        :disabled="loading"
      >
        <el-tooltip content="上传素材（图片/视频），AI将基于这些素材创作" placement="top">
          <el-button
            :icon="FolderAdd"
            circle
            size="large"
            :disabled="loading"
            class="creation-input__upload-btn"
          />
        </el-tooltip>
      </el-upload>

      <!-- 文本输入 -->
      <el-input
        v-model="inputValue"
        type="textarea"
        :placeholder="placeholder"
        :disabled="loading"
        :autosize="{ minRows: 1, maxRows: 5 }"
        resize="none"
        class="creation-input__textarea"
        @keydown.enter.exact.prevent="handleSubmit"
      />

      <!-- 发送按钮 -->
      <el-button
        type="primary"
        :icon="loading ? Loading : Promotion"
        circle
        size="large"
        :loading="loading"
        :disabled="!canSubmit"
        class="creation-input__send-btn"
        @click="handleSubmit"
      />
    </div>

    <!-- 提示信息 -->
    <div class="creation-input__tips">
      <div class="creation-input__tips-left">
        <el-icon><MagicStick /></el-icon>
        <span>描述你想要的内容，AI会自动理解并创作</span>
      </div>
      <span class="creation-input__tips-right">Enter 发送 · Shift+Enter 换行</span>
    </div>

    <!-- 示例提示 -->
    <div v-if="!inputValue && localFiles.length === 0" class="creation-input__examples">
      <span class="creation-input__examples-label">试试这些：</span>
      <el-tag
        v-for="example in examples"
        :key="example"
        class="creation-input__example-tag"
        effect="plain"
        @click="inputValue = example"
      >
        {{ example }}
      </el-tag>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Close, FolderAdd, Promotion, Loading, MagicStick } from '@element-plus/icons-vue'
import type { UploadFile } from 'element-plus'

interface Props {
  modelValue: string
  files?: File[]
  loading?: boolean
}

interface Emits {
  'update:modelValue': [value: string]
  'update:files': [files: File[]]
  submit: []
}

interface FilePreview {
  file: File
  name: string
  preview: string
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  files: () => []
})

const emit = defineEmits<Emits>()

// 输入值双向绑定
const inputValue = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// 文件列表
const localFiles = computed({
  get: () => props.files,
  set: (val) => emit('update:files', val)
})

// 文件预览列表
const filesPreviews = ref<FilePreview[]>([])

// 占位符
const placeholder = computed(() => {
  if (localFiles.value.length > 0) {
    return '描述你想要的创作效果，例如：让图片中的人物动起来...'
  }
  return '描述你想要创作的内容，例如：一只可爱的橘猫在阳光下打盹...'
})

// 是否可以提交
const canSubmit = computed(() => {
  return (inputValue.value.trim() || localFiles.value.length > 0) && !props.loading
})

// 示例提示词
const examples = [
  '一只可爱的橘猫在阳光下打盹',
  '赛博朋克风格的未来城市夜景',
  '水彩风格的樱花树下的少女',
  '让这张图片动起来'
]

/** 处理文件选择 */
const handleFileChange = (file: UploadFile) => {
  if (file.raw && localFiles.value.length < 4) {
    const reader = new FileReader()
    reader.onload = (e) => {
      filesPreviews.value.push({
        file: file.raw!,
        name: file.name,
        preview: e.target?.result as string
      })
      emit('update:files', [...localFiles.value, file.raw!])
    }
    reader.readAsDataURL(file.raw)
  }
}

/** 移除文件 */
const removeFile = (index: number) => {
  filesPreviews.value.splice(index, 1)
  const newFiles = [...localFiles.value]
  newFiles.splice(index, 1)
  emit('update:files', newFiles)
}

/** 处理提交 */
const handleSubmit = () => {
  if (!canSubmit.value) return
  emit('submit')
}

// 同步文件预览
watch(() => props.files, (newFiles) => {
  if (newFiles.length === 0) {
    filesPreviews.value = []
  }
}, { deep: true })
</script>

<style lang="scss" scoped>
.creation-input {
  background: var(--el-bg-color);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid var(--el-border-color-light);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);

  &__files {
    display: flex;
    gap: 12px;
    margin-bottom: 16px;
    flex-wrap: wrap;
    align-items: center;
  }

  &__file-item {
    position: relative;
    width: 72px;
    height: 72px;
    border-radius: 12px;
    overflow: hidden;
    border: 2px solid var(--el-border-color-light);

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  &__file-remove {
    position: absolute;
    top: 4px;
    right: 4px;
    width: 20px;
    height: 20px;
    background: rgba(0, 0, 0, 0.6);
    border-radius: 50%;
    color: #fff;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    transition: background 0.2s;

    &:hover {
      background: rgba(0, 0, 0, 0.8);
    }
  }

  &__file-tip {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  &__main {
    display: flex;
    align-items: flex-end;
    gap: 12px;
  }

  &__upload-btn {
    flex-shrink: 0;
  }

  &__textarea {
    flex: 1;

    :deep(.el-textarea__inner) {
      background: var(--el-fill-color-light);
      border: none;
      border-radius: 12px;
      padding: 14px 18px;
      font-size: 15px;
      line-height: 1.6;

      &:focus {
        box-shadow: 0 0 0 2px var(--el-color-primary-light-7);
      }
    }
  }

  &__send-btn {
    flex-shrink: 0;
  }

  &__tips {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 12px;
    font-size: 12px;
    color: var(--el-text-color-secondary);

    &-left {
      display: flex;
      align-items: center;
      gap: 6px;

      .el-icon {
        color: var(--el-color-primary);
      }
    }
  }

  &__examples {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 12px;
    flex-wrap: wrap;

    &-label {
      font-size: 12px;
      color: var(--el-text-color-secondary);
    }
  }

  &__example-tag {
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      background: var(--el-color-primary-light-9);
      border-color: var(--el-color-primary);
      color: var(--el-color-primary);
    }
  }
}
</style>

