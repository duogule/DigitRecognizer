package com.shuang;

import java.awt.Graphics;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.imageio.ImageIO;


public class DigitRecognizer extends Application {
    /**
     * The main class for Digit Recognition Application.
     */

    final static int CANVAS_WIDTH = 280;
    final static int CANVAS_HEIGHT = 280;
    final static int SCALE_BIN_WIDTH = 10;
    final static int SCALE_BIN_HEIGHT = 10;
    final static int BOUNDING_BOX_LENGTH = 190;
    final static int MODEL_INPUT_ROW = CANVAS_HEIGHT/SCALE_BIN_HEIGHT;
    final static int MODEL_INPUT_COL = CANVAS_WIDTH/SCALE_BIN_WIDTH;
    final static MnistModel MODEL = MnistModel.getModel();

    @Override
    public void start(final Stage primaryStage) {

        final Canvas canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        final GraphicsContext graphicsContext = canvas.getGraphicsContext2D();

        graphicsContext.setLineWidth(20);

        // Create event handler for mouse pressing
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED,
                event -> {
                    graphicsContext.setLineCap(StrokeLineCap.ROUND);
                    graphicsContext.setLineJoin(StrokeLineJoin.ROUND);
                    graphicsContext.beginPath();
                    graphicsContext.moveTo(event.getX(), event.getY());
                    graphicsContext.stroke();
                });

        // Create event handler for mouse dragging
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED,
                event -> {
                    graphicsContext.setLineCap(StrokeLineCap.ROUND);
                    graphicsContext.setLineJoin(StrokeLineJoin.ROUND);
                    graphicsContext.lineTo(event.getX(), event.getY());
                    graphicsContext.stroke();
                });


        Group root = new Group();

        // Create buttons for digit recognition, image saving and canvas clearance
        Button buttonRecognize = new Button("Recognize");
        Button buttonSave = new Button("Save");
        Button buttonClear = new Button("Clear");

        // Create labels to show instruction and predicted digit
        Label labelDigit = new Label();
        Label labelInstruction = new Label();
        labelInstruction.setText("Draw a digit in the box below and click \"Recognize\".");

        // Save the current canvas to a jpg image
        buttonSave.setOnAction(
                event -> {
                    FileChooser fileChooser = new FileChooser();

                    // Set extension filter
                    FileChooser.ExtensionFilter extFilter =
                            new FileChooser.ExtensionFilter("jpg files (*.jpg)", "*.jpg");
                    fileChooser.getExtensionFilters().add(extFilter);

                    // Show save file dialog
                    File file = fileChooser.showSaveDialog(primaryStage);

                    if (file != null) {
                        try {
                            BufferedImage img = getBufferedImage(canvas);
                            ImageIO.write(img, "jpg", file);
                        } catch (IOException ex) {
                            Logger.getLogger(DigitRecognizer.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
        );

        // Recognize the written digit by using tenserflow model
        buttonRecognize.setOnAction(
                event -> {
                    // Normalize canvas image to a proper format for feeding in model
                    float[][][][] imgArray = getMnistImage4DArray(canvas);
                    // Call our model to predict the digit
                    float[] results = MODEL.predict(imgArray);
                    int digit = (int) results[0];
                    labelDigit.setText(String.valueOf(digit));
                    labelDigit.setFont(new Font("Arial", 180));
                }
        );

        // Clear current canvas for new drawings
        buttonClear.setOnAction(
                event -> clear(graphicsContext)
        );

        // HBox for three buttons
        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(buttonRecognize, buttonClear, buttonSave);
        buttonBox.setPadding(new Insets(15, 0, 0, 50));

        // HBox for drawing on canvas
        HBox canvasBox = new HBox();
        canvasBox.getChildren().add(canvas);
        canvasBox.setStyle("-fx-padding: 0;" +
                "-fx-border-style: solid inside;" +
                "-fx-border-width: 2;" +
                "-fx-border-insets: 10;" +
                "-fx-border-radius: 0;" +
                "-fx-border-color: black;");

        // HBox for canvas box and prediction result
        HBox canvasAndPredBox = new HBox(80);
        canvasAndPredBox.getChildren().addAll(canvasBox, labelDigit);
        canvasAndPredBox.setAlignment(Pos.CENTER);

        // VBox for aligning all available boxes/labels
        VBox vBox = new VBox();
        vBox.getChildren().addAll(labelInstruction, canvasAndPredBox, buttonBox);
        vBox.setPadding(new Insets(10, 0, 0, 10));

        root.getChildren().add(vBox);
        Scene scene = new Scene(root, 600, 400);
        primaryStage.setTitle("Digit Recognizer by Shuang Gong");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private BufferedImage getBufferedImage(Canvas canvas) {
        /**
         * Transform javafx canvas to a BufferedImage
         */
        WritableImage writableImage = new WritableImage(CANVAS_WIDTH, CANVAS_HEIGHT);
        canvas.snapshot(null, writableImage);
        Image tmp = SwingFXUtils.fromFXImage(writableImage, null);
        BufferedImage canvasImage = new BufferedImage(CANVAS_WIDTH, CANVAS_HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
        Graphics graphics = canvasImage.getGraphics();
        graphics.drawImage(tmp, 0, 0, null);
        graphics.dispose();
        return canvasImage;
    }

    private float[][][][] getMnistImage4DArray(Canvas canvas) {
        /**
         * Canvas image normalization for getting a better model prediction
         * Output would be a 2D float array as model input
         */
        BufferedImage canvasImage = getBufferedImage(canvas);

        // Read canvas image as a GrayImage which is our customized class
        GrayImage grayImage = new GrayImage(canvasImage);

        // Transform to a re-scaled image with re-centering
        BufferedImage scaledCenterImage = grayImage.getScaledCenterImage(BOUNDING_BOX_LENGTH,0.01f);

        // Read the re-scaled image as GrayImage
        GrayImage centerGrayImage = new GrayImage(scaledCenterImage);

        // Resize (down-size) the image by bin blocking
        float[][] binBlockImageArray = centerGrayImage.getBinBlockImageArray(SCALE_BIN_HEIGHT, SCALE_BIN_WIDTH);

        // Transform the image to a 2D float array
        float[][] normalizedImageArray = GrayImage.toNormalizedArray(binBlockImageArray, true);

        // Reshape 2D array to 4D as model input
        float[][][][] finialArray = GrayImage.reshapeForModel(normalizedImageArray, MODEL_INPUT_ROW, MODEL_INPUT_COL);
        return finialArray;
    }

    private void clear(GraphicsContext gc) {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}