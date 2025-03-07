<template>
  <div class="home-banner" :style="bannerStyle">
    <div class="banner-content">
      <div class="avatar-container">
        <img class="avatar" :src="avatarUrl" alt="Exiao's Avatar" />
      </div>
      <h1 class="banner-text" style="font-size: 26px">🍐欢迎来到霄霄的博客~呀呼~</h1>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

// 背景图片 URL
const backgroundUrl = ref('')
// 你的头像 URL，请替换为你的实际头像地址
const avatarUrl = ref('/images/homeAvatar.jpg')

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

// 获取必应每日图片
const fetchBingImage = async () => {
  try {
    // 根据设备类型选择分辨率参数
    const resolution = isMobile() ? 'MBL' : 'UHD'
    const response = await fetch(
      `https://dailybing.com/api/v1/today/zh-cn/${resolution}`
    )

    if (response.ok) {
      backgroundUrl.value = response.url
      bannerStyle.value.backgroundImage = `url(${backgroundUrl.value})`
    } else {
      console.error('获取必应图片失败')
      // 使用默认背景图
      bannerStyle.value.backgroundImage = 'url(/images/default-home-banner.jpg)'
    }
  } catch (error) {
    console.error('获取必应图片出错:', error)
    // 使用默认背景图
    bannerStyle.value.backgroundImage = 'url(/images/default-home-banner.jpg)'
  }
}

onMounted(() => {
  fetchBingImage()
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