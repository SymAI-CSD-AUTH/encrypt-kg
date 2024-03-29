package org.example.A_Coordinator.config;

import com.google.gson.JsonObject;
import org.example.A_Coordinator.Inputs.PreprocessingNotification;
import org.example.util.FileHandler;
import org.example.util.JsonUtil;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import static org.example.util.FileHandler.fileExists;
import static org.example.util.FileHandler.getPath;

// =====================================================================================================================

public class Config {

    public static Path WORKDIR = Paths.get(System.getProperty("user.dir"));
    public static boolean IS_DOCKER_ENV = WORKDIR.toString().startsWith("/KnowledgeGraphsApp");

    public static String resourcesPath = IS_DOCKER_ENV ?
              getPath(String.format("%s/data/KnowledgeGraphsJava", WORKDIR))
            : getPath(String.format("%s/.KnowledgeGraphsData/KnowledgeGraphsJava", WORKDIR.getParent()));

    public static boolean DEV_MODE;

// ---------------------------------------------------------------------------------------------------------------------
    // Communication with preprocessing
    // TODO: DO NOT FORGET TO CHANGE VALUES FOR DEPLOYMENT!!!!!

    // GET http://preprocessing-tool:5000/download/files/original/<path:filename>
    public static final String PreprocessingEndpoint =
            IS_DOCKER_ENV ? "http://preprocessing-tool:5000" : "http://localhost:5000";

    private static boolean MAINTAIN_PREPROCESSING_RESULTS = true;

// ---------------------------------------------------------------------------------------------------------------------
    // Define the URL of the Python services and APIS

    public static String AAExpansionEndpoint =
            String.format("http://%s:7531/start_aa_expansion", IS_DOCKER_ENV ? "knowledge-graphs-python" : "localhost");

    public static String BertMapEndpoint =
            String.format("http://%s:7531/start_bertmap", IS_DOCKER_ENV ? "knowledge-graphs-python" : "localhost");

    public static String GraphDBEndpoint =
            String.format("http://%s:7200", IS_DOCKER_ENV ? "graphdb" : "localhost");

// =====================================================================================================================
    public InputPointConfig In;
    public KGOutputsConfig Out;
    public MappingConfig DOMap;
    public MappingConfig PiiMap;
    public PreprocessingNotification notification;


    public Config(String UseCase, String FileExtension, PreprocessingNotification notification) {
        this.notification = notification;
        setConfigParams(UseCase, FileExtension);
        createDirectories();

        LoggerFactory.getLogger(Config.class).info(String.format(
                "\n\tWORKDIR: %s\n\tIN DOCKER ? %s\n\tRESOURCES DIR: %s \n\tDEV_MODE: %b",
                WORKDIR, IS_DOCKER_ENV, resourcesPath, DEV_MODE
        ));
    }

    private void createDirectories() {
        FileHandler.createDirectories(List.of(
                In.DatasetResourcesPath,
                In.DownloadedDataDir,
                In.ProcessedDataDir,
                Out.KGOutputsDir,
                Out.LogDir
        ));
    }


    private void setConfigParams(String UseCase, String FileExtension) {

        String configFilePath = getConfigFilePath(UseCase, FileExtension);
        JsonObject configFile = JsonUtil.readJSON(configFilePath).getAsJsonObject();
        DEV_MODE = configFile.getAsJsonPrimitive("DEV_MODE").getAsBoolean();

        // Inputs parameters -----------------------------------------------------------------------
        In = new InputPointConfig(
                UseCase, FileExtension, configFile.getAsJsonObject("InputDataset"));

        // KG outputs parameters -----------------------------------------------------------------------
        Out = new KGOutputsConfig(
                In.DatasetName, In.DatasetResourcesPath, configFile.getAsJsonObject("KnowledgeGraphsOutputs"));

        // Mapping parameters ------------------------------------------------------------------------------
        DOMap = new MappingConfig(
                configFile.getAsJsonObject("DOMappingParameters"), null, null);

        PiiMap = new MappingConfig(
                configFile.getAsJsonObject("PiiMappingParameters"), In.UseCase, In.DatasetResourcesPath);
    }


    private String getConfigFilePath(String UseCase, String FileExtension) {
        String configFilePath = getPath(String.format(
                "%s/ConfigFiles/%s_%s_Config.json", resourcesPath, UseCase, FileExtension));

        if(InputPointConfig.isCSVlike(FileExtension) && !fileExists(configFilePath)) {
            for(String CSVlikeExtension : InputPointConfig.CSVlikeFileTypes) {
                configFilePath = getPath(String.format(
                        "%s/ConfigFiles/%s_%s_Config.json", resourcesPath, UseCase, CSVlikeExtension));
                if(fileExists(configFilePath))
                    break;
            }
        }
        return configFilePath;
    }



// =====================================================================================================================

