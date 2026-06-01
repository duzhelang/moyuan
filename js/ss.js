//处理锁定按钮点击事件
    // 注意：原代码中 lock-button 和 toggleSidebar 变量未定义，且存在多余的闭合括号 });
    // 根据上下文中的另一段锁定逻辑，这里应该使用正确的 DOM 元素引用。
    // 假设 lockButton 和 sidebar 是之前已经获取到的 DOM 元素（参考下文代码风格）
    // 如果 lock-button 是 ID，请使用 document.getElementById('lock-button')
    // 为了保持与选中代码意图一致并修复语法错误（移除多余的 });），重写如下：
    
    // 获取按钮元素（如果上方未定义，需确保此处可访问）
    var lockBtn = document.getElementById('lock-button'); 
    if (lockBtn) {
        lockBtn.addEventListener('click', function () {
            isLocked = !isLocked;
            // 切换侧边栏的锁定状态类名，具体类名需根据CSS定义调整，此处保留原意
            if (typeof toggleSidebar !== 'undefined' && toggleSidebar) {
                toggleSidebar.classList.toggle('locked');
            }
            
            if (isLocked) {
                this.textContent = '解锁';
                // 如果锁定意味着保持打开或特定状态，可在此添加类名操作
                // sidebar.classList.add('open'); 
            } else {
                this.textContent = '锁定';
                // sidebar.classList.remove('open');
            }
        });
    }

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