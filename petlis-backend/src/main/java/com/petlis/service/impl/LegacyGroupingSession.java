package com.petlis.service.impl;
import java.sql.*;import java.util.*;
/** One owned connection only; never changes global SQL mode or view definitions. */
final class LegacyGroupingSession {
 private LegacyGroupingSession(){}
 @FunctionalInterface interface Work<T>{T run()throws SQLException;}
 static <T>T query(Connection connection,Work<T> work)throws SQLException{
  String original;try(var statement=connection.createStatement();var result=statement.executeQuery("SELECT @@SESSION.sql_mode")){result.next();original=result.getString(1);}
  String legacy=String.join(",",Arrays.stream(original.split(",")).filter(mode->!mode.equalsIgnoreCase("ONLY_FULL_GROUP_BY")).toList());
  if(original.equals(legacy))return work.run();
  try{set(connection,legacy);return work.run();}finally{try{set(connection,original);}catch(SQLException failure){try{connection.abort(Runnable::run);}catch(SQLException abort){failure.addSuppressed(abort);}throw failure;}}
 }
 private static void set(Connection connection,String mode)throws SQLException{try(var statement=connection.prepareStatement("SET SESSION sql_mode=?")){statement.setString(1,mode);statement.execute();}}
}
