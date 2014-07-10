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

import es.unav.oncofuse.annotation.BasicAnnotationBuilder
import es.unav.oncofuse.annotation.BasicTranscriptLevelAnnotation
import es.unav.oncofuse.fusion.FusionData

class BasicFusionClassification {
    final ClassifierResult classifierResult
    final FusionData fusion
    final BasicTranscriptLevelAnnotation transcriptLevelAnnotation
    final BasicAnnotationBuilder annotationBuilder

    BasicFusionClassification(ClassifierResult classifierResult, FusionData fusion,
                               BasicTranscriptLevelAnnotation transcriptLevelAnnotation,
                               BasicAnnotationBuilder annotationBuilder) {
        this.classifierResult = classifierResult
        this.fusion = fusion
        this.transcriptLevelAnnotation = transcriptLevelAnnotation
        this.annotationBuilder = annotationBuilder
    }
}
