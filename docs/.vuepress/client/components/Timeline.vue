<template>
  <div class="timeline-container horizontal">
    <h2>博客时间轴 <span class="timeline-subtitle">/ TIMELINE</span></h2>
    <div class="timeline-wrapper">
      <ul class="timeline">
        <li v-for="(item, index) in sortedTimeline" :key="index" class="timeline-item">
          <div class="timeline-marker" :class="{'feature-marker': item.type === 'feature', 'post-marker': item.type === 'post'}">
            <span class="marker-icon">{{ item.type === 'feature' ? '🎯' : '📝' }}</span>
          </div>
          <div class="timeline-content">
            <div class="timeline-header">
              <span class="timeline-date">{{ formatDate(item.date) }}</span>
            </div>
            <h3 class="timeline-title">
              <a v-if="item.link" :href="item.link" target="_blank" rel="noopener noreferrer">{{ item.title }}</a>
              <span v-else>{{ item.title }}</span>
            </h3>
            <p v-if="item.description" class="timeline-description">{{ item.description }}</p>
          </div>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useSiteData } from '@vuepress/client';

const siteData = useSiteData();
const features = ref([]);
const posts = ref([]);

// 格式化日期 (极客风格，显示年月日)
const formatDate = (dateString) => {
  if (!dateString) return '';
  // 尝试解析多种可能的日期格式
  const date = new Date(dateString.replace(/-/g, '/')); // 兼容 YYYY-MM-DD
  if (isNaN(date.getTime())) {
      return dateString; // 如果解析失败，返回原始字符串
  }
  
  // 极客风格日期格式: YYYY.MM.DD
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}.${month}.${day}`;
};

// 合并并排序时间轴条目
const sortedTimeline = computed(() => {
  const combined = [...features.value, ...posts.value];
  // 按日期升序排列，更符合水平时间轴从左到右的习惯
  return combined.sort((a, b) => new Date(a.date) - new Date(b.date));
});

onMounted(async () => {
  // 1. 获取功能更新数据
  try {
    const response = await fetch('/data/features.json');
    if (response.ok) {
      const data = await response.json();
      features.value = data.filter(item => item.date).map(item => ({
          ...item,
          date: item.date
      }));
    } else {
      console.error('Failed to fetch features data');
    }
  } catch (error) {
    console.error('Error fetching features data:', error);
  }

  // 2. 获取博客文章数据
  const allPages = siteData.value.pages || [];
  posts.value = allPages
    .filter(page => page.path.startsWith('/article/') && page.frontmatter.createTime)
    .map(page => ({
      date: page.frontmatter.createTime,
      type: 'post',
      title: page.title,
      link: page.path,
      description: page.frontmatter.description || ''
    }));
});
</script>

<style scoped>
.timeline-container.horizontal {
  max-width: 100%; /* 宽度占满 */
  margin: 3rem 0; /* 上下边距 */
  padding: 1rem 0;
  font-family: 'Consolas', 'Monaco', monospace; /* 使用等宽字体增加极客感 */
}

.timeline-container.horizontal h2 {
  text-align: center;
  margin-bottom: 2rem;
  font-weight: 700;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.timeline-subtitle {
  font-size: 0.8em;
  opacity: 0.7;
  font-weight: normal;
  letter-spacing: 2px;
}

.timeline-wrapper {
  overflow-x: auto; /* 允许水平滚动 */
  padding: 2rem 0; /* 为内容和标记留出垂直空间 */
  position: relative;
  /* 添加左右渐变遮罩，提示可滚动 */
  -webkit-mask-image: linear-gradient(to right, transparent, black 10%, black 90%, transparent);
  mask-image: linear-gradient(to right, transparent, black 10%, black 90%, transparent);
  scrollbar-width: thin;
  scrollbar-color: var(--c-brand) transparent;
}

.timeline-wrapper::-webkit-scrollbar {
  height: 6px;
}

.timeline-wrapper::-webkit-scrollbar-thumb {
  background-color: var(--c-brand);
  border-radius: 3px;
}

.timeline {
  display: flex; /* 水平排列 */
  list-style: none;
  padding: 0 2rem; /* 左右留白 */
  margin: 0;
  position: relative;
  width: max-content; /* 让宽度根据内容决定 */
}

/* 水平时间线 */
.timeline::before {
  content: '';
  position: absolute;
  top: 50%; /* 垂直居中 */
  left: 0;
  right: 0; /* 贯穿整个 .timeline 宽度 */
  height: 2px;
  background: repeating-linear-gradient(
    to right,
    var(--c-brand) 0,
    var(--c-brand) 8px,
    transparent 8px,
    transparent 12px
  ); /* 虚线效果 */
  transform: translateY(-50%);
  z-index: 0;
}

.timeline-item {
  display: flex;
  flex-direction: column; /* 标记在上方，内容在下方 */
  align-items: center; /* 水平居中 */
  position: relative;
  padding: 0 1.5rem; /* 项目之间的间距 */
  flex-shrink: 0; /* 防止项目收缩 */
  min-width: 240px; /* 每个项目的最小宽度 */
  margin-top: -12px; /* 向上移动一点，让标记压在线上 */
}

.timeline-marker {
  position: relative; /* 相对于 item 定位 */
  width: 24px;
  height: 24px;
  border-radius: 4px; /* 方形边角，更极客 */
  background-color: var(--c-brand, #3eaf7c);
  border: 2px solid #fff;
  z-index: 1; /* 在时间线之上 */
  margin-bottom: 1rem; /* 标记和内容之间的距离 */
  display: flex;
  align-items: center;
  justify-content: center;
  transform: rotate(45deg); /* 菱形效果 */
  box-shadow: 0 0 0 4px rgba(62, 175, 124, 0.2);
}

.marker-icon {
  transform: rotate(-45deg); /* 抵消父元素的旋转 */
  font-size: 0.8em;
}

.feature-marker {
  background-color: #f3b821;
  box-shadow: 0 0 0 4px rgba(196, 135, 23, 0.2);
}

.post-marker {
  background-color: #2196F3;
  box-shadow: 0 0 0 4px rgba(33, 150, 243, 0.2);
}

.timeline-content {
  background-color: #f9f9f9;
  padding: 1rem 1.2rem;
  border-radius: 4px; /* 方形边角，更极客 */
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  text-align: left; /* 左对齐更易读 */
  width: 100%; /* 占满 item 宽度 */
  position: relative; /* 用于可能的微调 */
  border-left: 3px solid var(--c-brand);
}

.timeline-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

/* 暗黑模式 */
html.dark .timeline::before { 
  background: repeating-linear-gradient(
    to right,
    var(--c-brand) 0,
    var(--c-brand) 8px,
    transparent 8px,
    transparent 12px
  );
}
html.dark .timeline-marker { border-color: #1e1e1e; }
html.dark .timeline-content { 
  background-color: #252525; 
  color: #ccc; 
  border-left: 3px solid var(--c-brand);
}
html.dark .timeline-title a { color: #eee; }
html.dark .timeline-date { color: #aaa; }
html.dark .timeline-description { color: #bbb; }

.timeline-date {
  display: block;
  font-size: 0.85em;
  color: #666;
  font-weight: bold;
  font-family: 'Consolas', monospace;
  letter-spacing: 0.5px;
}

.timeline-title {
  margin: 0 0 0.5rem 0;
  font-size: 1.05em; /* 稍微调小字体 */
  font-weight: bold;
  line-height: 1.3;
}
.timeline-title a {
  color: var(--c-text);
  text-decoration: none;
  transition: color 0.3s;
  position: relative;
  padding-bottom: 2px;
}
.timeline-title a:hover { 
  color: var(--c-brand); 
}
.timeline-title a::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 0;
  height: 1px;
  background-color: var(--c-brand);
  transition: width 0.3s;
}
.timeline-title a:hover::after {
  width: 100%;
}

.timeline-description {
  margin: 0;
  font-size: 0.9em;
  color: #555;
  line-height: 1.5;
  /* 最多显示两行，超出省略 */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  min-height: 2.7em; /* 保证至少两行的高度 */
  border-top: 1px dashed #e0e0e0;
  padding-top: 0.5rem;
}

.tag {
  display: inline-block;
  padding: 0.1em 0.5em;
  font-size: 0.7em;
  border-radius: 2px;
  color: #fff;
  vertical-align: middle;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.feature-tag { 
  background-color: #4CAF50; 
}
.post-tag { 
  background-color: #2196F3; 
}

/* 响应式：在小屏幕上可能还是垂直显示更好，或者保持水平但优化 */
@media (max-width: 768px) {
  .timeline-item {
    min-width: 200px; /* 减小最小宽度 */
    padding: 0 1rem;
  }
  .timeline-title { font-size: 1em; }
  .timeline-description { font-size: 0.85em; }
}
</style>