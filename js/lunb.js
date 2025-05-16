
//首页大图
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
//轮播诗文
     var now_shi=1;
	var max_shi=7;
	function shiwen_LoopShow(diji){
		now_shi=diji;
		// 先让当前图片淡入（设置为显示并利用CSS过渡效果）
		//    document.getElementById("ad_scroll_shiwen").classList.add('active');
		document.getElementById("ad_scroll_shiwen").src="img/lb_shiwen ("+now_shi+").png";
		const carouselImages = document.querySelectorAll('.lb_zhushi');
		// function showNextImage() {
		//        carouselImages[currentIndex].classList.remove('active');
		//        currentIndex = (currentIndex + 1) % carouselImages.length;
		//        carouselImages[currentIndex].classList.add('active');
		//    }
		for(var i=1;i<=max_shi;i++){
			if(now_shi==i){
				document.getElementById("l"+i).style="background-color:rgb(249,255,0)";
			}
			else{
				document.getElementById("l"+i).style="background-color:#bfbcbc";
			}
		}
		if(now_shi>=max_shi){
			now_shi=0;
		}
	}
	setInterval("shiwen_LoopShow(++now_shi)",6000);
// 看图写诗
document.addEventListener('DOMContentLoaded', function() {
        const imageUpload = document.getElementById('imageUpload');
        const previewImage = document.getElementById('previewImage');
        const recognizeButton = document.getElementById('recognizeButton');
        const resultContainer = document.getElementById('resultContainer');
        const recognitionResult = document.getElementById('recognitionResult');

        // 监听文件选择事件
        imageUpload.addEventListener('change', function(event) {
            const file = event.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = function(e) {
                    previewImage.src = e.target.result;
                    previewImage.style.display = 'block';
                    recognizeButton.disabled = false;
                };
                reader.readAsDataURL(file);
            }
        });
    });
	 
	 function initPlayButton(buttonId) {
	     document.addEventListener('DOMContentLoaded', function () {
	         var audio = document.getElementById('myAudio_zhu');
	         var playButton = document.getElementById(buttonId);
	 
	         playButton.addEventListener('click', function () {
	             if (audio.paused || audio.ended) {
	                 audio.play();
	             } else {
	                 audio.pause();
	             }
	         });
	     });
	 }
	 initPlayButton('yinyuebofang_zhu');
	// 智能分析
// 为每个列表项添加鼠标悬停事件监听器,,#使用是需要去除li中的鼠标悬停事件
    // for (var i = 1; i <= max; i++) {
    //     (function(index) {
    //         document.getElementById("li" + index).addEventListener('mouseover', function() {
    //             imgLoopShow(index);
    //         });
    //     })(i); // 立即执行函数表达式(IIFE)，用于创建闭包保存当前索引
    // }
// 	var now=1;
// 	var max=6;
// 	function imgLoopShow(id){
// 		now=id;
// 		document.getElementById("ad_scroll").src="img/lb_ ("+now+").jpg";
// 		for(var i=1;i<=max;i++){
// 			if(now==i){
// 				document.getElementById("li"+i).style="color:rgb(249,255,0)";
// 			}
// 			else{
// 				document.getElementById("li"+i).style="color:black";
// 			}
// 		}
// 		let imgs=["img/lb_ (1).jpg","img/lb_ (2).jpg","img/lb_ (3).jpg","img/lb_ (4).jpg","img/lb_ (5).jpg","img/lb_ (6).jpg"];


// 		document.getElementById('lb_jt_z').addEventListener('click',function (){
// 		  i--;
// 		  if (i<1){
// 		    i=imgs.length-1;
// 		  }
// 		  document.getElementById('ad_scroll').src=imgs[i];
// 		});
// 		document.getElementById('lb_jt_y').addEventListener('click',function (){
// 		  i++;
// 		  if (i>=imgs.length){
// 		    i=1;

// 		  }
// 		  document.getElementById('ad_scroll').src=imgs[i];
// 		});
// 		if(now==max){
// 			now=1;
// 		}

// 	}
// 	setInterval("imgLoopShow(++now)",4000);
			function showTime() {//获取时间
			            var date = new Date();
			            // var time = date.toTimeString();
			            var time = date.toLocaleString();
			            // 通过类名获取元素，getElementsByClassName返回的是一个类数组对象，所以通过索引[0]获取第一个匹配的元素
			            var divElement = document.getElementsByClassName("shijian")[0];
			            if (divElement) {
			                divElement.innerHTML = time;
			            } else {
			                console.error("未找到类名为'shijian'的元素");
			            }
			        }
			        setInterval(showTime, 1000);

