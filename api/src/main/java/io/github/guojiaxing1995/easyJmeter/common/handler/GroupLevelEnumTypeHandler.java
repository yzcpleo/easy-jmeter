package io.github.guojiaxing1995.easyJmeter.common.handler;

import io.github.guojiaxing1995.easyJmeter.common.enumeration.GroupLevelEnum;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 自定义 GroupLevelEnum 类型处理器，处理数据库中的无效枚举值
 *
 * @author Assistant
 */
@MappedTypes(GroupLevelEnum.class)
public class GroupLevelEnumTypeHandler extends BaseTypeHandler<GroupLevelEnum> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, GroupLevelEnum parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.getValue());
    }

    @Override
    public GroupLevelEnum getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        if (rs.wasNull() || value == null) {
            return GroupLevelEnum.UNKNOWN;
        }
        return GroupLevelEnum.fromValue(value);
    }

    @Override
    public GroupLevelEnum getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        if (rs.wasNull() || value == null) {
            return GroupLevelEnum.UNKNOWN;
        }
        return GroupLevelEnum.fromValue(value);
    }

    @Override
    public GroupLevelEnum getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        if (cs.wasNull() || value == null) {
            return GroupLevelEnum.UNKNOWN;
        }
        return GroupLevelEnum.fromValue(value);
    }
}