    public static class InputPointConfig {
        // input data source
        public String UseCase;
        public String DatasetName;
        public String DatasetResourcesPath;

        public SQLCredentials credentials;

        /** This is the file extension of the original downloaded files, not the processed files */
        public String FileExtension;
        public boolean downloadOriginal;
        public String DownloadedDataDir;
        public String ProcessedDataDir;
        public String DefaultRootClassName;

        public InputPointConfig(String UseCase, String FileExtension, JsonObject inputDatasetParams) {
            this.UseCase = UseCase;
            this.FileExtension = FileExtension;
            this.DatasetName = inputDatasetParams.get("DatasetName").getAsString();
            this.downloadOriginal = inputDatasetParams.get("downloadOriginal").getAsBoolean();

            this.DefaultRootClassName = "Record";
            if("SQL".equals(FileExtension)) {
                JsonObject sqlCredParams = inputDatasetParams.getAsJsonObject("SQL");
                setSQLCredentials(
                        sqlCredParams.get("URL").getAsString(),
                        sqlCredParams.get("User").getAsString(),
                        sqlCredParams.get("Password").getAsString());
            }else{
                JsonObject datasetFolderParams = inputDatasetParams.getAsJsonObject("FilesDataset");
                try {
                    this.DefaultRootClassName = datasetFolderParams.get("DefaultRootClassName").getAsString();
                }catch (UnsupportedOperationException ignore) {}
            }
            setDirPaths();
        }


        protected void setSQLCredentials(String URL, String User, String Password) {
            this.credentials = new SQLCredentials(URL, User, Password);
        }


        private void setDirPaths() {                                                                                                    // String overrideDatasource
            DatasetResourcesPath = getPath(String.format("%s/Use_Case/%s/%s", resourcesPath, UseCase, DatasetName));
            DownloadedDataDir    = getPath(DatasetResourcesPath + "/Downloaded_Data");                                            // overrideDatasource == null ? DatasetResourcesPath + "Downloaded_Data/" : overrideDatasource;
            ProcessedDataDir     = getPath(DatasetResourcesPath + "/Processed_Data");
        }

        public boolean isJSON() {return "json".equals(FileExtension);}
        public boolean isDSON() {return "dcm".equals(FileExtension);}
        public boolean isSQL()  {return credentials != null || "SQL".equals(FileExtension);}

        public boolean isExcel(){return "xlsx".equals(FileExtension);}
        public boolean isCSV()  {return "csv".equals(FileExtension);}
        public boolean isTSV()  {return "tsv".equals(FileExtension);}

        public static Set<String> CSVlikeFileTypes = Set.of("xlsx", "csv", "tsv");
        public boolean isCSVlike(){return CSVlikeFileTypes.contains(FileExtension);}
        public static boolean isCSVlike(String fileExtension){return CSVlikeFileTypes.contains(fileExtension);}

    }

    public static class SQLCredentials {
        public String URL;
        public String User;
        public String Password;
        public SQLCredentials(String URL, String User, String Password) {
            this.URL = URL;
            this.User = User;
            this.Password = Password;
        }
    }

// =====================================================================================================================

    public static class KGOutputsConfig {
        public String KGOutputsDir;
        // preprocessing
        public boolean applyMedAbbrevExpansion;
        public boolean useScispacyEntityLinker;
        public String abbrevExpansionResultsFile;

        // po extraction
        public String POntologyName;
        public String POntology;
        public String POntologyBaseNS;
        public String RootClassName;
        public boolean turnAttributesToClasses;
        public boolean includeInverseAxioms;

        // po to do mappings
        public String PO2DO_Mappings;
        public boolean maintainStoredPreprocessingResults = MAINTAIN_PREPROCESSING_RESULTS;

        // output ontology
        public String RefinedOntology;

        // final knowledge graph
        public String IndividualsTTL;
        public String PathsTXT;
        public String FullGraph;
        public boolean uploadToGraphDB;

        public String LogDir;

        public KGOutputsConfig(String DatasetName, String DatasetResourcesPath, JsonObject kgOutsParams) {
            this(
                    DatasetName, DatasetResourcesPath,
                    kgOutsParams.get("applyMedicalAbbreviationExpansionStep").getAsBoolean(),
                    kgOutsParams.get("useScispacyEntityLinker").getAsBoolean(),
                    kgOutsParams.get("turnAttributesToClasses").getAsBoolean(),
                    kgOutsParams.get("includeInverseAxiom").getAsBoolean(),
                    kgOutsParams.get("uploadToGraphDB").getAsBoolean()
            );
        }

