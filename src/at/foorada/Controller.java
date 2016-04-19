package at.foorada;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

public class Controller {

    public static final List<String> ALLOWED_FILETYPES = Arrays.asList("image/jpeg");

    @FXML private TextField directoryString;
    private File folder;
    @FXML private TextField heightTextField;
    @FXML private TextField widthTextField;
    @FXML private TextField percentageTextField;
    @FXML private RadioButton scalingHeightRdBtn;
    @FXML private RadioButton scalingWidthRdBtn;
    @FXML private RadioButton scalingPercentageRdBtn;

    @FXML private Button directoryBtn;
    @FXML private Button resizeBtn;
    @FXML private ProgressBar resizingProgressBar;

    private ResizeModel resizingModel;

    private double resizeingProgress = 0d;

    @FXML public TableView<ResizingFile> resizingTable;
    private final ObservableList<ResizingFile> data = FXCollections.observableArrayList();


    private int targetPercentage, targetWidth, targetHeight;

    public void initalizeGUI() {

        this.prepareTable();

        resizingProgressBar.setDisable(true);
        resizingProgressBar.setProgress(0d);
    }

    public ResizeModel getResizingModel() {
        return resizingModel;
    }

    private void readValuesFromGUI() {
        try {
            if (scalingHeightRdBtn.isSelected()) {
                resizingModel = ResizeModel.SCALING_HEIGHT;
                targetHeight = Integer.parseInt(heightTextField.getText());

            } else if (scalingWidthRdBtn.isSelected()) {
                resizingModel = ResizeModel.SCALING_WIDTH;
                targetWidth = Integer.parseInt(widthTextField.getText());

            } else if (scalingPercentageRdBtn.isSelected()) {
                resizingModel = ResizeModel.SCALING_PERCENT;
                targetPercentage = Integer.parseInt(percentageTextField.getText());

            } else
                resizingModel = ResizeModel.SCALING_NONE;
        }catch (NumberFormatException nfe) {
            System.err.println("error in reading target scale number");
            resizingModel = ResizeModel.SCALING_NONE;
        }
    }

    public int getTargetPercentage() {
        return targetPercentage;
    }

    public int getTargetWidth() {
        return targetWidth;
    }

    public int getTargetHeight() {
        return targetHeight;
    }

    protected void loadFileList(File folder) {

        directoryString.setText(folder.getPath());

        File[] fullList = folder.listFiles();
        data.clear();
        for (File f : fullList) {
            if (f.isFile()) {
                try {
                    String type = Files.probeContentType(f.toPath());
                    if (ALLOWED_FILETYPES.contains(type))
                        this.data.add(new ResizingFile(this, f));
                }catch(IOException ioe){
                    System.err.println("file konnte nicht geladen werden: " + f.toPath());
                }
            }
        }
    }

    private void updateDisplayedFileList() {
        readValuesFromGUI();

        StringBuilder bfr = new StringBuilder();

        try {
            resizingTable.refresh();

//            if(isFirst) {
//                bfr.append("Keine Daten im ausgewählten Ordner vorhanden.");
//            }
        } catch (NullPointerException npe) {
            bfr.append("Angegebener ordner nicht korrekt.");
        }
    }

    @FXML protected void updateScreenOnAction(ActionEvent event) {
        this.updateDisplayedFileList();
    }


    @FXML protected void preCheckScalingWidth(KeyEvent event) {

        String input = event.getCharacter();
        if (Pattern.matches("[^0-9]", input))
            event.consume();
    }

    @FXML protected void checkScalingWidth(KeyEvent event) {

        try {
            int idx = Integer.parseInt(widthTextField.getText());
            if(idx < 0) {
                widthTextField.setText("0");
            }
        } catch(NumberFormatException nfe) {
            widthTextField.setText("0");
        }

        updateDisplayedFileList();
    }

    @FXML protected void preCheckScalingHeight(KeyEvent event) {
        String input = event.getCharacter();
        if (Pattern.matches("[^0-9]", input))
            event.consume();
    }

    @FXML protected void checkScalingHeight(KeyEvent event) {
        try {
            int idx = Integer.parseInt(heightTextField.getText());
            if(idx < 0) {
                heightTextField.setText("0");
            }
        } catch(NumberFormatException nfe) {
            heightTextField.setText("0");
        }

        updateDisplayedFileList();
    }

