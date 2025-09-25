package com.app84soft.check_in.services.user;

import com.app84soft.check_in.dto.request.IdsRequest;
import com.app84soft.check_in.dto.request.file.AddUserToExcelReq;
import com.app84soft.check_in.dto.request.file.DeleteExcelRowsReq;
import com.app84soft.check_in.dto.request.user.*;
import com.app84soft.check_in.dto.response.BaseResponse;
import com.app84soft.check_in.dto.response.file.AddUserToExcelRes;
import com.app84soft.check_in.dto.response.file.DeleteExcelRowsRes;
import com.app84soft.check_in.dto.response.file.ExcelUserRes;
import com.app84soft.check_in.dto.response.user.UserCheckInHistoryRes;
import com.app84soft.check_in.dto.response.user.UserCheckInRes;
import com.app84soft.check_in.dto.response.user.UserListRes;
import com.app84soft.check_in.dto.response.user.UserLoginRes;
import com.app84soft.check_in.entities.file.UploadFile;
import com.app84soft.check_in.entities.user.User;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface UserService {
}
