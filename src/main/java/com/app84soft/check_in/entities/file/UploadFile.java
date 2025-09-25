package com.app84soft.check_in.entities.file;

import com.app84soft.check_in.component.converter.DataSensitiveConverter;
import com.app84soft.check_in.entities.BaseEntity;
import com.app84soft.check_in.entities.user.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "upload_files")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UploadFile extends BaseEntity {
    String name;

    @Convert(converter = DataSensitiveConverter.class)
    String originFilePath;

    @Convert(converter = DataSensitiveConverter.class)
    String originUrl;

    Long size;
    String contentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id")
    User uploadedBy;
}
