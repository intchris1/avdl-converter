package ru.beeline.ucp.conversion.resolver;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static ru.beeline.ucp.conversion.ConvertIdl.AVDL_EXT;
import static ru.beeline.ucp.conversion.ConvertIdl.AVSC_EXT;

/**
 * Включает в генерацию только первую record из файла
 */
public class IncludeOnlyFirstRecordSchemaResolver implements SchemaResolver {

    private final Map<Path, String> firstRecordByFileNameCache = new HashMap<>();

    @Override
    public boolean isIncludeSchema(SchemaContext context) {
        Path path = context.originalFilePath();
        String firstRecordName = firstRecordByFileNameCache.get(path);
        return firstRecordName != null && context.generatedSchema().getName().equals(firstRecordName);
    }

    @Override
    public String buildResultFilePath(SchemaContext context) {
        String originalFileName = context.originalFilePath().getFileName().toString().replace(AVDL_EXT, AVSC_EXT);
        return context.outputDirPath() + "/" + originalFileName;
    }

    @Override
    public void preProcessFile(Path in) throws IOException {
        rememberFirstRecordName(in);
    }

    private void rememberFirstRecordName(Path in) throws IOException {
        String firstRecordName;
        var idlFileString = FileUtils.readFileToString(new File(in.toUri()), StandardCharsets.UTF_8);
        firstRecordName = StringUtils.substringBetween(idlFileString, "record ", " {");
        firstRecordByFileNameCache.put(in, firstRecordName);
    }
}
