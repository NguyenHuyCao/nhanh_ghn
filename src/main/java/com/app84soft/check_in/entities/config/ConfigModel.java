package com.app84soft.check_in.entities.config;


import com.app84soft.check_in.entities.BaseEntity;
import com.app84soft.check_in.entities.config.constant.ConfigKey;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Entity
@Table(name = "configs")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConfigModel extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "`key`", nullable = false)
    ConfigKey key;

    @Lob
    @Column(columnDefinition = "TEXT") 
    String value;

    String description;

    int status;

}