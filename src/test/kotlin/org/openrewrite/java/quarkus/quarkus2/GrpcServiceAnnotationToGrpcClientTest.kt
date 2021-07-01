/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.quarkus.quarkus2

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class GrpcServiceAnnotationToGrpcClientTest : JavaRecipeTest {
    override val parser: JavaParser = JavaParser.fromJavaVersion()
        .logCompilationWarningsAndErrors(true)
        .classpath("quarkus-grpc", "inject-api")
        .build()

    override val recipe: Recipe
        get() = GrpcServiceAnnotationToGrpcClient()

    companion object {
        private const val greeterBlockingStub: String = """
            package org.openrewrite.example;

            public class GreeterGrpc {
                public class GreeterBlockingStub {
                }
            }
        """
    }

    @Test
    fun grpcServiceAnnotationToGrpcClient() = assertChanged(
        dependsOn = arrayOf(greeterBlockingStub),
        before = """
            package org.openrewrite.example;

            import io.quarkus.grpc.runtime.annotations.GrpcService;

            import javax.inject.Inject;

            public class ExampleResource {
                @Inject
                @GrpcService("client")
                GreeterGrpc.GreeterBlockingStub client;
            }
        """,
        after = """
            package org.openrewrite.example;

            import io.quarkus.grpc.GrpcClient;

            import javax.inject.Inject;

            public class ExampleResource {
                @Inject
                @GrpcClient
                GreeterGrpc.GreeterBlockingStub client;
            }
        """
    )

    @Test
    fun keepValueArgumentIfNameDiffersFromFieldName() = assertChanged(
        dependsOn = arrayOf(greeterBlockingStub),
        before = """
            package org.openrewrite.example;

            import io.quarkus.grpc.runtime.annotations.GrpcService;

            import javax.inject.Inject;

            public class ExampleResource {
                @Inject
                @GrpcService("hello-service")
                GreeterGrpc.GreeterBlockingStub client;
            }
        """,
        after = """
            package org.openrewrite.example;

            import io.quarkus.grpc.GrpcClient;

            import javax.inject.Inject;

            public class ExampleResource {
                @Inject
                @GrpcClient("hello-service")
                GreeterGrpc.GreeterBlockingStub client;
            }
        """
    )
}
