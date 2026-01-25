package com.frontend;

import com.frontend.service.RoleService;
import com.frontend.view.FxmlView;
import com.frontend.view.StageManager;
import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
import javafx.application.Application;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.io.InputStream;

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
		"com.frontend.entity",
		"com.frontend.print",
		"com.frontend.enums",
		"com.frontend.api"
})
public class Main extends Application {
	private ConfigurableApplicationContext springContext;
	protected StageManager stageManager;

	public static void main(String[] args) {
		// Set font rendering properties BEFORE launching JavaFX
		configureFontRendering();
		Application.launch(args);
	}

	/**
	 * Configure system properties for better font rendering in JavaFX
	 * These must be set before JavaFX initialization
	 */
	private static void configureFontRendering() {
		// Enable LCD text rendering for sharper fonts (like Swing)
		System.setProperty("prism.lcdtext", "true");

		// Enable font smoothing
		System.setProperty("prism.text", "t2k");

		// Use DirectWrite on Windows for better font rendering
		System.setProperty("prism.fontcache", "true");

		// Enable subpixel positioning for smoother text
		System.setProperty("prism.subpixeltext", "true");

		// Set DPI awareness
		System.setProperty("prism.allowhidpi", "true");

		// Use hardware acceleration
		System.setProperty("prism.order", "d3d,sw");

		System.out.println("Font rendering properties configured for better quality");
	}

	@Override
	public void init() throws IOException {
		// Preload bundled Kiran font from resources BEFORE Spring context
		preloadBundledFont();

		springContext = bootstrapSpringApplicationContext();
	}

	@Override
	public void start(Stage stage) throws Exception {
		// Initialize MaterialFX theme
		initializeMaterialFXTheme();

		// Initialize default role permissions
		initializeRolePermissions();

		stageManager = springContext.getBean(StageManager.class, stage);
		displayInitialScene();
	}

	/**
	 * Initialize default role permissions on application startup
	 * Creates predefined roles (ADMIN, MANAGER, CASHIER, CAPTAIN, WAITER, USER) if they don't exist
	 */
	private void initializeRolePermissions() {
		try {
			RoleService roleService = springContext.getBean(RoleService.class);
			roleService.initializeDefaultRolePermissions();
			System.out.println("Default role permissions initialized successfully");
		} catch (Exception e) {
			System.err.println("Error initializing role permissions: " + e.getMessage());
			// Non-critical error - application can continue
		}
	}

	/**
	 * Preload bundled Kiran font from resources
	 * This ensures the font is registered with JavaFX before any UI is created
	 */
	private void preloadBundledFont() {
		try {
			// Load font from classpath resources
			InputStream fontStream = getClass().getResourceAsStream("/fonts/kiran.ttf");
			if (fontStream != null) {
				Font loadedFont = Font.loadFont(fontStream, 12);
				fontStream.close();

				if (loadedFont != null) {
					System.out.println("Bundled Kiran font preloaded successfully:");
					System.out.println("  Family: " + loadedFont.getFamily());
					System.out.println("  Name: " + loadedFont.getName());

					// Verify font is registered
					boolean registered = Font.getFamilies().contains(loadedFont.getFamily());
					System.out.println("  Registered in JavaFX: " + registered);
				} else {
					System.err.println("Failed to load bundled Kiran font");
				}
			} else {
				System.out.println("Bundled font not found at /fonts/kiran.ttf");
			}
		} catch (Exception e) {
			System.err.println("Error preloading bundled font: " + e.getMessage());
		}
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