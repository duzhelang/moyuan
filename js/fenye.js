// scripts.js
// document.querySelectorAll('.sidebar a').forEach(anchor => {
//     anchor.addEventListener('click', function(e) {
//         e.preventDefault();

//         document.querySelector(this.getAttribute('href')).scrollIntoView({
//             behavior: 'smooth'
//         });
//     });
// });

//     document.addEventListener('mousemove', (e) => {
//         if (e.clientX <= threshold) {
//             sidebar.classList.remove('hidden');
//         } else if (e.clientX > threshold && !sidebar.classList.contains('hidden')) {
//             sidebar.classList.add('hidden');
//         }
//     });
// });

//yishangchongtu, jinyong  以上禁用

// shouqi 收起
let isLocked = true;
document.addEventListener('DOMContentLoaded', () => {
    const sidebar = document.getElementById('celan');
    const toggleButton = document.getElementById('shouqi');
    const navLinks = document.querySelectorAll('.nav-link');

    // const showSidebarButton = document.getElementById('showSidebar');//展开按键
	
   var audio = document.getElementById('myAudio');
   audio.play();
    // 初始状态下隐藏侧边栏
//    sidebar.classList.add('hidden');
 // 初始状态下显示侧边栏
    sidebar.classList.remove('hidden');

    // 绑定按钮点击事件
    toggleButton.addEventListener('click', shouqi);
// showSidebarButton.addEventListener('click', shouqi);  //展开按键

    //平滑滚动到相应部分
    navLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const targetId = link.getAttribute('href');
            const targetSection = document.querySelector(targetId);
            targetSection.scrollIntoView({ behavior: 'smooth' });
            highlightActiveLink();
        });
    });

    // 高亮当前活动链接
    function highlightActiveLink() {
        const sections = document.querySelectorAll('article');
        let currentSection = '';

        sections.forEach(section => {
            const sectionTop = section.offsetTop;
            const sectionHeight = section.clientHeight;
            if (window.scrollY >= sectionTop-sectionHeight / 3) {
                currentSection = section.getAttribute('id');
            }
        });

        navLinks.forEach(link => {
            link.classList.remove('active');
            if (link.getAttribute('href') === `#${currentSection}`) {
                link.classList.add('active');
            }
        });
    }

    // 滚动时更新活动链接
    window.addEventListener('scroll', highlightActiveLink);
	
	
    // 监听鼠标进入和离开侧边区域
    const sideBarWidth = 250;
    const threshold = 20;
	// let isLocked = true;
	
	document.addEventListener('mousemove', function() {
	  // const sidebar = document.getElementById('sidebar');
	  if (isLocked) return; // 如果已锁定，则不改变侧边栏状态
	
	  if (event.clientX < 250 && sidebar.style.left === '-240px') { // 靠近左侧时展开
	    sidebar.style.left = '0';
	  } else if (event.clientX > 250 && sidebar.style.left !== '-240px') { // 远离左侧时收起
	    sidebar.style.left = '-250px';
	  }
	});

	document.addEventListener('mousemove', (e) => {
	        if (e.clientX <=threshold && sidebar.classList.contains('hidden')) {
	            sidebar.classList.remove('hidden');
	             toggleButton.textContent = '收      起';
	        }
			if (e.clientX > sideBarWidth && !sidebar.classList.contains('hidden')) {
			            sidebar.classList.add('hidden');
			             toggleButton.textContent = '已锁定';
			        }
	    });
	// 切换侧边栏显示
	function shouqi() {
	    sidebar.classList.toggle('hidden');
	    if (sidebar.classList.contains('hidden')) {
	        toggleButton.textContent = '展开';
	    } else {
	        toggleButton.textContent = '收起';
	    }
	}
    //监听鼠标离开页面区域
	// document.addEventListener('mouseleave', (e) => {
	//         if (e.clientX > sideBarWidth && !sidebar.classList.contains('hidden')) {
	//             sidebar.classList.add('hidden');
	//             toggleButton.textContent = '展开';
	//         }
	//     });
 	}); //总括号
	
