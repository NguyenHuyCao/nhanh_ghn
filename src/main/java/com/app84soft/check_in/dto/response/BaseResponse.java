package com.app84soft.check_in.dto.response;

import com.app84soft.check_in.component.Translator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseResponse<T> implements Serializable {

    private int code = HttpStatus.OK.value();
    private T data;
    private String message = Translator.toLocale("label_success");
    @JsonProperty("total_record")
    private Long totalRecord;
    @JsonProperty("current_page")
    private Integer currentPage;
    private Map<String, Object> errors;

    public BaseResponse() {
    }

    public BaseResponse(String message) {
        this.message = message;
    }

    public BaseResponse(String message, int code) {
        this.message = message;
        this.code = code;
    }

    public BaseResponse(Map<String, Object> errors, String message) {
        this.errors = errors;
        this.message = message;
    }

    public BaseResponse(T data) {
        this.data = data;
    }

    public BaseResponse(T data, String message) {
        this.message = message;
        this.data = data;
    }

    public BaseResponse(T data, String message, int code) {
        this.message = message;
        this.data = data;
        this.code = code;
    }

    public BaseResponse(T data, long totalRecord) {
        this.data = data;
        this.totalRecord = totalRecord;
    }

    public BaseResponse(T data, long totalRecord, String message) {
        this.message = message;
        this.data = data;
        this.totalRecord = totalRecord;
    }

    public BaseResponse(T data, long totalRecord, Integer currentPage) {
        this.data = data;
        this.totalRecord = totalRecord;
        this.currentPage = currentPage;
    }

    public BaseResponse(long totalRecord) {
        this.totalRecord = totalRecord;
    }

}
