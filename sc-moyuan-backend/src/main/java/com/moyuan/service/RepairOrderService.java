package com.moyuan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moyuan.dto.request.RepairCommentCreateRequest;
import com.moyuan.dto.request.RepairOrderCreateRequest;
import com.moyuan.entity.RepairComment;
import com.moyuan.entity.RepairOrder;

import java.util.List;
import java.util.Map;

public interface RepairOrderService extends IService<RepairOrder> {
    IPage<RepairOrder> getMyOrders(Long userId, int pageNum, int pageSize, Integer status);
    RepairOrder getOrderDetail(Long id, Long userId);
    RepairOrder createOrder(Long userId, RepairOrderCreateRequest request);
    void closeOrder(Long orderId, Long userId);
    void submitSatisfaction(Long orderId, Long userId, Integer satisfaction, String comment);
    IPage<RepairOrder> adminGetOrders(int pageNum, int pageSize, Integer status, String category, Integer priority, String keyword);
    RepairOrder adminGetOrderDetail(Long id);
    void updateStatus(Long orderId, Integer status, String resolveContent, Long assigneeId);
    void assignOrder(Long orderId, Long assigneeId);
    Map<String, Object> getStats();
    List<RepairComment> getComments(Long orderId, boolean isAdmin);
    RepairComment addComment(Long orderId, Long userId, RepairCommentCreateRequest request);
}
