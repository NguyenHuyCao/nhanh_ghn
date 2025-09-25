package com.app84soft.check_in.dto.request.file;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeleteExcelRowsReq {
    @NotNull
    Integer courseId;

    @NotNull(message = "ids must be required")
    @Size(min = 1)
    private List<Integer> ids;

}
