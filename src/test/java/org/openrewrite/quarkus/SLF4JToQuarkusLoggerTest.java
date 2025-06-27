/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.quarkus;

import org.openrewrite.DocumentExample;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class SLF4JToQuarkusLoggerTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new SLF4JToQuarkusLogger()).typeValidationOptions(TypeValidation.none());
    }

    @DocumentExample
    @Test
    void shouldReplaceSlf4jLoggerWithQuarkusLogAndRemoveInject() {
        rewriteRun(

                java(
                        //language=java
                        """
                                package org.refactor.recipe;
                                import jakarta.enterprise.event.Observes;
                                import jakarta.inject.Inject;
                                import org.slf4j.Logger;

                                public class AppInitializer {

                                    @Inject
                                    Logger logger;

                                    public void startService(@Observes StartupEvent ev) {
                                        logger.info("My Quarkus App - Starting");
                                    }

                                    public void stopService(@Observes ShutdownEvent ev) {
                                        logger.info("My Quarkus App - Stopping");
                                    }
                                }
                                """,
                        //language=java
                        """
                                package org.refactor.recipe;
                                import io.quarkus.logging.Log;
                                import jakarta.enterprise.event.Observes;

                                public class AppInitializer {

                                    public void startService(@Observes StartupEvent ev) {
                                        Log.info("My Quarkus App - Starting");
                                    }

                                    public void stopService(@Observes ShutdownEvent ev) {
                                        Log.info("My Quarkus App - Stopping");
                                    }
                                }
                                """
                )
        );
    }

    @Test
    void shouldReplaceSlf4jLoggerWithQuarkusLog() {
        rewriteRun(
                java(
                        //language=java
                        """
                                package org.refactor.recipe;

                                import org.slf4j.Logger;
                                import jakarta.annotation.security.RolesAllowed;
                                import jakarta.enterprise.context.ApplicationScoped;
                                import jakarta.inject.Inject;
                                import jakarta.transaction.Transactional;
                                import java.util.*;

                                @ApplicationScoped
                                public class MyServiceBean {

                                    @Inject
                                    private Logger logger;

                                    @Inject
                                    private MyBusinessBean businessBean;

                                    public void someMethod(String param0, String param1, Long param2) {
                                        logger.info("log informations for params {} {} {}", param0, param1, param2);
                                    }

                                    public AnimalDTO getAnimal(String param0, Long id) {
                                        logger.debug("search animal id {} with param {}", id, param0);
                                        try {
                                            throw new RuntimeException("fail");
                                        } catch (RuntimeException e) {
                                            logger.error("Cannot search animal with param {}. Service unavailable", param0);
                                            return null;
                                        }
                                    }

                                    public void warnExample(Long id, String param, Exception e) {
                                        logger.warn("No id {} found for param {} : {} {}", id, param, e.getClass().getSimpleName(), e.getMessage());
                                    }

                                    public void errorExample(Exception e) {
                                        logger.error("Error with message {}", e.getMessage());
                                    }
                                }
                                """,
                        //language=java
                        """
                                package org.refactor.recipe;

                                import io.quarkus.logging.Log;
                                import jakarta.annotation.security.RolesAllowed;
                                import jakarta.enterprise.context.ApplicationScoped;
                                import jakarta.inject.Inject;
                                import jakarta.transaction.Transactional;
                                import java.util.*;

                                @ApplicationScoped
                                public class MyServiceBean {

                                    @Inject
                                    private MyBusinessBean businessBean;

                                    public void someMethod(String param0, String param1, Long param2) {
                                        Log.infof("log informations for params %s %s %s", param0, param1, param2);
                                    }

                                    public AnimalDTO getAnimal(String param0, Long id) {
                                        Log.debugf("search animal id %s with param %s", id, param0);
                                        try {
                                            throw new RuntimeException("fail");
                                        } catch (RuntimeException e) {
                                            Log.errorf("Cannot search animal with param %s. Service unavailable", param0);
                                            return null;
                                        }
                                    }

                                    public void warnExample(Long id, String param, Exception e) {
                                        Log.warnf("No id %s found for param %s : %s %s", id, param, e.getClass().getSimpleName(), e.getMessage());
                                    }

                                    public void errorExample(Exception e) {
                                        Log.errorf("Error with message %s", e.getMessage());
                                    }
                                }
                                """
                )
        );
    }

    @Test
    void shouldRemoveSlf4jLoggerFieldIfUnused() {
        rewriteRun(
                java(
                        //language=java
                        """
                                package org.refactor.recipe;

                                import jakarta.inject.Inject;
                                import org.slf4j.Logger;
                                import com.fasterxml.jackson.databind.ObjectMapper;

                                public class MyService {

                                    @Inject
                                    ObjectMapper mapper;

                                    @Inject
                                    Logger logger;

                                    public void someMethod() {
                                        // no logger usage
                                    }
                                }
                                """,
                        //language=java
                        """
                                package org.refactor.recipe;

                                import jakarta.inject.Inject;
                                import com.fasterxml.jackson.databind.ObjectMapper;

                                public class MyService {

                                    @Inject
                                    ObjectMapper mapper;

                                    public void someMethod() {
                                        // no logger usage
                                    }
                                }
                                """
                )
        );
    }

    @Test
    void shouldNotChangeIfNoSlf4jLoggerPresent() {
        rewriteRun(
                java(
                        //language=java
                        """
                                package org.refactor.recipe;

                                public class NoLoggerClass {
                                    public void someMethod() {
                                        System.out.println("Hello");
                                    }
                                }
                                """
                )
        );
    }

    @Test
    void shouldReplaceSlf4jLoggerWithQuarkusLogAndRemoveLoggerFactory() {
        rewriteRun(

                java(
                        //language=java
                        """
                                package org.refactor.recipe;
                                import jakarta.enterprise.event.Observes;
                                import org.slf4j.Logger;

                                public class AppInitializer {

                                    private static final Logger log = LoggerFactory.getLogger(MyServiceClient.class);

                                    public void startService(@Observes StartupEvent ev) {
                                        Log.info("My Quarkus App - Starting");
                                    }

                                    public void stopService(@Observes ShutdownEvent ev) {
                                        log.info("My Quarkus App - Stopping");
                                    }
                                }
                                """,
                        //language=java
                        """
                                package org.refactor.recipe;
                                import io.quarkus.logging.Log;
                                import jakarta.enterprise.event.Observes;

                                public class AppInitializer {

                                    public void startService(@Observes StartupEvent ev) {
                                        Log.info("My Quarkus App - Starting");
                                    }

                                    public void stopService(@Observes ShutdownEvent ev) {
                                        Log.info("My Quarkus App - Stopping");
                                    }
                                }
                                """
                )
        );
    }

    @Test
    void shouldReplaceSlf4jLoggerWithQuarkusLogAndRemoveSlf4jAnnotation() {
        rewriteRun(

                java(
                        //language=java
                        """
                                package org.refactor.recipe;
                                import jakarta.enterprise.event.Observes;
                                import org.slf4j.Logger;
                                import lombok.extern.slf4j.Slf4j;

                                @Slf4j
                                public class AppInitializer {

                                    public void startService(@Observes StartupEvent ev) {
                                        log.info("My Quarkus App - Starting");
                                    }

                                    public void stopService(@Observes ShutdownEvent ev) {
                                        log.info("My Quarkus App - Stopping");
                                    }
                                }
                                """,
                        //language=java
                        """
                                package org.refactor.recipe;
                                import io.quarkus.logging.Log;
                                import jakarta.enterprise.event.Observes;

                                public class AppInitializer {

                                    public void startService(@Observes StartupEvent ev) {
                                        Log.info("My Quarkus App - Starting");
                                    }

                                    public void stopService(@Observes ShutdownEvent ev) {
                                        Log.info("My Quarkus App - Stopping");
                                    }
                                }
                                """
                )
        );
    }
}
