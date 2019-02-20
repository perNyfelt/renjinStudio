package se.alipsa.ride.inout.viewer;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.*;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static se.alipsa.ride.Constants.KEY_CODE_COPY;

public class ViewTab extends Tab {

  TabPane viewPane;

  public ViewTab() {
    setText("Viewer");
    viewPane = new TabPane();
    setContent(viewPane);
  }

  public void viewTable(List<String> colList, List<List<Object>> rowList, String... title) {
    NumberFormat numberFormatter = NumberFormat.getInstance();
    numberFormatter.setGroupingUsed(false);

    TableView<List<String>> tableView = new TableView<>();
    tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    tableView.setOnKeyPressed(event -> {
      if (KEY_CODE_COPY.match(event)) {
        copySelectionToClipboard(tableView);
      }
    });

    for (int i = 0; i < colList.size(); i++) {
      final int j = i;
      String colName = colList.get(i);
      TableColumn<List<String>, String> col = new TableColumn<>(colName);
      tableView.getColumns().add(col);
      col.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(j)));
    }
    ObservableList<List<String>> data = FXCollections.observableArrayList();
    for (List row: rowList) {
      List<String> obsRow = new ArrayList<>();
      for (Object obj : row) {
        if (obj instanceof Number) {
          obsRow.add(numberFormatter.format(obj));
        } else {
          obsRow.add(obj + "");
        }
      }
      data.add(obsRow);
    }
    tableView.setItems(data);
    Tab tab = new Tab();
    String tabTitle = " (" + rowList.size() + ")";
    if (title.length > 0) {
      tabTitle = title[0] + tabTitle;
    }
    tab.setText(tabTitle);
    viewPane.getTabs().add(tab);
    tab.setContent(tableView);

    SingleSelectionModel<Tab> selectionModel = viewPane.getSelectionModel();
    selectionModel.select(tab);
  }

  @SuppressWarnings("rawtypes")
  private void copySelectionToClipboard(final TableView<?> table) {
    final Set<Integer> rows = new TreeSet<>();
    for (final TablePosition tablePosition : table.getSelectionModel().getSelectedCells()) {
      rows.add(tablePosition.getRow());
    }
    final StringBuilder strb = new StringBuilder();
    boolean firstRow = true;
    for (final Integer row : rows) {
      if (!firstRow) {
        strb.append('\n');
      }
      firstRow = false;
      boolean firstCol = true;
      for (final TableColumn<?, ?> column : table.getColumns()) {
        if (!firstCol) {
          strb.append('\t');
        }
        firstCol = false;
        final Object cellData = column.getCellData(row);
        strb.append(cellData == null ? "" : cellData.toString());
      }
    }
    final ClipboardContent clipboardContent = new ClipboardContent();
    clipboardContent.putString(strb.toString());
    Clipboard.getSystemClipboard().setContent(clipboardContent);
  }
}