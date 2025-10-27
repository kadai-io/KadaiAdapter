package io.kadai.adapter.configuration.health;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "management.health.kadai-adapter.plugin")
public class PluginHealthConfigurationProperties
    extends CompositeHealthContributorConfigurationProperties {}
