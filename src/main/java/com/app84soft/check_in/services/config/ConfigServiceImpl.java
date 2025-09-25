package com.app84soft.check_in.services.config;

import com.app84soft.check_in.entities.config.ConfigModel;
import com.app84soft.check_in.entities.config.constant.ConfigKey;
import com.app84soft.check_in.repositories.config.ConfigModelRepository;
import com.app84soft.check_in.services.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
class ConfigServiceImpl extends BaseService implements ConfigService {

    @Autowired
    private ConfigModelRepository configModelRepository;


    @Override
    public ConfigModel getConfig(ConfigKey configKey) throws Exception {

        return configModelRepository.findConfigModelByKey(configKey);
    }

}
