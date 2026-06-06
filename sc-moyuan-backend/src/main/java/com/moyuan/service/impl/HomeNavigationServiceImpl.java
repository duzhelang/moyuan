package com.moyuan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyuan.entity.HomeNavigation;
import com.moyuan.mapper.HomeNavigationMapper;
import com.moyuan.service.HomeNavigationService;
import org.springframework.stereotype.Service;

@Service
public class HomeNavigationServiceImpl extends ServiceImpl<HomeNavigationMapper, HomeNavigation> implements HomeNavigationService {
}
