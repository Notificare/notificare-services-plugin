package re.notifica.gradle;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.resources.TextResource;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;

public class NotificareServicesTask extends DefaultTask {
    public final static String JSON_FILE_NAME = "notificare-services.json";
    // Some example of things that match this pattern are:
    // "aBunchOfFlavors/release"
    // "flavor/debug"
    // "test"
    // And here is an example with the capture groups in [square brackets]
    // [a][BunchOfFlavors]/[release]
    public final static Pattern VARIANT_PATTERN = Pattern.compile("(?:([^\\p{javaUpperCase}]+)((?:\\p{javaUpperCase}[^\\p{javaUpperCase}]*)*)\\/)?([^\\/]*)");
    // Some example of things that match this pattern are:
    // "TestTheFlavor"
    // "FlavorsOfTheRainbow"
    // "Test"
    // And here is an example with the capture groups in [square brackets]
    // "[Flavors][Of][The][Rainbow]"
    // Note: Pattern must be applied in a loop, not just once.
    public final static Pattern FLAVOR_PATTERN = Pattern.compile("(\\p{javaUpperCase}[^\\p{javaUpperCase}]*)");

    private File intermediateDir;
    private String variantDir;
    private TextResource packageNameResource;
    private String packageNameString;

    @OutputDirectory
    public File getIntermediateDir() {
        return intermediateDir;
    }

    @Input
    public String getVariantDir() {
        return variantDir;
    }

    @Input
    @Optional
    public TextResource getPackageNameResource() {
        return packageNameResource;
    }

    @Input
    @Optional
    public String getPackageNameString() {
        return packageNameString;
    }

    public void setIntermediateDir(File intermediateDir) {
        this.intermediateDir = intermediateDir;
    }

    public void setVariantDir(String variantDir) {
        this.variantDir = variantDir;
    }

    public void setPackageNameResource(TextResource packageName) {
        this.packageNameResource = packageName;
    }

    public void setPackageNameString(String packageName) {
        this.packageNameString = packageName;
    }


    @TaskAction
    public void action() throws IOException {
        File quickstartFile = null;
        List<String> fileLocations = getJsonLocations(variantDir);
        String searchedLocation = System.lineSeparator();
        for (String location : fileLocations) {
            File jsonFile = getProject().file(location + '/' + JSON_FILE_NAME);
            searchedLocation = searchedLocation + jsonFile.getPath() + System.lineSeparator();
            if (jsonFile.isFile()) {
                quickstartFile = jsonFile;
                break;
            }
        }

        if (quickstartFile == null) {
            quickstartFile = getProject().file(JSON_FILE_NAME);
            searchedLocation = searchedLocation + quickstartFile.getPath();
        }

        if (!quickstartFile.isFile()) {
            throw new GradleException(
                    String.format(
                            "File %s is missing. "
                                    + "The Notificare Services Plugin cannot function without it. %n Searched Location: %s",
                            quickstartFile.getName(), searchedLocation));
        }
        if (packageNameResource == null && packageNameString == null) {
            throw new GradleException(
                    String.format(
                            "PackageNameResource is required: packageNameResource: %s", packageNameResource));
        }

        getProject().getLogger().warn("Parsing json file: " + quickstartFile.getPath());

        // delete content of outputdir.
        deleteFolder(intermediateDir);
        if (!intermediateDir.mkdirs()) {
            throw new GradleException("Failed to create folder: " + intermediateDir);
        }

        JsonElement root = new JsonParser().parse(Files.newReader(quickstartFile, Charsets.UTF_8));

        if (!root.isJsonObject()) {
            throw new GradleException("Malformed root json");
        }

        JsonObject rootObject = root.getAsJsonObject();

        Map<String, String> resValues = new TreeMap<>();
        Map<String, Map<String, String>> resAttributes = new TreeMap<>();

        handleProjectInfo(rootObject, resValues);

        // write the values file.
        File values = new File(intermediateDir, "values");
        if (!values.exists() && !values.mkdirs()) {
            throw new GradleException("Failed to create folder: " + values);
        }

        Files.asCharSink(new File(values, "values.xml"), Charsets.UTF_8)
                .write(getValuesContent(resValues, resAttributes));
    }

