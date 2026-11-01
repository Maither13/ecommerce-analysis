package com.example.ecommerce.module.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.ecommerce.module.task.entity.MrJobLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * MapReduce任务日志Mapper接口
 */
@Mapper
public interface MrJobLogMapper extends BaseMapper<MrJobLog> {
}
