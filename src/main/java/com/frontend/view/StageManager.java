package com.frontend.view;

import com.frontend.config.SpringFXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.Getter;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;
@Component
public class StageManager {
    private static final Logger LOG = getLogger(StageManager.class);
    private final Stage primaryStage;
    @Getter
    private final SpringFXMLLoader springFXMLLoader;
    private Parent viewRootNodeHierarchy;

    public StageManager(SpringFXMLLoader springFXMLLoader,Stage stage)
    {
        this.springFXMLLoader = springFXMLLoader;
        this.primaryStage = stage;
    }
    public void switchScene1(final FxmlView view)
    {
        viewRootNodeHierarchy = loadViewNodeHierarchy(view.getFxmlFile());

        show(viewRootNodeHierarchy,view.getTitle());
    }

    public void switchScene(final FxmlView view) {
        System.out.println("Switching to scene: " + view.getTitle());
        viewRootNodeHierarchy = loadViewNodeHierarchy(view.getFxmlFile());

        if (viewRootNodeHierarchy != null) {
            show(viewRootNodeHierarchy, view.getTitle());
            System.out.println("Scene switched successfully to: " + view.getTitle());
        } else {
            System.out.println("Failed to load view: " + view.getFxmlFile());
        }
    }


    public void showFullScreen()
    {
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();
        primaryStage.setX(bounds.getMinX());
        primaryStage.setY(bounds.getMinY());
        primaryStage.setWidth(bounds.getWidth());
        primaryStage.setHeight(bounds.getHeight());

    }
    public Stage getPrimaryStage()
    {
        return primaryStage;
    }
    public Parent getParent()
    {
        return viewRootNodeHierarchy;
    }
    private void show(Parent rootnode, String title) {
        Scene scene = prepareScene(rootnode);
        primaryStage.setTitle(title);
        primaryStage.setScene(scene);

        if(title.equals("Login")) {
            // Login screen: centered, proportional to screen size
            showLoginCentered();
            // Allow closing for Login screen
            primaryStage.setOnCloseRequest(null);
        } else {
            // All other screens open in full screen mode
            showFullScreen();
            primaryStage.setOnCloseRequest(e->e.consume());
        }

        try {
            primaryStage.show();
        } catch (Exception exception) {
            logAndExit ("Unable to show scene for title" + title,  exception);
        }
    }

    public void showCentered(double width, double height) {
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();

        primaryStage.setWidth(width);
        primaryStage.setHeight(height);

        // Center the window on screen
        double centerX = bounds.getMinX() + (bounds.getWidth() - width) / 2;
        double centerY = bounds.getMinY() + (bounds.getHeight() - height) / 2;

        primaryStage.setX(centerX);
        primaryStage.setY(centerY);
    }

    /**
     * Show login screen centered with proportional sizing based on screen dimensions.
     * Adapts to different screen sizes while maintaining proper aspect ratio.
     */
    public void showLoginCentered() {
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();

        double screenWidth = bounds.getWidth();
        double screenHeight = bounds.getHeight();

        // Calculate proportional size (70% of screen, with min/max bounds)
        double width = Math.min(Math.max(screenWidth * 0.7, 850), 1200);
        double height = Math.min(Math.max(screenHeight * 0.8, 650), 900);

        // Ensure minimum height for login card content
        if (height < 700 && screenHeight >= 800) {
            height = 700;
        }

        primaryStage.setWidth(width);
        primaryStage.setHeight(height);

        // Center the window on screen
        double centerX = bounds.getMinX() + (screenWidth - width) / 2;
        double centerY = bounds.getMinY() + (screenHeight - height) / 2;

        primaryStage.setX(centerX);
        primaryStage.setY(centerY);

        System.out.println("Login screen size: " + width + "x" + height + " (Screen: " + screenWidth + "x" + screenHeight + ")");
    }

    private Scene prepareScene(Parent rootnode) {
        Scene scene = primaryStage.getScene();

        if (scene == null) {
            scene = new Scene(rootnode);
        }
        scene.setRoot(rootnode);
        return scene;
    }

    private Parent loadViewNodeHierarchy(String fxmlFile) {
        Parent rootNode=null;
        try{
            rootNode = springFXMLLoader.load(fxmlFile);
            Objects.requireNonNull(rootNode,"A Root FXML Node must not be null");
        }catch(Exception e)
        {
            logAndExit("unable to load fxml view: "+fxmlFile,e);
        }
        return rootNode;
    }

    private void logAndExit(String errorMsg, Exception e) {
        LOG.error(errorMsg, e, e.getCause());
    }

}