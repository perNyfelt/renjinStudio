package se.alipsa.ride.inout;

import static se.alipsa.ride.Constants.KEY_CODE_COPY;
import static se.alipsa.ride.utils.GitUtils.asRelativePath;

import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeItem.TreeModificationEvent;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Paint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import se.alipsa.ride.Ride;
import se.alipsa.ride.code.CodeComponent;
import se.alipsa.ride.utils.Alerts;
import se.alipsa.ride.utils.ExceptionAlert;
import se.alipsa.ride.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Objects;

public class FileTree extends TreeView<FileItem> {

  private final String folderUrl = Objects.requireNonNull(FileUtils.getResourceUrl("image/folder.png")).toExternalForm();
  private final String fileUrl = Objects.requireNonNull(FileUtils.getResourceUrl("image/file.png")).toExternalForm();
  private final String WORKING_DIR_PREF = "FileTree.WorkingDir";
  private TreeItemComparator treeItemComparator = new TreeItemComparator();
  private Ride gui;
  private static Logger log = LogManager.getLogger(FileTree.class);
  private FileOpener fileOpener;
  private DynamicContextMenu menu;

  FileTree(Ride gui) {
    this.gui = gui;
    CodeComponent codeComponent = gui.getCodeComponent();
    fileOpener = new FileOpener(codeComponent);
    this.getStyleClass().add("fileTree");

    String currentPath = new File(getWorkingDirPref()).getAbsolutePath();
    File current = new File(currentPath);
    setWorkingDir(current);

    setRoot(createTree(current));

    sortTree(getRoot());

    getRoot().setExpanded(true);
    setCellFactory(treeView -> new TreeCell<FileItem>() {

      @Override
      protected void updateItem(FileItem item, boolean empty) {
        if (item != null) {
          setText(item.getText());
          setStyle(item.getStyle());
          setGraphic(getTreeItem().getGraphic());
        } else {
          setText("");
          setGraphic(null);
        }
        super.updateItem(item, empty);
      }
    });

    setOnKeyPressed(event -> {
      if (KEY_CODE_COPY.match(event)) {
        copySelectionToClipboard();
      }
    });

    setOnMouseClicked(this::handleClick);
    menu = new DynamicContextMenu(this);
    //setContextMenu(createContextMenu());
    addEventHandler(MouseEvent.MOUSE_RELEASED, e->{
      if (e.getButton() == MouseButton.SECONDARY) {
        TreeItem<FileItem> selected = getSelectionModel().getSelectedItem();
        //item is selected - this prevents fail when clicking on empty space
        if (selected!=null) {
          //open context menu on current screen position
          openContextMenu(selected, e.getScreenX(), e.getScreenY());
        }
      } else {
        //any other click cause hiding menu
        menu.hide();
      }
    });
  }

  private void openContextMenu(TreeItem<FileItem> item, double x, double y) {
    menu.setContext(item);
    menu.show(this, x, y);
  }


  File getRootDir() {
    return getRoot().getValue().getFile();
  }

  private String getWorkingDirPref() {
    return gui.getPrefs().get(WORKING_DIR_PREF, ".");
  }

  private void setWorkingDirPref(File dir) {
    gui.getPrefs().put(WORKING_DIR_PREF, dir.getAbsolutePath());
    setWorkingDir(dir);
  }

  private void setWorkingDir(File dir) {
    gui.getConsoleComponent().setWorkingDir(dir);
    System.setProperty("user.dir", dir.getAbsolutePath());
  }

  private TreeItem<FileItem> createTree(File file) {
    TreeItem<FileItem> root = buildTree(file);
    if (Objects.requireNonNull(root.getValue().file.list((dir, name) -> name.equalsIgnoreCase(".git"))).length > 0) {
      gitColorTree(root);
    }
    return root;
  }

  private void gitColorTree(TreeItem<FileItem> root) {
    try {
      File rootDir = root.getValue().getFile();
      Git git = Git.open(rootDir);
      Files.walk(rootDir.toPath())
         .filter(p -> !p.toFile().getName().equalsIgnoreCase(".git"))
         .forEach( p -> {
           File file = p.toFile();
           if (file.isDirectory()) {
             try {
               Status status = git.status().addPath(asRelativePath(file, rootDir)).call();
               status.getUntracked();
             } catch (GitAPIException e) {
               throw new RuntimeException(e);
             }
           }
         });
    } catch (Exception e) {
      log.error("Failed to set git colors", e);
      ExceptionAlert.showAlert("Failed to set git colors", e);
    }
  }

