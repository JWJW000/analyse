package com.ethicssra.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "system_config")
public class SystemConfig {

    @Id
    @Column(name = "config_key", length = 128)
    private String configKey;

    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    public SystemConfig() {
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }
}
