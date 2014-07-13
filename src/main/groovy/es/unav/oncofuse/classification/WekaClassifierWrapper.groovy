/**
 Copyright 2014 Mikhail Shugay (mikhail.shugay@gmail.com)

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package es.unav.oncofuse.classification

import es.unav.oncofuse.Util
import weka.classifiers.Classifier
import weka.classifiers.UpdateableClassifier
import weka.core.Instance
import weka.core.Instances
import weka.core.SerializationHelper
import weka.core.converters.ArffLoader

class WekaClassifierWrapper implements ClassifierWrapper {
    private final Instances trainingData
    private final Classifier classifier
    private final String schema

    public static WekaClassifierWrapper load(String modelFileName, String schemaFileName, boolean fromResources) {
        def mdlFile = Util.getFile(modelFileName, fromResources),
            schemaFile = Util.getFile(schemaFileName, fromResources)

        // Legacy
        //def classifier = new SerializedClassifier()
        //classifier.setModelFile(mdlFile)

        def classifier = (Classifier) SerializationHelper.read(modelFileName)

        String schema = schemaFile.readLines().findAll { it.startsWith("@") }.join("\n")

        def trainingData = createTrainingData(schema)

        new WekaClassifierWrapper(trainingData, classifier, schema)
    }

    public static WekaClassifierWrapper train(Classifier classifier, String schema, List<double[]> dataMatrix) {
        def trainingData = createTrainingData(schema, dataMatrix)
        classifier.buildClassifier(trainingData)

        new WekaClassifierWrapper(trainingData, classifier, schema)
    }

    public void update(String schema, List<double[]> examples) {
        if (!(classifier instanceof UpdateableClassifier))
            throw new UnsupportedOperationException("Update operation not supported for ${classifier.class.canonicalName}")

        examples.each { double[] features ->
            (classifier as UpdateableClassifier).updateClassifier(createInstance(schema, features))
        }
    }

    public void save(String modelFileName, String schemaFileName) {
        // Serialize classifier model
        SerializationHelper.write(modelFileName, classifier)

        // Legacy
        //def oos = new ObjectOutputStream(new FileOutputStream(modelFileName))
        //oos.writeObject(classifier)
        //oos.flush()
        //oos.close()

        // Write schema only

        new File(schemaFileName).withPrintWriter { it.println(schema) }

        // With instances
        //def saver = new ArffSaver();
        //saver.setInstances(trainingData);
        //saver.setFile(new File(schemaFileName));
    }

    public void setClassifierOptions(String[] options) {
        classifier.setOptions(options)
    }

    public String[] getClassifierOptions() {
        classifier.getOptions()
    }

    private WekaClassifierWrapper(Instances trainingData, Classifier classifier, String schema) {
        this.trainingData = trainingData
        this.classifier = classifier
        this.schema = schema
    }

    private static Instances createTrainingData(String schema) {
        createTrainingData(schema, new LinkedList<>())
    }

    private static Instances createTrainingData(String schema, List<double[]> dataMatrix) {
        def loader = new ArffLoader()
        loader.setSource(new ByteArrayInputStream(schema.getBytes()))

        def trainingData = loader.getDataSet()
        trainingData.setClassIndex(trainingData.numAttributes() - 1)

        dataMatrix.each { double[] features ->
            trainingData.add(createInstance(trainingData, features))
        }
    }

    private static Instance createInstance(Instances parent, double[] features) {
        def instance = new Instance(1.0, features)
        features.eachWithIndex { double it, int ind -> if (Double.isNaN(it)) instance.setMissing(ind) }
        instance.setDataset(parent)

        instance
    }

    private Instance createInstance(String schema, double[] features) {
        if (this.schema != schema)
            throw new IllegalArgumentException("Classifier and instance schema don't match")

        createInstance(trainingData, features)
    }

    @Override
    ClassifierResult classify(String schema, double[] features) {
        classifier.distributionForInstance(createInstance(schema, features))[1]
    }
}
