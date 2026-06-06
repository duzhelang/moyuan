package com.moyuan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyuan.common.R;
import com.moyuan.dto.StatsTrendDTO;
import com.moyuan.entity.*;
import com.moyuan.mapper.StatsMapper;
import com.moyuan.service.*;
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
    private final PoemService poemService;
    private final PoetService poetService;
    private final CategoryService categoryService;
    private final DynastyService dynastyService;
    private final ForumPostService forumPostService;
    private final OperationLogService operationLogService;
    private final PasswordEncoder passwordEncoder;
    private final PoetFeaturedService poetFeaturedService;
    private final HomeNavigationService homeNavigationService;
    private final VisitLogService visitLogService;
    private final PoetSyncService poetSyncService;
    private final PoetProfileService poetProfileService;
    private final StatsMapper statsMapper;

    // ========== 统计数据 ==========

    @Operation(summary = "获取统计数据")
    @GetMapping("/stats")
    public R<Map<String, Object>> getStats() {
        long userCount = userService.count();
        long poemCount = poemService.count();
        long categoryCount = categoryService.count();
        long dynastyCount = dynastyService.count();
        long poetCount = poetService.count();
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
        int days = 7;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        List<String> dates = IntStream.rangeClosed(0, days - 1)
                .mapToObj(i -> LocalDate.now().minusDays(days - 1 - i).format(formatter))
                .toList();

        List<StatsTrendDTO> trendData = statsMapper.selectStatsTrend(days);

        Map<String, Map<String, Long>> grouped = trendData.stream()
                .collect(Collectors.groupingBy(
                        StatsTrendDTO::getType,
                        Collectors.toMap(
                                StatsTrendDTO::getDate,
                                StatsTrendDTO::getCount,
                                Long::sum)));

        List<Long> poemCounts = dates.stream()
                .map(d -> grouped.getOrDefault("poem", Map.of()).getOrDefault(d, 0L))
                .toList();
        List<Long> postCounts = dates.stream()
                .map(d -> grouped.getOrDefault("post", Map.of()).getOrDefault(d, 0L))
                .toList();
        List<Long> userCounts = dates.stream()
                .map(d -> grouped.getOrDefault("user", Map.of()).getOrDefault(d, 0L))
                .toList();

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
        User existing = userService.getOne(
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
        return R.success(poemService.page(new Page<>(page, size), wrapper));
    }

    @Operation(summary = "获取诗词详情")
    @GetMapping("/poems/{id}")
    public R<Poem> getPoem(@PathVariable Long id) {
        Poem poem = poemService.getById(id);
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
        poemService.save(poem);
        return R.success(poem);
    }

    @Operation(summary = "更新诗词")
    @PutMapping("/poems/{id}")
    public R<Poem> updatePoem(@PathVariable Long id, @RequestBody Poem poem) {
        poem.setId(id);
        poemService.updateById(poem);
        return R.success(poem);
    }

    @Operation(summary = "删除诗词")
    @DeleteMapping("/poems/{id}")
    public R<Void> deletePoem(@PathVariable Long id) {
        poemService.removeById(id);
        return R.success();
    }

    // ========== 分类管理 ==========

    @Operation(summary = "获取分类列表")
    @GetMapping("/categories")
    public R<?> listCategories(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return R.success(categoryService.page(new Page<>(page, size), null));
    }

    @Operation(summary = "获取分类详情")
    @GetMapping("/categories/{id}")
    public R<Category> getCategory(@PathVariable Long id) {
        Category category = categoryService.getById(id);
        if (category == null) {
            return R.error("分类不存在");
        }
        return R.success(category);
    }

    @Operation(summary = "创建分类")
    @PostMapping("/categories")
    public R<Category> createCategory(@RequestBody Category category) {
        categoryService.save(category);
        return R.success(category);
    }

    @Operation(summary = "更新分类")
    @PutMapping("/categories/{id}")
    public R<Category> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        category.setId(id);
        categoryService.updateById(category);
        return R.success(category);
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/categories/{id}")
    public R<Void> deleteCategory(@PathVariable Long id) {
        categoryService.removeById(id);
        return R.success();
    }

    // ========== 朝代管理 ==========

    @Operation(summary = "获取朝代列表")
    @GetMapping("/dynasties")
    public R<?> listDynasties(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return R.success(dynastyService.page(new Page<>(page, size), null));
    }

    @Operation(summary = "获取朝代详情")
    @GetMapping("/dynasties/{id}")
    public R<Dynasty> getDynasty(@PathVariable Long id) {
        Dynasty dynasty = dynastyService.getById(id);
        if (dynasty == null) {
            return R.error("朝代不存在");
        }
        return R.success(dynasty);
    }

    @Operation(summary = "创建朝代")
    @PostMapping("/dynasties")
    public R<Dynasty> createDynasty(@RequestBody Dynasty dynasty) {
        dynastyService.save(dynasty);
        return R.success(dynasty);
    }

    @Operation(summary = "更新朝代")
    @PutMapping("/dynasties/{id}")
    public R<Dynasty> updateDynasty(@PathVariable Long id, @RequestBody Dynasty dynasty) {
        dynasty.setId(id);
        dynastyService.updateById(dynasty);
        return R.success(dynasty);
    }

    @Operation(summary = "删除朝代")
    @DeleteMapping("/dynasties/{id}")
    public R<Void> deleteDynasty(@PathVariable Long id) {
        dynastyService.removeById(id);
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
        return R.success(poetService.page(new Page<>(page, size), wrapper));
    }

    @Operation(summary = "获取诗人详情")
    @GetMapping("/poets/{id}")
    public R<Poet> getPoet(@PathVariable Long id) {
        Poet poet = poetService.getById(id);
        if (poet == null) {
            return R.error("诗人不存在");
        }
        return R.success(poet);
    }

    @Operation(summary = "创建诗人")
    @PostMapping("/poets")
    public R<Poet> createPoet(@RequestBody Poet poet) {
        poetService.save(poet);
        return R.success(poet);
    }

    @Operation(summary = "更新诗人")
    @PutMapping("/poets/{id}")
    public R<Poet> updatePoet(@PathVariable Long id, @RequestBody Poet poet) {
        poet.setId(id);
        poetService.updateById(poet);
        return R.success(poet);
    }

    @Operation(summary = "删除诗人")
    @DeleteMapping("/poets/{id}")
    public R<Void> deletePoet(@PathVariable Long id) {
        poetService.removeById(id);
        return R.success();
    }

    // ========== 精选诗人管理 ==========

    @Operation(summary = "获取精选诗人列表")
    @GetMapping("/poet-featured")
    public R<?> listPoetFeatured(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        LambdaQueryWrapper<PoetFeatured> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(PoetFeatured::getSortOrder)
               .orderByDesc(PoetFeatured::getCreateTime);
        return R.success(poetFeaturedService.page(new Page<>(page, size), wrapper));
    }

    @Operation(summary = "获取精选诗人详情")
    @GetMapping("/poet-featured/{id}")
    public R<PoetFeatured> getPoetFeatured(@PathVariable Long id) {
        PoetFeatured poetFeatured = poetFeaturedService.getById(id);
        if (poetFeatured == null) {
            return R.error("精选诗人不存在");
        }
        return R.success(poetFeatured);
    }

    @Operation(summary = "创建精选诗人")
    @PostMapping("/poet-featured")
    public R<PoetFeatured> createPoetFeatured(@RequestBody PoetFeatured poetFeatured) {
        poetFeaturedService.save(poetFeatured);
        return R.success(poetFeatured);
    }

    @Operation(summary = "更新精选诗人")
    @PutMapping("/poet-featured/{id}")
    public R<PoetFeatured> updatePoetFeatured(@PathVariable Long id, @RequestBody PoetFeatured poetFeatured) {
        poetFeatured.setId(id);
        poetFeaturedService.updateById(poetFeatured);
        return R.success(poetFeatured);
    }

    @Operation(summary = "删除精选诗人")
    @DeleteMapping("/poet-featured/{id}")
    public R<Void> deletePoetFeatured(@PathVariable Long id) {
        poetFeaturedService.removeById(id);
        return R.success();
    }

    // ========== 首页导航管理 ==========

    @Operation(summary = "获取首页导航列表")
    @GetMapping("/home-navigation")
    public R<?> listHomeNavigation(
            @RequestParam(required = false) String type) {
        LambdaQueryWrapper<HomeNavigation> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(type)) {
            wrapper.eq(HomeNavigation::getType, type);
        }
        wrapper.eq(HomeNavigation::getStatus, 1)
               .orderByAsc(HomeNavigation::getSortOrder);
        return R.success(homeNavigationService.list(wrapper));
    }

    @Operation(summary = "管理端获取首页导航列表")
    @GetMapping("/home-navigation/manage")
    public R<?> listHomeNavigationManage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type) {
        LambdaQueryWrapper<HomeNavigation> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(type)) {
            wrapper.eq(HomeNavigation::getType, type);
        }
        wrapper.orderByAsc(HomeNavigation::getType)
               .orderByAsc(HomeNavigation::getSortOrder);
        return R.success(homeNavigationService.page(new Page<>(page, size), wrapper));
    }

    @Operation(summary = "创建首页导航")
    @PostMapping("/home-navigation")
    public R<HomeNavigation> createHomeNavigation(@RequestBody HomeNavigation homeNavigation) {
        homeNavigationService.save(homeNavigation);
        return R.success(homeNavigation);
    }

    @Operation(summary = "更新首页导航")
    @PutMapping("/home-navigation/{id}")
    public R<HomeNavigation> updateHomeNavigation(@PathVariable Long id, @RequestBody HomeNavigation homeNavigation) {
        homeNavigation.setId(id);
        homeNavigationService.updateById(homeNavigation);
        return R.success(homeNavigation);
    }

    @Operation(summary = "删除首页导航")
    @DeleteMapping("/home-navigation/{id}")
    public R<Void> deleteHomeNavigation(@PathVariable Long id) {
        homeNavigationService.removeById(id);
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

    // ========== 访问统计 ==========

    @Operation(summary = "获取访问统计数据")
    @GetMapping("/stats/visits")
    public R<Map<String, Object>> getVisitStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime weekStart = now.toLocalDate().minusDays(7).atStartOfDay();
        LocalDateTime monthStart = now.toLocalDate().minusDays(30).atStartOfDay();

        long todayVisits = visitLogService.count(
                new LambdaQueryWrapper<VisitLog>().ge(VisitLog::getCreateTime, todayStart));
        long weekVisits = visitLogService.count(
                new LambdaQueryWrapper<VisitLog>().ge(VisitLog::getCreateTime, weekStart));
        long monthVisits = visitLogService.count(
                new LambdaQueryWrapper<VisitLog>().ge(VisitLog::getCreateTime, monthStart));
        long totalVisits = visitLogService.count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("todayVisits", todayVisits);
        stats.put("weekVisits", weekVisits);
        stats.put("monthVisits", monthVisits);
        stats.put("totalVisits", totalVisits);
        return R.success(stats);
    }

    @Operation(summary = "获取访问趋势")
    @GetMapping("/stats/visits/trend")
    public R<Map<String, Object>> getVisitTrend(@RequestParam(defaultValue = "7") int days) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        List<String> dates = IntStream.rangeClosed(0, days - 1)
                .mapToObj(i -> LocalDate.now().minusDays(days - 1 - i).format(formatter))
                .toList();

        LocalDateTime startTime = LocalDateTime.now().minusDays(days).toLocalDate().atStartOfDay();
        List<VisitLog> visitLogs = visitLogService.list(
                new LambdaQueryWrapper<VisitLog>().ge(VisitLog::getCreateTime, startTime));

        Map<String, Long> visitsByDate = visitLogs.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getCreateTime().format(formatter),
                        Collectors.counting()));

        Map<String, Long> uniqueVisitorsByDate = visitLogs.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getCreateTime().format(formatter),
                        Collectors.mapping(VisitLog::getIp, Collectors.toSet())))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (long) e.getValue().size()));

        List<Long> visitCounts = dates.stream().map(d -> visitsByDate.getOrDefault(d, 0L)).toList();
        List<Long> uniqueVisitorCounts = dates.stream().map(d -> uniqueVisitorsByDate.getOrDefault(d, 0L)).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("visits", visitCounts);
        result.put("uniqueVisitors", uniqueVisitorCounts);
        return R.success(result);
    }

    // ========== 诗人数据同步 ==========

    @Operation(summary = "同步单个诗人数据")
    @PostMapping("/poets/sync/{name}")
    public R<Map<String, Object>> syncPoet(@PathVariable String name) {
        Map<String, Object> result = poetSyncService.syncPoetData(name);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return R.success(result);
        }
        return R.error((String) result.get("message"));
    }

    @Operation(summary = "同步所有诗人数据")
    @PostMapping("/poets/sync-all")
    public R<Map<String, Object>> syncAllPoets() {
        Map<String, Object> result = poetSyncService.syncAllPoets();
        return R.success(result);
    }

    // ========== 内容审核 ==========

    @Operation(summary = "审核诗词")
    @PutMapping("/poems/{id}/audit")
    public R<Void> auditPoem(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        Integer status = (Integer) params.get("status");
        String reason = (String) params.get("reason");
        if (status == null) {
            return R.error("审核状态不能为空");
        }
        poemService.auditPoem(id, status, reason);
        return R.success();
    }

    @Operation(summary = "获取诗人认证申请列表")
    @GetMapping("/poet-profiles")
    public R<?> getPoetProfiles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<PoetProfile> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(PoetProfile::getVerifiedStatus, status);
        }
        wrapper.orderByDesc(PoetProfile::getCreateTime);
        return R.success(poetProfileService.page(new Page<>(page, size), wrapper));
    }

    @Operation(summary = "审核诗人认证")
    @PutMapping("/poet-profiles/{id}/verify")
    public R<Void> verifyPoetProfile(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        Integer status = (Integer) params.get("status");
        String reason = (String) params.get("reason");
        if (status == null) {
            return R.error("审核状态不能为空");
        }
        poetProfileService.verifyProfile(id, status, reason);
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
        return R.success(operationLogService.page(new Page<>(page, size), wrapper));
    }
}
