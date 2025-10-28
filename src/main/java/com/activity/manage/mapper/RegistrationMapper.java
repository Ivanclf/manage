package com.activity.manage.mapper;

import com.activity.manage.pojo.entity.Registration;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RegistrationMapper {
    void insert(Registration registration);
}