    @FXML protected void preCheckPercentage(KeyEvent event) {
        String input = event.getCharacter();
        if (Pattern.matches("[^0-9]", input))
            event.consume();
    }

    @FXML protected void checkPercentage(KeyEvent event) {
        try {
            int idx = Integer.parseInt(percentageTextField.getText());
            if(idx < 0) {
                percentageTextField.setText("0");
            }
        } catch(NumberFormatException nfe) {
            percentageTextField.setText("0");
        }
        updateDisplayedFileList();
    }

    @FXML protected void directoryFieldAction(ActionEvent event) {

        this.folder = new File(directoryString.getText());

        try {
            this.loadFileList(folder);
        } catch(NullPointerException npe) {

        }
        this.updateDisplayedFileList();
    }

    @FXML protected void handleDirectoryChooser(ActionEvent event) {
        DirectoryChooser dChooser = new DirectoryChooser();

        try {
            if(this.folder != null && this.folder.isDirectory())
                dChooser.setInitialDirectory(this.folder);
        } catch(NullPointerException npe) {

            Alert alertException = new Alert(Alert.AlertType.ERROR);
            alertException.setTitle("Fehler");
            alertException.setHeaderText("Ordner konnte nicht geladen werden.");
            alertException.showAndWait();
        }

        this.folder = dChooser.showDialog(directoryBtn.getScene().getWindow());

        try {
            this.loadFileList(this.folder);
        } catch (NullPointerException npe) {
            Alert alertException = new Alert(Alert.AlertType.ERROR);
            alertException.setTitle("Fehler");
            alertException.setHeaderText("Ausgewählter Ordner konnte nicht geladen werden.");
            alertException.showAndWait();
        }
        this.updateDisplayedFileList();
    }

    @FXML protected void resizeFilesBtn(ActionEvent event) {

        readValuesFromGUI();

        doResizing();
    }

    @FXML protected void keyPressedResizingTableView(KeyEvent event) {
        if(event.getCode() == KeyCode.SPACE) {

            ObservableList<ResizingFile> list = resizingTable.getSelectionModel().getSelectedItems();

            for(ResizingFile f:list) {
                f.setChecked(!f.getChecked());
            }

            event.consume();
        }
    }

    private int countCheckedFiles(){
        int cnt = 0;
        for(ResizingFile f:data) {
            if(f.isChecked())
                cnt++;
        }
        return cnt;
    }

    private void doResizing() {

        File resizeFolder = new File(folder,"resized");

        if(!(resizeFolder.mkdir() || resizeFolder.isDirectory())) {

            Alert alertException = new Alert(Alert.AlertType.ERROR);
            alertException.setTitle("Ordner konnte nicht erstellt werden.");
            alertException.setHeaderText("Der Ordner für die Geänderten Bilder konnte nicht erstellt werden.");
            alertException.setContentText(resizeFolder.getPath());
            alertException.showAndWait();
        }

        resizingProgressBar.setDisable(false);
        resizeBtn.setDisable(true);

        ResizingTask task = new ResizingTask(data, resizeFolder);
        resizingProgressBar.progressProperty().bind(task.progressProperty());
        task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                if(task.getValue()){
                    System.out.println("resizing done");
                    resizingFinished();
                } else {

                Alert alertException = new Alert(Alert.AlertType.ERROR);
                alertException.setTitle("Größenänderungen nicht möglich");
                alertException.setHeaderText("Ein Foto konnte nicht geändert werden.");
//                alertException.setContentText(f.getPath());
                alertException.showAndWait();
                }
            }
        });

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private void prepareTable() {
        resizingTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        resizingTable.setItems(data);
    }

    public void resizingFinished() {

        resizeBtn.setDisable(false);
        resizingProgressBar.setDisable(true);


        Alert alertException = new Alert(Alert.AlertType.INFORMATION);
        alertException.setTitle("Fertig");
        alertException.setHeaderText("Größenänderung abgeschlossen.");
        alertException.showAndWait();

        this.updateDisplayedFileList();

    }

    public enum ResizeModel {
        SCALING_NONE, SCALING_HEIGHT, SCALING_WIDTH, SCALING_PERCENT
    }
}