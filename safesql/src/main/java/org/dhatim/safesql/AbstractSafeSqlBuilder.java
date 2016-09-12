package org.dhatim.safesql;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public abstract class AbstractSafeSqlBuilder<S extends AbstractSafeSqlBuilder<S>> implements SafeSqlizable, SafeSqlAppendable {
    
    static class Position {

        private final int sqlPosition;
        private final int paramPosition;

        private Position(int sqlPosition, int paramPosition) {
            this.sqlPosition = sqlPosition;
            this.paramPosition = paramPosition;
        }

    }
    
    private static final SafeSql DEFAULT_SEPARATOR = SafeSqlUtils.fromConstant(", ");
    private static final char[] HEX_CODE = "0123456789ABCDEF".toCharArray();
    
    protected final S myself;
    
    protected final StringBuilder sqlBuilder;
    protected final List<Object> parameters;
    
    public AbstractSafeSqlBuilder(Class<S> selfType, StringBuilder stringBuilder, List<Object> parameters) {
        this.myself = selfType.cast(this);
        this.sqlBuilder = stringBuilder;
        this.parameters = parameters;
    }
    
    public abstract S copy();
    
    @Override
    public S param(int num) {
        appendObject(num);
        return myself;
    }

    @Override
    public S param(long num) {
        appendObject(num);
        return myself;
    }

    @Override
    public S param(double num) {
        appendObject(num);
        return myself;
    }

    @Override
    public S param(boolean bool) {
        appendObject(bool);
        return myself;
    }

    @Override
    public S param(BigDecimal num) {
        appendObject(num);
        return myself;
    }

    @Override
    public S param(Object obj) {
        appendObject(obj);
        return myself;
    }
    
    @Override
    public S params(Object param1, Object param2) {
        appendObject(param1);
        append(DEFAULT_SEPARATOR);
        appendObject(param2);
        return myself;
    }

    @Override
    public S params(Object param1, Object param2, Object param3) {
        appendObject(param1);
        append(DEFAULT_SEPARATOR);
        appendObject(param2);
        append(DEFAULT_SEPARATOR);
        appendObject(param3);
        return myself;
    }

    @Override
    public S params(Object... parameters) {
        switch (parameters.length) {
            case 0:
                break; // Do nothing
            case 1:
                param(parameters[0]);
                break;
            case 2:
                params(parameters[0], parameters[1]);
                break;
            case 3:
                params(parameters[0], parameters[1], parameters[2]);
            default:
                params(DEFAULT_SEPARATOR, Arrays.stream(parameters));
        }
        return myself;
    }
    
    @Override
    public S params(Collection<?> collection) {
        return params(DEFAULT_SEPARATOR, collection.stream());
    }

    @Override
    public S params(Stream<?> stream) {
        return params(DEFAULT_SEPARATOR, stream);
    }
    
    @Override
    public S append(SafeSql sql) {
        sqlBuilder.append(sql.asSql());
        parameters.addAll(Arrays.asList(sql.getParameters()));
        return myself;
    }
    
    @Override
    public S append(SafeSqlizable sqlizable) {
        sqlizable.appendTo(this);
        return myself;
    }

    @Override
    public S append(String s) {
        sqlBuilder.append(s);
        return myself;
    }

    @Override
    public S append(char ch) {
        sqlBuilder.append(ch);
        return myself;
    }

    @Override
    public S append(int i) {
        sqlBuilder.append(i);
        return myself;
    }
    
    /**
     * write a string literal by escaping
     *
     * @param s Append this string as literal string in SQL code
     * @return a reference to this object.
     */
    @Override
    public S appendStringLiteral(String s) {
        sqlBuilder.append(SafeSqlUtils.escapeString(s));
        return myself;
    }
    
    @Override
    public S appendFormatted(String sql, Object... args) {
        SafeSqlUtils.formatTo(this, sql, args);
        return myself;
    }
    
    @Override
    public S appendJoined(String delimiter, Collection<? extends SafeSqlizable> collection) {
        return appendJoined(SafeSqlUtils.fromConstant(delimiter), collection.stream());
    }

    @Override
    public S appendJoined(String delimiter, String prefix, String suffix, Collection<? extends SafeSqlizable> collection) {
        return appendJoined(delimiter, prefix, suffix, collection.stream());
    }

    @Override
    public S appendJoined(String delimiter, Stream<? extends SafeSqlizable> stream) {
        return appendJoined(SafeSqlUtils.fromConstant(delimiter), stream);
    }

    @Override
    public S appendJoined(String delimiter, String prefix, String suffix, Stream<? extends SafeSqlizable> stream) {
        return appendJoined(SafeSqlUtils.fromConstant(delimiter), SafeSqlUtils.fromConstant(prefix), SafeSqlUtils.fromConstant(suffix), stream);
    }

    @Override
    public S appendJoined(SafeSql delimiter, Collection<? extends SafeSqlizable> collection) {
        return appendJoined(delimiter, collection.stream());
    }

    @Override
    public S appendJoined(SafeSql delimiter, SafeSql prefix, SafeSql suffix, Collection<? extends SafeSqlizable> collection) {
        return appendJoined(delimiter, prefix, suffix, collection.stream());
    }

    @Override
    public S appendJoined(SafeSql delimiter, Stream<? extends SafeSqlizable> stream) {
        SafeSqlJoiner joiner = stream.collect(() -> new SafeSqlJoiner(delimiter), SafeSqlJoiner::add, SafeSqlJoiner::merge);
        joiner.appendTo(this);
        return myself;
    }

    @Override
    public S appendJoined(SafeSql delimiter, SafeSql prefix, SafeSql suffix, Stream<? extends SafeSqlizable> stream) {
        SafeSqlJoiner joiner = stream.collect(() -> new SafeSqlJoiner(delimiter, prefix, suffix), SafeSqlJoiner::add, SafeSqlJoiner::merge);
        joiner.appendTo(this);
        return myself;
    }
    
    /**
     * Write a byte array as literal in PostgreSQL
     *
     * @param bytes bytes to write as literal
     * @return a reference to this object.
     */
    @Override
    public S appendBytesLiteral(byte[] bytes) {
        sqlBuilder.append("'\\x");
        for (byte b : bytes) {
            sqlBuilder.append(HEX_CODE[(b >> 4) & 0xF]);
            sqlBuilder.append(HEX_CODE[(b & 0xF)]);
        }
        sqlBuilder.append('\'');
        return myself;
    }

    @Override
    public S appendIdentifier(String identifier) {
        sqlBuilder.append(SafeSqlUtils.mayEscapeIdentifier(identifier));
        return myself;
    }

    @Override
    public S appendIdentifier(String container, String identifier) {
        sqlBuilder.append(SafeSqlUtils.mayEscapeIdentifier(container)).append('.').append(SafeSqlUtils.mayEscapeIdentifier(identifier));
        return myself;
    }
    
    @Override
    public SafeSql toSafeSql() {
        return new SafeSqlImpl(sqlBuilder.toString(), parameters.toArray());
    }

    @Override
    public void appendTo(SafeSqlAppendable builder) {
        if (builder instanceof AbstractSafeSqlBuilder<?>) {
            ((AbstractSafeSqlBuilder<?>) builder).parameters.addAll(parameters);
            ((AbstractSafeSqlBuilder<?>) builder).sqlBuilder.append(sqlBuilder);
        } else {
            builder.append(toSafeSql());
        }
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
        parameters.addAll(Arrays.asList(other.parameters).subList(afterLength, other.parameters.size() - afterLength));
    }

    static Position getLength(SafeSql sql) {
        return new Position(sql.asSql().length(), sql.getParameters().length);
    }

}