        public KGOutputsConfig(String DatasetName, String DatasetResourcesPath,
                  boolean applyMedAbbrevExpansion, boolean useScispacyEntityLinker,
                  boolean turnAttributesToClasses, boolean includeInverseAxioms,
                  boolean uploadToGraphDB
        ){
            this.turnAttributesToClasses = turnAttributesToClasses;
            this.includeInverseAxioms = includeInverseAxioms;

            this.applyMedAbbrevExpansion = applyMedAbbrevExpansion;
            this.useScispacyEntityLinker = useScispacyEntityLinker;
            // TODO modify if you want to use:
            abbrevExpansionResultsFile = getPath(String.format("%s/Other/abbrevExpansionResults.json", DatasetResourcesPath));

            this.uploadToGraphDB = uploadToGraphDB;

            KGOutputsDir    = getPath(String.format("%s/KG_Outputs/", DatasetResourcesPath));
            POntologyName   = DatasetName;
            POntology       = getPath(KGOutputsDir + "/POntology.ttl");
            POntologyBaseNS = String.format("http://www.example.net/ontologies/%s.owl/", POntologyName);

            PO2DO_Mappings  = getPath(KGOutputsDir + "/PO2DO_Mappings.json");
            RefinedOntology = getPath(KGOutputsDir + "/refinedOntology.ttl");
            IndividualsTTL  = getPath(KGOutputsDir + "/individuals.ttl");
            FullGraph       = getPath(KGOutputsDir + "/fullGraph.ttl");
            LogDir          = getPath(DatasetResourcesPath + "/Log");
            PathsTXT        = getPath(LogDir + "/paths.txt");
        }
    }

// =====================================================================================================================

    public static class MappingConfig {
        // tgt ontology is either DOntology or DPV
        public String TgtOntology;
        public boolean offlineOntology;

        public static final String EXACT_MAPPER = "ExactMapper";
        public static final String BERTMAP = "BERTMap";
        public String Mapper;

        public double BES_HIGH_THRS;
        public double BES_LOW_THRS;
        public double PJ_HIGH_THRS;

        public double PJ_REJECT_THRS;
        public double BES_REJECT_THRS;

        public int DEPTH_THRS;
        public boolean rejectPropertyMaps;

        public String UseCase2DPV_file_path = null;
        public String PiisResultsJsonPath = null;

        public MappingConfig(JsonObject params, String UseCase, String DatasetResourcesPath) {

            TgtOntology = params.get("TgtOntology").getAsString();
            TgtOntology = getPath(String.format("%s/DOntologies/%s", resourcesPath, TgtOntology));
            offlineOntology = params.get("offlineOntology").getAsBoolean();

            Mapper = params.get("Mapper").getAsString();
            BES_HIGH_THRS   = params.get("BES_HIGH_THRS").getAsDouble();
            BES_LOW_THRS    = params.get("BES_LOW_THRS").getAsDouble();
            PJ_HIGH_THRS    = params.get("PJ_HIGH_THRS").getAsDouble();
            PJ_REJECT_THRS  = params.get("PJ_REJECT_THRS").getAsDouble();
            BES_REJECT_THRS = params.get("BES_REJECT_THRS").getAsDouble();
            DEPTH_THRS      = params.get("DEPTH_THRS").getAsInt();
            rejectPropertyMaps = params.get("rejectPropertyMaps").getAsBoolean();

            // for pii cross mapping. stored in the PiiMap
            if(UseCase != null && DatasetResourcesPath != null) {

                PiisResultsJsonPath = getPath(String.format("%s/KG_Outputs/piiResults.json", DatasetResourcesPath)); // in the KG_Outputs subdir

                UseCase2DPV_file_path = getPath(
                        String.format("%s/DPVMappings/%s2DPV.json",
                                resourcesPath, UseCase));
                if(!fileExists(UseCase2DPV_file_path))
                    UseCase2DPV_file_path = null;
            }
        }

        public void printUnsupportedMapperError() {
            System.err.printf("Unsupported mapper %s. Choose %s or %s\n", Mapper, EXACT_MAPPER, BERTMAP);
            throw new RuntimeException();
        }
    }
}


/* if(FileExtension == null) {
       overrideDatasource = datasetFolderParams.get("DownloadedDataDir").getAsString();
       FileExtension = datasetFolderParams.get("FileExtension").getAsString();
}*/