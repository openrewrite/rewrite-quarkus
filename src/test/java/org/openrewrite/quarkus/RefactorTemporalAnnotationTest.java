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

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class RefactorTemporalAnnotationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new RefactorTemporalAnnotation(null))
          .parser(JavaParser.fromJavaVersion()
            .classpath("jakarta.persistence-api"));
    }

    @Test
    void shouldRemoveTemporalAnnotationAndKeepOtherAnnotations() {
        rewriteRun(
          //language=java
          java(
            """
              package org.refactor.model;

              import jakarta.persistence.Column;
              import jakarta.persistence.Entity;
              import jakarta.persistence.Id;
              import jakarta.persistence.Table;
              import jakarta.persistence.Temporal;

              import java.util.Date;

              @Entity
              @Table(name = "rent_house")
              public class RentHouseEntity {
                  @Id
                  @Column(name = "rent_house_id")
                  private Long id;

                  @Column(name = "status")
                  private String status;

                  @Column(name = "start_date")
                  @Temporal(TemporalType.DATE)
                  private Date startDate;

                  @Column(name = "end_date")
                  @Temporal(TemporalType.DATE)
                  private Date endDate;

                  @Column(name = "creation_date")
                  @Temporal(TemporalType.TIMESTAMP)
                  private Date creationDate;
              }
              """,
            """
              package org.refactor.model;

              import jakarta.persistence.Column;
              import jakarta.persistence.Entity;
              import jakarta.persistence.Id;
              import jakarta.persistence.Table;

              import java.time.LocalDate;
              import java.time.LocalDateTime;

              @Entity
              @Table(name = "rent_house")
              public class RentHouseEntity {
                  @Id
                  @Column(name = "rent_house_id")
                  private Long id;

                  @Column(name = "status")
                  private String status;

                  @Column(name = "start_date")
                  private LocalDate startDate;

                  @Column(name = "end_date")
                  private LocalDate endDate;

                  @Column(name = "creation_date")
                  private LocalDateTime creationDate;
              }
              """
          )
        );
    }

    @Test
    void shouldChangeNothing() {
        rewriteRun(
          //language=java
          java(
            """
              package org.refactor.model;

              import jakarta.persistence.Entity;
              import jakarta.persistence.Table;
              import java.util.Date;

              @Entity
              @Table(name = "some_entity")
              public class SomeEntity {
                  private Date createdOn;
              }
              """
          )
        );
    }

    @Test
    void shouldRemoveTemporalAnnotationAndUseGoodType() {
        rewriteRun(
          //language=java
          java(
            """
              package org.refactor.model;

              import java.util.Date;
              import jakarta.persistence.Temporal;
              import jakarta.persistence.TemporalType;
              import jakarta.persistence.Entity;
              import jakarta.persistence.Table;

              @Entity
              @Table(name = "some_entity")
              public class MultiTemporalEntity {
                  @Temporal(TemporalType.DATE)
                  private Date dateField;

                  @Temporal(TemporalType.TIMESTAMP)
                  private Date timestampField;

                  @Temporal(TemporalType.TIME)
                  private Date timeField;
              }
              """,
            """
              package org.refactor.model;

              import jakarta.persistence.Entity;
              import jakarta.persistence.Table;

              import java.time.LocalDate;
              import java.time.LocalDateTime;
              import java.time.LocalTime;

              @Entity
              @Table(name = "some_entity")
              public class MultiTemporalEntity {
                  private LocalDate dateField;

                  private LocalDateTime timestampField;

                  private LocalTime timeField;
              }
              """
          )
        );
    }

    @Test
    void shouldRemoveTemporalAnnotationAndUseLocalDateTimeTypeWhenStaticImport() {
        rewriteRun(
          //language=java
          java(
            """
              package org.refactor.model;

              import java.util.Date;
              import static jakarta.persistence.TemporalType.DATE;
              import jakarta.persistence.Temporal;
              import jakarta.persistence.Entity;
              import jakarta.persistence.Table;

              @Entity
              @Table(name = "some_entity")
              public class SomeEntity {
                  @Temporal(DATE)
                  private Date dateField;
              }
              """,
            """
              package org.refactor.model;

              import jakarta.persistence.Entity;
              import jakarta.persistence.Table;

              import java.time.LocalDate;

              @Entity
              @Table(name = "some_entity")
              public class SomeEntity {
                  private LocalDate dateField;
              }
              """
          )
        );
    }

    @Test
    void shouldRemoveTemporalAnnotationWithOffsetDateTime() {
        rewriteRun(
          spec -> spec.recipe(new RefactorTemporalAnnotation(true)),
          //language=java
          java(
            """
              package org.refactor.model;

              import java.util.Date;
              import jakarta.persistence.Temporal;
              import jakarta.persistence.TemporalType;
              import jakarta.persistence.Entity;
              import jakarta.persistence.Table;

              @Entity
              @Table(name = "some_entity")
              public class MultiTemporalEntity {
                  @Temporal(TemporalType.DATE)
                  private Date dateField;

                  @Temporal(TemporalType.TIMESTAMP)
                  private Date timestampField;

                  @Temporal(TemporalType.TIME)
                  private Date timeField;
              }
              """,
            """
              package org.refactor.model;

              import jakarta.persistence.Entity;
              import jakarta.persistence.Table;

              import java.time.LocalDate;
              import java.time.LocalTime;
              import java.time.OffsetDateTime;

              @Entity
              @Table(name = "some_entity")
              public class MultiTemporalEntity {
                  private LocalDate dateField;

                  private OffsetDateTime timestampField;

                  private LocalTime timeField;
              }
              """
          )
        );
    }
}
