package com.petlis.service.impl;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AccountCsvTest {
    @Test void spreadsheetExpressionsAreEscaped(){assertEquals("\"'=1+1\"",AccountService.csv("=1+1"));assertEquals("\"a\"\"b\"",AccountService.csv("a\"b"));}
}
