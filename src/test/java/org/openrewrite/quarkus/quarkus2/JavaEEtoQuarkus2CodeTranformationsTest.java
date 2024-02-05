package org.openrewrite.quarkus.quarkus2;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class JavaEEtoQuarkus2CodeTranformationsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .classpath("javaee-api"))
          .recipeFromResources("org.openrewrite.quarkus.quarkus2.JavaEEtoQuarkus2CodeMigration");
    }

    @Test
    @DocumentExample
    void javaEEtoQuarkus2CodeTransformationsTest() {
        rewriteRun(
          java(
            //language=java
            """
              package org.acme;
                            
              import javax.ejb.EJB;
              import javax.ejb.Local;
              import javax.ejb.SessionContext;
              import javax.ejb.Singleton;
              import javax.ejb.Stateful;
              import javax.ejb.Stateless;
                            
              import javax.annotation.Resource;
              import javax.persistence.EntityManager;
              import javax.persistence.PersistenceContext;
                                          
              @Stateless
              public class PingEJBSLS {
                         
                  @PersistenceContext
                  private EntityManager entityManager;
                  
                  @Resource
                  private SessionContext context;
                  
                  @EJB
                  private PingEJBLocal pingEJBLocal;
                  
                  @EJB(lookup = "java:global/PingEJBSingleton")
                  private PingEJBSingleton pingEJBSingleton;
                                              
                  public String getMsg() {
                            
                      return "PingEJBSLS: " + pingEJBLocal.getMsg() + " " + pingEJBSingleton.getMsg();
                  }
                            
              }
                            
              @Stateful
              @Local
              public class PingEJBLocal {
                            
                  private static int hitCount;
                            
                  public String getMsg() {
                            
                      return "PingEJBLocal: " + hitCount++;
                  }
                            
              }
                            
              @Singleton
              public class PingEJBSingleton {
                             
                    private static int hitCount;
                    
                    @PersistenceContext
                    private EntityManager entityManager;
                             
                    public String getMsg() {
                             
                         return "PingEJBSingleton: " + hitCount++;
                    }
              }
              """,
            //language=java
            """
              package org.acme;
                            
              import javax.ejb.SessionContext;
              import javax.enterprise.context.ApplicationScoped;
              import javax.enterprise.context.Dependent;
              import javax.enterprise.context.SessionScoped;
              import javax.inject.Inject;
              import javax.annotation.Resource;
              import javax.persistence.EntityManager;
                                                        
              @Dependent
              public class PingEJBSLS {
                         
                  @Inject
                  private EntityManager entityManager;
                  
                  @Resource
                  private SessionContext context;
                  
                  @Inject
                  private PingEJBLocal pingEJBLocal;
                  
                  @Inject
                  private PingEJBSingleton pingEJBSingleton;
                                              
                  public String getMsg() {
                            
                      return "PingEJBSLS: " + pingEJBLocal.getMsg() + " " + pingEJBSingleton.getMsg();
                  }
                            
              }
                            
              @SessionScoped
              public class PingEJBLocal {
                            
                  private static int hitCount;
                            
                  public String getMsg() {
                            
                      return "PingEJBLocal: " + hitCount++;
                  }
                            
              }
                            
              @ApplicationScoped
              public class PingEJBSingleton {
                             
                    private static int hitCount;
                    
                    @Inject
                    private EntityManager entityManager;
                             
                    public String getMsg() {
                             
                         return "PingEJBSingleton: " + hitCount++;
                    }
              }
              """
          )
        );
    }
}
