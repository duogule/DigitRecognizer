package com.shuang;

import java.awt.Graphics;
import java.awt.image.DataBufferByte;
import java.awt.image.BufferedImage;


public class GrayImage {
    /**
     * Customized class for handling hand-written digit image
     */
    final BufferedImage bufferedImage;
    final float[][] pixelArray;
    final int nrow;
    final int ncol;

    GrayImage(BufferedImage bufferedImage){
        this.bufferedImage = bufferedImage;
        ncol = bufferedImage.getWidth();
        nrow = bufferedImage.getHeight();
        pixelArray = get2DArray(bufferedImage);
    }

    public float[][] get2DArray(BufferedImage img) {
        /**
         * Transform the image to a 2D array
         */
        float[][] pixelArray = new float[nrow][ncol];
        byte[] pixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < nrow; i++) {
            for (int j = 0; j < ncol; j++) {
                pixelArray[i][j] = (float) (pixels[i * nrow + j] & 0xFF);
            }
        }
        return pixelArray;
    }


    public BoundingBox getBoundary(float threshold) {
        /**
         * Get image boundaries along X-axis and Y-axis
         * (ignoring noise points by a pixel magnitude threshold)
         */
        int minX = ncol;
        int minY = nrow;
        int maxX = -1;
        int maxY = -1;
        for (int i = 0; i < nrow; i++) {
            for (int j = 0; j < ncol; j++) {
                if (pixelArray[i][j] < threshold * 255f) {
                    minX = minX > j ? j : minX;
                    maxX = maxX < j ? j : maxX;
                    minY = minY > i ? i : minY;
                    maxY = maxY < i ? i : maxY;
                }
            }
        }
        if (maxX < 0) {
            // when a whole white image
            minX = maxX = ncol/2;
            minY = maxY = nrow/2;
        }
        BoundingBox boundBox = new BoundingBox();
        boundBox.setMinX(minX);
        boundBox.setMinY(minY);
        boundBox.setMaxX(maxX);
        boundBox.setMaxY(maxY);
        return boundBox;

    }

    public float[][] getBinBlockImageArray(int binRow, int binCol) {
        /**
         * Down-size the image array by using bin box blocking average
         */
        float[][] scaledImagePixel = new float[nrow/binRow][ncol/binCol];
        // Reduce image size by grouping into bins and taking average pixel values as new one
        for (int i = 0; i < nrow/binRow; i++) {
            for (int j = 0; j < ncol/binCol; j++) {
                float mean = 0f;
                for (int v = 0; v < binRow; v++) {
                    for (int h = 0; h < binCol; h++) {
                        mean += pixelArray[i*binRow + v][j*binCol + h];
                    }
                }
                // Taking average pixel values in current bin as bin's value
                mean /= (float)(binRow * binCol);
                scaledImagePixel[i][j] = mean;
            }
        }
        return scaledImagePixel;
    }

    public BufferedImage getScaledCenterImage(int scaledLength, float threshold) {
        /**
         * Get re-sizeed and centered image
         */
        BoundingBox boundBox = getBoundary(threshold);

        // Get length of bounding box
        int bndX = boundBox.getMaxX() - boundBox.getMinX() + 1;
        int bndY = boundBox.getMaxY() - boundBox.getMinY() + 1;
        int bndLength = bndX > bndY ? bndX : bndY;

        // Calculate scale factor and corresponding new width and height
        float scaleFactor = scaledLength / (float) bndLength;
        int scaledWidth = (int)(bndX * scaleFactor);
        int scaledHeight = (int)(bndY * scaleFactor);

        // Crop image by estimated bounding box
        BufferedImage croppedImage = bufferedImage.getSubimage(boundBox.getMinX(), boundBox.getMinY(), bndX, bndY);
        BufferedImage scaledCenterImage = new BufferedImage(ncol, nrow, BufferedImage.TYPE_BYTE_GRAY);
        Graphics graphics = scaledCenterImage.getGraphics();
        graphics.setColor(java.awt.Color.WHITE);
        graphics.fillRect(0,0, ncol, nrow);

        // Re-draw cropped image with re-sizing
        graphics.drawImage(croppedImage, (ncol-scaledWidth)/2, (nrow-scaledHeight)/2, scaledWidth, scaledHeight,null);
        graphics.dispose();

        return scaledCenterImage;
    }


    public static float[][] toNormalizedArray(float[][] array, boolean flipColor) {
        /**
         * Normalize from [0, 255] to [0.0, 1.0] and (optionally) flip color
         */
        float[][] normalizedArray = new float[array.length][array[0].length];
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                normalizedArray[i][j] = array[i][j] / 255.0f;  // Normalize pixels
                if (flipColor) {
                    normalizedArray[i][j] = 1.0f - normalizedArray[i][j];  // Invert color
                }
            }
        }
        return normalizedArray;
    }

    public static float[][][][] reshapeForModel(float[][] array, int r, int c) {
        /**
         * Reshape a 2D array to 4D image format
         */
        int totalElements = array.length * array[0].length;
        assert (totalElements != r * c || totalElements % r != 0);
        final float[][][][] result = new float[1][r][c][1];
        int newR = 0;
        int newC = 0;
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[i].length; j++) {
                result[0][newR][newC][0] = array[i][j];
                newC++;
                if (newC == c) {
                    newC = 0;
                    newR++;
                }
            }
        }
        return result;
    }

}
