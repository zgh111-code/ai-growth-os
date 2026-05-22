<template>
  <div class="markdown-body" v-html="renderedContent"></div>
</template>

<script setup>
import { computed } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

const props = defineProps({
  content: {
    type: String,
    default: ''
  }
})

// 配置 marked 使用 highlight.js 进行代码高亮
marked.setOptions({
  breaks: true,       // 支持 GitHub 风格的换行
  gfm: true,          // 支持 GitHub 风格的 Markdown
  highlight: function (code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(code, { language: lang }).value
      } catch (e) {
        // ignore
      }
    }
    // 如果没有指定语言或语言不支持，自动检测
    try {
      return hljs.highlightAuto(code).value
    } catch (e) {
      return code
    }
  }
})

const renderedContent = computed(() => {
  if (!props.content) return ''
  try {
    return marked.parse(props.content)
  } catch (e) {
    console.error('Markdown 渲染失败:', e)
    return props.content
  }
})
</script>

<style scoped>
.markdown-body {
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
  white-space: normal;
}

/* 标题 */
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  margin-top: 16px;
  margin-bottom: 8px;
  font-weight: 600;
  color: inherit;
}

.markdown-body :deep(h1) { font-size: 1.4em; }
.markdown-body :deep(h2) { font-size: 1.25em; }
.markdown-body :deep(h3) { font-size: 1.1em; }

/* 段落 */
.markdown-body :deep(p) {
  margin: 6px 0;
}

/* 列表 */
.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 20px;
  margin: 6px 0;
}

.markdown-body :deep(li) {
  margin: 3px 0;
}

/* 引用 */
.markdown-body :deep(blockquote) {
  margin: 8px 0;
  padding: 6px 12px;
  border-left: 3px solid #4F46E5;
  background: rgba(79, 70, 229, 0.06);
  border-radius: 0 6px 6px 0;
  color: #6b7280;
}

/* 行内代码 */
.markdown-body :deep(code:not(pre code)) {
  background: rgba(79, 70, 229, 0.08);
  color: #7C3AED;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.9em;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
}

/* 代码块 */
.markdown-body :deep(pre) {
  position: relative;
  margin: 10px 0;
  border-radius: 10px;
  overflow: hidden;
  background: #1e1e2e !important;
}

.markdown-body :deep(pre code) {
  display: block;
  padding: 16px 18px;
  font-size: 13px;
  line-height: 1.6;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  overflow-x: auto;
  color: #cdd6f4;
  background: transparent !important;
}

/* 代码块顶部语言标签 */
.markdown-body :deep(pre)::before {
  content: attr(data-language);
  display: block;
  padding: 6px 16px;
  font-size: 12px;
  color: #a6adc8;
  background: #181825;
  border-bottom: 1px solid rgba(255,255,255,0.06);
  font-family: inherit;
}

/* 表格 */
.markdown-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 10px 0;
  font-size: 13px;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid #e5e7eb;
  padding: 8px 12px;
  text-align: left;
}

.markdown-body :deep(th) {
  background: #f9fafb;
  font-weight: 600;
  color: #374151;
}

.markdown-body :deep(tr:nth-child(even)) {
  background: #f9fafb;
}

/* 链接 */
.markdown-body :deep(a) {
  color: #4F46E5;
  text-decoration: none;
}

.markdown-body :deep(a:hover) {
  text-decoration: underline;
}

/* 分割线 */
.markdown-body :deep(hr) {
  border: none;
  border-top: 1px solid #e5e7eb;
  margin: 16px 0;
}

/* 图片 */
.markdown-body :deep(img) {
  max-width: 100%;
  border-radius: 8px;
  margin: 8px 0;
}

/* 加粗/斜体 */
.markdown-body :deep(strong) {
  font-weight: 600;
}

.markdown-body :deep(em) {
  font-style: italic;
}

/* 任务列表 */
.markdown-body :deep(input[type="checkbox"]) {
  margin-right: 6px;
  accent-color: #4F46E5;
}
</style>
