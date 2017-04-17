package com.epam.reportportal.auth.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.PathProvider;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.paths.RelativePathProvider;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.UiConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.servlet.ServletContext;
import java.security.Principal;

import static com.google.common.base.Predicates.not;
import static com.google.common.base.Predicates.or;
import static springfox.documentation.builders.RequestHandlerSelectors.basePackage;

@Configuration
@EnableSwagger2
@ComponentScan(basePackages = "com.epam.reportportal.auth")
public class Swagger2Configuration {

    @Autowired
    private ServletContext servletContext;

    @Autowired
    @Value("${info.build.version}")
    private String buildVersion;


    @Bean
    public Docket docket(){
        ApiInfo rpInfo = new ApiInfo("Report Portal", "Report Portal UAT documentation", buildVersion, "urn:tos",
                new Contact("EPAM Systems", "https://www.epam.com", ""),
                "GPLv3", "https://www.gnu.org/licenses/licenses.html#GPL");

        Docket rpDocket = new Docket(DocumentationType.SWAGGER_2)
                .ignoredParameterTypes(Principal.class)
                .useDefaultResponseMessages(false)
                .pathProvider(rpPathProvider())
				/* remove default endpoints from listing */
                .select().apis(not(or(
                        basePackage("org.springframework.boot"),
                        basePackage("org.springframework.cloud"))))
                .build();
        //@formatter:on

        rpDocket.apiInfo(rpInfo);
        return rpDocket;
    }

    @Bean
    public PathProvider rpPathProvider() {
        return new RelativePathProvider(servletContext);
    }

    @Bean
    public UiConfiguration uiConfig() {
        return new UiConfiguration(null);
    }

}
