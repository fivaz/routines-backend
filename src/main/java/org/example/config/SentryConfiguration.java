package org.example.config;

import io.sentry.spring.jakarta.EnableSentry;
import org.springframework.context.annotation.Configuration;

@EnableSentry(
        dsn = "https://017db1606244b7e730f52a4d6977350b@o4508857555550208.ingest.de.sentry.io/4508857606996048",
        sendDefaultPii = true // Enables collection of request headers, user IP, etc.
)
@Configuration
public class SentryConfiguration {
}
