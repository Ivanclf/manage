package com.activity.manage.mapper;

import com.activity.manage.pojo.entity.Administrator;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminMapper {
    Administrator loginById(Administrator administrator);

    Administrator loginByName(Administrator administrator);
}
