package com.frontend.config;

import com.frontend.logging.ExceptionWriter;
import com.frontend.view.StageManager;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ResourceBundle;

@Configuration
@EnableJpaRepositories(basePackages = "com.frontend.repository")
@EntityScan(basePackages = "com.frontend.entity")
public class AppJavaConfig {

    @Autowired
    @Lazy
    SpringFXMLLoader springFXMLLoader;

    @Bean
    @Scope("prototype")
    public ExceptionWriter exceptionWriter() {
        return new ExceptionWriter(new StringWriter());
    }

    @Bean
    public ResourceBundle resourceBundle() {
        return ResourceBundle.getBundle("Bundle");
    }

    @Bean
    @Lazy(value = true) // Stage only created after Spring context bootstrap
    public Stage stage() {
        return new Stage(); // Create a new Stage instance
    }

    @Bean
    @Lazy(value = true) // Stage only created after Spring context bootstrap
    public StageManager stageManager(Stage stage) throws IOException {
        return new StageManager(springFXMLLoader, stage);
    }
}