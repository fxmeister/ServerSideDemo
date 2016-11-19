package com.uiuc;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.firebase.tubesock.Base64;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class Main extends Application {

    public static Firebase myFirebaseRef = new Firebase(Constants.FIREBASE_URL);
    private ImageView selectedImage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Pluto camera demo by tjmille2");
        primaryStage.setWidth(1024);
        primaryStage.setHeight(1024);
        Scene scene = new Scene(new Group());
        VBox root = new VBox();
        selectedImage = new ImageView();
        root.getChildren().addAll(selectedImage);
        scene.setRoot(root);
        primaryStage.setScene(scene);
        primaryStage.show();

        initDatabase();
    }


    private void initDatabase() {

        // init local variables from server
        // In charge of sanitizing input to prevent code from crashing.
        Main.myFirebaseRef.child(Constants.FIREBASE_OBJECT_ADDRESS).addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snapshot) {

                String encodedValue = snapshot.getValue().toString();

                byte[] rawImageData = Base64.decode(encodedValue);

                BufferedImage img = null;
                try {
                    img = ImageIO.read(new ByteArrayInputStream(rawImageData));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                WritableImage mWritableImage = null;
                if (img != null) {
                    mWritableImage = new WritableImage(img.getWidth(), img.getHeight());
                    PixelWriter pw = mWritableImage.getPixelWriter();
                    for (int x = 0; x < img.getWidth(); x++) {
                        for (int y = 0; y < img.getHeight(); y++) {
                            pw.setArgb(x, y, img.getRGB(x, y));
                        }
                    }
                }

                ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();

                try {
                    ImageIO.write(SwingFXUtils.fromFXImage(mWritableImage, null), "png", byteOutput);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    Image image = ImageIO.read(new ByteArrayInputStream(byteOutput.toByteArray()));
                    image = resizeImage(image, 500, 500, true);
                    selectedImage.setImage(createImage(image));
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }

            @Override
            public void onCancelled(FirebaseError error) {
            }

        });
    }


    /**
     * This method resizes the given image using Image.SCALE_SMOOTH.
     *
     * @param image  the image to be resized
     * @param width  the desired width of the new image. Negative values force the only constraint to be height.
     * @param height the desired height of the new image. Negative values force the only constraint to be width.
     * @param max    if true, sets the width and height as maximum heights and widths, if false, they are minimums.
     * @return the resized image.
     */
    public static Image resizeImage(Image image, int width, int height, boolean max) {
        if (width < 0 && height > 0) {
            return resizeImageBy(image, height, false);
        } else if (width > 0 && height < 0) {
            return resizeImageBy(image, width, true);
        } else if (width < 0 && height < 0) {
            System.out.print("Setting the image size to (width, height) of: ("
                    + width + ", " + height + ") effectively means \"do nothing\"... Returning original image");
            return image;
            //alternatively you can use System.err.println("");
            //or you could just ignore this case
        }
        int currentHeight = image.getHeight(null);
        int currentWidth = image.getWidth(null);
        int expectedWidth = (height * currentWidth) / currentHeight;
        //Size will be set to the height
        //unless the expectedWidth is greater than the width and the constraint is maximum
        //or the expectedWidth is less than the width and the constraint is minimum
        int size = height;
        if (max && expectedWidth > width) {
            size = width;
        } else if (!max && expectedWidth < width) {
            size = width;
        }
        return resizeImageBy(image, size, (size == width));
    }

    /**
     * Resizes the given image using Image.SCALE_SMOOTH.
     *
     * @param image    the image to be resized
     * @param size     the size to resize the width/height by (see setWidth)
     * @param setWidth whether the size applies to the height or to the width
     * @return the resized image
     */
    public static Image resizeImageBy(Image image, int size, boolean setWidth) {
        if (setWidth) {
            return image.getScaledInstance(size, -1, Image.SCALE_SMOOTH);
        } else {
            return image.getScaledInstance(-1, size, Image.SCALE_SMOOTH);
        }
    }

    public static javafx.scene.image.Image createImage(java.awt.Image image) throws IOException {
        if (!(image instanceof RenderedImage)) {
            BufferedImage bufferedImage = new BufferedImage(image.getWidth(null),
                    image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
            Graphics g = bufferedImage.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();

            image = bufferedImage;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write((RenderedImage) image, "png", out);
        out.flush();
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        return new javafx.scene.image.Image(in);
    }


    public static void main(String[] args) {
        launch(args);
    }
}
