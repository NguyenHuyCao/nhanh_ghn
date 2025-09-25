package com.app84soft.check_in.services.config;

import com.app84soft.check_in.entities.config.ConfigModel;
import com.app84soft.check_in.entities.config.constant.ConfigKey;

import java.util.Map;

public interface ConfigService {

    ConfigModel getConfig(ConfigKey configKey) throws Exception;
}
