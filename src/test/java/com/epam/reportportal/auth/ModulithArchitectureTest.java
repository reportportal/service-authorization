package com.epam.reportportal.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Test class to verify the Spring Modulith architecture of the application.
 * This test ensures that:
 * - Module boundaries are properly defined
 * - Dependencies between modules are valid
 * - No circular dependencies exist
 * - Module structure follows Spring Modulith conventions
 */
@SpringBootTest
class ModulithArchitectureTest {

    private final ApplicationModules modules = ApplicationModules.of(AuthServerApplication.class);

    @Test
    void verifyModularStructure() {
        modules.verify();
    }

    @Test
    void writeDocumentationSnippets() {
        new Documenter(modules)
            .writeModulesAsPlantUml()
            .writeIndividualModulesAsPlantUml();
    }
}