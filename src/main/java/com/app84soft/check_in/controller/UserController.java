package com.app84soft.check_in.controller;
import com.app84soft.check_in.dto.request.IdsRequest;
import com.app84soft.check_in.dto.request.user.UpdateUserRequest;
import com.app84soft.check_in.dto.request.user.UserCheckInReq;
import com.app84soft.check_in.dto.response.BaseResponse;
import com.app84soft.check_in.dto.response.user.UserCheckInHistoryRes;
import com.app84soft.check_in.dto.response.user.UserCheckInRes;
import com.app84soft.check_in.dto.response.user.UserListRes;
import com.app84soft.check_in.entities.user.User;
import com.app84soft.check_in.services.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/")
@RequiredArgsConstructor
public class UserController {

}
