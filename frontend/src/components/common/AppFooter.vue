<script setup lang="ts">
</script>

<template>
  <footer class="app-footer">
    <div class="footer-container">
      <div class="footer-links">
        <h4>联系我们</h4>
        <ul>
          <li>事务联络</li>
          <li>媒体采访</li>
          <li>人才应聘</li>
        </ul>
      </div>
      
      <div class="footer-links">
        <h4>友情链接</h4>
        <ul>
          <li>教育部</li>
          <li>江苏省教育厅</li>
          <li>中国传媒大学</li>
        </ul>
      </div>
      
      <div class="footer-contact">
        <h4>联系方式</h4>
        <p>电话：(025)86179990</p>
        <p>地址：江苏省南京市江宁区弘景大道3666号</p>
        <p>邮编：211172</p>
      </div>
    </div>
    
    <div class="footer-copyright">
      <p>&copy; {{ new Date().getFullYear() }} 古今诗话——墨渊 版权所有</p>
    </div>
  </footer>
</template>

<style scoped lang="scss">
.app-footer {
  background-color: $background-color-dark;
  color: #fff;
  padding: $spacing-xl 0 $spacing-lg;
}

.footer-container {
  @include container;
  display: flex;
  gap: $spacing-xxl;
  margin-bottom: $spacing-xl;
  
  @include responsive(md) {
    flex-direction: column;
    gap: $spacing-xl;
  }
}

.footer-links {
  h4 {
    font-size: $font-size-lg;
    margin-bottom: $spacing-md;
    color: #fff;
  }
  
  ul {
    li {
      margin-bottom: $spacing-sm;
      color: rgba(255, 255, 255, 0.7);
      font-size: $font-size-sm;
    }
  }
}

.footer-contact {
  h4 {
    font-size: $font-size-lg;
    margin-bottom: $spacing-md;
    color: #fff;
  }
  
  p {
    margin-bottom: $spacing-sm;
    color: rgba(255, 255, 255, 0.7);
    font-size: $font-size-sm;
  }
}

.footer-copyright {
  @include container;
  text-align: center;
  padding-top: $spacing-lg;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  
  p {
    color: rgba(255, 255, 255, 0.5);
    font-size: $font-size-sm;
  }
}
</style>