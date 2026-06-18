package com.moyuan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyuan.entity.StaticPage;
import com.moyuan.mapper.StaticPageMapper;
import com.moyuan.service.StaticPageService;
import org.springframework.stereotype.Service;

@Service
public class StaticPageServiceImpl extends ServiceImpl<StaticPageMapper, StaticPage> implements StaticPageService {
}
