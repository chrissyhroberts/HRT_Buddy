package com.hrtbuddy;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.assertEquals;

public class BackupManagerTest {
    @Test public void csvEscapingRoundTripsQuotedValues() {
        String value="Light, \"tracked\"";
        List<String> parsed=BackupManager.parse("DAILY,pain,"+BackupManager.escape(value)+",2026-10-10,");
        assertEquals("DAILY",parsed.get(0));
        assertEquals(value,parsed.get(2));
    }

    @Test public void escapeLeavesSimpleValuesReadable() {
        assertEquals("\"TUE,SAT\"",BackupManager.escape("TUE,SAT"));
        assertEquals("\"a\"\"b\"",BackupManager.escape("a\"b"));
    }
}
