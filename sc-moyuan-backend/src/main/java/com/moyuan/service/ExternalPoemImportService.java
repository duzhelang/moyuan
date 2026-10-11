package com.moyuan.service;

import java.util.List;
import java.util.Map;

/**
 * 外部诗词收录服务
 *
 * <p>统一封装"外部诗词落库"逻辑，供两类场景复用：</p>
 * <ul>
 *     <li>显式导入（用户主动触发，如诗人详情页）：不存在则新建并直接上架；</li>
 *     <li>后台静默收录（列表页外部搜索、每日诗词）：不存在则新建为"待审核"，不直接公开。</li>
 * </ul>
 *
 * <p>两种模式共用同一套"标题 + 诗人"查重规则，避免重复入库。</p>
 */
public interface ExternalPoemImportService {

    /**
     * 显式导入：不存在则新建并直接上架（status=1, auditStatus=1）。
     * 若已存在同题同作者的外部落库记录，则补齐作者/朝代信息，并将其提升为已上架（兼容历史行为）。
     *
     * @return {id, imported}：id 为诗词主键；imported 表示本次是否产生了新的可见记录
     */
    Map<String, Object> importExternal(String title, String content, String author, String dynasty);

    /**
     * 后台静默收录（best-effort）：不存在则新建为待审核（status=0, auditStatus=0）；已存在则跳过。
     * 单条失败不会中断整体流程，也不会向调用方抛出异常。
     *
     * @param poems 外部诗词列表，元素需包含 title / content / author / dynasty 字段
     * @return 本次新收录的条数
     */
    int collectPending(List<Map<String, Object>> poems);
}
