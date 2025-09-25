package com.app84soft.check_in.services;

import com.app84soft.check_in.component.Translator;
import com.app84soft.check_in.dto.constant.RoleType;
import com.app84soft.check_in.entities.user.User;
import com.app84soft.check_in.exceptions.BusinessException;
import com.app84soft.check_in.security.SecurityContexts;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;

@Log4j2
public class BaseService {

    protected User getUser() {
        try {
            return (User) SecurityContexts.getContext().getData();
        } catch (Exception e) {
            log.error("getUser", e);
        }
        throw new BusinessException(Translator.toLocale("invalid_permission"), HttpStatus.FORBIDDEN);
    }

//    protected Admin getAdmin(RoleType... roleTypes) {
//        Admin admin = getAdmin();
//        if (roleTypes == null || roleTypes.length == 0) {
//            return admin;
//        }
//        if (admin.getRoleType() == null) {
//            return admin;
//        }
//        for (RoleType role : roleTypes) {
//            if (admin.getRoleType() == role) {
//                return admin;
//            }
//        }
//        throw new BusinessException(Translator.toLocale("invalid_permission"), HttpStatus.UNAUTHORIZED);
//    }
//
//    protected Admin getAdmin() {
//        try {
//            return (Admin) SecurityContexts.getContext().getData();
//        } catch (Exception e) {
//            log.error(e.getMessage());
//            throw new BusinessException("Unauthorized", HttpStatus.UNAUTHORIZED);
//        }
//    }
}
