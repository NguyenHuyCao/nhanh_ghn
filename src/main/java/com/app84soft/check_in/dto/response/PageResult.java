package com.app84soft.check_in.dto.response;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
public class PageResult<T> {
    private int page;
    private int limit;
    private long total;
    private int totalPages;
    private List<T> items;
}
