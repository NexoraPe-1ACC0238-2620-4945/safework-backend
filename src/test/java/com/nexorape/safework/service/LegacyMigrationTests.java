package com.nexorape.safework.service;
import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import java.sql.*;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
/** The only destructive fixture reset is guarded by the local server's actual data directory. */
class LegacyMigrationTests {
    @Test void explicitLegacyMigrationPreservesDataAndRemovesGlobalAdminAuthority() throws Exception {
        assertEquals("127.0.0.1",System.getenv("DB_HOST"));assertEquals("33319",System.getenv("DB_PORT"));
        assertEquals("safework_alternative_test",System.getenv("DB_NAME"));
        String url="jdbc:mysql://127.0.0.1:33319/safework_legacy_schema?sslMode=DISABLED&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        String user=System.getenv("DB_USERNAME"),password=System.getenv("DB_PASSWORD");
        try(var c=DriverManager.getConnection(url,user,password);var s=c.createStatement();var r=s.executeQuery("SELECT @@datadir,DATABASE()")) {
            assertTrue(r.next());String actual=r.getString(1).replace('\\','/').toLowerCase(Locale.ROOT);
            String directory=System.getenv("SAFEWORK_TEST_MYSQL_DATA_DIR");
            Path expectedPath=directory==null?Path.of(".local","mysql-data"):Path.of(directory);
            String expected=expectedPath.toAbsolutePath().normalize().toString().replace('\\','/').toLowerCase(Locale.ROOT)+"/";
            assertEquals(expected,actual.endsWith("/")?actual:actual+"/");
            assertEquals("safework_legacy_schema",r.getString(2));
        }
        // This schema is exclusively a synthetic migration fixture, never a runtime database.
        Flyway.configure().dataSource(url,user,password).cleanDisabled(false).load().clean();
        Flyway.configure().dataSource(url,user,password).target("1").load().migrate();
        String hash=new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(UUID.randomUUID().toString());
        long company,admin,roleless,incident;
        try(var c=DriverManager.getConnection(url,user,password)) {
            company=insert(c,"INSERT INTO companies(created_at,updated_at,active,name) VALUES(UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),1,'Synthetic Migration Company')");
            admin=insert(c,"INSERT INTO users(created_at,updated_at,email,enabled,full_name,password_hash,security_version,fk_company_id) VALUES(UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),'migration.admin@example.test',1,'Synthetic Legacy Admin',?,7,?)",hash,company);
            roleless=insert(c,"INSERT INTO users(created_at,updated_at,email,enabled,full_name,password_hash,security_version,fk_company_id) VALUES(UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),'migration.roleless@example.test',1,'Synthetic Roleless',?,3,?)",hash,company);
            try(var s=c.createStatement()) {
                s.executeUpdate("INSERT INTO roles(name) VALUES('ADMIN'),('WORKER'),('EMPLOYER')");
                s.executeUpdate("INSERT INTO user_roles(user_id,role_id) SELECT "+admin+",id FROM roles WHERE name='ADMIN'");
                s.executeUpdate("INSERT INTO user_sessions(id,company_id,created_at,expires_at,roles_snapshot,security_version,user_id) VALUES(UNHEX('00000000000000000000000000000001'),"+company+",UTC_TIMESTAMP(6),DATE_ADD(UTC_TIMESTAMP(6),INTERVAL 1 DAY),'ADMIN',7,"+admin+")");
            }
            incident=insert(c,"INSERT INTO incidents(created_at,updated_at,description,location,status,title,version,fk_company_id,created_by_user_id) VALUES(UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),'Synthetic','Synthetic','OPEN','Migration preservation',0,?,?)",company,admin);
            try(var s=c.createStatement()) {s.executeUpdate("INSERT INTO notifications(id,company_id,recipient_id,status,subject) VALUES(UNHEX('00000000000000000000000000000002'),"+company+",'"+admin+"','SENT','Synthetic migration notice')");s.executeUpdate("DROP TABLE flyway_schema_history");}
        }
        assertThrows(FlywayException.class,()->Flyway.configure().dataSource(url,user,password).load().migrate(),"A populated unreviewed database must not be auto-baselined.");
        var flyway=Flyway.configure().dataSource(url,user,password).baselineVersion("1").load();
        flyway.baseline();assertEquals(2,flyway.migrate().migrationsExecuted);
        try(var c=DriverManager.getConnection(url,user,password);var s=c.createStatement()) {
            try(var r=s.executeQuery("SELECT password_hash,security_version,fk_company_id,membership_active FROM users WHERE id="+admin)) {
                assertTrue(r.next());assertEquals(hash,r.getString(1));assertEquals(8,r.getLong(2));assertEquals(company,r.getLong(3));assertTrue(r.getBoolean(4));
            }
            try(var r=s.executeQuery("SELECT r.name FROM roles r JOIN user_roles ur ON ur.role_id=r.id WHERE ur.user_id="+admin)) {assertTrue(r.next());assertEquals("EMPLOYER",r.getString(1));assertFalse(r.next());}
            try(var r=s.executeQuery("SELECT membership_active FROM users WHERE id="+roleless)) {assertTrue(r.next());assertFalse(r.getBoolean(1));}
            try(var r=s.executeQuery("SELECT COUNT(*) FROM roles WHERE name='ADMIN'")) {r.next();assertEquals(0,r.getInt(1));}
            try(var r=s.executeQuery("SELECT COUNT(*) FROM legacy_admin_assignments WHERE user_id="+admin)) {r.next();assertEquals(1,r.getInt(1));}
            try(var r=s.executeQuery("SELECT COUNT(*) FROM incidents WHERE id="+incident+" AND fk_company_id="+company)) {r.next();assertEquals(1,r.getInt(1));}
            try(var r=s.executeQuery("SELECT COUNT(*) FROM notifications")) {r.next();assertEquals(1,r.getInt(1));}
            try(var r=s.executeQuery("SELECT COUNT(*) FROM user_sessions WHERE revoked_at IS NOT NULL")) {r.next();assertEquals(1,r.getInt(1));}
        }
        assertEquals(0,flyway.migrate().migrationsExecuted);
    }
    private long insert(Connection c,String sql,Object...args)throws Exception {
        try(var p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);p.executeUpdate();
            try(var keys=p.getGeneratedKeys()){assertTrue(keys.next());return keys.getLong(1);}
        }
    }
}