function updateToggleButtonState(isLocked) {
    var toggleButton = document.getElementById('shouqi');
    if (toggleButton) {
        toggleButton.disabled = isLocked;
    }
}
function getIsLocked() {
					    return localStorage.getItem('sidebarLocked') === 'true';
					}
					
					function setIsLocked(newIsLocked) {
					    localStorage.setItem('sidebarLocked', newIsLocked);
					}
					
					function toggleLock() {
					    var button = document.querySelector('.lock-button');
					    var toggleButton = document.getElementById('shouqi');
					    // var isLocked = getIsLocked();
						var newIsLocked = getIsLocked();
						const sidebar = document.getElementById('celan');
					    if (button.innerHTML.includes('锁定')) {
					        button.innerHTML = '<i class="fas fa-lock"></i> 解锁';
					        newIsLocked = true;
					        setIsLocked(newIsLocked);
					        if (toggleButton) {
					            toggleButton.disabled = true;
								sidebar.classList.add('suoding');
					        }
					    } else {
							button.innerHTML = '<i class="fas fa-unlock"></i> 锁定';
							newIsLocked = false;
							setIsLocked(newIsLocked);
							if (toggleButton) {
							    toggleButton.disabled = false;
								sidebar.classList.remove('suoding');
							}
					    }
					}
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
						// const sideBarWidth = 250;
						// const threshold = 20;
						// let isLocked = true;
						// const sidebar = document.getElementById('sidebar');
						// const toggleButton = document.getElementById('toggleButton');
						
						// document.addEventListener('mousemove', function (e) {
						//     if (isLocked) return;
						//     if (e.clientX < sideBarWidth && sidebar.style.left === '-240px') {
						//         sidebar.style.left = '0';
						//     } else if (e.clientX > sideBarWidth && sidebar.style.left!== '-240px') {
						//         sidebar.style.left = '-250px';
						//     }
						//     if (e.clientX <= threshold && sidebar.classList.contains('hidden')) {
						//         sidebar.classList.remove('hidden');
						//         if (toggleButton) {
						//             toggleButton.textContent = '收  起';
						//         }
						//     }
						// });
						
						
				//朗读播放		
