package com.app84soft.check_in.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class IdsRequest {

    @NotNull(message = "ids must be required")
    @Size(min = 1)
    private List<Integer> ids;

}
