package at.foorada;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;

public class ResizingFile extends File {
    private Controller cntr;
    private SimpleBooleanProperty checked;
    private final SimpleStringProperty newName;

    private ImageScaler.ImageType imageType;

    private int currentWidth, currentHeight;

    private Image img;

    public ResizingFile(Controller cntr, File f) {
        this(cntr, f.getPath());
    }

    private ResizingFile(Controller cntr, String filename) {
        super(filename);
        this.cntr = cntr;
        this.checked = new SimpleBooleanProperty(false);
        this.checked.addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                cntr.resizingTable.refresh();
                cntr.resizingTable.requestFocus();
            }
        });
        this.newName = new SimpleStringProperty(getName());

        try(ImageInputStream in = ImageIO.createImageInputStream(this)) {
            final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(in);
                    currentWidth = reader.getWidth(0);
                    currentHeight = reader.getHeight(0);
                    switch(Files.probeContentType(Paths.get(this.getAbsolutePath()))) {
                        case "image/png":
                            imageType = ImageScaler.ImageType.IMAGE_PNG;
                            break;
                        case "image/jpg":
                            imageType = ImageScaler.ImageType.IMAGE_JPG;
                            break;
                        default:
                        case "image/jpeg":
                            imageType = ImageScaler.ImageType.IMAGE_JPEG;
                            break;
                    }
                } catch (IOException ioe1) {}
                finally {
                    reader.dispose();
                }
            }


//            newSize = currentSize;
        } catch(IOException ioe) {}
    }

    public String getFileName() {
        return getName();
    }

    public String getCurSize()
    {
        if(currentHeight > 0 && currentWidth > 0)
            return currentWidth + " x " + currentHeight;
        else
            return "unknown";
    }

    private int getNewWidth() {

        if(!isChecked()) {
            return currentWidth;
        }

        if(currentHeight > 0 && currentWidth > 0) {
            float f;

            switch (cntr.getResizingModel()){
                case SCALING_HEIGHT:
                    f = ((float)cntr.getTargetHeight()) / ((float)currentHeight);
                    return Math.round(currentWidth * f);
                case SCALING_WIDTH:
                    return cntr.getTargetWidth();
                case SCALING_PERCENT:
                    f = ((float)cntr.getTargetPercentage()) / 100f;
                    return Math.round(currentWidth * f);
                default:
                case SCALING_NONE:
                    return currentWidth;
            }

        }
        else
            return currentWidth;
    }

    private int getNewHeight() {

        if(!isChecked()) {
            return currentHeight;
        }

        if(currentHeight > 0 && currentWidth > 0) {
            float f;

            switch (cntr.getResizingModel()){
                case SCALING_HEIGHT:
                    return cntr.getTargetHeight();
                case SCALING_WIDTH:
                    f = ((float)cntr.getTargetWidth()) / ((float)currentWidth);
                    return Math.round(currentHeight * f);
                case SCALING_PERCENT:
                    f = ((float)cntr.getTargetPercentage()) / 100f;
                    return Math.round(currentHeight * f);
                default:
                case SCALING_NONE:
                    return currentHeight;
            }
        }
        else
            return currentHeight;
    }

    public String getNewSize()
    {
        if(!isChecked()) {
            return getCurSize();
        }

        if(currentHeight > 0 && currentWidth > 0) {

            return getNewWidth() + " x " + getNewHeight();
        }
        else
            return "unknown";
    }

    public SimpleBooleanProperty checkedProperty() {
        return this.checked;
    }

    public boolean getChecked() {
        return this.checked.get();
    }

    public void setChecked(boolean checked) {
        this.checked.set(checked);
    }

    public boolean isChecked(){
        return this.getChecked();
    }

    public void doResize(File destinationFolder) throws IOException {
        if(!destinationFolder.isDirectory())
            throw new FileNotFoundException("given object is no folder");

        File rsFile = new File(destinationFolder, this.getName());

        ImageScaler is = new ImageScaler(this);
        is.createScaledImage(getNewWidth(), getNewHeight(), ImageScaler.ScaleType.FIT);

        is.saveScaledImage(rsFile, imageType);
    }

    private static void copyFileUsingStream(File source, File dest) throws IOException {
        try(InputStream is = new FileInputStream(source);
            OutputStream os = new FileOutputStream(dest)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }
}
