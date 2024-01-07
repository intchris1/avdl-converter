package ru.beeline.ucp.conversion.resolver;

import java.io.IOException;
import java.nio.file.Path;

import static ru.beeline.ucp.conversion.ConvertIdl.AVSC_EXT;

public interface SchemaResolver {

    default boolean isIncludeSchema(SchemaContext context) {
        return true;
    }

    default String buildResultFilePath(SchemaContext context) {
        return context.outputDirPath() + "/" + context.generatedSchema().getName() + AVSC_EXT;
    }

    default void preProcessFile(Path in) throws IOException {
        //noop
    }
}
