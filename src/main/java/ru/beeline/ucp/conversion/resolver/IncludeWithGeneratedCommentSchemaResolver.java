package ru.beeline.ucp.conversion.resolver;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.beeline.ucp.conversion.ConvertIdl.AVDL_EXT;
import static ru.beeline.ucp.conversion.ConvertIdl.AVSC_EXT;

/**
 * Включает в генерацию все record по примеру:
 * //generated
 * record Individual {}
 */
public class IncludeWithGeneratedCommentSchemaResolver implements SchemaResolver {

    public static final Pattern GENERATED_REGEX = Pattern.compile("//generated\\s*record (.*?) \\{");
    private final Map<Path, Set<String>> generatedRecordsByFileNameCache = new HashMap<>();

    @Override
    public boolean isIncludeSchema(SchemaContext context) {
        Path path = context.originalFilePath();
        Set<String> generatedRecordNames = generatedRecordsByFileNameCache.get(path);
        return generatedRecordNames != null && generatedRecordNames.contains(context.generatedSchema().getName());
    }

    @Override
    public String buildResultFilePath(SchemaContext context) {
        String originalFileName = context.originalFilePath().getFileName().toString().replace(AVDL_EXT, AVSC_EXT);
        return context.outputDirPath() + "/" + originalFileName;
    }

    @Override
    public void preProcessFile(Path in) throws IOException {
        rememberGeneratedRecordNames(in);
    }

    private void rememberGeneratedRecordNames(Path in) throws IOException {
        var idlFileString = FileUtils.readFileToString(new File(in.toUri()), StandardCharsets.UTF_8);
        Matcher matcher = GENERATED_REGEX.matcher(idlFileString);
        Set<String> names = new HashSet<>();
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        generatedRecordsByFileNameCache.put(in, names);
    }
}
