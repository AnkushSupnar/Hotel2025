package com.frontend;

import com.frontend.view.FxmlView;
import com.frontend.view.StageManager;
import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;

@SpringBootApplication(scanBasePackages = {
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
})
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
		// Initialize MaterialFX theme
		initializeMaterialFXTheme();

		stageManager = springContext.getBean(StageManager.class, stage);
		displayInitialScene();
	}

	/**
	 * Initialize MaterialFX theming system
	 */
	private void initializeMaterialFXTheme() {
		try {
			UserAgentBuilder.builder()
					.themes(JavaFXThemes.MODENA)
					.themes(MaterialFXStylesheets.forAssemble(true))
					.setDeploy(true)
					.setResolveAssets(true)
					.build()
					.setGlobal();
			System.out.println("MaterialFX theme initialized successfully");
		} catch (Exception e) {
			System.err.println("Error initializing MaterialFX theme: " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {
		springContext.close();
	}

	protected void displayInitialScene() {
		// For now, always show login screen
		// Later, this will check with the backend API for shop configuration
		stageManager.switchScene(FxmlView.LOGIN);
		// stageManager.switchScene(FxmlView.BILLING);
	}

	private ConfigurableApplicationContext bootstrapSpringApplicationContext() {
		SpringApplicationBuilder builder = new SpringApplicationBuilder(Main.class);
		String[] args = getParameters().getRaw().stream().toArray(String[]::new);
		builder.headless(false); // needed for TestFX integration testing or eles will get a
									// java.awt.HeadlessException during tests
		return builder.run(args);
	}
}