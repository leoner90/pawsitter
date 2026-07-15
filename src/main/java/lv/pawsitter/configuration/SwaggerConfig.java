package lv.pawsitter.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "PawSitter Application",
                version = "0.9.0",
                description = "Service for pet booking management, including user accounts, sitter profiles, bookings, payments, and analytics.",
                termsOfService = "https://example.com/terms",
                contact = @Contact(
                        name = "Support Team",
                        email = "support@example.com",
                        url = "https://example.com"
                ),
                license = @License(
                        name = "Apache 2.0",
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                )
        ),
        servers = {
                @Server(
                        url = "http://localhost:8080",
                        description = "Local development server"
                ),
                @Server(
                        url = "https://api.example.com",
                        description = "Production server"
                )
        }//,

        // ❌ REMOVE FOR THYMELEAF (JWT security is not used in a stateful MVC application)
        // If the frontend will be separate (React, Vue, Angular),
        // then you SHOULD keep this security requirement because JWT will be used.
//        security = {
//                @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
//        }
)

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()

                // ❌ REMOVE FOR THYMELEAF (JWT security is not used)
                // If the frontend will be separate (React, Vue, Angular),
                // then you SHOULD keep this because Swagger must describe JWT authentication.
                // .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))

                // ❌ REMOVE FOR THYMELEAF (JWT security scheme is not needed)
                // If the frontend will be separate (React, Vue, Angular),
                // then you SHOULD keep this because Swagger must expose the JWT scheme.
                // .components(new Components().addSecuritySchemes("Bearer Authentication",
                //         jwtSecurityScheme()))
                ;
    }

    // ❌ REMOVE FOR THYMELEAF (JWT is not used)
    // If the frontend will be separate (React, Vue, Angular),
    // then you SHOULD keep this method because JWT is required.
    /*
    private SecurityScheme jwtSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer");
    }
    */
}