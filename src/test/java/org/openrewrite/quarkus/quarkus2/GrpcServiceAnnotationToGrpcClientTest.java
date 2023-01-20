/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.quarkus.quarkus2;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class GrpcServiceAnnotationToGrpcClientTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .classpath("quarkus-grpc", "inject-api")
            .dependsOn(
              """
                package org.openrewrite.example;

                final class GreeterGrpc {
                    public static final class GreeterBlockingStub {
                    }
                }
                """
            )
          ).recipe(new GrpcServiceAnnotationToGrpcClient());
    }

    @Test
    void grpcServiceAnnotationToGrpcClient() {
        rewriteRun(
          java(
            """
              package org.openrewrite.example;

              import io.quarkus.grpc.runtime.annotations.GrpcService;

              import javax.inject.Inject;

              class ExampleResource {
                  @Inject
                  @GrpcService("client")
                  GreeterGrpc.GreeterBlockingStub client;
              }
              """,
            """
              package org.openrewrite.example;

              import io.quarkus.grpc.GrpcClient;

              import javax.inject.Inject;

              class ExampleResource {
                  @Inject
                  @GrpcClient
                  GreeterGrpc.GreeterBlockingStub client;
              }
              """
          )
        );
    }

    @Test
    void keepValueArgumentIfNameDiffersFromFieldName() {
        rewriteRun(
          java(
            """
              package org.openrewrite.example;

              import io.quarkus.grpc.runtime.annotations.GrpcService;

              import javax.inject.Inject;

              class ExampleResource {
                  @Inject
                  @GrpcService("hello-service")
                  GreeterGrpc.GreeterBlockingStub client;
              }
              """,
            """
              package org.openrewrite.example;

              import io.quarkus.grpc.GrpcClient;

              import javax.inject.Inject;

              class ExampleResource {
                  @Inject
                  @GrpcClient("hello-service")
                  GreeterGrpc.GreeterBlockingStub client;
              }
              """
          )
        );
    }

}
