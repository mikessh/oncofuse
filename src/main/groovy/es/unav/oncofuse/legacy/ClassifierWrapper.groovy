/*
 * Copyright 2013-2014 Mikhail Shugay (mikhail.shugay@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Last modified on 22.11.2014 by mikesh
 */


package es.unav.oncofuse.legacy


import weka.classifiers.misc.SerializedClassifier
import weka.core.Instance
import weka.core.Instances
import weka.core.converters.ArffLoader

class ClassifierWrapper {
    final Instances trainingData
    final SerializedClassifier sc

    ClassifierWrapper(String trainingSetFname, String model) {
        sc = new SerializedClassifier()
        sc.setModelFile(new File(model))

        ArffLoader loader = new ArffLoader()
        loader.setFile(new File(trainingSetFname))
        trainingData = loader.getDataSet()
        trainingData.setClassIndex(trainingData.numAttributes() - 1)
    }

    private Instance fusion2Instance(List<Double> featureVector) {
        double[] arr = new double[featureVector.size()]
        featureVector.eachWithIndex { it, ind -> arr[ind] = it }
        Instance inst = new Instance(1.0, arr)
        featureVector.eachWithIndex { it, ind -> if (Double.isNaN(it)) inst.setMissing(ind) }
        inst.setDataset(trainingData)
        return inst
    }

    double[] getPValue(List<Double> featureVector) {
        sc.distributionForInstance(fusion2Instance(featureVector))
    }
}