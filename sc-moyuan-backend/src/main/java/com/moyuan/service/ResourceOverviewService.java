package com.moyuan.service;

import java.util.Map;

/**
 * 管理端「资源分布」总览：内容 / AI / 存储 / 运行 四域聚合。
 * 所有数值均来自本项目自身数据源，不采集宿主机 CPU、内存等系统指标。
 */
public interface ResourceOverviewService {

    /**
     * 获取四域资源总览。
     * 结果在服务端缓存，默认 60 秒；前端每 5 分钟自动刷新一次。
     */
    Map<String, Object> overview();
}
