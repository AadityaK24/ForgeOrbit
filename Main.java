import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application
{
    @Override
    public void start(Stage stage)
    {
        MissionDashboard dashboard = new MissionDashboard();
        Scene scene = new Scene(dashboard, 1600, 960);

        stage.setTitle("ForgeOrbit V5 | Time-Stepped Mission Simulation");
        stage.setMinWidth(1280);
        stage.setMinHeight(820);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> dashboard.dispose());
        stage.show();
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
