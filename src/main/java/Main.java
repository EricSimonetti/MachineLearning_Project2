import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.dataset.api.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.iterator.MultipleEpochsIterator;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.schedule.ExponentialSchedule;
import org.nd4j.linalg.schedule.ScheduleType;

import java.io.File;
import java.io.IOException;

public class Main {
    private final static boolean DEBUG_MODE = true;

    //program parameters
    private final static double LEARNING_RATE = .05,
            MOMENTUM = 0.9,
            LAMBDA = .0001;
    private final static int
            NUM_FEATURES = 138,     //Number of nodes in the input layer (Large due to one-hot-encoding)
            NUM_HIDDEN = 90,        //Number of nodes in the hidden layer
            NUM_OUTPUT = 1,         //Number of nodes in output layer (just one for the binary variable "Died"
            BATCH_POW = 10,         //The power of 2 that is used for the batch size (2^8 = 256) <--- Mini-batching
            SEED = 2351346;         //rng seed

    public static void main(String[] args) {
        //program parameters
        final boolean PREPROCESS = false;
        String dataDirectory = "";

        try {
            dataDirectory = (new java.io.File(".").getCanonicalPath()) + "\\Data\\Processed\\processedData.csv";
        }catch (IOException e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        if(PREPROCESS) {
            if (DEBUG_MODE) System.out.println("PreProcessing Data...");
            DataHelper.processData(dataDirectory, DEBUG_MODE);
        }
        else {
            if(DEBUG_MODE) System.out.println("Loading Processed Data...");
            DataSet data = null;
            try {
                //Load Data
                RecordReader rr = new CSVRecordReader( 0, ',');
                rr.initialize(new FileSplit(new File(dataDirectory)));

                DataSetIterator iterator = new RecordReaderDataSetIterator(rr, 69647, NUM_FEATURES, NUM_OUTPUT);
                data = iterator.next();
                data.shuffle();

                //Process Data
                SplitTestAndTrain tnt = data.splitTestAndTrain(0.70);
                DataSet trainingData = tnt.getTrain();
                DataSet testData = tnt.getTest();

                //Run the network
                MultiLayerNetwork model = new MultiLayerNetwork(configureNetwork(trainingData, testData));
                model.setListeners(new ScoreIterationListener(10));
                model.init();
                int epochs = 100;
                for(int i = 0; i<epochs; i++){
                    iterator.reset();
                    model.fit(trainingData);
                }

                //testing
                String results = test(model, testData);
                //System.out.println("Result of testing: " + results);
            } catch (IOException | InterruptedException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static MultiLayerConfiguration configureNetwork(DataSet trainingData, DataSet testData) {
        return new NeuralNetConfiguration.Builder()
                .seed(SEED)
                .activation(Activation.SIGMOID)
                .weightInit(WeightInit.XAVIER)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Nesterovs(LEARNING_RATE, MOMENTUM))
                .l2(LAMBDA)
                .list()
                .layer(0, new DenseLayer.Builder().nIn(NUM_FEATURES).nOut(NUM_HIDDEN).build())
                .layer(1, new DenseLayer.Builder().nIn(NUM_HIDDEN).nOut(NUM_HIDDEN).build())
                .layer(2, new OutputLayer.Builder(
                        LossFunctions.LossFunction.XENT).activation(Activation.SIGMOID)
                        .nIn(NUM_HIDDEN).nOut(NUM_OUTPUT).build())
                .backpropType(BackpropType.Standard)
                .build();
    }

    private static String test(MultiLayerNetwork model, DataSet testData){
        INDArray output = model.output(testData.getFeatures());
        Evaluation eval = new Evaluation(NUM_OUTPUT);
        eval.eval(testData.getLabels(), output);
        return(eval.stats());
    }
}

