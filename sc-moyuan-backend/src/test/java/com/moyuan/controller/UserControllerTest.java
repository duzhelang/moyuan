package com.moyuan.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyuan.dto.request.UserUpdateRequest;
import com.moyuan.entity.User;
import com.moyuan.security.LoginUser;
import com.moyuan.service.ForumPostService;
import com.moyuan.service.PoetProfileService;
import com.moyuan.service.UserService;
import com.moyuan.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = UserController.class, properties = "app.mapper-scan-enabled=false")
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;
    @MockBean
    private ForumPostService forumPostService;
    @MockBean
    private PoetProfileService poetProfileService;
    @MockBean
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        // 手动构造 LoginUser 安全上下文，供 SecurityUtil.getCurrentUserId() 使用
        User loginUserEntity = new User();
        loginUserEntity.setId(1L);
        loginUserEntity.setUsername("testuser");
        loginUserEntity.setPassword("encodedPassword");
        loginUserEntity.setRole("USER");
        loginUserEntity.setStatus(1);
        LoginUser loginUser = new LoginUser(loginUserEntity, "USER");
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getUserInfo_返回用户信息() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setNickname("测试用户");
        user.setRole("user");

        when(userService.getUserInfo(1L)).thenReturn(user);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("测试用户"));
    }

    @Test
    void updateUser_成功更新() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setEmail("new@example.com");
        request.setNickname("新昵称");

        when(userService.updateUserInfo(anyLong(), any(UserUpdateRequest.class))).thenReturn(new User());

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
