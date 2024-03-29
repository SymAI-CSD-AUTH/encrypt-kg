package org.example.D_MappingGeneration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.example.A_Coordinator.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import java.time.Duration;

import static org.example.A_Coordinator.Pipeline.config;
import static org.example.util.FileHandler.getAbsolutePath;

public class BertMap {

    private class BertmapRequest {
        private String use_case;
        private String dataset_name;
        private boolean run_for_do_mapping;
        private String pontology_path;
        private String dontology_path;
        private String dpv_path;

        public BertmapRequest(String use_case, String dataset_name, boolean run_for_do_mapping,
                              String pontology_path, String dontology_path, String dpv_path)
        {
            this.use_case = use_case;
            this.dataset_name = dataset_name;
            this.run_for_do_mapping = run_for_do_mapping;
            this.pontology_path = getAbsolutePath(pontology_path);
            this.dontology_path = dontology_path;
            this.dpv_path = dpv_path;
        }

        public String getUse_case() {
            return use_case;
        }
        public void setUse_case(String use_case) {
            this.use_case = use_case;
        }
        public String getDataset_name() {
            return dataset_name;
        }
        public void setDataset_name(String dataset_name) {
            this.dataset_name = dataset_name;
        }
        public boolean isRun_for_do_mapping() {
            return run_for_do_mapping;
        }
        public void setRun_for_do_mapping(boolean run_for_do_mapping) {
            this.run_for_do_mapping = run_for_do_mapping;
        }
        public String getPontology_path() {
            return pontology_path;
        }
        public void setPontology_path(String pontology_path) {
            this.pontology_path = pontology_path;
        }
        public String getDontology_path() {
            return dontology_path;
        }
        public void setDontology_path(String dontology_path) {
            this.dontology_path = dontology_path;
        }
        public String getDpv_path() {
            return dpv_path;
        }
        public void setDpv_path(String dpv_path) {
            this.dpv_path = dpv_path;
        }
    }

    Logger LG = LoggerFactory.getLogger(BertMap.class);

    public JsonObject startBertmap(boolean run_for_do_mapping) {
        return startBertmap(run_for_do_mapping, 0);
    }
    // -calls->
    private JsonObject startBertmap(boolean run_for_do_mapping, int attempt) {
        BertmapRequest requestBody = new BertmapRequest(
                config.In.UseCase,
                config.In.DatasetName,
                run_for_do_mapping,
                config.Out.POntology,
                config.DOMap.TgtOntology,
                config.PiiMap.TgtOntology
        );

        try {
            LG.info("Making request to " +Config.BertMapEndpoint+" . Attempt # " + (attempt+1));
            return startBertmap(requestBody);

        }catch (Exception e) { // attempt was not successful. wait 10'' and retry
            if(attempt < 5) {
                try {Thread.sleep(10000);} catch (InterruptedException ignore) {}
                return startBertmap(run_for_do_mapping, ++attempt);
            }else {
                LG.error("Bertmap service unavailable. Max number of attempts (5) reached. Exiting...");
                e.printStackTrace();
                throw new RuntimeException();
            }
        }
    }
    // -calls->
    private JsonObject startBertmap(BertmapRequest requestBody) {

        String response = WebClient.builder()

                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs()
                                .maxInMemorySize(16 * 1024 * 1024)) // Set the buffer size to 16 MB
                        .build()
                ).build()

                .post()
                .uri(Config.BertMapEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofHours(64))
                .block();

        // Parse JSON string to JsonObject
        if (response != null)
            return JsonParser.parseString(response).getAsJsonObject();
        else {
            LG.error("bertmap service returned null!");
            throw new RuntimeException();
        }
    }

}
