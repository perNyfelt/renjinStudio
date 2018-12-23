package se.alipsa.ride.inout;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import org.renjin.primitives.packaging.DllInfo;
import org.renjin.sexp.StringVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.alipsa.ride.Ride;
import se.alipsa.ride.inout.plot.PlotCanvas;

import java.io.File;

public class InoutComponent extends TabPane {

    FileTree fileTree;
    Tab plots;
    TabPane imageTabpane;
    Tab packages;
    Ride gui;

    Logger log = LoggerFactory.getLogger(InoutComponent.class);

    public InoutComponent(Ride gui) {

        this.gui = gui;

        fileTree = new FileTree(gui.getCodeComponent());

        Tab filesTab = new Tab();
        filesTab.setText("Files");

        BorderPane filesPane = new BorderPane();
        FlowPane filesButtonPane = new FlowPane();

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(this::handleRefresh);
        filesButtonPane.getChildren().add(refreshButton);

        Button changeDirButton = new Button("Change dir");
        changeDirButton.setOnAction(this::handleChangeDir);
        filesButtonPane.getChildren().add(changeDirButton);

        filesPane.setTop(filesButtonPane);
        filesPane.setCenter(fileTree);
        filesTab.setContent(filesPane);
        getTabs().add(filesTab);

        plots = new Tab();
        plots.setText("Plots");
        imageTabpane = new TabPane();
        plots.setContent(imageTabpane);

        getTabs().add(plots);

        packages = new Tab();
        packages.setText("Packages");
        packages.setContent(new TextArea());

        getTabs().add(packages);

        Tab help = new Tab();
        help.setText("Help");

        getTabs().add(help);

        Tab viewer = new Tab();
        viewer.setText("Viewer");

        getTabs().add(viewer);
    }

    private void handleChangeDir(ActionEvent actionEvent) {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setInitialDirectory(gui.getInoutComponent().getRootDir());
        File selectedDirectory = dirChooser.showDialog(gui.getStage());

        if (selectedDirectory == null){
            log.info("No Directory selected");
        } else {
            fileTree.refresh(selectedDirectory);
        }
    }

    private void handleRefresh(ActionEvent actionEvent) {
        fileTree.refresh();
    }

    public void fileAdded(File file) {
        fileTree.addFile(file);
    }

    public File getRootDir() {
        return fileTree.getRootDir();
    }

    public void plot(Node node, String... title) {
        Tab tab = new Tab();
        imageTabpane.getTabs().add(tab);
        if (title.length > 0) {
            tab.setText(title[0]);
        }
        tab.setContent(node);
        SingleSelectionModel<Tab> selectionModel = getSelectionModel();
        selectionModel.select(plots);

        SingleSelectionModel<Tab> imageTabsSelectionModel = imageTabpane.getSelectionModel();
        imageTabsSelectionModel.select(tab);
    }

    public void plot(Image img, String... title) {
        ImageView node = new ImageView(img);
        plot(node, title);
    }

    public void setPackages(StringVector pkgs) {
        TextArea ta = (TextArea)packages.getContent();
        ta.clear();
        if (pkgs == null) {
            return;
        }
        for (String pkg: pkgs) {
            ta.appendText(pkg + "\n");
        }
    }

    @Override
    public String toString() {
        return "The Ride InOutComponent";
    }
}