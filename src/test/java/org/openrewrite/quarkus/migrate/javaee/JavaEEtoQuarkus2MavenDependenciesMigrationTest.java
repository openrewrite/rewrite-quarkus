/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.quarkus.migrate.javaee;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class JavaEEtoQuarkus2MavenDependenciesMigrationTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.quarkus.migrate.javaee.JavaEEtoQuarkus2Migration");
    }

    @Test
    @DocumentExample
    void convertJavaEEToQuarkusDependencies1() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(2),
          // language=xml
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.openrewrite.sample</groupId>
                <name>Sample Java EE7 EJB Module</name>
                <artifactId>ee7-ejb</artifactId>
                <version>1.0-SNAPSHOT</version>
                <packaging>war</packaging>

                <properties>
                  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                  <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
                  <maven.compiler.source>1.8</maven.compiler.source>
                  <maven.compiler.target>1.8</maven.compiler.target>
                </properties>

                <dependencies>
                  <dependency>
                    <groupId>javax</groupId>
                    <artifactId>javaee-api</artifactId>
                    <version>7.0</version>
                    <scope>provided</scope>
                  </dependency>
                  <dependency>
                    <groupId>javax.annotation</groupId>
                    <artifactId>javax.annotation-api</artifactId>
                    <version>1.3.2</version>
                    <scope>provided</scope>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.openrewrite.sample</groupId>
                <artifactId>ee7-ejb</artifactId>
                <version>1.0-SNAPSHOT</version>
                <name>Sample Java EE7 EJB Module</name>

                <properties>
                  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                  <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
                  <maven.compiler.source>11</maven.compiler.source>
                  <maven.compiler.target>11</maven.compiler.target>
                </properties>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>io.quarkus.platform</groupId>
                      <artifactId>quarkus-bom</artifactId>
                      <version>2.16.12.Final</version>
                      <type>pom</type>
                      <scope>import</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>

                <dependencies>
                  <dependency>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-arc</artifactId>
                  </dependency>
                  <dependency>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-hibernate-orm</artifactId>
                  </dependency>
                  <dependency>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-jdbc-h2</artifactId>
                  </dependency>
                  <dependency>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-resteasy</artifactId>
                  </dependency>
                  <dependency>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-resteasy-jackson</artifactId>
                  </dependency>
                  <dependency>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-undertow</artifactId>
                  </dependency>
                  <dependency>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-junit5</artifactId>
                    <scope>test</scope>
                  </dependency>
                  <dependency>
                    <groupId>io.rest-assured</groupId>
                    <artifactId>rest-assured</artifactId>
                    <scope>test</scope>
                  </dependency>
                </dependencies>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>io.quarkus.platform</groupId>
                      <artifactId>quarkus-maven-plugin</artifactId>
                      <version>2.16.12.Final</version>
                      <executions>
                        <execution>
                          <goals>
                            <goal>build</goal>
                            <goal>generate-code</goal>
                            <goal>generate-code-tests</goal>
                          </goals>
                        </execution>
                      </executions>
                    </plugin>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <version>3.13.0</version>
                      <configuration>
                        <compilerArgs>
                          <arg>-parameters</arg>
                        </compilerArgs>
                      </configuration>
                    </plugin>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-surefire-plugin</artifactId>
                      <version>3.3.1</version>
                      <configuration>
                        <systemPropertyVariables>
                          <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager>
                          <maven.home>${maven.home}</maven.home>
                        </systemPropertyVariables>
                      </configuration>
                    </plugin>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-failsafe-plugin</artifactId>
                      <version>3.3.1</version>
                      <executions>
                        <execution>
                          <goals>
                            <goal>integration-test</goal>
                            <goal>verify</goal>
                          </goals>
                        </execution>
                      </executions>
                      <configuration>
                        <systemPropertyVariables>
                          <native.image.path>${project.build.directory}/${project.build.finalName}-runner</native.image.path>
                          <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager>
                          <maven.home>${maven.home}</maven.home>
                        </systemPropertyVariables>
                      </configuration>
                    </plugin>
                  </plugins>
                </build>
                <profiles>
                  <profile>
                    <id>native</id>
                    <activation>
                      <property>
                        <name>native</name>
                      </property>
                    </activation>
                    <properties>
                      <skipITs>false</skipITs>
                      <quarkus.package.type>native</quarkus.package.type>
                    </properties>
                  </profile>
                </profiles>
              </project>
              """
          )
        );
    }

    @Test
    void convertJavaEEToQuarkusDependencies2() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(2),
          // language=xml
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8" ?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.dummyapp</groupId>
                <artifactId>oms-winter</artifactId>
                <version>0.0.1-SNAPSHOT</version>
                <name>oms-winter</name>
                <packaging>war</packaging>
                <description>Demo project for Java EE 7 JAX-RS, CDI, EJB, JPA, JTA</description>

                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <configuration>
                        <source>8</source>
                        <target>8</target>
                      </configuration>
                    </plugin>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-war-plugin</artifactId>
                      <version>3.2.3</version>
                      <configuration>
                        <failOnMissingWebXml>false</failOnMissingWebXml>
                      </configuration>
                    </plugin>
                  </plugins>
                </build>
                <dependencies>
                  <dependency>
                    <groupId>javax</groupId>
                    <artifactId>javaee-api</artifactId>
                    <version>7.0</version>
                    <scope>provided</scope>
                  </dependency>
                  <dependency>
                    <groupId>org.apache.commons</groupId>
                    <artifactId>commons-lang3</artifactId>
                    <version>3.12.0</version>
                  </dependency>
                  <dependency>
                    <groupId>commons-beanutils</groupId>
                    <artifactId>commons-beanutils</artifactId>
                    <version>1.9.4</version>
                  </dependency>
                  <dependency>
                    <groupId>org.projectlombok</groupId>
                    <artifactId>lombok</artifactId>
                    <version>1.18.30</version>
                    <scope>provided</scope>
                  </dependency>
                  <dependency>
                    <groupId>com.fasterxml.jackson.jaxrs</groupId>
                    <artifactId>jackson-jaxrs-json-provider</artifactId>
                    <version>2.12.3</version>
                  </dependency>
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13</version>
                    <scope>test</scope>
                  </dependency>
                  <dependency>
                    <groupId>org.mockito</groupId>
                    <artifactId>mockito-core</artifactId>
                    <version>3.2.4</version>
                    <scope>test</scope>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <?xml version="1.0" encoding="UTF-8" ?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.dummyapp</groupId>
                <artifactId>oms-winter</artifactId>
                <version>0.0.1-SNAPSHOT</version>
                <name>oms-winter</name>
                <description>Demo project for Java EE 7 JAX-RS, CDI, EJB, JPA, JTA</description>
                <properties>
                  <maven.compiler.source>11</maven.compiler.source>
                  <maven.compiler.target>11</maven.compiler.target>
                </properties>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>io.quarkus.platform</groupId>
                      <artifactId>quarkus-bom</artifactId>
                      <version>2.16.12.Final</version>
                      <type>pom</type>
                      <scope>import</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>

                <dependencies>
                  <dependency>
                    <groupId>org.apache.commons</groupId>
                    <artifactId>commons-lang3</artifactId>
                  </dependency>
                  <dependency>
                    <groupId>commons-beanutils</groupId>
                    <artifactId>commons-beanutils</artifactId>
                    <version>1.9.4</version>
                  </dependency>
                  <dependency>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-arc</artifactId>
                  </dependency>
                  <dependency>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-hibernate-orm</artifactId>
                  </dependency>
                  <dependency>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-jdbc-h2</artifactId>
                  </dependency>
                  <dependency>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-resteasy</artifactId>
                  </dependency>
                  <dependency>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-resteasy-jackson</artifactId>
                  </dependency>
                  <dependency>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-undertow</artifactId>
                  </dependency>
                  <dependency>
                    <groupId>org.projectlombok</groupId>
                    <artifactId>lombok</artifactId>
                    <version>1.18.34</version>
                    <scope>provided</scope>
                  </dependency>
                  <dependency>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-junit5</artifactId>
                    <scope>test</scope>
                  </dependency>
                  <dependency>
                    <groupId>io.rest-assured</groupId>
                    <artifactId>rest-assured</artifactId>
                    <scope>test</scope>
                  </dependency>
                  <dependency>
                    <groupId>com.fasterxml.jackson.jaxrs</groupId>
                    <artifactId>jackson-jaxrs-json-provider</artifactId>
                    <version>2.12.3</version>
                  </dependency>
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13</version>
                    <scope>test</scope>
                  </dependency>
                  <dependency>
                    <groupId>org.mockito</groupId>
                    <artifactId>mockito-core</artifactId>
                    <version>3.2.4</version>
                    <scope>test</scope>
                  </dependency>
                </dependencies>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <version>3.13.0</version>
                      <configuration>
                        <release>11</release>
                      </configuration>
                    </plugin>
                    <plugin>
                      <groupId>io.quarkus.platform</groupId>
                      <artifactId>quarkus-maven-plugin</artifactId>
                      <version>2.16.12.Final</version>
                      <executions>
                        <execution>
                          <goals>
                            <goal>build</goal>
                            <goal>generate-code</goal>
                            <goal>generate-code-tests</goal>
                          </goals>
                        </execution>
                      </executions>
                    </plugin>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-surefire-plugin</artifactId>
                      <version>3.3.1</version>
                      <configuration>
                        <systemPropertyVariables>
                          <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager>
                          <maven.home>${maven.home}</maven.home>
                        </systemPropertyVariables>
                      </configuration>
                    </plugin>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-failsafe-plugin</artifactId>
                      <version>3.3.1</version>
                      <executions>
                        <execution>
                          <goals>
                            <goal>integration-test</goal>
                            <goal>verify</goal>
                          </goals>
                        </execution>
                      </executions>
                      <configuration>
                        <systemPropertyVariables>
                          <native.image.path>${project.build.directory}/${project.build.finalName}-runner</native.image.path>
                          <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager>
                          <maven.home>${maven.home}</maven.home>
                        </systemPropertyVariables>
                      </configuration>
                    </plugin>
                  </plugins>
                </build>
                <profiles>
                  <profile>
                    <id>native</id>
                    <activation>
                      <property>
                        <name>native</name>
                      </property>
                    </activation>
                    <properties>
                      <skipITs>false</skipITs>
                      <quarkus.package.type>native</quarkus.package.type>
                    </properties>
                  </profile>
                </profiles>
              </project>
              """
          )
        );
    }

}
