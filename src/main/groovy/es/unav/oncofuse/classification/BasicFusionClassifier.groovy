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

import es.unav.oncofuse.annotation.BasicFusionAnnotation
import es.unav.oncofuse.annotation.BasicTranscriptLevelAnnotation

class BasicFusionClassifier {
    final Classifier classifier

    BasicFusionClassifier(Classifier classifier) {
        this.classifier = classifier
    }

    BasicFusionClassification classify(BasicFusionAnnotation fusionAnnotation) {
        BasicTranscriptLevelAnnotation bestTranscriptLevelAnnotation = null
        ClassifierResult bestClassifierResult = ClassifierResult.BLANK

        fusionAnnotation.annotations.each { BasicTranscriptLevelAnnotation transcriptLevelAnnotation ->
            def classifierResult = classifier.classify(transcriptLevelAnnotation.featureArray())
            if (classifierResult.p < bestClassifierResult.p) {
                bestTranscriptLevelAnnotation = transcriptLevelAnnotation
                bestClassifierResult = classifierResult
            }
        }

        new BasicFusionClassification(bestClassifierResult, fusionAnnotation.fusion,
                bestTranscriptLevelAnnotation, fusionAnnotation.annotationBuilder)
    }
}
