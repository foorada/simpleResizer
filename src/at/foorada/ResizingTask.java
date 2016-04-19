package at.foorada;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import java.io.File;
import java.io.IOException;

public class ResizingTask extends Task<Boolean> {

    private ObservableList<ResizingFile> data;
    private File dest;

    @Override
    protected Boolean call() throws Exception {

        int allCnt = countCheckedFiles();
        int cnt = 0;

        for(ResizingFile f:data) {
            try {

                if(f.isChecked()) {
                    System.out.println("resizing file: "+f.getName());

                    try {
                        f.doResize(dest);
                        cnt++;
                        updateProgress(cnt, allCnt);
                    } catch(IOException ioe) {
                        return false;
                    }
                }

            } catch(Exception e) {
                return false;
            }
        }

        return true;
    }

    public ResizingTask(ObservableList<ResizingFile> data, File destination) {
        this.data = data;
        dest = destination;
    }

    private int countCheckedFiles(){
        int cnt = 0;
        for(ResizingFile f:data) {
            if(f.isChecked())
                cnt++;
        }
        return cnt;
    }
}
