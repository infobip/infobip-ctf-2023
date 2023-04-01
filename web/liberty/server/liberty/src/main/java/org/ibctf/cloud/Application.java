package org.ibctf.cloud;

import io.micronaut.runtime.Micronaut;

import java.nio.file.Path;

public class Application {

    public static final Path FOLDER_DATA = Path.of("data");
    public static final Path FOLDER_FILES = Path.of(FOLDER_DATA.toString(), "files");
    public static final Path FOLDER_CONFIGS = Path.of(FOLDER_DATA.toString(), "configs");
    public static final Path FOLDER_SCRIPTS = Path.of(FOLDER_DATA.toString(), "scripts");

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}