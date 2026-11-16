package com.mathtutor.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import javax.sql.DataSource;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PersistenceTestApplication.class)
@Import(PersistenceTestConfig.class)
class PostgresContractTest {
    @Autowired DataSource dataSource;

    @Test
    void repositoriosSeVerificanContraPostgreSQLConPgvector() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("select count(*) from pg_extension where extname = 'vector'")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(1);
            }
        }
    }
}