//.header.fixed,.sousuo.fixed
window.addEventListener('scroll', function() {
	var navbar = document.querySelector('.daohang');
	if (window.pageYOffset > 640) { // 当页面向下滚动超过50像素时
		navbar.classList.add('fixed');
	} else {
		navbar.classList.remove('fixed');
	}
});
window.addEventListener('scroll', function() {
	var navbar = document.querySelector('.sousuo');
	if (window.pageYOffset > 640) { // 当页面向下滚动超过50像素时
		navbar.classList.add('fixed');
	} else {
		navbar.classList.remove('fixed');
	}
});
// 获取所有左移按钮、右移按钮和可滚动的 div 元素（使用 querySelectorAll 获取类名对应的元素集合）
		const leftButtons = document.querySelectorAll('.leftButton');
		const rightButtons = document.querySelectorAll('.rightButton');
		// const scrollableDivs = document.querySelectorAll('.h6_li_1,.h6_li_2,.h6_li_3');
// const scrollableDivs = document.querySelectorAll('.h6_li_1');
		// 定义每次点击按钮滚动条移动的像素距离，可根据实际需求调整
		const scrollStep = 295;

// 添加点击事件处理函数给左移按钮集合
leftButtons.forEach(function (leftButton) {
	leftButton.addEventListener('click', function () {
		// 根据按钮的 data-target 属性找到对应的可滚动区域
		const targetClass = '.' + this.getAttribute('data-target');
		const scrollableDiv = document.querySelector(targetClass);

		if (scrollableDiv) {
			scrollableDiv.scrollLeft -= scrollStep;
		}
	});
});

// 添加点击事件处理函数给右移按钮集合
rightButtons.forEach(function (rightButton) {
	rightButton.addEventListener('click', function () {
		// 根据按钮的 data-target 属性找到对应的可滚动区域
		const targetClass = '.' + this.getAttribute('data-target');
		const scrollableDiv = document.querySelector(targetClass);

		if (scrollableDiv) {
			scrollableDiv.scrollLeft += scrollStep;
		}
	});
});

// // 获取所有左移按钮、右移按钮和可滚动的 div 元素（使用 querySelectorAll 获取类名对应的元素集合）
// 		const leftButtons = document.querySelectorAll('.leftButton');
// 		const rightButtons = document.querySelectorAll('.rightButton');
// 		const scrollableDivs = document.querySelectorAll('.h6_li');
		
// 		// 定义每次点击按钮滚动条移动的像素距离，可根据实际需求调整
// 		const scrollStep = 295;
		
// 		// 循环遍历左移按钮集合，为每个按钮添加点击事件处理函数
// 		leftButtons.forEach(function (leftButton) {
// 		    leftButton.addEventListener('click', function () {
// 		        scrollableDivs.forEach(function (scrollableDiv) {
// 		            scrollableDiv.scrollLeft -= scrollStep;
// 		        });
// 		    });
// 		});
		
// 		// 循环遍历右移按钮集合，为每个按钮添加点击事件处理函数
// 		rightButtons.forEach(function (rightButton) {
// 		    rightButton.addEventListener('click', function () {
// 		        scrollableDivs.forEach(function (scrollableDiv) {
// 		            scrollableDiv.scrollLeft += scrollStep;
// 		        });
// 		    });
// 		});
//                       //错误禁用
					// document.getElementById('sousuo').onsubmit = function() {
					// 	// 假设要跳转到https://example.com/next-page.html
					// 	// 模版  window.location.href = 'https://example.com/next - page.html';
					// 	window.location.href =
					// 		'https://wenku.baidu.com/ndlaunch/browse/chat?noAutoSend=1&fr=launch_ad&utm_source=bingss-WD&utm_medium=cpa&utm_account=SS-bingtg10';
					// 	return false;
					// };
//暂弃用 // 获取 div 和 li 元素
// var toggleDiv = document.getElementById('xdl_kaiguan');
// var toggleLi = document.getElementById('sy_xdlchuang');

// // 添加点击事件监听器给 div
// toggleDiv.addEventListener('click', function() {
// //	 event.stopPropagation(); // 阻止事件冒泡
//   // 切换 'active' 类以显示或隐藏登录框
//   toggleLi.classList.toggle('active');
// });



// // 获取 div 和 li 元素
// var toggleDiv = document.getElementById('xdl_kaiguan');
// var toggleLi = document.getElementById('sy_xdlchuang');

// // 添加点击事件监听器给 div
// toggleDiv.addEventListener('click', function() {
//   // 检查 li 的 display 样式
//    var huoquwaibu = window.getComputedStyle(toggleLi);
//   if (huoquwaibu.style.display === 'none') {
//     // 如果是 'none'，则设置为 'block' 来显示它
//     huoquwaibu.style.display = 'block';
//   } else {
//     // 否则隐藏它
//     huoquwaibu.style.display = 'none';
//   }
//   // 切换 'clicked' 类来控制显示状态
//         toggleItem.classList.toggle('clicked');
// });

// 模版
// window.addEventListener('scroll', function() {
//     var navbar = document.querySelector('.navbar');
//     if (window.pageYOffset > 50) { // 当页面向下滚动超过50像素时
//         navbar.classList.add('fixed');
//     } else {
//         navbar.classList.remove('fixed');
//     }
// });

