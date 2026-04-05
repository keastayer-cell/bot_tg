package com.example.bot;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource(Environment env) throws URISyntaxException {
        String databaseUrl = env.getProperty("DATABASE_URL");
        if (databaseUrl != null && !databaseUrl.isBlank()) {
            URI dbUri = new URI(databaseUrl);
            String[] userInfo = dbUri.getUserInfo() != null ? dbUri.getUserInfo().split(":", 2) : new String[] {"", ""};
            String username = userInfo[0];
            String password = userInfo.length > 1 ? userInfo[1] : "";
            String url = String.format("jdbc:postgresql://%s:%d%s", dbUri.getHost(), dbUri.getPort(), dbUri.getPath());
            if (dbUri.getQuery() != null && !dbUri.getQuery().isEmpty()) {
                url += "?" + dbUri.getQuery();
            }

            return DataSourceBuilder.create()
                    .url(url)
                    .username(username)
                    .password(password)
                    .driverClassName("org.postgresql.Driver")
                    .build();
        }

        String fallbackUrl = env.getProperty("spring.datasource.url", "jdbc:h2:file:./data/h2db;AUTO_SERVER=TRUE;DB_CLOSE_ON_EXIT=FALSE");
        String fallbackUsername = env.getProperty("spring.datasource.username", "sa");
        String fallbackPassword = env.getProperty("spring.datasource.password", "");
        String fallbackDriver = env.getProperty("spring.datasource.driver-class-name", "org.h2.Driver");

        return DataSourceBuilder.create()
                .url(fallbackUrl)
                .username(fallbackUsername)
                .password(fallbackPassword)
                .driverClassName(fallbackDriver)
                .build();
    }
}