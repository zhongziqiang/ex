package com.kline.interceptor;

import cn.hutool.core.util.ObjectUtil;
import com.kline.page.Page;
import com.kline.page.PageContext;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;


@Intercepts(
        {
                @Signature(
                        type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}
                ),
                @Signature(
                        type = ResultSetHandler.class, method = "handleResultSets", args = {java.sql.Statement.class}
                )
        })
public class PageInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if ((PageContext.getLocalPage().get() == null) || (PageContext.getIsPage().get() == false)) {
            return invocation.proceed();
        }
        /*
         * stamtementHandler   有两个实现BaseStatementHandler 和 RoutingStatementHandler 默认执行BaseStatementHandler的实例方法
         *  查询的时候 会去执行 该方法的prepare方法  perpare方法中 执行到instantiateStatement()时,(PreparedStatementHandler的实现)该方法返回值是 connection.prepareStatement(sql) 就是真正的查询sql
         *  具体的sql也在 该BaseStatementHandler实现类的boundSql.sql 中
         */
        if (invocation.getTarget() instanceof StatementHandler) {
            StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
//		MetaObject metaObject = MetaObject.forObject(statementHandler,
//				 SystemMetaObject.DEFAULT_OBJECT_FACTORY,
//				 SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY
//													);
            MetaObject metaStatementHandler;
            Object object;
            for (metaStatementHandler = SystemMetaObject.forObject(statementHandler); metaStatementHandler.hasGetter("h"); metaStatementHandler = SystemMetaObject.forObject(object)) {
                object = metaStatementHandler.getValue("h");
            }
            while (metaStatementHandler.hasGetter("target")) {
                object = metaStatementHandler.getValue("target");
                metaStatementHandler = SystemMetaObject.forObject(object);
            }
            MappedStatement mappedStatment = (MappedStatement) metaStatementHandler.getValue("delegate.mappedStatement");
            SqlCommandType sqlCommandType = mappedStatment.getSqlCommandType();
            if (sqlCommandType != SqlCommandType.SELECT) {
                return invocation.proceed();
            }
            //System.out.println(mappedStatment.getSqlCommandType());
            //mappedStatment.getId();//获取 sql 方法名
            String sql = statementHandler.getBoundSql().getSql();
            if (PageContext.getIsPage().get() == true &&
                    (PageContext.getLocalPage().get().getPageSize() == 0 &&
                            PageContext.getLocalPage().get().getPageNum() == 0)) {
                PageContext.getLocalPage().set(new Page(0, 10));
            }
            Page page = PageContext.getLocalPage().get();
            StringBuilder newSql = new StringBuilder(100);
            if (ObjectUtil.isEmpty(Integer.valueOf(page.getPageSize()))) {
                newSql.append(sql);
            } else {
                newSql.append(sql);
                newSql.append(" limit ");
                newSql.append(page.getStartRow()).append(",");
                newSql.append(page.getPageSize()).append(" ");
            }
            String countSql = "select count(*) from (" + sql + ") a";
            ResultSet rs = null;
            Connection connection = null;
            PreparedStatement countStatement = null;
            int totals = 0;
            try {
                connection = (Connection) invocation.getArgs()[0];
                countStatement = connection.prepareStatement(countSql);
                //获取查询参数
                BoundSql boundSql = (BoundSql) metaStatementHandler.getValue("delegate.boundSql");
//			ParameterHandler parameterHandler = (ParameterHandler)metaObject.getValue("delegate.parameterHandler");
                MappedStatement mappedStatement = (MappedStatement) metaStatementHandler.getValue("delegate.mappedStatement");

                BoundSql countBS = new BoundSql(mappedStatement.getConfiguration(), countSql, boundSql.getParameterMappings(), boundSql.getParameterObject());
                this.setParameters(countStatement, mappedStatement, countBS, boundSql.getParameterObject());

                //将查询参数 set进sql

//			parameterHandler.setParameters(countStatement);
                //具体执行查询sql
                rs = countStatement.executeQuery();
                if (rs.next()) {
                    totals = rs.getInt(1);
                }
                metaStatementHandler.setValue("delegate.boundSql.sql", newSql.toString());
//			metaObject.setValue("delegate.boundSql.sql", newSql.toString());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
			/*rs.close();
			connection.close();
			countStatement.close();*/
            }
            page.setTotal(totals);
            //Object parameter = statementHandler.getBoundSql().getParameterObject();
            //page.setRows((List)result);
            return invocation.proceed();
        }
        if (invocation.getTarget() instanceof ResultSetHandler) {
            Object result = invocation.proceed();
            Page page = (Page) PageContext.getLocalPage().get();
            page.setRows((List) result);
            return result;
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
/*		if ((target instanceof StatementHandler) || (target instanceof ResultSetHandler)) {
			return Plugin.wrap(target, this);
		}
		return target;*/
        return !(target instanceof StatementHandler) && !(target instanceof ResultSetHandler) ? target : Plugin.wrap(target, this);


    }

    @Override
    public void setProperties(Properties arg0) {
    }

    private void setParameters(PreparedStatement ps, MappedStatement mappedStatement, BoundSql boundSql, Object parameterObject) throws SQLException {
        ParameterHandler parameterHandler = new DefaultParameterHandler(mappedStatement, parameterObject, boundSql);
        parameterHandler.setParameters(ps);
    }
}
