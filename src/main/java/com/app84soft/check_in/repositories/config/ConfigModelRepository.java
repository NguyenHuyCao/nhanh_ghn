package com.app84soft.check_in.repositories.config;

import com.app84soft.check_in.entities.config.ConfigModel;
import com.app84soft.check_in.entities.config.constant.ConfigKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfigModelRepository extends JpaRepository<ConfigModel, Integer>, ConfigModelRepositoryCustom {
    ConfigModel findConfigModelByKey(ConfigKey key);
    
}
