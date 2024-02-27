package org.airsonic.player.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "airsonic.cue")
public class AirsonicCueConfig {

    // properties
    private final boolean enabled;
    private final boolean hideIndexedFiles;

    @ConstructorBinding
    public AirsonicCueConfig(
        boolean enabled,
        boolean hideIndexedFiles) {
        this.enabled = enabled;
        this.hideIndexedFiles = enabled && hideIndexedFiles;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isHideIndexedFiles() {
        return hideIndexedFiles;
    }
}
