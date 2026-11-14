package com.nexorape.safework.service;
import com.nexorape.safework.service.shared.infrastructure.persistence.jpa.configuration.ProductionTlsConfiguration;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class ProductionTlsConfigurationTests {
    @TempDir Path directory;
    void validate(MockEnvironment env) {
        ProductionTlsConfiguration.validateProductionCertificate(env).postProcessBeanFactory(new DefaultListableBeanFactory());
    }
    @Test void absentCustomStoreLeavesTheJvmTrustConfigurationUntouched() {
        var env=new MockEnvironment();
        assertDoesNotThrow(()->validate(env));
        try(var pool=new HikariDataSource()) {
            ProductionTlsConfiguration.configureProductionCertificate(env).postProcessBeforeInitialization(pool,"dataSource");
            assertTrue(pool.getDataSourceProperties().isEmpty());
        }
    }
    @Test void incompleteExplicitStoreFailsBeforeTheConnectionPool() {
        assertThrows(IllegalStateException.class,()->validate(new MockEnvironment().withProperty("DB_TRUSTSTORE_URL","file:///not-used.p12")));
    }
    @Test void unresolvedPlaceholderNeverReachesTheDriver() {
        assertThrows(IllegalStateException.class,()->validate(new MockEnvironment().withProperty("DB_TRUSTSTORE_URL","${DB_TRUSTSTORE_URL}").withProperty("DB_TRUSTSTORE_PASSWORD","synthetic")));
    }
    @Test void unreadableStoreHasAClearConfigurationError() {
        assertThrows(IllegalStateException.class,()->validate(new MockEnvironment().withProperty("DB_TRUSTSTORE_URL",directory.resolve("missing.p12").toUri().toString()).withProperty("DB_TRUSTSTORE_PASSWORD","synthetic")));
    }
    @Test void aReadableStoreIsAppliedWithExactConnectorPropertyNames()throws Exception {
        var file=Files.write(directory.resolve("synthetic.p12"),new byte[0]);
        var env=new MockEnvironment().withProperty("DB_TRUSTSTORE_URL",file.toUri().toString()).withProperty("DB_TRUSTSTORE_PASSWORD","synthetic");
        assertDoesNotThrow(()->validate(env));
        try(var pool=new HikariDataSource()) {
            ProductionTlsConfiguration.configureProductionCertificate(env).postProcessBeforeInitialization(pool,"dataSource");
            assertEquals(file.toUri().toString(),pool.getDataSourceProperties().getProperty("trustCertificateKeyStoreUrl"));
            assertEquals("PKCS12",pool.getDataSourceProperties().getProperty("trustCertificateKeyStoreType"));
            assertEquals("false",pool.getDataSourceProperties().getProperty("fallbackToSystemTrustStore"));
        }
    }
}
