package com.moyuan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyuan.entity.VisitLog;
import com.moyuan.mapper.VisitLogMapper;
import com.moyuan.service.VisitLogService;
import org.springframework.stereotype.Service;

@Service
public class VisitLogServiceImpl extends ServiceImpl<VisitLogMapper, VisitLog> implements VisitLogService {
}
