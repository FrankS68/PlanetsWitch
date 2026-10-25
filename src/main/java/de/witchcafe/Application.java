package de.witchcafe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.lumo.Lumo;

@SpringBootApplication
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet(Lumo.COMPACT_STYLESHEET)
@ColorScheme(ColorScheme.Value.DARK)
// @NpmPackage(value = "apexcharts", version = "3.45.2")
// @JsModule("apexcharts") // Vite weiß automatisch, wo in node_modules die main-Datei liegt
// @JsModule("@appreciated/apexcharts-flow/apexcharts-wrapper.ts")
// @NpmPackage(value = "@webcomponents/shadycss", version = "1.11.2")
@EnableNeo4jRepositories(basePackages = "de.witchcafe.planets")
@EntityScan(basePackages = "de.witchcafe.planets")
@EnableScheduling
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
