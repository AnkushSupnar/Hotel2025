package com.frontend;

import com.frontend.service.RoleService;
import com.frontend.view.FxmlView;
import com.frontend.view.SplashScreen;
import com.frontend.view.StageManager;
import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

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
		"com.frontend.print",
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
	public void init() {
		// Preload bundled Kiran font from resources BEFORE Spring context
		// This is fast and does not require Spring
		preloadBundledFont();
	}

	@Override
	public void start(Stage stage) {
		// Show splash screen immediately
		SplashScreen splash = new SplashScreen();
		splash.show();

		// Bootstrap Spring context in background with real-time progress
		Task<ConfigurableApplicationContext> bootstrapTask = createBootstrapTask(splash);

		bootstrapTask.setOnSucceeded(event -> {
			springContext = bootstrapTask.getValue();

			// Initialize MaterialFX theme
			initializeMaterialFXTheme();

			// Initialize default role permissions in background (non-blocking)
			new Thread(this::initializeRolePermissions, "role-init").start();

			stageManager = springContext.getBean(StageManager.class, stage);

			// Fade out splash, then show login
			FadeTransition fadeOut = new FadeTransition(Duration.millis(400), splash.getStage().getScene().getRoot());
			fadeOut.setFromValue(1.0);
			fadeOut.setToValue(0.0);
			fadeOut.setOnFinished(e -> {
				splash.getStage().close();
				displayInitialScene();
			});
			fadeOut.play();
		});

		bootstrapTask.setOnFailed(event -> {
			Throwable ex = bootstrapTask.getException();
			System.err.println("Failed to start application: " + ex.getMessage());
			ex.printStackTrace();

			splash.updateProgress(0, "Startup failed: " + ex.getMessage());

			// Show error alert
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle("Startup Error");
			alert.setHeaderText("Application failed to start");
			alert.setContentText(ex.getMessage());
			alert.showAndWait();

			Platform.exit();
		});

		Thread bootstrapThread = new Thread(bootstrapTask, "spring-bootstrap");
		bootstrapThread.setDaemon(true);
		bootstrapThread.start();
	}

	/**
	 * Create a Task that bootstraps the Spring context with real-time progress
	 * updates driven by Spring lifecycle events.
	 */
	private Task<ConfigurableApplicationContext> createBootstrapTask(SplashScreen splash) {
		return new Task<>() {
			@Override
			protected ConfigurableApplicationContext call() throws Exception {
				splash.updateProgress(0.1, "Loading fonts...");
				Thread.sleep(300);

				splash.updateProgress(0.2, "Starting application...");

				SpringApplicationBuilder builder = new SpringApplicationBuilder(Main.class);
				String[] args = getParameters().getRaw().stream().toArray(String[]::new);
				builder.headless(false);

				// Register listeners for real-time progress during Spring bootstrap
				builder.listeners(
						(ApplicationListener<ApplicationEnvironmentPreparedEvent>) event ->
								splash.updateProgress(0.35, "Loading configuration..."),
						(ApplicationListener<ApplicationContextInitializedEvent>) event ->
								splash.updateProgress(0.5, "Initializing context..."),
						(ApplicationListener<ApplicationPreparedEvent>) event ->
								splash.updateProgress(0.65, "Initializing database..."),
						(ApplicationListener<ApplicationStartedEvent>) event ->
								splash.updateProgress(0.85, "Preparing user interface...")
				);

				ConfigurableApplicationContext context = builder.run(args);

				splash.updateProgress(1.0, "Ready!");
				Thread.sleep(200);

				return context;
			}
		};
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
		if (springContext != null) {
			springContext.close();
		}
	}

	protected void displayInitialScene() {
		// For now, always show login screen
		// Later, this will check with the backend API for shop configuration
		stageManager.switchScene(FxmlView.LOGIN);
		// stageManager.switchScene(FxmlView.BILLING);
	}
}
