package com.activity.manage.mapper;

import com.activity.manage.pojo.entity.Registration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RegistrationMapper {
    void insert(Registration registration);

    void checkin(Registration registration);

    List<String> selectPhoneByActivity(Long id);

    List<Long> selectActivityIdByPhone(String phone);

    void delete(@Param("activityId") Long activityId, @Param("phone") String phone);
}
