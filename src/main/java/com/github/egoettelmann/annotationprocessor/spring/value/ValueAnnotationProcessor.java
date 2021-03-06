package com.github.egoettelmann.annotationprocessor.spring.value;

import com.github.egoettelmann.annotationprocessor.spring.value.exceptions.ValueAnnotationException;
import com.github.egoettelmann.annotationprocessor.spring.value.core.ValueAnnotationMetadata;
import com.github.egoettelmann.annotationprocessor.spring.value.reader.ElementReader;
import com.github.egoettelmann.annotationprocessor.spring.value.writer.JsonWriter;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * TODO:
 *  - add configuration options (checkout: https://stackoverflow.com/a/53274771):
 *    - failOnError: allows to ignore any exception occurring during processing
 *    - targetPath: customize output path
 *    - targetFile: customize output file name
 */
@SupportedAnnotationTypes(ElementReader.VALUE_ANNOTATION_CLASS)
public class ValueAnnotationProcessor extends AbstractProcessor {

    private static final String TARGET_PACKAGE = "";
    private static final String TARGET_FILE_NAME = "META-INF/spring-configuration-metadata.json";

    private ElementReader elementReader;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elementReader = new ElementReader(processingEnv.getElementUtils());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }
        List<ValueAnnotationMetadata> metadataList = new ArrayList<>();
        try {
            for (TypeElement annotation : annotations) {
                for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                    metadataList.add(this.elementReader.read(element));
                }
            }
            this.writeToTargetFile(metadataList);
        } catch (Exception e) {
            // TODO: exceptions should be caught at annotation level to continue processing
            e.printStackTrace();
            throw new ValueAnnotationException("Error while processing Value annotations", e);
        }
        return false;
    }

    private void writeToTargetFile(final List<ValueAnnotationMetadata> metadata) {
        try (Writer writer = this.getFileWriter()) {
            JsonWriter jsonWriter = new JsonWriter(writer);
            jsonWriter.write(metadata);
        } catch (IOException ioe) {
            throw new ValueAnnotationException("Could not create target resource file", ioe);
        }
    }

    private Writer getFileWriter() throws IOException {
        return processingEnv.getFiler()
                .createResource(
                        StandardLocation.CLASS_OUTPUT,
                        TARGET_PACKAGE,
                        TARGET_FILE_NAME
                ).openWriter();
    }
}
