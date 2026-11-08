package com.petlis.service.impl;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import javax.sql.DataSource;
/** JDBC service tests use real JDBC savepoints; production transaction configuration stays unchanged. */
@TestConfiguration
class LegacyBankJdbcTestConfiguration {
 @Bean @Primary DataSourceTransactionManager legacyBankTestTransactions(DataSource source){return new DataSourceTransactionManager(source);}
}