  private TreeItem<FileItem> buildTree(File file) {
    TreeItem<FileItem> item = new TreeItem<>(new FileItem(file));
    File[] children = file.listFiles();
    if (children != null) {
      for (File child : children) {
        item.getChildren().add(buildTree(child));
      }
      item.setGraphic(new ImageView(folderUrl));
    } else {
      setLeafProperties(item);
    }
    return item;
  }

  private void setLeafProperties(TreeItem<FileItem> item) {
    item.setGraphic(new ImageView(fileUrl));
    ChangeListener<String> fillListener = (obs, oldName, newName) -> {
      TreeModificationEvent<FileItem> event = new TreeModificationEvent<>(TreeItem.valueChangedEvent(), item);
      log.info("item {} changed color", item.getValue());
      Event.fireEvent(item, event);
    };
    FileItem fileItem = item.getValue();
    fileItem.addListener(fillListener);
  }

  private void handleClick(MouseEvent event) {
    if (event.getClickCount() == 2) {
      TreeItem<FileItem> item = getSelectionModel().getSelectedItem();
      if (item == null) {
        return;
      }
      File file = item.getValue().getFile();
      if (file.isDirectory()) {
        return;
      }
      fileOpener.openFile(file);
    }
  }

  void addTreeNode(File file) {
    TreeItem<FileItem> item = findTreeViewItem(this.getRoot(), file.getParentFile());
    if (item == null) {
      log.info("File saved outside of current working dir");
      return;
    }
    TreeItem<FileItem> fileItem = new TreeItem<>(new FileItem(file));
    addTreeNode(item, fileItem);
  }

  private void addTreeNode(TreeItem<FileItem> dirItem, TreeItem<FileItem> fileItem) {
    if (fileItem.getValue().getFile().isDirectory()) {
      fileItem.setGraphic(new ImageView(folderUrl));
    } else {
      setLeafProperties(fileItem);
    }
    dirItem.getChildren().add(fileItem);
    dirItem.getChildren().sort(treeItemComparator);
    dirItem.setExpanded(true);
  }

  void refresh(File dir) {
    if (dir == null) {
      Alerts.warn("Dir is missing (null)", "Cannot refresh file tree when dir specified is missing");
      return;
    }
    if (dir.isFile()) {
      dir = dir.getParentFile();
    }
    setRoot(createTree(dir));
    sortTree(getRoot());
    getRoot().setExpanded(true);
    setWorkingDirPref(dir);
    menu = new DynamicContextMenu(this);
  }

  public void refresh() {
    File current = getRoot().getValue().getFile();
    setRoot(createTree(current));
    sortTree(getRoot());
    getRoot().setExpanded(true);
    menu = new DynamicContextMenu(this);
  }

  private TreeItem<FileItem> findTreeViewItem(TreeItem<FileItem> item, File value) {
    if (item != null && item.getValue().getFile().equals(value)) {
      return item;
    }
    if (item != null) {
      for (TreeItem<FileItem> child : item.getChildren()) {
        TreeItem<FileItem> s = findTreeViewItem(child, value);
        if (s != null) {
          return s;
        }
      }
    }
    return null;
  }

  private void sortTree(TreeItem<FileItem> item) {
    ObservableList<TreeItem<FileItem>> children = item.getChildren();
    children.sort(treeItemComparator);
    for (TreeItem<FileItem> child : item.getChildren()) {
      sortTree(child);
    }
  }

  private static class TreeItemComparator implements Comparator<TreeItem<FileItem>>, Serializable {

    private static final long serialVersionUID = -7749561517249799967L;

    @Override
    public int compare(TreeItem<FileItem> fileTreeItem, TreeItem<FileItem> t1) {
      return fileTreeItem.getValue().getFile().getName().compareTo(t1.getValue().getFile().getName());
    }
  }

  void copySelectionToClipboard() {
    TreeItem<FileItem> treeItem = getSelectionModel().getSelectedItem();
    final ClipboardContent clipboardContent = new ClipboardContent();
    String value = treeItem.getValue().getFile().getName();
    clipboardContent.putString(value);
    Clipboard.getSystemClipboard().setContent(clipboardContent);
  }
}
