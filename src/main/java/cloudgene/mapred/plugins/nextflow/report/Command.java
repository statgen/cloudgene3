package cloudgene.mapred.plugins.nextflow.report;

import java.util.Map;

public record Command(String name, Map<String, String> parameters) {
}
