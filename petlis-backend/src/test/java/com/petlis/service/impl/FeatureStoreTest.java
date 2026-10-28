package com.petlis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class FeatureStoreTest {
    @Test
    void missingGeneratedKeyFallsBackToLastInsertIdOnSameConnection() throws Exception {
        var dataSource = mock(javax.sql.DataSource.class);
        var connection = mock(Connection.class);
        var insert = mock(PreparedStatement.class);
        var emptyKeys = mock(ResultSet.class);
        var query = mock(Statement.class);
        var lastId = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("INSERT INTO qa VALUES(?)", Statement.RETURN_GENERATED_KEYS)).thenReturn(insert);
        when(insert.getGeneratedKeys()).thenReturn(emptyKeys);
        when(emptyKeys.next()).thenReturn(false);
        when(connection.createStatement()).thenReturn(query);
        when(query.executeQuery("SELECT LAST_INSERT_ID()")).thenReturn(lastId);
        when(lastId.next()).thenReturn(true);
        when(lastId.getLong(1)).thenReturn(41L);
        var store = new FeatureStore(new JdbcTemplate(dataSource), new ObjectMapper());

        assertEquals(41L, store.insert("INSERT INTO qa VALUES(?)", "QA"));
        verify(insert, times(1)).executeUpdate();
        verify(connection).createStatement();
    }
}
