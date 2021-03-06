package org.dhatim.safesql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class SafeSqlBuilder implements SafeSqlizable {

    static class Position {

        private final int sqlPosition;
        private final int paramPosition;

        private Position(int sqlPosition, int paramPosition) {
            this.sqlPosition = sqlPosition;
            this.paramPosition = paramPosition;
        }

    }

    private static final String DEFAULT_SEPARATOR = ", ";
    private static final char[] HEX_CODE = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    protected final StringBuilder sqlBuilder;
    protected final List<Object> parameters;

    public SafeSqlBuilder() {
        this(new StringBuilder(), new ArrayList<>());
    }

    public SafeSqlBuilder(String query) {
        this(new StringBuilder(query), new ArrayList<>());
    }

    public SafeSqlBuilder(SafeSqlBuilder other) {
        this(new StringBuilder(other.sqlBuilder), new ArrayList<>(other.parameters));
    }

    protected SafeSqlBuilder(StringBuilder stringBuilder, List<Object> parameters) {
        // Without copy buffers
        this.sqlBuilder = stringBuilder;
        this.parameters = parameters;
    }

    /**
     * include integer parameter in SQL with a placeholder <b>?</b>
     *
     * @param num integer parameter
     * @return a reference of this object
     */
    public SafeSqlBuilder param(int num) {
        appendObject(num);
        return this;
    }

    /**
     * include long parameter in SQL with a placeholder <b>?</b>
     *
     * @param num long parameter
     * @return a reference of this object
     */
    public SafeSqlBuilder param(long num) {
        appendObject(num);
        return this;
    }

    /**
     * include double parameter in SQL with a placeholder <b>?</b>
     *
     * @param num double parameter
     * @return a reference of this object
     */
    public SafeSqlBuilder param(double num) {
        appendObject(num);
        return this;
    }

    /**
     * include boolean parameter in SQL with a placeholder <b>?</b>
     *
     * @param bool boolean parameter
     * @return a reference of this object
     */
    public SafeSqlBuilder param(boolean bool) {
        appendObject(bool);
        return this;
    }

    /**
     * include generic parameter in SQL with a placeholder <b>?</b>
     *
     * @param obj object parameter
     * @return a reference of this object
     */
    public SafeSqlBuilder param(Object obj) {
        appendObject(obj);
        return this;
    }

    /**
     * include multiple parameters in SQL with placeholders <b>?</b>
     *
     * @param parameters list of parameter to include
     * @return a reference of this object
     */
    public SafeSqlBuilder params(Object... parameters) {
        if (parameters.length == 1) {
            param(parameters[0]);
        } else if (parameters.length != 0) {
            for (int i=0; i<parameters.length; i++) {
                if (i > 0) {
                    append(DEFAULT_SEPARATOR);
                }
                param(parameters[i]);
            }
        }
        return this;
    }

    /**
     * include multiple parameters in SQL with placeholders <b>?</b>
     *
     * @param iterable {@link Iterable} of parameter to include
     * @return a reference of this object
     */
    public SafeSqlBuilder params(Iterable<?> iterable) {
        paramsIterator(DEFAULT_SEPARATOR, iterable.iterator());
        return this;
    }

    /**
     * include multiple parameters in SQL with placeholders <b>?</b>
     *
     * @param stream stream of parameter to include
     * @return a reference of this object
     */
    public SafeSqlBuilder params(Stream<?> stream) {
        paramsIterator(DEFAULT_SEPARATOR, stream.iterator());
        return this;
    }

    private void paramsIterator(String delimiter, Iterator<?> iterator) {
        boolean first = true;
        while (iterator.hasNext()) {
            if (first) {
                first = false;
            } else {
                append(delimiter);
            }
            param(iterator.next());
        }
    }

    private void paramsIterator(SafeSql delimiter, Iterator<?> iterator) {
        boolean first = true;
        while (iterator.hasNext()) {
            if (first) {
                first = false;
            } else {
                append(delimiter);
            }
            param(iterator.next());
        }
    }

    /**
     * append a {@link SafeSql} to SQL
     *
     * @param sql {@link SafeSql} to append to the final SQL
     * @return a reference of this object
     */
    public SafeSqlBuilder append(SafeSql sql) {
        sqlBuilder.append(sql.asSql());
        parameters.addAll(Arrays.asList(sql.getParameters()));
        return this;
    }

    public SafeSqlBuilder append(SafeSqlizable sqlizable) {
        sqlizable.appendTo(this);
        return this;
    }

    public SafeSqlBuilder append(String s) {
        sqlBuilder.append(s);
        return this;
    }

    public SafeSqlBuilder append(char ch) {
        sqlBuilder.append(ch);
        return this;
    }

    public SafeSqlBuilder append(int i) {
        sqlBuilder.append(i);
        return this;
    }

    public SafeSqlBuilder append(long l) {
        sqlBuilder.append(l);
        return this;
    }

    /**
     * write a string literal by escaping
     *
     * @param s this string as literal string in SQL code
     * @return a reference to this object.
     */
    public SafeSqlBuilder literal(String s) {
        sqlBuilder.append(SafeSqlUtils.escapeString(s));
        return this;
    }

    /**
     * Appends a formatted sql string using the specified arguments.
     *
     * @param sql string query with some <code>{}</code> argument place. The
     * argument can have a number inside to force a argument index (start at 1).
     * The escape sequence is <code>{{.*}}</code>.
     * @param args arguments list
     * @return a reference to this object.
     */
    public SafeSqlBuilder format(String sql, Object... args) {
        SafeSqlUtils.formatTo(this, sql, args);
        return this;
    }

    public SafeSqlBuilder joined(String delimiter, Iterable<String> iterable) {
        SafeSqlJoiner joiner = new SafeSqlJoiner(SafeSqlUtils.fromConstant(delimiter));
        iterable.forEach(joiner::add);
        joiner.appendTo(this);
        return this;
    }

    public SafeSqlBuilder joined(String delimiter, String prefix, String suffix, Iterable<String> iterable) {
        SafeSqlJoiner joiner = new SafeSqlJoiner(SafeSqlUtils.fromConstant(delimiter), SafeSqlUtils.fromConstant(prefix), SafeSqlUtils.fromConstant(suffix));
        iterable.forEach(joiner::add);
        joiner.appendTo(this);
        return this;
    }

    public SafeSqlBuilder joined(String delimiter, Stream<String> stream) {
        SafeSqlJoiner joiner = stream.collect(() -> new SafeSqlJoiner(SafeSqlUtils.fromConstant(delimiter)),
                SafeSqlJoiner::add, SafeSqlJoiner::merge);
        joiner.appendTo(this);
        return this;
    }

    public SafeSqlBuilder joined(String delimiter, String prefix, String suffix, Stream<String> stream) {
        SafeSqlJoiner joiner = stream.collect(() -> new SafeSqlJoiner(SafeSqlUtils.fromConstant(delimiter), SafeSqlUtils.fromConstant(prefix), SafeSqlUtils.fromConstant(suffix)),
                SafeSqlJoiner::add, SafeSqlJoiner::merge);
        joiner.appendTo(this);
        return this;
    }

    public SafeSqlBuilder joinedSafeSqls(SafeSql delimiter, Iterable<SafeSql> iterable) {
        return joinedSafeSqls(delimiter, SafeSqlUtils.EMPTY, SafeSqlUtils.EMPTY, iterable);
    }

    public SafeSqlBuilder joinedSafeSqls(SafeSql delimiter, SafeSql prefix, SafeSql suffix, Iterable<SafeSql> iterable) {
        SafeSqlJoiner joiner = new SafeSqlJoiner(delimiter, prefix, suffix);
        iterable.forEach(joiner::add);
        joiner.appendTo(this);
        return this;
    }

    public SafeSqlBuilder joinedSafeSqls(SafeSql delimiter, SafeSql prefix, SafeSql suffix, Stream<SafeSql> stream) {
        SafeSqlJoiner joiner = stream.collect(() -> new SafeSqlJoiner(delimiter, prefix, suffix), SafeSqlJoiner::add, SafeSqlJoiner::merge);
        joiner.appendTo(this);
        return this;
    }

    public SafeSqlBuilder joinedSafeSqls(SafeSql delimiter, Stream<SafeSql> stream) {
        return joinedSafeSqls(delimiter, SafeSqlUtils.EMPTY, SafeSqlUtils.EMPTY, stream);
    }

    public SafeSqlBuilder joinedSafeSqls(String delimiter, Iterable<SafeSql> iterable) {
        return joinedSafeSqls(SafeSqlUtils.fromConstant(delimiter), SafeSqlUtils.EMPTY, SafeSqlUtils.EMPTY, iterable);
    }

    public SafeSqlBuilder joinedSafeSqls(String delimiter, Stream<SafeSql> stream) {
        return joinedSafeSqls(delimiter, "", "", stream);
    }

    public SafeSqlBuilder joinedSafeSqls(String delimiter, String prefix, String suffix, Iterable<SafeSql> iterable) {
        return joinedSafeSqls(SafeSqlUtils.fromConstant(delimiter), SafeSqlUtils.fromConstant(prefix), SafeSqlUtils.fromConstant(suffix), iterable);
    }

    public SafeSqlBuilder joinedSafeSqls(String delimiter, String prefix, String suffix, Stream<SafeSql> stream) {
        return joinedSafeSqls(SafeSqlUtils.fromConstant(delimiter), SafeSqlUtils.fromConstant(prefix), SafeSqlUtils.fromConstant(suffix), stream);
    }

    public SafeSqlBuilder joinedSqlizables(SafeSql delimiter, Iterable<? extends SafeSqlizable> iterable) {
        return joinedSqlizables(delimiter, SafeSqlUtils.EMPTY, SafeSqlUtils.EMPTY, iterable);
    }

    public SafeSqlBuilder joinedSqlizables(SafeSql delimiter, SafeSql prefix, SafeSql suffix, Iterable<? extends SafeSqlizable> iterable) {
        SafeSqlJoiner joiner = new SafeSqlJoiner(delimiter, prefix, suffix);
        iterable.forEach(joiner::add);
        joiner.appendTo(this);
        return this;
    }

    public SafeSqlBuilder joinedSqlizables(SafeSql delimiter, SafeSql prefix, SafeSql suffix, Stream<? extends SafeSqlizable> stream) {
        SafeSqlJoiner joiner = stream.collect(() -> new SafeSqlJoiner(delimiter, prefix, suffix), SafeSqlJoiner::add, SafeSqlJoiner::merge);
        joiner.appendTo(this);
        return this;
    }

    public SafeSqlBuilder joinedSqlizables(SafeSql delimiter, Stream<? extends SafeSqlizable> stream) {
        return joinedSqlizables(delimiter, SafeSqlUtils.EMPTY, SafeSqlUtils.EMPTY, stream);
    }

    public SafeSqlBuilder joinedSqlizables(String delimiter, Iterable<? extends SafeSqlizable> iterable) {
        return joinedSqlizables(SafeSqlUtils.fromConstant(delimiter), SafeSqlUtils.EMPTY, SafeSqlUtils.EMPTY, iterable);
    }

    public SafeSqlBuilder joinedSqlizables(String delimiter, Stream<? extends SafeSqlizable> stream) {
        return joinedSqlizables(SafeSqlUtils.fromConstant(delimiter), SafeSqlUtils.EMPTY, SafeSqlUtils.EMPTY, stream);
    }

    public SafeSqlBuilder joinedSqlizables(String delimiter, String prefix, String suffix, Iterable<? extends SafeSqlizable> iterable) {
        return joinedSqlizables(SafeSqlUtils.fromConstant(delimiter), SafeSqlUtils.fromConstant(prefix), SafeSqlUtils.fromConstant(suffix), iterable);
    }

    public SafeSqlBuilder joinedSqlizables(String delimiter, String prefix, String suffix, Stream<? extends SafeSqlizable> stream) {
        return joinedSqlizables(SafeSqlUtils.fromConstant(delimiter), SafeSqlUtils.fromConstant(prefix), SafeSqlUtils.fromConstant(suffix), stream);
    }

    /**
     * Write a byte array as literal in PostgreSQL
     *
     * @param bytes bytes to write as literal
     * @return a reference to this object.
     */
    public SafeSqlBuilder literal(byte[] bytes) {
        sqlBuilder.append("'\\x");
        for (byte b : bytes) {
            sqlBuilder.append(HEX_CODE[(b >> 4) & 0xF]);
            sqlBuilder.append(HEX_CODE[(b & 0xF)]);
        }
        sqlBuilder.append('\'');
        return this;
    }

    public SafeSqlBuilder identifier(String identifier) {
        sqlBuilder.append(SafeSqlUtils.mayEscapeIdentifier(identifier));
        return this;
    }

    public SafeSqlBuilder identifier(String container, String identifier) {
        if (null == container) {
            return identifier(identifier);
        } else {
            sqlBuilder.append(SafeSqlUtils.mayEscapeIdentifier(container)).append('.').append(SafeSqlUtils.mayEscapeIdentifier(identifier));
            return this;
        }
    }

    protected final String mayEscapeIdentifier(String identifier) {
        return SafeSqlUtils.mayEscapeIdentifier(identifier);
    }

    /**
     * @deprecated Use {@link #literal(String)} instead.
     */
    @Deprecated
    public SafeSqlBuilder appendStringLiteral(String s) {
        return literal(s);
    }

    /**
     * @deprecated Use {@link #format(String, Object...)} instead.
     */
    @Deprecated
    public SafeSqlBuilder appendFormat(String sql, Object... args) {
        return format(sql, args);
    }

    /**
     * @deprecated Use {@link #joinedSafeSqls(String, Iterable)} instead
     */
    @Deprecated
    public SafeSqlBuilder appendJoined(String delimiter, Collection<? extends SafeSqlizable> collection) {
        return joinedSqlizables(delimiter, collection);
    }

    /**
     * @deprecated Use {@link #joinedSafeSqls(String, String, String, Iterable)} instead
     */
    @Deprecated
    public SafeSqlBuilder appendJoined(String delimiter, String prefix, String suffix, Collection<? extends SafeSqlizable> collection) {
        return joinedSqlizables(delimiter, prefix, suffix, collection);
    }

    /**
     * @deprecated Use {@link #joinedSafeSqls(String, Stream)} instead
     */
    @Deprecated
    public SafeSqlBuilder appendJoined(String delimiter, Stream<? extends SafeSqlizable> stream) {
        return joinedSqlizables(delimiter, stream);
    }

    /**
     * @deprecated Use {@link #joinedSafeSqls(String, String, String, Stream)} instead
     */
    @Deprecated
    public SafeSqlBuilder appendJoined(String delimiter, String prefix, String suffix, Stream<? extends SafeSqlizable> stream) {
        return joinedSqlizables(delimiter, prefix, suffix, stream);
    }

    /**
     * @deprecated Use {@link #joinedSafeSqls(SafeSql, Iterable)} instead
     */
    @Deprecated
    public SafeSqlBuilder appendJoined(SafeSql delimiter, Collection<? extends SafeSqlizable> collection) {
        return joinedSqlizables(delimiter, collection);
    }

    /**
     * @deprecated Use {@link #joinedSafeSqls(SafeSql, SafeSql, SafeSql, Iterable)} instead
     */
    @Deprecated
    public SafeSqlBuilder appendJoined(SafeSql delimiter, SafeSql prefix, SafeSql suffix, Collection<? extends SafeSqlizable> collection) {
        return joinedSqlizables(delimiter, prefix, suffix, collection);
    }

    /**
     * @deprecated Use {@link #joinedSafeSqls(SafeSql, Stream)} instead
     */
    @Deprecated
    public SafeSqlBuilder appendJoined(SafeSql delimiter, Stream<? extends SafeSqlizable> stream) {
        return joinedSqlizables(delimiter, stream);
    }

    /**
     * @deprecated Use {@link #joinedSafeSqls(SafeSql, SafeSql, SafeSql, Stream)} instead
     */
    @Deprecated
    public SafeSqlBuilder appendJoined(SafeSql delimiter, SafeSql prefix, SafeSql suffix, Stream<? extends SafeSqlizable> stream) {
        return joinedSqlizables(delimiter, prefix, suffix, stream);
    }

    /**
     * @deprecated Use {@link #literal(byte[])} instead.
     */
    @Deprecated
    public SafeSqlBuilder appendByteLiteral(byte[] bytes) {
        return literal(bytes);
    }

    /**
     * @deprecated Use {@link #identifier(String)} instead.
     */
    @Deprecated
    public SafeSqlBuilder appendIdentifier(String identifier) {
        return identifier(identifier);
    }

    /**
     * @deprecated Use {@link #identifier(String, String)} instead.
     */
    @Deprecated
    public SafeSqlBuilder appendIdentifier(String container, String identifier) {
        return identifier(container, identifier);
    }

    @Deprecated
    public SafeSqlBuilder params(String delimiter, Collection<?> collection) {
        paramsIterator(delimiter, collection.iterator());
        return this;
    }

    @Deprecated
    public SafeSqlBuilder params(String delimiter, String prefix, String suffix, Collection<?> collection) {
        append(prefix);
        paramsIterator(delimiter, collection.iterator());
        append(suffix);
        return this;
    }

    @Deprecated
    public SafeSqlBuilder params(String delimiter, Stream<?> stream) {
        paramsIterator(delimiter, stream.iterator());
        return this;
    }

    @Deprecated
    public SafeSqlBuilder params(String delimiter, String prefix, String suffix, Stream<?> stream) {
        append(prefix);
        paramsIterator(delimiter, stream.iterator());
        append(suffix);
        return this;
    }

    @Deprecated
    public SafeSqlBuilder params(SafeSql delimiter, Collection<?> collection) {
        paramsIterator(delimiter, collection.iterator());
        return this;
    }

    @Deprecated
    public SafeSqlBuilder params(SafeSql delimiter, SafeSql prefix, SafeSql suffix, Collection<?> collection) {
        append(prefix);
        paramsIterator(delimiter, collection.iterator());
        append(suffix);
        return this;
    }

    @Deprecated
    public SafeSqlBuilder params(SafeSql delimiter, Stream<?> stream) {
        paramsIterator(delimiter, stream.iterator());
        return this;
    }

    @Deprecated
    public SafeSqlBuilder params(SafeSql delimiter, SafeSql prefix, SafeSql suffix, Stream<?> stream) {
        append(prefix);
        paramsIterator(delimiter, stream.iterator());
        append(suffix);
        return this;
    }

    @Override
    public SafeSql toSafeSql() {
        return new SafeSqlImpl(sqlBuilder.toString(), parameters.toArray());
    }

    @Override
    public void appendTo(SafeSqlBuilder builder) {
        builder.sqlBuilder.append(sqlBuilder);
        builder.parameters.addAll(parameters);
    }

    public boolean isEmpty() {
        return sqlBuilder.length() == 0 && parameters.isEmpty();
    }

    private void appendObject(Object o) {
        sqlBuilder.append('?');
        parameters.add(o);
    }

    Position getLength() {
        return new Position(sqlBuilder.length(), parameters.size());
    }

    void setLength(Position position) {
        sqlBuilder.setLength(position.sqlPosition);
        int currentSize = parameters.size();
        if (position.paramPosition < currentSize) {
            parameters.subList(position.paramPosition, currentSize).clear();
        }
    }

    void append(SafeSqlBuilder other, Position after) {
        sqlBuilder.append(other.sqlBuilder, after.sqlPosition, other.sqlBuilder.length());
        int afterLength = after.paramPosition;
        parameters.addAll(other.parameters.subList(afterLength, other.parameters.size() - afterLength));
    }

    static Position getLength(SafeSql sql) {
        return new Position(sql.asSql().length(), sql.getParameters().length);
    }

}
