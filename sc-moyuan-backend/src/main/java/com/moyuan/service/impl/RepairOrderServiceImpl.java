package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyuan.common.ResultCode;
import com.moyuan.dto.request.RepairCommentCreateRequest;
import com.moyuan.dto.request.RepairOrderCreateRequest;
import com.moyuan.entity.RepairComment;
import com.moyuan.entity.RepairOrder;
import com.moyuan.entity.User;
import com.moyuan.exception.BusinessException;
import com.moyuan.mapper.RepairCommentMapper;
import com.moyuan.mapper.RepairOrderMapper;
import com.moyuan.mapper.UserMapper;
import com.moyuan.service.RepairOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RepairOrderServiceImpl extends ServiceImpl<RepairOrderMapper, RepairOrder> implements RepairOrderService {

    private final RepairCommentMapper repairCommentMapper;
    private final UserMapper userMapper;

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        int random = ThreadLocalRandom.current().nextInt(100, 1000);
        return "RO" + timestamp + random;
    }

    private void fillUserInfo(List<RepairOrder> orders) {
        if (orders == null || orders.isEmpty()) return;
        List<Long> userIds = orders.stream()
                .map(RepairOrder::getUserId)
                .distinct()
                .collect(Collectors.toList());
        List<Long> assigneeIds = orders.stream()
                .map(RepairOrder::getAssigneeId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        userIds.addAll(assigneeIds);
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds)
                .stream().collect(Collectors.toMap(User::getId, u -> u));
        for (RepairOrder order : orders) {
            User user = userMap.get(order.getUserId());
            if (user != null) {
                order.setUsername(user.getNickname() != null ? user.getNickname() : user.getUsername());
                order.setAvatar(user.getAvatar());
            }
            if (order.getAssigneeId() != null) {
                User assignee = userMap.get(order.getAssigneeId());
                if (assignee != null) {
                    order.setAssigneeName(assignee.getNickname() != null ? assignee.getNickname() : assignee.getUsername());
                }
            }
        }
    }

    private void fillUserInfo(RepairOrder order) {
        if (order == null) return;
        User user = userMapper.selectById(order.getUserId());
        if (user != null) {
            order.setUsername(user.getNickname() != null ? user.getNickname() : user.getUsername());
            order.setAvatar(user.getAvatar());
        }
        if (order.getAssigneeId() != null) {
            User assignee = userMapper.selectById(order.getAssigneeId());
            if (assignee != null) {
                order.setAssigneeName(assignee.getNickname() != null ? assignee.getNickname() : assignee.getUsername());
            }
        }
    }

    private void fillCommentUserInfo(List<RepairComment> comments) {
        if (comments == null || comments.isEmpty()) return;
        List<Long> userIds = comments.stream()
                .map(RepairComment::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        for (RepairComment comment : comments) {
            User user = userMap.get(comment.getUserId());
            if (user != null) {
                comment.setUsername(user.getNickname() != null ? user.getNickname() : user.getUsername());
                comment.setAvatar(user.getAvatar());
            }
        }
    }

    @Override
    public IPage<RepairOrder> getMyOrders(Long userId, int pageNum, int pageSize, Integer status) {
        LambdaQueryWrapper<RepairOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RepairOrder::getUserId, userId);
        if (status != null) {
            wrapper.eq(RepairOrder::getStatus, status);
        }
        wrapper.orderByDesc(RepairOrder::getCreateTime);
        IPage<RepairOrder> page = this.baseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        fillUserInfo(page.getRecords());
        return page;
    }

    @Override
    public RepairOrder getOrderDetail(Long id, Long userId) {
        RepairOrder order = this.baseMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.REPAIR_ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.REPAIR_ORDER_NO_PERMISSION);
        }
        fillUserInfo(order);
        return order;
    }

    @Override
    @Transactional
    public RepairOrder createOrder(Long userId, RepairOrderCreateRequest request) {
        RepairOrder order = new RepairOrder();
        order.setOrderNo(generateOrderNo());
        order.setTitle(request.getTitle());
        order.setDescription(request.getDescription());
        order.setCategory(request.getCategory());
        order.setPriority(request.getPriority() != null ? request.getPriority() : 2);
        order.setStatus(0);
        order.setImages(request.getImages());
        order.setUserId(userId);
        this.baseMapper.insert(order);
        fillUserInfo(order);
        return order;
    }

    @Override
    @Transactional
    public void closeOrder(Long orderId, Long userId) {
        RepairOrder order = this.baseMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.REPAIR_ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.REPAIR_ORDER_NO_PERMISSION);
        }
        if (order.getStatus() == 3) {
            throw new BusinessException(ResultCode.REPAIR_ORDER_CLOSED);
        }
        order.setStatus(3);
        order.setCloseTime(LocalDateTime.now());
        this.baseMapper.updateById(order);
    }

    @Override
    @Transactional
    public void submitSatisfaction(Long orderId, Long userId, Integer satisfaction, String comment) {
        RepairOrder order = this.baseMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.REPAIR_ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.REPAIR_ORDER_NO_PERMISSION);
        }
        order.setSatisfaction(satisfaction);
        order.setSatisfactionComment(comment);
        this.baseMapper.updateById(order);
    }

    @Override
    public IPage<RepairOrder> adminGetOrders(int pageNum, int pageSize, Integer status, String category, Integer priority, String keyword) {
        LambdaQueryWrapper<RepairOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(RepairOrder::getStatus, status);
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(RepairOrder::getCategory, category);
        }
        if (priority != null) {
            wrapper.eq(RepairOrder::getPriority, priority);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(RepairOrder::getTitle, keyword)
                    .or().like(RepairOrder::getDescription, keyword)
                    .or().like(RepairOrder::getOrderNo, keyword));
        }
        wrapper.orderByDesc(RepairOrder::getCreateTime);
        IPage<RepairOrder> page = this.baseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        fillUserInfo(page.getRecords());
        return page;
    }

    @Override
    public RepairOrder adminGetOrderDetail(Long id) {
        RepairOrder order = this.baseMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.REPAIR_ORDER_NOT_FOUND);
        }
        fillUserInfo(order);
        return order;
    }

    @Override
    @Transactional
    public void updateStatus(Long orderId, Integer status, String resolveContent, Long assigneeId) {
        RepairOrder order = this.baseMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.REPAIR_ORDER_NOT_FOUND);
        }
        order.setStatus(status);
        if (StringUtils.hasText(resolveContent)) {
            order.setResolveContent(resolveContent);
        }
        if (assigneeId != null) {
            order.setAssigneeId(assigneeId);
        }
        if (status == 2) {
            order.setResolveTime(LocalDateTime.now());
        }
        if (status == 3) {
            order.setCloseTime(LocalDateTime.now());
        }
        this.baseMapper.updateById(order);
    }

    @Override
    @Transactional
    public void assignOrder(Long orderId, Long assigneeId) {
        RepairOrder order = this.baseMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.REPAIR_ORDER_NOT_FOUND);
        }
        order.setAssigneeId(assigneeId);
        if (order.getStatus() == 0) {
            order.setStatus(1);
        }
        this.baseMapper.updateById(order);
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", this.baseMapper.selectCount(null));
        stats.put("pending", this.baseMapper.selectCount(
                new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getStatus, 0)));
        stats.put("processing", this.baseMapper.selectCount(
                new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getStatus, 1)));
        stats.put("resolved", this.baseMapper.selectCount(
                new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getStatus, 2)));
        stats.put("closed", this.baseMapper.selectCount(
                new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getStatus, 3)));
        stats.put("rejected", this.baseMapper.selectCount(
                new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getStatus, 4)));
        return stats;
    }

    @Override
    public List<RepairComment> getComments(Long orderId, boolean isAdmin) {
        LambdaQueryWrapper<RepairComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RepairComment::getRepairOrderId, orderId);
        if (!isAdmin) {
            wrapper.eq(RepairComment::getIsInternal, 0);
        }
        wrapper.orderByAsc(RepairComment::getCreateTime);
        List<RepairComment> comments = repairCommentMapper.selectList(wrapper);
        fillCommentUserInfo(comments);
        return comments;
    }

    @Override
    @Transactional
    public RepairComment addComment(Long orderId, Long userId, RepairCommentCreateRequest request) {
        RepairOrder order = this.baseMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.REPAIR_ORDER_NOT_FOUND);
        }
        RepairComment comment = new RepairComment();
        comment.setRepairOrderId(orderId);
        comment.setUserId(userId);
        comment.setContent(request.getContent());
        comment.setImages(request.getImages());
        comment.setIsInternal(request.getIsInternal() != null ? request.getIsInternal() : 0);
        repairCommentMapper.insert(comment);
        fillCommentUserInfo(List.of(comment));
        return comment;
    }
}
