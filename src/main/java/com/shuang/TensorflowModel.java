package com.shuang;

import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.SavedModelBundle;


public interface TensorflowModel {
    float[] predict(float[][][][] imgArray);
}


class MnistModel implements TensorflowModel {

    private static SavedModelBundle model;
    private static String MODEL_DIR = "mnist_model";
    private static final MnistModel instance = new MnistModel();

    // private constructor to avoid client applications to use constructor
    private MnistModel(){
        model = SavedModelBundle.load(String.format("./%s", MODEL_DIR), "serve");
    }

    public static MnistModel getModel() {
        return instance;
    }

    public float[] predict(float[][][][] imgArray) {
        Session s = model.session();

        Tensor inputTensor = Tensor.create(imgArray);
        // make sure the input/output layer names are identical to model layer names
        float[][] prob = s.runner()
                .feed("inputs:0", inputTensor)
                .fetch("outputs/Softmax:0")
                .run()
                .get(0)
                .copyTo(new float[1][10]);

        float maxProb = prob[0][0];
        int digit = 0;
        int prediction = -1;
        for (float p : prob[0]) {
            if (p >= maxProb) {
                prediction = digit;
                maxProb = p;
            }
            digit++;
        }
        System.out.println(String.format("The digit is %d with probability %.2f%%", prediction, maxProb * 100));
        return new float[] {prediction, maxProb};
    }

}