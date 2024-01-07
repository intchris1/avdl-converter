package ru.beeline.ucp.conversion.resolver;

import org.apache.avro.Schema;

import java.nio.file.Path;

public record SchemaContext(Path originalFilePath,
                            Schema generatedSchema,
                            Path outputDirPath) {

}