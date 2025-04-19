<template>
  <div class="home-banner" :style="bannerStyle">
    <div class="banner-content">
      <div class="avatar-container">
        <img class="avatar" :src="avatarUrl" alt="Exiao's Avatar" />
      </div>
      <h1 class="banner-text" style="font-size: 26px">🍐欢迎来到霄霄的技术博客~呀呼~</h1>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

// 背景图片 URL
const backgroundUrl = ref('')
// 你的头像 URL，请替换为你的实际头像地址
const avatarUrl = ref('/images/homeAvatar.webp')

// 判断是否为移动端
const isMobile = () => {
  return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(
    navigator.userAgent
  )
}

// 背景样式
const bannerStyle = ref({
  backgroundImage: 'none',
  backgroundSize: 'cover',
  backgroundPosition: 'center'
})

// 使用本地 Bing 壁纸
const useBingWallpaper = () => {
  try {
    // 直接使用本地壁纸路径
    const imageUrl = '/images/bingWallpaper.webp'
    backgroundUrl.value = imageUrl
    bannerStyle.value.backgroundImage = `url(${imageUrl})`
  } catch (error) {
    console.error('加载本地壁纸出错:', error)
    // 使用默认背景图
    bannerStyle.value.backgroundImage = 'url(/images/defaultHomeBanner.webp)'
  }
}

onMounted(() => {
  useBingWallpaper()
})
</script>

<style scoped>
.home-banner {
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: hidden;
}

.home-banner::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.4);
  z-index: 1;
}

.banner-content {
  position: relative;
  z-index: 2;
  text-align: center;
  color: white;
  padding: 2rem;
}

.avatar-container {
  margin-bottom: 2rem;
}

.avatar {
  width: 150px;
  height: 150px;
  border-radius: 50%;
  border: 3px solid white;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.3);
  object-fit: cover;
}

.banner-text {
  font-size: 2.5rem;
  font-weight: bold;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.5);
  margin: 0;
}

@media (max-width: 768px) {
  .avatar {
    width: 120px;
    height: 120px;
  }
  
  .banner-text {
    font-size: 1.8rem;
  }
}
</style>