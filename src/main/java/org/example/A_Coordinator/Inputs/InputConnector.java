package org.example.A_Coordinator.Inputs;

import org.example.A_Coordinator.Runner;
import org.example.A_Coordinator.config.Config;

import static org.example.A_Coordinator.config.Config.*;

public class InputConnector {

    public static String USE_CASE = HEATH;
    public static String filename = "fileInputFromUI.dcm";


    public InputConnector() {
        Runner runner = new Runner(setupConfig(USE_CASE, filename));
        runner.pipeline();
    }

    private Config setupConfig(String UseCase, String filename) {
        String FileExtension = filename.equals("SQL") ? "SQL" :
                filename.substring(filename.lastIndexOf(".")+1);
        return new Config(UseCase, FileExtension);
    }

    public static void main(String[] args) {
        new InputConnector();
    }
}
