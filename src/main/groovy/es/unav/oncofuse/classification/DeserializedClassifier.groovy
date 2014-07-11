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
import weka.classifiers.misc.SerializedClassifier
import weka.core.Instance
import weka.core.Instances
import weka.core.converters.ArffLoader

class DeserializedClassifier implements Classifier {
    private final Instances trainingData
    private final SerializedClassifier sc = new SerializedClassifier()
    private final String schema

    DeserializedClassifier(String modelFileName, String schemaFileName, boolean fromResources) {
        def mdlFile = Util.getFile(modelFileName, fromResources),
            schemaFile = Util.getFile(schemaFileName, fromResources)

        sc.setModelFile(mdlFile)

        schema = schemaFile.readLines().findAll { it.startsWith("@") }.join("\n")

        def loader = new ArffLoader()
        loader.setSource(new ByteArrayInputStream(schema.getBytes()))
        trainingData = loader.getDataSet()
        trainingData.setClassIndex(trainingData.numAttributes() - 1)
    }

    private Instance features2Instance(String schema, double[] features) {
        if (this.schema != schema)
            throw new IllegalArgumentException("Classifier and instance schema don't match")

        def instance = new Instance(1.0, features)
        features.eachWithIndex { double it, int ind -> if (Double.isNaN(it)) instance.setMissing(ind) }
        instance.setDataset(trainingData)
        instance
    }

    @Override
    ClassifierResult classify(String schema, double[] features) {
        sc.distributionForInstance(features2Instance(schema, features))[1]
    }
}
