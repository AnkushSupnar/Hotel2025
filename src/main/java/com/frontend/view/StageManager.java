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

        // All screens open in full screen mode
        showFullScreen();

        if(!title.equals("Login"))
        {
            primaryStage.setOnCloseRequest(e->e.consume());
        } else {
            // Allow closing for Login screen
            primaryStage.setOnCloseRequest(null);
        }

        try {
            primaryStage.show();
        } catch (Exception exception) {
            logAndExit ("Unable to show scene for title" + title,  exception);
        }
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