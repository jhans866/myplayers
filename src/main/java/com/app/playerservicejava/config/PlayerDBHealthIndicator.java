package com.app.playerservicejava.config;

import com.app.playerservicejava.repository.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("playerDB")  // ✅ Shows as "playerDB" in health response
public class PlayerDBHealthIndicator implements HealthIndicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerDBHealthIndicator.class);

    @Autowired
    private PlayerRepository playerRepository;

    @Override
    public Health health() {
        try {
            // ✅ Dummy query - checks DB is reachable
            long count = playerRepository.count();

            if (count > 0) {
                return Health.up()
                        .withDetail("database", "H2 in-memory")
                        .withDetail("playerCount", count)
                        .withDetail("status", "DB connection OK")
                        .build();
            } else {
                return Health.down()
                        .withDetail("database", "H2 in-memory")
                        .withDetail("playerCount", 0)
                        .withDetail("status", "DB empty - CSV may not have loaded")
                        .build();
            }
        } catch (Exception e) {
            LOGGER.error("Health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("database", "H2 in-memory")
                    .withDetail("error", e.getMessage())
                    .withDetail("status", "DB connection FAILED")
                    .build();
        }
    }
}
