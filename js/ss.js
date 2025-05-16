//处理锁定按钮点击事件
    lock-button.addEventListener('click', function () {
        isLocked = !isLocked;
        toggleSidebar.classList.toggle('locked');
        // mainContent.classList.toggle('locked');
        if (isLocked) {
            lock-button.textContent = '解锁';
        } else {
            lock-button.textContent = '锁定';
        }
    });
});

// 锁定按钮点击事件
  document.getElementById('lock-button').addEventListener('click', function() {
    isLocked = !isLocked;
    if (isLocked) {
      this.textContent = '解锁';
      sidebar.classList.add('open');
    } else {
      this.textContent = '锁';
      sidebar.classList.remove('open');
    }
  });
  
  window.onload = function() {
      var now = 1;
      var max = 6;
  
      // 绑定一次性的点击事件监听器
      document.getElementById('lb_jt_z').addEventListener('click', prevImage);
      document.getElementById('lb_jt_y').addEventListener('click', nextImage);
  
      // 开始自动轮播
      setInterval(nextImage, 4000); // 使用nextImage函数来处理自动轮播
  
      window.imgLoopShow=function(di) {//将函数名前置，把局部imgLoopShow暴露给全局
          now=di;
          updateImage();
      }
  
      function prevImage() {
          now--;
          if (now < 1) now = max;
          updateImage();
      }
  
      function nextImage() {
          now++;
          if (now > max) now = 1;
          updateImage();
      }
  
      function updateImage() {
          document.getElementById("ad_scroll").src = "img/lb_ (" + now + ").jpg";
          for (var i = 1; i <= max; i++) {
              document.getElementById("li" + i).style.color = now == i ? "rgb(249,255,0)" : "black";
          }
      }
  
      // 初始化显示第一张图片
      updateImage();
  };