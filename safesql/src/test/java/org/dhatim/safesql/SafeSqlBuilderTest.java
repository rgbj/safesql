package org.dhatim.safesql;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.dhatim.safesql.assertion.Assertions.*;
import org.junit.Test;

public class SafeSqlBuilderTest {

    private static class MySafeSqlizable implements SafeSqlizable {

        private static final String MUST_BE = "SELECT * FROM table WHERE column = ? GROUP BY id";

        @Override
    public void appendTo(SafeSqlBuilder builder) {
            builder.append("SELECT * FROM table WHERE column = ")
                    .param(5)
                    .append(" GROUP BY id");
        }
    }

    @Test
    public void testAppendConstant() {
        assertThat(new SafeSqlBuilder().append("SELECT").append(" * ").append("FROM table").toSafeSql())
                .hasSql("SELECT * FROM table")
                .hasEmptyParameters();
    }

    @Test
    public void testAppendNumber() {
        assertThat(new SafeSqlBuilder().param(5).append(" ").param(1.1).toSafeSql())
                .hasSql("? ?")
                .hasParameters(5, 1.1);
    }

    @Test
    public void testAppendObject() {
        assertThat(new SafeSqlBuilder().param(true).toSafeSql())
                .hasSql("?")
                .hasParameters(true);
    }

    @Test
    public void testAppendIdentifier() {
        assertThat(new SafeSqlBuilder().append("SELECT ").identifier("S21.G00.32.001").toSafeSql())
                .hasSql("SELECT \"S21.G00.32.001\"")
                .hasEmptyParameters();

        assertThat(new SafeSqlBuilder().append("SELECT ").identifier("hello").toSafeSql())
                .hasSql("SELECT hello")
                .hasEmptyParameters();
    }

    @Test
    public void testIdentifier2() {
        assertThat(new SafeSqlBuilder().append("SELECT ").identifier("foo", "bar").toSafeSql())
                .hasSql("SELECT foo.bar")
                .hasEmptyParameters();

        assertThat(new SafeSqlBuilder().append("SELECT ").identifier(null, "baz").toSafeSql())
                .hasSql("SELECT baz")
                .hasEmptyParameters();
    }

    @Test
    public void testAppendEscaped() {
        assertThat(new SafeSqlBuilder().append("SELECT * FORM table WHERE column = ").param("Hello the world").toSafeSql())
                .hasSql("SELECT * FORM table WHERE column = ?")
                .hasParameters("Hello the world");
    }

    @Test
    public void testAppendSafeSql() {
        assertThat(new SafeSqlBuilder().append("SELECT").append(SafeSqlUtils.fromConstant(" * FROM table")).toSafeSql())
                .as("Without parameters")
                .hasSql("SELECT * FROM table")
                .hasEmptyParameters();

        assertThat(new SafeSqlBuilder().append("SELECT ").append(SafeSqlUtils.escape("Hello the world")).toSafeSql())
                .as("With parameters")
                .hasSql("SELECT ?")
                .hasParameters("Hello the world");
    }

    @Test
    public void testAppendSafeSqlizable() {
        assertThat(new SafeSqlBuilder().append(new MySafeSqlizable()).append(" ORDER BY name").toSafeSql())
                .hasSql(MySafeSqlizable.MUST_BE + " ORDER BY name")
                .hasParameters(5);
    }

    @Test
    public void testParams() {
        assertThat(new SafeSqlBuilder().params(1, 2, 3).toSafeSql())
                .hasSql("?, ?, ?")
                .hasParameters(1, 2, 3);
    }

    @Test
    public void testStreamParams() {
        assertThat(new SafeSqlBuilder().params(Stream.of(1, 2, 3)).toSafeSql())
                .hasSql("?, ?, ?")
                .hasParameters(1, 2, 3);
    }

    @Test
    public void testIterableParams() {
        assertThat(new SafeSqlBuilder().params(Arrays.asList(1, 2, 3)).toSafeSql())
                .hasSql("?, ?, ?")
                .hasParameters(1, 2, 3);
    }

    @Test
    public void testLongParam() {
        assertThat(new SafeSqlBuilder().param(1L).toSafeSql())
                .hasSql("?")
                .hasParameters(1L);
    }

    @Test
    public void testAppends() {
        assertThat(new SafeSqlBuilder().append(1).append(1L).toSafeSql())
                .hasSql("11")
                .hasEmptyParameters();
    }

    @Test
    public void testJoined() {
        assertThat(new SafeSqlBuilder().joined(",", Arrays.asList("1", "2", "3")).toSafeSql())
                .hasSql("1,2,3")
                .hasEmptyParameters();
    }

    @Test
    public void testJoinedSafeSqls() {
        SafeSql sql = SafeSql.parameter(1);
        assertThat(new SafeSqlBuilder().joinedSafeSqls(",", Arrays.asList(sql, sql, sql)).toSafeSql())
                .hasSql("?,?,?")
                .hasParameters(1, 1, 1);
    }

    @Test
    public void testAppendJoined() {
        List<SafeSqlizable> list = Arrays.asList(new MySafeSqlizable(), new MySafeSqlizable());

        assertThat(new SafeSqlBuilder().append("(").joinedSqlizables("; ", list).append(")").toSafeSql())
                .hasSql("(" + MySafeSqlizable.MUST_BE + "; " + MySafeSqlizable.MUST_BE + ")")
                .hasParameters(5, 5);
        assertThat(new SafeSqlBuilder().joinedSqlizables(", ", Arrays.asList(new MySafeSqlizable())).toSafeSql())
                .hasSql(MySafeSqlizable.MUST_BE)
                .hasParameters(5);
        assertThat(new SafeSqlBuilder().joinedSafeSqls(", ", Arrays.asList()).toSafeSql())
                .hasEmptySql()
                .hasEmptyParameters();
    }

}
