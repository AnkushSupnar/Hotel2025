package com.frontend;

import com.frontend.view.FxmlView;
import com.frontend.view.StageManager;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;

@SpringBootApplication(
    scanBasePackages = {
        "com.frontend.config",
        "com.frontend.controller",
        "com.frontend.view",
        "com.frontend.common",
        "com.frontend.customUI",
        "com.frontend.logging",
        "com.frontend.service",
        "com.frontend.util",
        "com.frontend.repository",
        "com.frontend.entity"
    }
)
public class Main extends Application {
	private ConfigurableApplicationContext springContext;
	protected StageManager stageManager;

	public static void main(String[] args) {
		Application.launch(args);
	}

	@Override
	public void init() throws IOException {
		springContext = bootstrapSpringApplicationContext();
	}
	
	@Override
	public void start(Stage stage) throws Exception {
		stageManager = springContext.getBean(StageManager.class, stage);
		displayInitialScene();
	}
	@Override
	public void stop()
	{
		springContext.close();
	}
	protected void displayInitialScene() {
		// For now, always show login screen
		// Later, this will check with the backend API for shop configuration
		//stageManager.switchScene(FxmlView.LOGIN);
		stageManager.switchScene(FxmlView.BILLING);
	}
	private ConfigurableApplicationContext bootstrapSpringApplicationContext() {
		SpringApplicationBuilder builder = new SpringApplicationBuilder(Main.class);
		String[] args = getParameters().getRaw().stream().toArray(String[]::new);
		builder.headless(false); //needed for TestFX integration testing or eles will get a java.awt.HeadlessException during tests
		return builder.run(args);
	}
}