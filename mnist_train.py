import keras
import tensorflow as tf
from keras.datasets import mnist
from keras.models import Model
from keras.layers import Conv2D, Dense, Dropout, Flatten, Input, MaxPooling2D
from keras import backend as K

batch_size = 1024
num_classes = 10
epochs = 20

# input image dimensions
img_rows, img_cols = 28, 28

# the data, split between train and test sets
(x_train, y_train), (x_test, y_test) = mnist.load_data()

# reshape the data for model input
x_train = x_train.reshape(x_train.shape[0], img_rows, img_cols, 1)
x_test = x_test.reshape(x_test.shape[0], img_rows, img_cols, 1)
input_shape = (img_rows, img_cols, 1)

# normalize image pixel
x_train = x_train.astype('float32')
x_test = x_test.astype('float32')
x_train /= 255
x_test /= 255
print('x_train shape:', x_train.shape)
print(x_train.shape[0], 'train samples')
print(x_test.shape[0], 'test samples')

# convert class vectors to binary class matrices
y_train = keras.utils.to_categorical(y_train, num_classes)
y_test = keras.utils.to_categorical(y_test, num_classes)

# construct model
inputs = Input(shape=(28, 28, 1), name="inputs")
x = Conv2D(32, kernel_size=5, activation='relu', padding='same')(inputs)
x = MaxPooling2D(pool_size=(2, 2), strides=None, padding='same')(x)
x = Conv2D(64, kernel_size=5, activation='relu', padding='same')(x)
x = MaxPooling2D(pool_size=(2, 2), strides=None, padding='same')(x)
x = Flatten()(x)
x = Dense(1024, activation='relu')(x)
x = Dropout(0.4, name='dropout')(x)
outputs = Dense(num_classes, activation='softmax', name="outputs")(x)

model = Model(inputs=inputs, outputs=outputs)

model.compile(loss=keras.losses.categorical_crossentropy,
              optimizer=keras.optimizers.Adam(),
              metrics=['accuracy'])

model.fit(x_train, y_train,
          batch_size=batch_size,
          epochs=epochs,
          verbose=1,
          validation_data=(x_test, y_test))
score = model.evaluate(x_test, y_test, verbose=0)
print('Test loss:', score[0])
print('Test accuracy:', score[1])

# save model for severing in java
tf.saved_model.simple_save(
    keras.backend.get_session(), 
    "/nfs/zhifeng/deep/mnist_model",
    inputs={"inputs": model.input},
    outputs={"outputs": t for t in model.outputs})

# check model input/output layer names
print(f"The name of input layer is '{model.input.name}'")
print(f"The name of ouput layer is '{model.output.name}'")
