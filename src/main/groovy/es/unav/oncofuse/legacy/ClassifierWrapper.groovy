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
    Instances trainingData
    SerializedClassifier sc
    String schema = "@RELATION ONCOFUSE\n" +
            "@ATTRIBUTE\t5_r_prom1\tNUMERIC\n" +
            "@ATTRIBUTE\t5_r_prom2\tNUMERIC\n" +
            "@ATTRIBUTE\tr_self_pii\tNUMERIC\n" +
            "@ATTRIBUTE\tr_pii\tNUMERIC\n" +
            "@ATTRIBUTE\tr_pii_expr\tNUMERIC\n" +
            "@ATTRIBUTE\tr_CTF\tNUMERIC\n" +
            "@ATTRIBUTE\tr_G\tNUMERIC\n" +
            "@ATTRIBUTE\tr_H\tNUMERIC\n" +
            "@ATTRIBUTE\tr_K\tNUMERIC\n" +
            "@ATTRIBUTE\tr_P\tNUMERIC\n" +
            "@ATTRIBUTE\tr_TF\tNUMERIC\n" +
            "@ATTRIBUTE\t3_r_utr1\tNUMERIC\t\n" +
            "@ATTRIBUTE\tb_CTF\tNUMERIC\n" +
            "@ATTRIBUTE\tb_G\tNUMERIC\n" +
            "@ATTRIBUTE\tb_H\tNUMERIC\n" +
            "@ATTRIBUTE\tb_K\tNUMERIC\n" +
            "@ATTRIBUTE\tb_P\tNUMERIC\n" +
            "@ATTRIBUTE\tb_TF\tNUMERIC\t\n" +
            "@ATTRIBUTE\t3_l_prom1\tNUMERIC\n" +
            "@ATTRIBUTE\t3_l_prom2\tNUMERIC\n" +
            "@ATTRIBUTE\tl_self_pii\tNUMERIC\n" +
            "@ATTRIBUTE\tl_pii\tNUMERIC\n" +
            "@ATTRIBUTE\tl_pii_expr\tNUMERIC\n" +
            "@ATTRIBUTE\tl_CTF\tNUMERIC\n" +
            "@ATTRIBUTE\tl_G\tNUMERIC\n" +
            "@ATTRIBUTE\tl_H\tNUMERIC\n" +
            "@ATTRIBUTE\tl_K\tNUMERIC\n" +
            "@ATTRIBUTE\tl_P\tNUMERIC\n" +
            "@ATTRIBUTE\tl_TF\tNUMERIC\n" +
            "@ATTRIBUTE\t5_l_utr1\tNUMERIC\n" +
            "@ATTRIBUTE\tclass\t{0,1}\n" +
            "@DATA"

    ClassifierWrapper(String trainingSetFname, String model) {
        sc = new SerializedClassifier()
        sc.setModelFile(new File(model))

        ArffLoader loader = new ArffLoader()
        loader.setFile(new File(trainingSetFname))
        //loader.setSource(new ByteArrayInputStream(schema.getBytes()))
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