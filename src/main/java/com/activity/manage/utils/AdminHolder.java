package com.activity.manage.utils;

import com.activity.manage.pojo.dto.AdministratorDTO;

public class AdminHolder {
    public static final ThreadLocal<AdministratorDTO> tl = new ThreadLocal<>();

    public static void saveAdmin(AdministratorDTO administratorDTO) {
        tl.set(administratorDTO);
    }

    public static AdministratorDTO getAdmin() {
        return tl.get();
    }

    public static void removeAdmin() {
        tl.remove();
    }
}
