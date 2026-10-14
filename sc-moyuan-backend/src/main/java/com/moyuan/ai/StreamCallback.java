package com.moyuan.ai;

/**
 * 流式回调集合：把增量、错误、完成回调与取消信号聚合为一个对象，
 * 避免适配器方法参数过多。
 */
public interface StreamCallback {

    /** 每个增量片段（非空） */
    void onDelta(String delta);

    /** 出错（含上游 HTTP 非 2xx、解析异常等） */
    void onError(Throwable error);

    /** 正常结束（收到 [DONE] 或上游流自然结束） */
    void onComplete();

    /** 客户端是否已断开；为 true 时上游消费循环应立即停止 */
    boolean isCancelled();
}
