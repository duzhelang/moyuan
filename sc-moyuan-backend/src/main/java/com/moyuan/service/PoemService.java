package com.moyuan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moyuan.entity.Poem;

import java.util.List;

public interface PoemService extends IService<Poem> {
    IPage<Poem> getPoemList(int pageNum, int pageSize, Long dynastyId, Long poetId, Long categoryId, String keyword);
    IPage<Poem> getAdminPoemList(int pageNum, int pageSize, String keyword, Integer auditStatus);
    IPage<Poem> getModernPoems(int pageNum, int pageSize, Boolean isOriginal, Boolean hasCertifiedPoet, String sortBy, String keyword);
    /** 查询已入库的外文诗（poem_type='foreign'），按标题/作者模糊匹配，最多返回 limit 条 */
    List<Poem> listForeignPoems(String keyword, String author, int limit);
    Poem getPoemDetail(Long id);
    void toggleLike(Long userId, Long poemId);
    boolean isLike(Long userId, Long poemId);
    IPage<Poem> getFavorites(Long userId, int pageNum, int pageSize);
    void createPoemWithAudit(Poem poem);
    void auditPoem(Long poemId, Integer status, String reason);
    void clearAllCache();
}
