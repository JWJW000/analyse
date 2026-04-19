package com.ethicssra.service;

import com.ethicssra.domain.SystemConfig;
import com.ethicssra.repository.SystemConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

    public SystemConfigService(SystemConfigRepository systemConfigRepository) {
        this.systemConfigRepository = systemConfigRepository;
    }

    public Map<String, String> all() {
        Map<String, String> m = new HashMap<>();
        systemConfigRepository.findAll().forEach(c -> m.put(c.getConfigKey(), c.getConfigValue()));
        return m;
    }

    @Transactional
    public void put(String key, String value) {
        SystemConfig c = new SystemConfig();
        c.setConfigKey(key);
        c.setConfigValue(value);
        systemConfigRepository.save(c);
    }
}
