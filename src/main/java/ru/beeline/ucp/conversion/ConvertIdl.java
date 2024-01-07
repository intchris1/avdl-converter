package ru.beeline.ucp.conversion;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.compiler.idl.Idl;
import org.apache.commons.io.FileUtils;
import ru.beeline.ucp.conversion.resolver.SchemaContext;
import ru.beeline.ucp.conversion.resolver.SchemaResolver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConvertIdl {

    public static final String AVDL_EXT = ".avdl";
    public static final CharSequence AVSC_EXT = ".avsc";

    public static void main(String[] args) throws Exception {
        String inDirProp = getInDir();
        Path inDir = Path.of(inDirProp);
        String excludeFilesRegex = System.getProperty("excludeFilesRegex");
        List<Path> files = getAllFilesRecursively(inDir);
        for (Path filePath : files) {
            String filePathString = filePath.toString();
            if (excludeFilesRegex != null && filePathString.matches(excludeFilesRegex)) {
                continue;
            }
            Path outDirForFile = filePath.getParent();
            writeAvroScheme(filePath, outDirForFile);
        }
    }

    private static List<Path> getAllFilesRecursively(Path inDir) throws IOException {
        List<Path> files;
        try (Stream<Path> stream = Files.find(inDir, Integer.MAX_VALUE,
                                                (filePath, fileAttr) -> fileAttr.isRegularFile())
                                        .filter(it -> it.getFileName().toString().endsWith(AVDL_EXT))) {
            files = stream.toList();
        }
        return files;
    }

    private static void writeAvroScheme(Path in, Path out) throws Exception {
        makeDirs(out);
        Idl parser = new Idl(in.toFile());
        Protocol protocol = parser.CompilationUnit();
        List<String> warnings = parser.getWarningsAfterParsing();
        for (String warning : warnings) {
            System.err.println("Warning: " + warning);
        }
        var resolvers = getSchemaResolvers();
        for (SchemaResolver resolver : resolvers) {
            resolver.preProcessFile(in);
        }
        for (Schema schema : protocol.getTypes()) {
            var context = new SchemaContext(in, schema, out);
            for (SchemaResolver resolver : resolvers) {
                if (resolver.isIncludeSchema(context)) {
                    String resultFilePath = resolver.buildResultFilePath(context);
                    String jsonString = toPrettyJson(schema);
                    FileUtils.write(new File(resultFilePath), jsonString, StandardCharsets.UTF_8);
                }
            }
        }
        parser.close();
    }

    private static void makeDirs(Path out) {
        var outFile = out.toFile();
        if (!outFile.exists()) {
            boolean mkdir = outFile.mkdirs();
            if (!mkdir) {
                throw new RuntimeException("Directory creation failed: " + out);
            }
        }
    }

    private static String toPrettyJson(Schema schema) {
        String schemaString = schema.toString(true);
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        JsonElement jsonElement = JsonParser.parseString(schemaString);
        return gson.toJson(jsonElement);
    }

    private static List<SchemaResolver> getSchemaResolvers() {
        String includeRecordsPredicateClass = System.getProperty("schemaResolvers", "");
        var classes = includeRecordsPredicateClass.split(",");
        var resolvers = Arrays.stream(classes)
                              .map(ConvertIdl::getSchemaResolver)
                              .filter(Objects::nonNull)
                              .collect(Collectors.toList());
        if (resolvers.isEmpty()) {
            return List.of(new SchemaResolver() {
            });
        }
        return resolvers;
    }

    private static SchemaResolver getSchemaResolver(String resolverClass) {
        try {
            return (SchemaResolver) Class.forName(resolverClass.trim())
                                         .getDeclaredConstructor()
                                         .newInstance();
        } catch (Exception e) {
            System.err.printf("Error loading schema resolver: %s%n", resolverClass);
            return null;
        }
    }

    private static String getInDir() {
        String property = System.getProperty("inDir");
        if (property == null) {
            throw new IllegalArgumentException("Не указан путь к исходной папке" + ": " + "inDir");
        }
        return property;
    }
}