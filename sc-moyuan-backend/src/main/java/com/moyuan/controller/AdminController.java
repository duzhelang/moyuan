package com.moyuan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyuan.common.R;
import com.moyuan.entity.*;
import com.moyuan.mapper.*;
import com.moyuan.service.ForumPostService;
import com.moyuan.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Tag(name = "后台管理接口")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final PoemMapper poemMapper;
    private final PoetMapper poetMapper;
    private final CategoryMapper categoryMapper;
    private final DynastyMapper dynastyMapper;
    private final ForumPostService forumPostService;
    private final OperationLogMapper operationLogMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // ========== 统计数据 ==========

    @Operation(summary = "获取统计数据")
    @GetMapping("/stats")
    public R<Map<String, Object>> getStats() {
        long userCount = userService.count();
        long poemCount = poemMapper.selectCount(null);
        long categoryCount = categoryMapper.selectCount(null);
        long dynastyCount = dynastyMapper.selectCount(null);
        long poetCount = poetMapper.selectCount(null);
        long postCount = forumPostService.count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userCount);
        stats.put("poemCount", poemCount);
        stats.put("categoryCount", categoryCount);
        stats.put("dynastyCount", dynastyCount);
        stats.put("poetCount", poetCount);
        stats.put("postCount", postCount);
        return R.success(stats);
    }

    @Operation(summary = "获取统计趋势")
    @GetMapping("/stats/trend")
    public R<Map<String, Object>> getStatsTrend() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        List<String> dates = IntStream.rangeClosed(0, 6)
                .mapToObj(i -> LocalDate.now().minusDays(6 - i).format(formatter))
                .toList();

        List<Poem> recentPoems = poemMapper.selectList(
                new LambdaQueryWrapper<Poem>().ge(Poem::getCreateTime, sevenDaysAgo));
        Map<String, Long> poemByDate = recentPoems.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getCreateTime().format(formatter),
                        Collectors.counting()));

        List<ForumPost> recentPosts = forumPostService.list(
                new LambdaQueryWrapper<ForumPost>().ge(ForumPost::getCreateTime, sevenDaysAgo));
        Map<String, Long> postByDate = recentPosts.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getCreateTime().format(formatter),
                        Collectors.counting()));

        List<User> recentUsers = userMapper.selectList(
                new LambdaQueryWrapper<User>().ge(User::getCreateTime, sevenDaysAgo));
        Map<String, Long> userByDate = recentUsers.stream()
                .collect(Collectors.groupingBy(
                        u -> u.getCreateTime().format(formatter),
                        Collectors.counting()));

        List<Long> poemCounts = dates.stream().map(d -> poemByDate.getOrDefault(d, 0L)).toList();
        List<Long> postCounts = dates.stream().map(d -> postByDate.getOrDefault(d, 0L)).toList();
        List<Long> userCounts = dates.stream().map(d -> userByDate.getOrDefault(d, 0L)).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("poems", poemCounts);
        result.put("posts", postCounts);
        result.put("users", userCounts);
        return R.success(result);
    }

    // ========== 用户管理 ==========

    @Operation(summary = "获取用户列表")
    @GetMapping("/users")
    public R<?> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(User::getUsername, keyword).or()
                    .like(User::getNickname, keyword).or()
                    .like(User::getEmail, keyword);
        }
        wrapper.orderByDesc(User::getCreateTime);
        IPage<User> result = userService.page(new Page<>(page, size), wrapper);
        result.getRecords().forEach(u -> u.setPassword(null));
        return R.success(result);
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/users/{id}")
    public R<User> getUser(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return R.error("用户不存在");
        }
        user.setPassword(null);
        return R.success(user);
    }

    @Operation(summary = "创建用户")
    @PostMapping("/users")
    public R<User> createUser(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        if (username == null || password == null) {
            return R.error("用户名和密码不能为空");
        }
        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (existing != null) {
            return R.error("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(params.get("email"));
        user.setNickname(params.getOrDefault("nickname", username));
        user.setPhone(params.get("phone"));
        user.setStatus(1);
        userService.save(user);
        user.setPassword(null);
        return R.success(user);
    }

    @Operation(summary = "更新用户")
    @PutMapping("/users/{id}")
    public R<User> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        User user = userService.getById(id);
        if (user == null) {
            return R.error("用户不存在");
        }
        if (params.containsKey("nickname")) user.setNickname((String) params.get("nickname"));
        if (params.containsKey("email")) user.setEmail((String) params.get("email"));
        if (params.containsKey("phone")) user.setPhone((String) params.get("phone"));
        if (params.containsKey("bio")) user.setBio((String) params.get("bio"));
        if (params.containsKey("status")) user.setStatus((Integer) params.get("status"));
        if (params.containsKey("password") && StringUtils.hasText((String) params.get("password"))) {
            user.setPassword(passwordEncoder.encode((String) params.get("password")));
        }
        userService.updateById(user);
        user.setPassword(null);
        return R.success(user);
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/users/{id}")
    public R<Void> deleteUser(@PathVariable Long id) {
        userService.removeById(id);
        return R.success();
    }

    // ========== 诗词管理 ==========

    @Operation(summary = "获取诗词列表")
    @GetMapping("/poems")
    public R<?> listPoems(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Poem> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Poem::getTitle, keyword).or()
                    .like(Poem::getContent, keyword);
        }
        wrapper.orderByDesc(Poem::getCreateTime);
        return R.success(poemMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @Operation(summary = "获取诗词详情")
    @GetMapping("/poems/{id}")
    public R<Poem> getPoem(@PathVariable Long id) {
        Poem poem = poemMapper.selectById(id);
        if (poem == null) {
            return R.error("诗词不存在");
        }
        return R.success(poem);
    }

    @Operation(summary = "创建诗词")
    @PostMapping("/poems")
    public R<Poem> createPoem(@RequestBody Poem poem) {
        poem.setViewCount(0);
        poem.setLikeCount(0);
        poem.setFavoriteCount(0);
        poemMapper.insert(poem);
        return R.success(poem);
    }

    @Operation(summary = "更新诗词")
    @PutMapping("/poems/{id}")
    public R<Poem> updatePoem(@PathVariable Long id, @RequestBody Poem poem) {
        poem.setId(id);
        poemMapper.updateById(poem);
        return R.success(poem);
    }

    @Operation(summary = "删除诗词")
    @DeleteMapping("/poems/{id}")
    public R<Void> deletePoem(@PathVariable Long id) {
        poemMapper.deleteById(id);
        return R.success();
    }

    // ========== 分类管理 ==========

    @Operation(summary = "获取分类列表")
    @GetMapping("/categories")
    public R<?> listCategories(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return R.success(categoryMapper.selectPage(new Page<>(page, size), null));
    }

    @Operation(summary = "获取分类详情")
    @GetMapping("/categories/{id}")
    public R<Category> getCategory(@PathVariable Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            return R.error("分类不存在");
        }
        return R.success(category);
    }

    @Operation(summary = "创建分类")
    @PostMapping("/categories")
    public R<Category> createCategory(@RequestBody Category category) {
        categoryMapper.insert(category);
        return R.success(category);
    }

    @Operation(summary = "更新分类")
    @PutMapping("/categories/{id}")
    public R<Category> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        category.setId(id);
        categoryMapper.updateById(category);
        return R.success(category);
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/categories/{id}")
    public R<Void> deleteCategory(@PathVariable Long id) {
        categoryMapper.deleteById(id);
        return R.success();
    }

    // ========== 朝代管理 ==========

    @Operation(summary = "获取朝代列表")
    @GetMapping("/dynasties")
    public R<?> listDynasties(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return R.success(dynastyMapper.selectPage(new Page<>(page, size), null));
    }

    @Operation(summary = "获取朝代详情")
    @GetMapping("/dynasties/{id}")
    public R<Dynasty> getDynasty(@PathVariable Long id) {
        Dynasty dynasty = dynastyMapper.selectById(id);
        if (dynasty == null) {
            return R.error("朝代不存在");
        }
        return R.success(dynasty);
    }

    @Operation(summary = "创建朝代")
    @PostMapping("/dynasties")
    public R<Dynasty> createDynasty(@RequestBody Dynasty dynasty) {
        dynastyMapper.insert(dynasty);
        return R.success(dynasty);
    }

    @Operation(summary = "更新朝代")
    @PutMapping("/dynasties/{id}")
    public R<Dynasty> updateDynasty(@PathVariable Long id, @RequestBody Dynasty dynasty) {
        dynasty.setId(id);
        dynastyMapper.updateById(dynasty);
        return R.success(dynasty);
    }

    @Operation(summary = "删除朝代")
    @DeleteMapping("/dynasties/{id}")
    public R<Void> deleteDynasty(@PathVariable Long id) {
        dynastyMapper.deleteById(id);
        return R.success();
    }

    // ========== 诗人管理 ==========

    @Operation(summary = "获取诗人列表")
    @GetMapping("/poets")
    public R<?> listPoets(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Poet> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Poet::getName, keyword);
        }
        wrapper.orderByDesc(Poet::getCreateTime);
        return R.success(poetMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @Operation(summary = "获取诗人详情")
    @GetMapping("/poets/{id}")
    public R<Poet> getPoet(@PathVariable Long id) {
        Poet poet = poetMapper.selectById(id);
        if (poet == null) {
            return R.error("诗人不存在");
        }
        return R.success(poet);
    }

    @Operation(summary = "创建诗人")
    @PostMapping("/poets")
    public R<Poet> createPoet(@RequestBody Poet poet) {
        poetMapper.insert(poet);
        return R.success(poet);
    }

    @Operation(summary = "更新诗人")
    @PutMapping("/poets/{id}")
    public R<Poet> updatePoet(@PathVariable Long id, @RequestBody Poet poet) {
        poet.setId(id);
        poetMapper.updateById(poet);
        return R.success(poet);
    }

    @Operation(summary = "删除诗人")
    @DeleteMapping("/poets/{id}")
    public R<Void> deletePoet(@PathVariable Long id) {
        poetMapper.deleteById(id);
        return R.success();
    }

    // ========== 帖子管理 ==========

    @Operation(summary = "获取帖子列表")
    @GetMapping("/forum-posts")
    public R<?> listForumPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<ForumPost> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(ForumPost::getTitle, keyword).or()
                    .like(ForumPost::getContent, keyword);
        }
        wrapper.orderByDesc(ForumPost::getCreateTime);
        return R.success(forumPostService.page(new Page<>(page, size), wrapper));
    }

    @Operation(summary = "获取帖子详情")
    @GetMapping("/forum-posts/{id}")
    public R<ForumPost> getForumPost(@PathVariable Long id) {
        ForumPost post = forumPostService.getById(id);
        if (post == null) {
            return R.error("帖子不存在");
        }
        return R.success(post);
    }

    @Operation(summary = "更新帖子状态")
    @PutMapping("/forum-posts/{id}/status")
    public R<Void> updateForumPostStatus(@PathVariable Long id, @RequestBody Map<String, Integer> params) {
        ForumPost post = forumPostService.getById(id);
        if (post == null) {
            return R.error("帖子不存在");
        }
        post.setStatus(params.get("status"));
        forumPostService.updateById(post);
        return R.success();
    }

    @Operation(summary = "删除帖子")
    @DeleteMapping("/forum-posts/{id}")
    public R<Void> deleteForumPost(@PathVariable Long id) {
        forumPostService.removeById(id);
        return R.success();
    }

    // ========== 操作日志 ==========

    @Operation(summary = "获取操作日志")
    @GetMapping("/logs")
    public R<?> getLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(OperationLog::getOperation, keyword).or()
                    .like(OperationLog::getMethod, keyword);
        }
        if (StringUtils.hasText(startTime)) {
            wrapper.ge(OperationLog::getCreateTime, LocalDateTime.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (StringUtils.hasText(endTime)) {
            wrapper.le(OperationLog::getCreateTime, LocalDateTime.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        wrapper.orderByDesc(OperationLog::getCreateTime);
        return R.success(operationLogMapper.selectPage(new Page<>(page, size), wrapper));
    }
}