// 用于存储当前正在播放的语音实例

		var currentUtterance;
		var isReading = false; // 记录是否正在朗读，初始化为false
		var isPaused = false;  // 新增变量，记录是否处于暂停状态，初始化为false
			var clickCount = 0; // 新增变量，用于记录点击次数
			 // 获取进度条元素
			var progressBar = document.getElementById('progress-bar');
			
		function readText(textId) {
		    if ('speechSynthesis' in window) {
		        var text = document.getElementById(textId).innerText; // 使用innerText以避免HTML标签
		        var synth = window.speechSynthesis;
				var langdu_you = document.querySelector('.langdu_you');
		// langdu_you.style.display = (langdu_you.style.display === 'block')? 'none' : 'block';
		langdu_you.style.display = 'flex';
		        if (isReading && !isPaused) { // 如果正在朗读且未暂停，则取消当前朗读任务
		            synth.cancel();
		            isReading = false;
		            isPaused = false;
		        }
		
		        if (!isReading || isPaused) { // 如果没有在朗读或之前被暂停
		            currentUtterance = new SpeechSynthesisUtterance(text);
					currentUtterance.onboundary = function (event) {
					                        // onboundary事件会在语音到达一个词或句子边界时触发，这里用于更新进度条
					                        var progress = (event.charIndex / text.length) * 100;
					                        progressBar.value = progress;
					                    };
		            synth.speak(currentUtterance);
		            isReading = true;
		            isPaused = false; // 开始朗读时重置暂停状态
		        }
		    } else {
		        alert('您的浏览器不支持语音朗读功能，请更换浏览器后再试');
		    }
		}
		
		// 暂停朗读
		function pauseText() {
		    var synth = window.speechSynthesis;
		    if (currentUtterance && synth.speaking && !synth.paused) {
		        synth.pause();
		        isPaused = true;
		        document.querySelector('.langdu_you').classList.add('paused'); // 添加暂停类名
		    }
		}
		
		// 继续朗读
		function resumeText() {
		    var synth = window.speechSynthesis;
		    if (currentUtterance && synth.paused) {
		        synth.resume();
		        isPaused = false;
		        document.querySelector('.langdu_you').classList.remove('paused'); // 移除暂停类名
		    }
		}
		
		//点击处理函数
			function handleClick() {
			    clickCount++;
			    if (clickCount % 2 === 1) { // 第一次点击，执行暂停操作
			        pauseText();
			    } else { // 第二次点击，执行继续朗读操作
			        resumeText();
			    }
			}
			var clickableElement = document.querySelector('.langdu_you');
			clickableElement.addEventListener('click',handleClick);
				
					
	//音频
	
	
					//自动播放，现代浏览器禁用```不禁用的浏览器会自动播放
					// function playAudio() {
					//             var audio = document.getElementById("myAudio");
					//             audio.play();
					//         }
					// window.onload = function () {
					//          var audio = document.getElementById('myAudio');
					//          audio.play();
					//        };
			//左右按键合并
			function initPlayButton(buttonId) {
			    document.addEventListener('DOMContentLoaded', function () {
			        var audio = document.getElementById('myAudio');
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
			initPlayButton('yinyuebofang');
			initPlayButton('yinyuebofang_you');
				
				
	//图标变换
function initPlayButton(buttonId) {
						    document.addEventListener('DOMContentLoaded', function () {
						        var audio = document.getElementById('myAudio');
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
						
						window.addEventListener('load', function () {
						    const musicButton = document.getElementById('yinyueButton');
						    const audioDiv = document.getElementById('yinpinbofangqi');
						    const expandTextElement = document.getElementById('zhankai_wz');
						    const statusIcon = document.getElementById('statusIcon');
						    const laba = document.getElementById('laba2');
						
						    const toggleAudioDiv = function () {
						        if (audioDiv.style.display === 'none') {
						            audioDiv.style.display = 'block';
						        } else {
						            audioDiv.style.display = 'none';
						        }
						    };
						
						    const toggleExpandText = function () {
						        const currentStatus = expandTextElement.dataset.status;
						        if (currentStatus === 'expand') {
						            expandTextElement.dataset.status ='retract';
						            expandTextElement.textContent = '展开';
						            statusIcon.src = '../img/jianzu (2).png';
						            laba.src = '../img/laba (2).png';
						        } else {
						            expandTextElement.dataset.status = 'expand';
						            expandTextElement.textContent = '收回';
						            statusIcon.src = '../img/jianzu (1).png';
						            laba.src = '../img/laba (1).png';
						        }
						    };
						
						    musicButton.addEventListener('click', function () {
						        toggleAudioDiv();
						        toggleExpandText();
						    });
						
						    initPlayButton('yinyuebofang');
						    initPlayButton('yinyuebofang_you');
						});		
		//左侧播放按钮
		// document.addEventListener('DOMContentLoaded', function () {
		//     var audio = document.getElementById('myAudio'); // 获取音频元素
		//     var playButton = document.getElementById('yinyuebofang'); // 获取按钮元素
		
		//     playButton.addEventListener('click', function () { // 为按钮添加点击事件监听器
		//         if (audio.paused || audio.ended) { // 如果音频暂停或结束，则播放
		//             audio.play();
		//         } else { // 否则暂停音频
		//             audio.pause();
		//         }
		//     });
		// });
		// //右侧播放按钮
		// document.addEventListener('DOMContentLoaded', function () {
		//     var audio = document.getElementById('myAudio'); // 获取音频元素
		//     var playButton2 = document.getElementById('yinyuebofang_you'); // 获取按钮元素
		
		//     playButton2.addEventListener('click', function () { // 为按钮添加点击事件监听器
		//         if (audio.paused || audio.ended) { // 如果音频暂停或结束，则播放
		//             audio.play();
		//         } else { // 否则暂停音频
		//             audio.pause();
		//         }
		//     });
		// });
						
						
						
	// function toggleyinyue() {
	//   var button = document.querySelector('.yinyueButton');
	//   if (button.innerHTML.includes('展开')) {
	//     button.innerHTML = '<p>展开<img class="jianzu" src="../img/jianzu (1).png"/></p>';
	//     // Add your code to lock the sidebar here
	//   } else {
	//     button.innerHTML = '<p>隐藏<img class="jianzu" src="../img/jianzu (2).png"/></p>';
	//     // Add your code to unlock the sidebar here
	//   }
	// }
	
//浮动输入框
document.getElementById('showInputButton').addEventListener('click', function() {
	document.getElementById('floatingInputBox').classList.remove('fy_yincang_kuang');
});

document.getElementById('closeInputButton').addEventListener('click', function() {
	document.getElementById('floatingInputBox').classList.add('fy_yincang_kuang');
});
	
	
	
	// //备用研究 可选：当鼠标离开整个导航区域时自动关闭菜单
	//   document.querySelector('.daohang').addEventListener('mouseleave', function() {
	//     toggleItems.forEach(toggleItem => {
	//       toggleItem.classList.remove('clicked');
	//     });
	// document.addEventListener('DOMContentLoaded', function () {
	//         var audio = document.getElementById('myAudio');
	//         audio.play();
	//       });
	
			//可行的
			// 		var audio = document.getElementById('myAudio');
					
			// 		var playButton = document.getElementById('yinyueButton');
			// 		playButton.addEventListener('click', function () {
			// 			if (audio.paused) {
			// 				audio.play();
			// 			} else {
			// 				audio.pause();
			// 			}
			// 		});
					 
			
	// 锁按键变换
	// function toggleLock() {
	//   var button = document.querySelector('.lock-button');
	//   if (button.innerHTML.includes('Lock')) {
	//     button.innerHTML = '<i class="fas fa-unlock"></i> Unlock Sidebar';
	// 	 isLocked = false; // 设置为未锁定状态，允许侧边栏响应鼠标移动事件改变状态
	//     // Add your code to lock the sidebar here
	//   } else {
	//     button.innerHTML = '<i class="fas fa-lock"></i> Lock Sidebar';
	// 	 isLocked = true; // 设置为锁定状态，禁止侧边栏响应鼠标移动事件改变状态
	//     // Add your code to unlock the sidebar here
	//   }
	// }
	

 // 点击按钮时切换侧边栏的固定状态
//   lock-button.addEventListener('click', () => {
//     isPinned = !isPinned;
//     if (isPinned) {
//       lock-button.textContent = '取消固定';
//     } else {
//       lock-button.textContent = '固定';
//       sidebar.classList.add('hidden'); // 如果之前是展开的，则先隐藏再等待鼠标事件
//     }
//   });
// });
		//处理锁定按钮点击事件
// 		    lockButton.addEventListener('#toggleSidebar', function () {
// 		        isLocked = !isLocked;
// 		        sidebar.classList.toggle('locked');
// 		        mainContent.classList.toggle('locked');
// 		        if (isLocked) {
// 		            lockButton.textContent = '解锁';
// 		        } else {
// 		            lockButton.textContent = '锁定';
// 		        }
// 		    });
// 		});
// //锁定
// let isLocked = false;

// function toggleLock() {
//   isLocked = !isLocked;
//   document.getElementById('lock-button').textContent = isLocked ? '🔓' : '🔒';
// }



// document.addEventListener('DOMContentLoaded', function() {
//   const sidebar = document.getElementById('sidebar');
//   let isLocked = false;

  // 锁定按钮点击事件
  // document.getElementById('lock-button').addEventListener('click', function() {
  //   isLocked = !isLocked;
  //   if (isLocked) {
  //     this.textContent = '解锁';
  //     sidebar.classList.add('open');
  //   } else {
  //     this.textContent = '锁';
  //     sidebar.classList.remove('open');
  //   }
  // });

//   // 鼠标进入/离开侧边栏的事件
//   sidebar.addEventListener('mouseenter', function() {
//     if (!isLocked) {
//       sidebar.classList.add('open');
//     }
//   });

//   sidebar.addEventListener('mouseleave', function() {
//     if (!isLocked) {
//       sidebar.classList.remove('open');
//     }
//   });
// });
//图标翻转   作废
