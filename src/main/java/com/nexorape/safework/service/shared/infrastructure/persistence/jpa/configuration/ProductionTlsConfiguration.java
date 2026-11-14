package com.nexorape.safework.service.shared.infrastructure.persistence.jpa.configuration;
import java.net.URI;
import java.nio.file.*;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.config.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

/** Optional custom CA settings stay out of the normal datasource properties. */
@Configuration(proxyBeanMethods=false) @Profile("prod")
public class ProductionTlsConfiguration {
    private static String setting(Environment env,String key) {
        try { return env.getProperty(key); }
        catch(IllegalArgumentException exception) {
            throw new IllegalStateException("Production TLS settings contain an unresolved placeholder.");
        }
    }
    private static String certificateUrl(Environment env) {
        String url=setting(env,"DB_TRUSTSTORE_URL"),password=setting(env,"DB_TRUSTSTORE_PASSWORD");
        if((url==null || url.isBlank()) && (password==null || password.isBlank())) return null;
        if(url==null || url.isBlank() || password==null || password.isBlank())
            throw new IllegalStateException("An explicit PKCS12 store requires its URL and password. Upload the Aiven CA as ca.pem to configure both automatically.");
        try {
            var uri=URI.create(url);
            if(!"file".equalsIgnoreCase(uri.getScheme()) || !Files.isReadable(Path.of(uri)))
                throw new IllegalArgumentException();
        } catch(IllegalArgumentException exception) {
            throw new IllegalStateException("The production truststore must be a readable local file URL.");
        }
        return url;
    }
    @Bean
    public static BeanFactoryPostProcessor validateProductionCertificate(Environment environment) {
        return factory -> certificateUrl(environment);
    }
    @Bean
    public static BeanPostProcessor configureProductionCertificate(Environment environment) {
        return new BeanPostProcessor() {
            @Override public Object postProcessBeforeInitialization(Object bean,String name) {
                if(bean instanceof HikariDataSource dataSource) {
                    String url=certificateUrl(environment);
                    if(url!=null) {
                        dataSource.addDataSourceProperty("trustCertificateKeyStoreUrl",url);
                        dataSource.addDataSourceProperty("trustCertificateKeyStoreType","PKCS12");
                        dataSource.addDataSourceProperty("trustCertificateKeyStorePassword",setting(environment,"DB_TRUSTSTORE_PASSWORD"));
                        dataSource.addDataSourceProperty("fallbackToSystemTrustStore","false");
                    }
                }
                return bean;
            }
        };
    }
}