    /**
     * Handle project_info/application_id for @string/notificare_application_id, and fill the res map with
     * the read value.
     *
     * @param rootObject the root Json object.
     * @throws IOException
     */
    private void handleProjectInfo(JsonObject rootObject, Map<String, String> resValues)
            throws IOException {
        JsonObject projectInfo = rootObject.getAsJsonObject("project_info");
        if (projectInfo == null) {
            throw new GradleException("Missing project_info object");
        }

        JsonPrimitive applicationId = projectInfo.getAsJsonPrimitive("application_id");
        if (applicationId == null) {
            throw new GradleException("Missing project_info/application_id object");
        }
        resValues.put("notificare_services_application_id", applicationId.getAsString());

        JsonPrimitive applicationKey = projectInfo.getAsJsonPrimitive("application_key");
        if (applicationKey == null) {
            throw new GradleException("Missing project_info/application_key object");
        }
        resValues.put("notificare_services_application_key", applicationKey.getAsString());

        JsonPrimitive applicationSecret = projectInfo.getAsJsonPrimitive("application_secret");
        if (applicationSecret == null) {
            throw new GradleException("Missing project_info/application_secret object");
        }
        resValues.put("notificare_services_application_secret", applicationSecret.getAsString());

        JsonPrimitive useTestApi = projectInfo.getAsJsonPrimitive("use_test_api");
        if (useTestApi != null && useTestApi.getAsBoolean()) {
            resValues.put("notificare_services_use_test_api", "true");
        }

    }

    private static String getValuesContent(
            Map<String, String> values, Map<String, Map<String, String>> attributes) {
        StringBuilder sb = new StringBuilder(256);

        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<resources>\n");

        for (Map.Entry<String, String> entry : values.entrySet()) {
            String name = entry.getKey();
            sb.append("    <string name=\"").append(name).append("\" translatable=\"false\"");
            if (attributes.containsKey(name)) {
                for (Map.Entry<String, String> attr : attributes.get(name).entrySet()) {
                    sb.append(" ").append(attr.getKey()).append("=\"").append(attr.getValue()).append("\"");
                }
            }
            sb.append(">").append(entry.getValue()).append("</string>\n");
        }

        sb.append("</resources>\n");

        return sb.toString();
    }

    private static void deleteFolder(final File folder) {
        if (!folder.exists()) {
            return;
        }
        File[] files = folder.listFiles();
        if (files != null) {
            for (final File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file);
                } else {
                    if (!file.delete()) {
                        throw new GradleException("Failed to delete: " + file);
                    }
                }
            }
        }
        if (!folder.delete()) {
            throw new GradleException("Failed to delete: " + folder);
        }
    }

    private String getPackageName() {
        if (packageNameString == null) {
            return packageNameResource.asString();
        }
        return packageNameString;
    }

    private static List<String> splitVariantNames(String variant) {
        if (variant == null) {
            return new ArrayList<>();
        }
        List<String> flavors = new ArrayList<>();
        Matcher flavorMatcher = FLAVOR_PATTERN.matcher(variant);
        while (flavorMatcher.find()) {
            String match = flavorMatcher.group(1);
            if (match != null) {
                flavors.add(match.toLowerCase());
            }
        }
        return flavors;
    }

    private static long countSlashes(String input) {
        return input.codePoints().filter(x -> x == '/').count();
    }

    static List<String> getJsonLocations(String variantDirname) {
        Matcher variantMatcher = VARIANT_PATTERN.matcher(variantDirname);
        List<String> fileLocations = new ArrayList<>();
        if (!variantMatcher.matches()) {
            return fileLocations;
        }
        List<String> flavorNames = new ArrayList<>();
        if (variantMatcher.group(1) != null) {
            flavorNames.add(variantMatcher.group(1).toLowerCase());
        }
        flavorNames.addAll(splitVariantNames(variantMatcher.group(2)));
        String buildType = variantMatcher.group(3);
        String flavorName = variantMatcher.group(1) + variantMatcher.group(2);
        fileLocations.add("src/" + flavorName + "/" + buildType);
        fileLocations.add("src/" + buildType + "/" + flavorName);
        fileLocations.add("src/" + flavorName);
        fileLocations.add("src/" + buildType);
        fileLocations.add("src/" + flavorName + capitalize(buildType));
        fileLocations.add("src/" + buildType);
        String fileLocation = "src";
        for (String flavor : flavorNames) {
            fileLocation += "/" + flavor;
            fileLocations.add(fileLocation);
            fileLocations.add(fileLocation + "/" + buildType);
            fileLocations.add(fileLocation + capitalize(buildType));
        }
        fileLocations = fileLocations.stream().distinct().sorted(Comparator.comparing(NotificareServicesTask::countSlashes)).collect(toList());
        return fileLocations;
    }

    public static String capitalize(String s) {
        if (s.length() == 0) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
